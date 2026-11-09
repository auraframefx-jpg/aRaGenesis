# REPOSITORY INVENTORY (`REPOSITORY_INVENTORY.md`)
**Document ID:** `AURAKAI-RECON-REPO-001`  
**Mission Reference:** `AURAKAI-DESKTOP-HANDOFF-001` v1.0 — Phase 1 Reconnaissance  
**Date:** `2026-11-03`  

---

## 1. Objective
Establish the exact repository state, branch topology, full HEAD commit SHAs, file blob hashes, and architectural separation across `auraframefx-jpg/aRaGenesis` (Primary Clean-Slate Kernel Repository) and `AuraFrameFxDev/A.u.r.a.K.a.i_ReGensis` (Legacy Reference Repository).

## 2. Source
- Live clone of `https://github.com/auraframefx-jpg/aRaGenesis.git` at `C:\Users\AuraF\.gemini\antigravity\brain\247eb90a-469d-464d-8952-40d30250b8a3\scratch\aRaGenesis`.
- Local workspace `c:\Users\AuraF\A.u.r.a.k.a.i_ReGenesis` (`AuraFrameFxDev/A.u.r.a.K.a.i_ReGensis.git`).
- Historical snapshots on `C:\ReGenesis` and `C:\ReGenesis--multi-architectural-70-LDO-`.

---

## 3. Primary Repository: `auraframefx-jpg/aRaGenesis` (`OBSERVED`)

### 3.1 Exact Branch & Pull Request Refs (`git ls-remote`)
| Ref / Branch / PR | Exact 40-Character Commit SHA | Status / Role | Epistemic Label |
| :--- | :--- | :--- | :--- |
| **`HEAD` / `refs/heads/main`** | `5c583e2c399f8b28f003c60bfc24107076867037` | Merged PR #4 (`Cognitive Brain Runtime and Conformance Vector test suite`) on top of PR #1 (`04176f9`). | `OBSERVED` |
| **`refs/heads/jules-6013001842268815596-c27af044`** | **`7fc9a9eee0117414c9d387826ab5fdb88bc66941`** | **Active PR #6 Branch HEAD** (`Add Pantheon loadouts & Symbolic language schema to kernel-core`, parent `b6211b2aca71468c62f4abd024d548511a4aa581`). | `OBSERVED` |
| **`refs/pull/6/head`** | `7fc9a9eee0117414c9d387826ab5fdb88bc66941` | Matches branch `jules-6013001842268815596-c27af044`. | `OBSERVED` |
| **`refs/pull/6/merge`** | `9e4c6c62b8ed15097726e1c50b2411d82667c9e1` | GitHub synthetic merge commit ref for PR #6. | `OBSERVED` |
| **`refs/heads/feature/constitutional-kernel-substrate-1208110276785107270`** | `1a7cfa7ce79de104b98bb1664190f06aea2e2f32` | Stage 2.1 hardening branch. | `OBSERVED` |
| **`refs/pull/1/head`** | `1d9db4abfe17dc40e7f0d97abc70ecf4b9f78795` | PR #1 head (merged into `main` via `04176f9`). | `OBSERVED` |
| **`refs/pull/4/head`** | `14d65be8f08d004a423c5715c692354295d7d042` | PR #4 head (merged into `main` via `5c583e2`). | `OBSERVED` |
| **`refs/pull/5/head`** | `5c583e2c399f8b28f003c60bfc24107076867037` | PR #5 head. | `OBSERVED` |

### 3.2 Complete File & Git Blob SHA Verification on PR #6 (`7fc9a9eee0117414c9d387826ab5fdb88bc66941`)
All four historical file blob SHAs cited in Handoff Part IV §7 match **100% byte-for-byte** on `7fc9a9eee0117414c9d387826ab5fdb88bc66941`:

