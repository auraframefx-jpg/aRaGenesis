package dev.aurakai.kernel.verification

import dev.aurakai.kernel.epistemic.*

@JvmInline
value class ReceiptId(val value: String)

enum class VerificationStatus {
    VERIFIED,
    REJECTED,
    VETOED
}

/**
 * Immutable Verification Receipt.
 * Primary constructor is internal so callers outside the verification engine cannot forge receipts.
 */
class VerificationReceipt internal constructor(
    val id: ReceiptId,
    val timestamp: KernelTimestamp,
    val evaluatedHypothesisIds: List<HypothesisId>,
    val results: Map<HypothesisId, VerificationStatus>,
    val vetoExecuted: Boolean,
    val vetoReason: String?,
    val inputDigest: String,
    val payloadDigest: String = inputDigest
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VerificationReceipt) return false
        return id == other.id &&
                timestamp == other.timestamp &&
                evaluatedHypothesisIds == other.evaluatedHypothesisIds &&
                results == other.results &&
                vetoExecuted == other.vetoExecuted &&
                vetoReason == other.vetoReason &&
                inputDigest == other.inputDigest &&
                payloadDigest == other.payloadDigest
    }

    override fun hashCode(): Int {
        var res = id.hashCode()
        res = 31 * res + timestamp.hashCode()
        res = 31 * res + evaluatedHypothesisIds.hashCode()
        res = 31 * res + results.hashCode()
        res = 31 * res + vetoExecuted.hashCode()
        res = 31 * res + (vetoReason?.hashCode() ?: 0)
        res = 31 * res + inputDigest.hashCode()
        res = 31 * res + payloadDigest.hashCode()
        return res
    }

    override fun toString(): String {
        return "VerificationReceipt(id='${id.value}', vetoExecuted=$vetoExecuted, payloadDigest='$payloadDigest')"
    }
}

/**
 * Epistemic Verification Engine (Constitutional Gate).
 * Applies deterministic rules against existing observations and hypotheses.
 */
class VerificationEngine {

    fun verify(
        hypotheses: List<Hypothesis>,
        observations: List<AshRecord>,
        timestamp: KernelTimestamp = KernelTimestamp(System.currentTimeMillis())
    ): VerificationReceipt {
        val ids = hypotheses.map { it.id }
        require(ids.distinct().size == ids.size) {
            "DUPLICATE HYPOTHESIS DETECTED: Each hypothesis supplied to VerificationEngine must have a unique HypothesisId."
        }

        val results = mutableMapOf<HypothesisId, VerificationStatus>()
        var vetoExecuted = false
        val vetoReasons = mutableListOf<String>()

        val obsMap = observations.associateBy { it.id }

        for (h in hypotheses) {
            val hasExplicitContradictoryAssessment = h.counterEvidence.any { ce ->
                obsMap.containsKey(ce.evidence.ashId) && ce.assessment == CounterAssessment.CONTRADICTORY
            }
            val isExplicitlyContradicted = h.evidenceGrade == EvidenceGrade.CONTRADICTED || h.status == HypothesisStatus.CONTRADICTED

            val isContradicted = isExplicitlyContradicted || hasExplicitContradictoryAssessment

            if (isContradicted) {
                results[h.id] = VerificationStatus.VETOED
                vetoExecuted = true
                vetoReasons.add("Hypothesis ${h.id.value} is contradicted by explicit counter-evidence.")
            } else if (h.supportingAshIds.isNotEmpty() && h.supportingAshIds.all { obsMap.containsKey(it) }) {
                results[h.id] = VerificationStatus.VERIFIED
            } else {
                results[h.id] = VerificationStatus.REJECTED
            }
        }

        val payloadDigest = Canonicalizer.computeDigest(hypotheses, observations).value
        val receiptId = ReceiptId("rcpt-${timestamp.epochMillis}-$payloadDigest")

        return VerificationReceipt(
            id = receiptId,
            timestamp = timestamp,
            evaluatedHypothesisIds = hypotheses.map { it.id }.toList(),
            results = results.toMap(),
            vetoExecuted = vetoExecuted,
            vetoReason = if (vetoExecuted) vetoReasons.joinToString("; ") else null,
            inputDigest = payloadDigest,
            payloadDigest = payloadDigest
        )
    }

    companion object {
        fun computePayloadDigest(hypotheses: List<Hypothesis>, observations: List<AshRecord>): String {
            return Canonicalizer.computeDigest(hypotheses, observations).value
        }
    }
}
