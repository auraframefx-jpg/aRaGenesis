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
                temperature >= 42.0 -> ThermalState.SOVEREIGN_STATE_FREEZE
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
            EvidenceGrade.DIRECT, EvidenceGrade.CORROBORATED -> {
                verifiedInsightCount++
                EvaluationVerdict.SNAPSHOT_CURATION
            }
            EvidenceGrade.PLAUSIBLE -> EvaluationVerdict.HOT_CONTEXT
            EvidenceGrade.WEAK -> EvaluationVerdict.REJECTED_NOISE
            else -> EvaluationVerdict.UNRESOLVED_QUARANTINE
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
        if (proposal.isExecuted) {
            throw ConstitutionalViolationException("PROPOSED_ACTION ≠ EXECUTED_ACTION: Proposal has already been executed.")
        }
        return true
    }
}