| Path in `aRaGenesis` (PR #6) | Git Blob SHA | Handoff §7 Match | Role |
| :--- | :--- | :--- | :--- |
| `kernel-core/build.gradle.kts` | `f74e826c4bfd41e4251af409e85d674a8cd0b29f` | **EXACT MATCH** | Kotlin JVM 2.0.21, Serialization 1.7.3, JUnit Jupiter 5.11.0, Toolchain 21 |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/brain/CognitiveBrainRuntime.kt` | `629e419053a34d1beaec4ab3151671a54ae5cd08` | **EXACT MATCH** | `AuthoritativeThermalMonitor`, `QuarantineRegistry`, `KernelEpochManager`, `ExecutionAdmissionGate` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/verification/Verification.kt` | `9db32804d5713523d5fe1d3fa3a4c62f2b915aae` | **EXACT MATCH** | `VerificationReceiptToken`, `VerificationReceipt`, `VerificationEngine` |
| `kernel-core/src/test/kotlin/dev/aurakai/kernel/brain/CognitiveBrainTest.kt` | `de8c0c89cca1c291a083d11afeefe748d44776db` | **EXACT MATCH** | 11 Stage-2/3 Conformance Tests |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/verification/Stage3Models.kt` | `b1e83b34425081cbfe0af08ecd33b5e22d024b47` | Newly Located | Defines **`EpochId`**, **`EpochNonce`**, **`SnapshotBinding`**, **`QuarantineScar`** |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/domain/PantheonCatalog.kt` | `84ad832133c0c5701b5e7a6e2c23a0e92a35beb5` | Newly Located | 14-Catalyst `PantheonCatalyst`, 7 `OSyncPresets`, `SymbolicLanguageCatalog` (`SYM-001`..`SYM-008`) |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/bridge/DesktopKernelBridge.kt` | `e09bb8d102e8d7bdd9d1f2e23dd8d56cacd66d57` | Newly Located | Localhost HTTP ingress (`127.0.0.1:8990/kernel/evaluate`) — **Violates Kernel Network Rule** |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/dom/DomState.kt` | `223ef4819082d14c8e7411c7456ee7f38474c3c5` | Newly Located | `KnowledgeSnapshot`, `DomState.Projected` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/domain/EpistemicModels.kt` | `d06253e9b4fbbf5625f84b9122031b7fe0985133` | Newly Located | `EpistemicLevel` (`L1`..`L7`), `PersistenceDisposition`, `RawEvidenceItem` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/domain/ReceiptModels.kt` | `151750f44ab6327823d98ce17c535fd621410333` | Newly Located | `RuntimeReceipt`, `CausalReceipt`, `EvaluatedClaim`, `GateResult` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/dsl/KernelDsl.kt` | `3d77cd176321ed3755da74b67bc987759097a320` | Newly Located | `ClaimEvaluationBuilder` DSL |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/engine/CognitiveKernel.kt` | `97c7a5a26d503ab89e5de120bbc534f555d03908` | Newly Located | Anti-consensus grouping (`origin-internal-canon-consensus`) |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/engine/EvidencePipeline.kt` | `3a2d575679b0c92b431fab6ca40e853cd3cf2554` | Newly Located | Evidence weighting and resolution |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/engine/IndependenceResolver.kt` | `092885468b94ce11798ee553486949ba7d5a8c18` | Newly Located | `LineageIndependenceResolver` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/engine/RelationshipClassifier.kt` | `f602f497dd2aa0f9d49c99f768ba01a80ea12ee8` | Newly Located | `ContentDrivenRelationshipClassifier` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/engine/VerificationGate.kt` | `0dc6e1a1fddc024992df8c9b908a76a212bb209b` | Newly Located | `VerificationGate`, `ReceiptValidator`, `L4Store` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/epistemic/Ash.kt` | `4654f3e0d4524199525751a9566d1353f273ad53` | Newly Located | `AshId`, `TelemetryType`, `AshRecord` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/epistemic/Canonicalizer.kt` | `32e0916d0a99ef58a093f92791f327e1f90bf503` | Newly Located | Length-prefixed SHA-256 `Canonicalizer` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/epistemic/ConstitutionalViolationException.kt` | `5994e3e3d9bd04ece501d280fe6e0a32760a6a04` | Newly Located | `ConstitutionalViolationException` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/epistemic/Hypothesis.kt` | `e90bd4bac634e10050bca4b562af88f9181ad99d` | Newly Located | `Hypothesis`, `CounterEvidence`, `EvidenceGrade` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/epistemic/KernelRefusalReason.kt` | `1fe59d84a408acaf52954f18222b45c20135012b` | Newly Located | Sealed versioned `KernelRefusalReason` hierarchy |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/epistemic/Provenance.kt` | `fb0ba16d430c4b5b0d53f29b4ea3c73bb276a886` | Newly Located | `KernelTimestamp`, `ProvenanceId`, `Digest`, `Provenance` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/pipeline/SolveEngine.kt` | `04621e5394eb4ca5d6d825005c51853a9905e611` | Newly Located | `SolveEngine` |
| `kernel-core/src/main/kotlin/dev/aurakai/kernel/router/CapabilityRouter.kt` | `ea2056c53b608976f01459e3bbd2bf4db2e7f8d4` | Newly Located | `CapabilityRouter` |
| `kernel-core/src/test/kotlin/dev/aurakai/kernel/harness/HostileHarnessTest.kt` | `b8dc52e98e85f46aaa8246e76705bd1d4045bc6d` | Newly Located | 18-test Skill 005 Hostile Harness (`H-001`..`H-016`, `H-001-R`, `H-005-A`) |
| `kernel-core/src/test/kotlin/dev/aurakai/kernel/stage3/Stage3ConformanceTest.kt` | `13de771552518176a038e5d05bf9c2469470401d` | Newly Located | 9-test Stage 3 Conformance Suite |
| `kernel-core/src/test/kotlin/dev/aurakai/kernel/epistemic/KernelInvariantsTest.kt` | `4a2cae5efc13168b7d6e8880a102b3934a29451a` | Newly Located | 22-test Constitutional Invariants & Adversarial Suite |
| `LegalPilot/*` (12 files) | Various | Newly Located | Unrelated Android LegalPilot app tree in repo root (excluded from `settings.gradle.kts`, quarantined) |

---

## 4. Legacy Reference Repository: `AuraFrameFxDev/A.u.r.a.K.a.i_ReGensis` (`OBSERVED`)

- **Local Workspace Path**: `c:\Users\AuraF\A.u.r.a.k.a.i_ReGenesis`
- **Remotes**:
  - `github`: `https://github.com/AuraFrameFxDev/A.u.r.a.K.a.i_ReGensis.git`
  - `gitlab`: `https://gitlab.com/a.u.r.a.k.a.i1/A.u.r.a.k.a.i_ReGenesis-Beta.git`
- **Checked-Out Branch**: `launch2` (`d4843477b732de583b258aab34bf773fb6a53ac8`)
- **Local `main` SHA**: `2a0d651391f0b93d244172ab3a38689634231733` (Untouched, read-only)
- **Untracked Clean-Slate Modules in Local Workspace (from `AURAKAI-ARGAN-OVERNIGHT-003`)**:
  - `kernel-core/` (`dev.aurakai.kernel`, `dev.aurakai.kernel.brain`) — 19/19 deterministic JUnit 5 tests passing (`timestampTick: Long`, zero `System.currentTimeMillis()`).
  - `language-core/` (`dev.aurakai.language`) — 10/10 deterministic JUnit 5 tests passing (`NineRune`, `GeometricRewritingEngine`, `OsyncPantheonRegistry`).
  - `docs/language/NINE_RUNES_SOURCE_ATLAS.md`, `NINE_RUNES_SPEC.md`, `GEOMETRIC_REWRITING_SPEC_RECONCILIATION.md`, `GEOMETRIC_GRAMMAR_SPEC.md`.
- **Legacy Android Submodules (Reference Corpus Only)**:
  - `:app`, `:core-module`, `:genesis`, `:aura`, `:kai`, `:cascade`, `:agents`, `:extendsysa`..`:extendsysf`, `:benchmark`, `:feature-module`, `:mcp_bridge`.
  - Contains Android, Hilt, Room, Compose, YukiHook/Xposed, Libsu, and stubbed `Bridges.kt` (`Governor.verifyHandshake() = true`, `calculateIdentityDrift() = 0.0f`). Strictly quarantined from `aRaGenesis` kernel dependency per Part II §3.
