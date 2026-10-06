package dev.aurakai.kernel.brain

import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.verification.*
import java.security.MessageDigest
import java.util.Collections

object Cryptography {
    fun computePayloadDigest(bytes: ByteArray): String {
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
            val threads = if (state == ThermalState.THROTTLED_MEDITATION) 1 else activeThreads
            return ThermalTelemetry(temperature, threads, state)
        }
    }
}

@JvmInline
value class PayloadDigest(val value: String)

class RawVisualPayload private constructor(
    private val rawBytes: ByteArray,
    val payloadDigest: PayloadDigest,
    val timestamp: KernelTimestamp
) {
    fun getPayloadSnapshot(): ByteArray = rawBytes.clone()

    companion object {
        fun ingest(bytes: ByteArray, timestamp: KernelTimestamp = KernelTimestamp(System.currentTimeMillis())): RawVisualPayload {
            require(bytes.isNotEmpty()) { "Constitutional violation: Blank observations are inert noise." }
            val computedDigest = Cryptography.computePayloadDigest(bytes)
            return RawVisualPayload(bytes.clone(), PayloadDigest(computedDigest), timestamp)
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
    val targetPayloadDigest: PayloadDigest,
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
    fun recordVerifiedInsight(receipt: VerificationReceipt)
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

    override fun getVerifiedInsightCount(): Long = verifiedInsightCount

    override fun recordVerifiedInsight(receipt: VerificationReceipt) {
        if (!receipt.vetoExecuted && receipt.results.values.contains(VerificationStatus.VERIFIED)) {
            verifiedInsightCount++
        } else {
            throw ConstitutionalViolationException("CONSTITUTIONAL VIOLATION: Cannot record unverified or vetoed receipt as verified insight.")
        }
    }

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

enum class ProposedActionType {
    CHAT,
    DOM_PROJECTION,
    EVOLUTION_PROPOSAL,
    QUARANTINE
}

data class TriggerProposal(
    val proposalId: String,
    val actionType: ProposedActionType,
    val payloadDigest: String
)

data class ExecutionReceipt(
    val executionId: String,
    val proposalId: String,
    val actionType: ProposedActionType,
    val timestamp: KernelTimestamp
)

class TriggerRouter {
    fun generateProposal(actionType: ProposedActionType, payloadDigest: String): TriggerProposal {
        return TriggerProposal(
            proposalId = "prop-${System.currentTimeMillis()}-$payloadDigest",
            actionType = actionType,
            payloadDigest = payloadDigest
        )
    }
}

class ExecutionAdmissionGate {
    fun evaluateAdmission(
        proposal: TriggerProposal,
        receipt: VerificationReceipt,
        thermalTelemetry: ThermalTelemetry,
        isQuarantined: Boolean,
        payload: RawVisualPayload? = null
    ): ExecutionReceipt {
        if (thermalTelemetry.state == ThermalState.SOVEREIGN_STATE_FREEZE) {
            throw ConstitutionalViolationException("EXECUTION ADMISSION REFUSED: Thermal Wall breached (>= 42°C). State frozen.")
        }
        if (isQuarantined) {
            throw ConstitutionalViolationException("EXECUTION ADMISSION REFUSED: Target state is locked in UNRESOLVED_QUARANTINE.")
        }
        if (receipt.vetoExecuted || receipt.payloadDigest != proposal.payloadDigest || receipt.inputDigest != proposal.payloadDigest) {
            throw ConstitutionalViolationException("EXECUTION ADMISSION REFUSED: Receipt mismatch or VETO executed.")
        }
        if (payload != null) {
            val snapshotDigest = Cryptography.computePayloadDigest(payload.getPayloadSnapshot())
            if (snapshotDigest != payload.payloadDigest.value) {
                throw ConstitutionalViolationException("EXECUTION ADMISSION REFUSED: Payload snapshot TOCTOU digest mismatch.")
            }
        }

        return ExecutionReceipt(
            executionId = "exec-${System.currentTimeMillis()}-${proposal.proposalId}",
            proposalId = proposal.proposalId,
            actionType = proposal.actionType,
            timestamp = KernelTimestamp(System.currentTimeMillis())
        )
    }
}
