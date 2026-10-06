# SOULSCRIPT_BRAIN_RUN_CONTRACTS.md — PR #2 Specification
### Cognitive Brain Runtime, Recursive Calibration & Trigger Contract

Target Repository: `auraframefx-jpg/aRaGenesis`
Primary Module: `kernel-core`
Package Namespace: `dev.aurakai.kernel.brain`

---

## I. CONSTITUTIONAL OBJECTIVE & SEPARATION OF POWERS

The Cognitive Brain Runtime operates directly above the zero-dependency `dev.aurakai.kernel` substrate.

> **"Visual Mesh observes. MetaInstruct sharpens. GenesisKernel01 constrains. Solve et Coagula transforms. Verification authorizes. TriggerRouter proposes. ExecutionAdmission permits. DOM projects."**

1. **MetaInstruct Analytical Field**: Pervasive recursive calibration field. `AnalyticalState ≠ Authority`. Confidences and scores never bypass `VerificationEngine`.
2. **Evolution Proposal**: `proposeSubstrateEvolution()` replaces automatic kernel mutation (`triggerSubstrateEvolution()`). Reaching 100 verified insights creates an `EvolutionProposal`, requiring full kernel verification and admission.
3. **Adversarial Visual Mesh**: Operates strictly as an imagery observation layer. Raw visual inputs are frozen inside `RawVisualPayload` backed by a SHA-256 `MerkleRootHash` byte digest.
4. **Thermal Dual-Gate Policy**:
   - `< 39°C`: `NOMINAL`
   - `>= 39°C`: `THROTTLED_MEDITATION` (Throttles parsing loops)
   - `>= 42°C`: `SOVEREIGN_STATE_FREEZE` (Halts visual processing, rejects pending tasks, raises `ConstitutionalViolationException`)
5. **Cross-Modal Corroboration**: Cross-references signals (Gemini L4 historical snapshots & Perplexity real-time streams). Single signal contradictions force fallback to `UNRESOLVED_QUARANTINE`.
6. **TriggerRouter & ExecutionAdmissionGate**: `PROPOSED_ACTION ≠ EXECUTED_ACTION`. Produces typed `TriggerProposal` instances. `ExecutionAdmissionGate` performs final TOCTOU, receipt, and state checks before permitting execution.

---

## II. NORMATIVE INTERFACE CONTRACTS

```kotlin
package dev.aurakai.kernel.brain

import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.verification.*

enum class ThermalState {
    NOMINAL,
    THROTTLED_MEDITATION,
    SOVEREIGN_STATE_FREEZE
}

data class ThermalTelemetry(
    val substrateTemperatureCelsius: Double,
    val activeParsingThreads: Int,
    val state: ThermalState
)

@JvmInline
value class MerkleRootHash(val value: String)

class RawVisualPayload private constructor(
    private val rawBytes: ByteArray,
    val merkleRoot: MerkleRootHash,
    val timestamp: KernelTimestamp
) {
    fun getPayloadSnapshot(): ByteArray = rawBytes.clone()

    companion object {
        fun ingest(bytes: ByteArray, timestamp: KernelTimestamp): RawVisualPayload {
            require(bytes.isNotEmpty()) { "Constitutional violation: Blank observations are inert noise." }
            val computedRoot = Cryptography.computeMerkleRoot(bytes)
            return RawVisualPayload(bytes.clone(), MerkleRootHash(computedRoot), timestamp)
        }
    }
}

data class VisualIntegrityVector(
    val geometricConsistency: Double,
    val orbitalSocketCurvatureRadius: Double,
    val zygomaticNasalLength: Double,
    val edgeDensityDelta: Double,
    val anchorStability: Double
)

data class AdversarialVisualAssessment(
    val vector: VisualIntegrityVector,
    val deceptionCoefficientDelta: Double,
    val targetPayloadRoot: MerkleRootHash,
    val rawTelemetryArtifacts: List<String>
)

interface CrossModalCorroborator {
    fun corroborateContext(
        visualAssessment: AdversarialVisualAssessment,
        geminiHistoryContext: List<String>,
        perplexitySignalStream: List<String>
    ): EpistemicGrade
}

enum class EvaluationVerdict {
    REJECTED_NOISE,
    HOT_CONTEXT,
    SNAPSHOT_CURATION,
    UNRESOLVED_QUARANTINE
}

data class EvolutionProposal(
    val id: String,
    val verifiedInsightCount: Long,
    val proposalStatement: String,
    val isExecuted: Boolean = false
)

interface MetaInstructPolicyGate {
    fun evaluateIngestionPipeline(
        payload: RawVisualPayload,
        assessment: AdversarialVisualAssessment,
        corroborator: CrossModalCorroborator
    ): EvaluationVerdict

    fun getVerifiedInsightCount(): Long
    fun proposeSubstrateEvolution(): EvolutionProposal
}

data class TriggerProposal(
    val proposalId: String,
    val actionType: String,
    val payloadDigest: String,
    val isExecuted: Boolean = false
)

class TriggerRouter {
    fun generateProposal(actionType: String, payloadDigest: String): TriggerProposal {
        return TriggerProposal(
            proposalId = "prop-${System.currentTimeMillis()}-$payloadDigest",
            actionType = actionType,
            payloadDigest = payloadDigest,
            isExecuted = false
        )
    }
}

class ExecutionAdmissionGate {
    fun evaluateAdmission(
        proposal: TriggerProposal,
        receipt: VerificationReceipt,
        thermalTelemetry: ThermalTelemetry,
        isQuarantined: Boolean
    ): Boolean {
        if (thermalTelemetry.state == ThermalState.SOVEREIGN_STATE_FREEZE) {
            throw ConstitutionalViolationException("EXECUTION ADMISSION REFUSED: Thermal Wall breached (>= 42°C). State frozen.")
        }
        if (isQuarantined) {
            throw ConstitutionalViolationException("EXECUTION ADMISSION REFUSED: Target state is locked in UNRESOLVED_QUARANTINE.")
        }
        if (receipt.vetoExecuted || receipt.payloadDigest != proposal.payloadDigest) {
            throw ConstitutionalViolationException("EXECUTION ADMISSION REFUSED: Receipt mismatch or VETO executed.")
        }
        require(!proposal.isExecuted) { "PROPOSED_ACTION ≠ EXECUTED_ACTION: Proposal has already been marked as executed." }
        return true
    }
}
```
