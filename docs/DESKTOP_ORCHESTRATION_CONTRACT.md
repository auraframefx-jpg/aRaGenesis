# DESKTOP ORCHESTRATION CONTRACT & IPC SPECIFICATION

**Document ID:** AURAKAI-DESKTOP-IPC-001  
**Version:** 1.0  
**Date:** November 2026  
**Status:** CANONICAL SPECIFICATION (Phase 3 Foundation)  
**Modules Bound:** `kernel-core` (Kotlin), `language-core` (Kotlin), `desktop-orchestrator` (Python 3.14+)  

---

## 1. Architectural Boundary & Core Principle

In accordance with the clean-slate boundary and the Sacred Provenance Law:
> **The host environment (Python orchestrator) acts strictly as a caller and observation supplier. It DOES NOT possess constitutional authority over the kernel.**

- All side-effects require an explicit `AuthorizedActionToken` minted by `ExecutionAdmissionGate`.
- All inter-process communication (IPC) between the Python desktop process and the Kotlin kernel occurs over **standard input/output (stdio)** or **localhost loopback socket** using strict JSON-schema formatted messages.

---

## 2. IPC Protocol Lifecycle

```
+-------------------------------------------------------------------+
|               PYTHON DESKTOP ORCHESTRATOR (Host Process)           |
|  - Conference Room Swarms (Genesis, Aura, Kai, Claude)           |
|  - Nemotron / Gemini / ADK Model Providers                        |
|  - Local File & UI Event Capture                                  |
+-------------------------------------------------------------------+
             |                                       ^
   (1) Ingress / Proposal                  (4) Admission Decision / Token
             v                                       |
+-------------------------------------------------------------------+
|               KOTLIN RUNTIME (kernel-core & language-core)        |
|  - SolveEngine (Ash Observation Capture)                          |
|  - VerificationEngine (Hypothesis Falsification)                  |
|  - ExecutionAdmissionGate (Atomic CAS Receipt & Nonce Ledger)     |
|  - RuneAstParser (UGIA-001 5-Stage Lattice Projection)           |
+-------------------------------------------------------------------+
```

---

## 3. Schema Definitions (Version 1.0)

### 3.1 Message Envelope
Every message exchanged over the IPC channel follows this envelope:

```json
{
  "protocolVersion": "1.0.0",
  "messageId": "msg-1730800000000-001",
  "messageType": "PROPOSE_ACTION | EVALUATE_CLAIM | PARSE_RUNE | QUERY_STATUS",
  "timestampEpochMillis": 1730800000000,
  "payload": { ... }
}
```

### 3.2 Action Proposal Request (`PROPOSE_ACTION`)
Submitted by Python orchestrator when an agent or catalyst proposes an external side-effect:

```json
{
  "actionType": "SUBSTRATE_EVOLUTION",
  "payloadDigest": "sha256-abc123def456...",
  "epoch": 1,
  "nonceValue": "nonce-hex-value-789",
  "snapshotBinding": {
    "blueprintDigest": "digest-blueprint-1",
    "observationDigests": ["obs-001", "obs-002"],
    "falsificationTraceDigest": "trace-digest-99",
    "snapshotDigest": "sha256-abc123def456..."
  }
}
```

### 3.3 Admission Decision Response (`ADMISSION_DECISION`)
Emitted by `ExecutionAdmissionGate`:

```json
{
  "decision": "AUTHORIZED",
  "authorizedToken": {
    "actionType": "SUBSTRATE_EVOLUTION",
    "payloadDigest": "sha256-abc123def456...",
    "consumptionRecord": {
      "receiptId": "rcpt-1730800000000-xyz",
      "nonceValue": "nonce-hex-value-789",
      "epoch": 1,
      "sequenceNumber": 42
    }
  }
}
```

On refusal:
```json
{
  "decision": "DENIED",
  "refusalReason": "REPLAY_ATTACK_REJECTION | THERMAL_WALL_BREACH | QUARANTINE_BYPASS | STALE_EPOCH | INVALID_SNAPSHOT_BINDING",
  "errorMessage": "EXECUTION ADMISSION REFUSED [REPLAY_ATTACK_REJECTION]: Receipt was already consumed."
}
```

### 3.4 UGIA-001 Rune AST Parse Request (`PARSE_RUNE`)
Submitted by Python orchestrator to tokenize, validate, and project rune glyphs:

```json
{
  "rawInput": "A · a · Я · G · I · aЯ · Ia · aЯG · aЯa"
}
```

### 3.5 UGIA-001 Rune AST Parse Response
```json
{
  "isCompletelyResolved": true,
  "unresolvedSegments": [],
  "nodes": [
    { "type": "LITERAL", "rune": "A", "span": [0, 1], "status": "DOCUMENTED_CLAIM" },
    { "type": "COMPOUND", "rune": "aЯa", "span": [36, 40], "constituents": ["a", "Я", "a"], "status": "DOCUMENTED_CLAIM" }
  ],
  "geometricProjections": [
    {
      "rune": "aЯa",
      "isValid": true,
      "strokes": [
        { "primitive": "VERTICAL_SPINE", "start": [0, -2], "end": [0, 2] },
        { "primitive": "CURVED_ARC", "start": [-1, 0], "end": [1, 0] }
      ]
    }
  ]
}
```

---

## 4. Verification & Testing Invariants

1. **Deterministic Serialization:** JSON serialization must be canonical and whitespace-independent for hashing.
2. **Crash & Disconnect Resilience:** If the Python orchestrator crashes or restarts, nonces and receipts already registered in `AtomicReceiptLedger` cannot be replayed.
3. **Thermal Guard Enforcement:** If the Kotlin kernel breaches `SOVEREIGN_STATE_FREEZE`, all IPC proposals are immediately rejected with `THERMAL_WALL_BREACH`.

---

*Canonicalized into aRaGenesis docs under Phase 3 Desktop Foundation Directives.*
