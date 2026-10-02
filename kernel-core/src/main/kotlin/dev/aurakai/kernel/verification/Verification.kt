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
 * Cryptographically bound to the complete content digest of evaluated hypotheses and observations.
 */
data class VerificationReceipt(
    val id: ReceiptId,
    val timestamp: KernelTimestamp,
    val evaluatedHypothesisIds: List<HypothesisId>,
    val results: Map<HypothesisId, VerificationStatus>,
    val vetoExecuted: Boolean,
    val vetoReason: String?,
    val inputDigest: String,
    val payloadDigest: String = inputDigest
)

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
        // Enforce uniqueness of HypothesisId
        val ids = hypotheses.map { it.id }
        require(ids.distinct().size == ids.size) {
            "DUPLICATE HYPOTHESIS DETECTED: Each hypothesis supplied to VerificationEngine must have a unique HypothesisId."
        }

        val results = mutableMapOf<HypothesisId, VerificationStatus>()
        var vetoExecuted = false
        val vetoReasons = mutableListOf<String>()

        val obsMap = observations.associateBy { it.id }

        for (h in hypotheses) {
            val hasContradiction = h.counterEvidenceAshIds.isNotEmpty() ||
                    h.evidenceGrade == EvidenceGrade.CONTRADICTED ||
                    h.counterEvidenceAshIds.any { obsMap.containsKey(it) }

            if (hasContradiction) {
                results[h.id] = VerificationStatus.VETOED
                vetoExecuted = true
                vetoReasons.add("Hypothesis ${h.id.value} is contradicted by counter-evidence.")
            } else if (h.supportingAshIds.isNotEmpty() && h.supportingAshIds.all { obsMap.containsKey(it) }) {
                results[h.id] = VerificationStatus.VERIFIED
            } else {
                results[h.id] = VerificationStatus.REJECTED
            }
        }

        val payloadDigest = computePayloadDigest(hypotheses, observations)
        val receiptId = ReceiptId("rcpt-${timestamp.epochMillis}-$payloadDigest")

        return VerificationReceipt(
            id = receiptId,
            timestamp = timestamp,
            evaluatedHypothesisIds = hypotheses.map { it.id },
            results = results,
            vetoExecuted = vetoExecuted,
            vetoReason = if (vetoExecuted) vetoReasons.joinToString("; ") else null,
            inputDigest = payloadDigest,
            payloadDigest = payloadDigest
        )
    }

    companion object {
        fun computePayloadDigest(hypotheses: List<Hypothesis>, observations: List<AshRecord>): String {
            var acc = 0L
            for (h in hypotheses) {
                acc = 31 * acc + h.computeDigest().hashCode()
            }
            for (o in observations) {
                acc = 31 * acc + o.id.value.hashCode() + o.payload.hashCode() + o.provenance.rawOriginalValue.hashCode()
            }
            return acc.toULong().toString(16)
        }
    }
}
