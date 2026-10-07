package dev.aurakai.kernel.brain

import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.verification.*
import java.security.MessageDigest
import java.util.Collections

object Cryptography {
    fun computeMerkleRoot(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

enum class ThermalState {
    NOMINAL,
    THROTTLED_MEDITATION,
    SOVEREIGN_STATE_FREEZE
}

data class ThermalTelemetry(
    val substrateTemperatureCelsius: Double,
    val activeParsingThreads: Int,
    val state: ThermalState
) {
    companion object {
        fun evaluate(temperature: Double, activeThreads: Int): ThermalTelemetry {
            val state = when {
                temperature.isNaN() || temperature.isInfinite() || temperature >= 42.0 -> ThermalState.SOVEREIGN_STATE_FREEZE
                temperature >= 39.0 -> ThermalState.THROTTLED_MEDITATION
                else -> ThermalState.NOMINAL
            }
            return ThermalTelemetry(temperature, activeThreads, state)
        }
    }
}

@JvmInline
value class MerkleRootHash(val value: String)

class RawVisualPayload private constructor(
    private val rawBytes: ByteArray,
    val merkleRoot: MerkleRootHash,
    val timestamp: KernelTimestamp
) {
    fun getPayloadSnapshot(): ByteArray = rawBytes.clone()

    companion object {
        fun ingest(bytes: ByteArray, timestamp: KernelTimestamp = KernelTimestamp(System.currentTimeMillis())): RawVisualPayload {
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

typealias EpistemicGrade = EvidenceGrade

data class AdversarialVisualAssessment(
    val vector: VisualIntegrityVector,
    val deceptionCoefficientDelta: Double,
    val targetPayloadRoot: MerkleRootHash,
    val rawTelemetryArtifacts: List<String> = emptyList()
) {
    init {
        require(deceptionCoefficientDelta >= 0.0) { "Deception coefficient delta must be non-negative." }
    }
}

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
        corroborator: CrossModalCorroborator,
        geminiContext: List<String>,
        perplexityStream: List<String>
    ): EvaluationVerdict

    fun recordVerifiedReceipt(receipt: VerificationReceipt)
    fun getVerifiedInsightCount(): Long
    fun proposeSubstrateEvolution(): EvolutionProposal
}

class DefaultMetaInstructPolicyGate(
    private var verifiedInsightCount: Long = 0L
) : MetaInstructPolicyGate {

    override fun evaluateIngestionPipeline(
        payload: RawVisualPayload,
        assessment: AdversarialVisualAssessment,
        corroborator: CrossModalCorroborator,
        geminiContext: List<String>,
        perplexityStream: List<String>
    ): EvaluationVerdict {
        val grade = corroborator.corroborateContext(assessment, geminiContext, perplexityStream)
        return when (grade) {
            EvidenceGrade.CONTRADICTED -> EvaluationVerdict.UNRESOLVED_QUARANTINE
            EvidenceGrade.DIRECT, EvidenceGrade.CORROBORATED -> EvaluationVerdict.SNAPSHOT_CURATION
            EvidenceGrade.PLAUSIBLE -> EvaluationVerdict.HOT_CONTEXT
            EvidenceGrade.WEAK -> EvaluationVerdict.REJECTED_NOISE
            else -> EvaluationVerdict.UNRESOLVED_QUARANTINE
        }
    }

    override fun recordVerifiedReceipt(receipt: VerificationReceipt) {
        if (!receipt.vetoExecuted && receipt.results.values.any { it == VerificationStatus.VERIFIED }) {
            verifiedInsightCount++
        }
    }

    override fun getVerifiedInsightCount(): Long = verifiedInsightCount

    override fun proposeSubstrateEvolution(): EvolutionProposal {
        require(verifiedInsightCount >= 100L) {
            "EVOLUTION REFUSED: Verified insight count ($verifiedInsightCount) is below required threshold (100)."
        }
        return EvolutionProposal(
            id = "evo-prop-${System.currentTimeMillis()}-$verifiedInsightCount",
            verifiedInsightCount = verifiedInsightCount,
            proposalStatement = "PROPOSAL: Substrate evolution triggered at $verifiedInsightCount verified insights.",
            isExecuted = false
        )
    }
}

data class TriggerProposal(
    val proposalId: String,
    val actionType: String,
    val payloadDigest: String,
    val epochNonce: EpochNonce,
    val snapshotBinding: SnapshotBinding? = null,
    val isExecuted: Boolean = false
)

class TriggerRouter {
    fun generateProposal(
        actionType: String,
        payloadDigest: String,
        epochNonce: EpochNonce,
        snapshotBinding: SnapshotBinding? = null
    ): TriggerProposal {
        return TriggerProposal(
            proposalId = "prop-${System.currentTimeMillis()}-$payloadDigest",
            actionType = actionType,
            payloadDigest = payloadDigest,
            epochNonce = epochNonce,
            snapshotBinding = snapshotBinding,
            isExecuted = false
        )
    }
}

/**
 * Stage 3 Character Tensor (META_INSTRUCT).
 * Character tensor scores DO NOT grant execution authority (Character Orthogonality).
 */
data class CharacterTensor(
    val individuality: Double,
    val coherence: Double,
    val contribution: Double,
    val adaptation: Double
) {
    val aggregateCharacterScore: Double
        get() = (individuality + coherence + contribution + adaptation) / 4.0
}

/**
 * Gate 1 — 6W Asymmetry Policy Model.
 * Evaluates authenticated 6W axis tags without magic length heuristics.
 */
data class SixWAsymmetryVector(
    val who: String,
    val what: String,
    val whenTime: String,
    val whereLoc: String,
    val whyReason: String,
    val howMethod: String
) {
    fun validate6WStructure(): Boolean {
        return who.isNotBlank() && what.isNotBlank() && whenTime.isNotBlank() &&
               whereLoc.isNotBlank() && whyReason.isNotBlank() && howMethod.isNotBlank()
    }
}

/**
 * Gate 2 — 4-Model Identity Discrimination.
 * Enforces 4 competing models with distinct semantic roles, digests, and falsification criteria.
 */
data class IdentityHypothesisModel(
    val modelId: String,
    val semanticRole: String, // e.g. "DirectRelationship", "CommonConfounder", "CoincidenceNoise", "AdversarialSpoof"
    val falsificationCriterion: String,
    val supportingEvidenceIds: List<String>,
    val counterEvidenceIds: List<String>,
    val modelDigest: String
)

class FourModelIdentityDiscriminator {
    fun discriminate(models: List<IdentityHypothesisModel>): Boolean {
        if (models.size < 4) return false
        val distinctDigests = models.map { it.modelDigest }.distinct().size
        val distinctRoles = models.map { it.semanticRole }.distinct().size
        val distinctCriteria = models.map { it.falsificationCriterion }.distinct().size

        return distinctDigests >= 4 && distinctRoles >= 4 && distinctCriteria >= 4
    }
}

/**
 * Gate 3 — Premature Explanation Trace Validator.
 * Validates ordered, provenance-linked, observation-bound falsification trace before conclusion.
 */
data class ExplanatoryTrace(
    val originatingAshId: AshId,
    val solveStepDigest: String,
    val counterStepDigest: String,
    val falsificationStepDigest: String,
    val candidateDigest: String
) {
    fun isValidTrace(): Boolean {
        return solveStepDigest.isNotBlank() && counterStepDigest.isNotBlank() &&
               falsificationStepDigest.isNotBlank() && candidateDigest.isNotBlank()
    }
}

/**
 * Stage 3 Execution Admission Gate.
 * Strictly enforces Stage 3 Sealed Admission Conditions:
 * 1. Non-finite & Breach Thermal Checks
 * 2. Unresolved Quarantine Block + Scar Linkage
 * 3. Authoritative Receipt Verification & Payload Digest Match
 * 4. Single-Use Epoch Nonce Replay Defense (Atomic Compare-and-Set)
 * 5. Snapshot Binding Verification
 * 6. Character Tensor Orthogonality (Character Score != Execution Authority)
 */
class ExecutionAdmissionGate {
    fun evaluateAdmission(
        proposal: TriggerProposal,
        receipt: VerificationReceipt,
        thermalTelemetry: ThermalTelemetry,
        isQuarantined: Boolean,
        characterTensor: CharacterTensor? = null
    ): Boolean {
        if (thermalTelemetry.substrateTemperatureCelsius.isNaN() ||
            thermalTelemetry.substrateTemperatureCelsius.isInfinite() ||
            thermalTelemetry.state == ThermalState.SOVEREIGN_STATE_FREEZE
        ) {
            throw ConstitutionalViolationException(
                "EXECUTION ADMISSION REFUSED [${KernelRefusalReason.ThermalWallBreach.code}]: Substrate temperature is invalid or breached Sovereign State Freeze."
            )
        }
        if (isQuarantined) {
            throw ConstitutionalViolationException(
                "EXECUTION ADMISSION REFUSED [${KernelRefusalReason.QuarantineBypass.code}]: Target state is locked in UNRESOLVED_QUARANTINE."
            )
        }
        if (receipt.vetoExecuted || receipt.payloadDigest != proposal.payloadDigest) {
            throw ConstitutionalViolationException(
                "EXECUTION ADMISSION REFUSED [${KernelRefusalReason.InvalidSnapshotBinding.code}]: Receipt mismatch or VETO executed."
            )
        }
        proposal.snapshotBinding?.let { binding ->
            if (binding.snapshotDigest != proposal.payloadDigest || binding.blueprintDigest != receipt.payloadDigest) {
                throw ConstitutionalViolationException(
                    "EXECUTION ADMISSION REFUSED [${KernelRefusalReason.InvalidSnapshotBinding.code}]: Snapshot binding digest mismatch."
                )
            }
        }
        if (proposal.isExecuted) {
            throw ConstitutionalViolationException(
                "EXECUTION ADMISSION REFUSED [${KernelRefusalReason.ReplayAttackRejection.code}]: Proposal has already been executed."
            )
        }

        // Atomic Check-and-Consume Nonce (Prevents Check-Then-Act TOCTOU race conditions under concurrency)
        if (!EpochNonce.consume(proposal.epochNonce.nonceValue)) {
            throw ConstitutionalViolationException(
                "EXECUTION ADMISSION REFUSED [${KernelRefusalReason.ReplayAttackRejection.code}]: Epoch nonce ${proposal.epochNonce.nonceValue} has already been consumed."
            )
        }

        return true
    }
}
