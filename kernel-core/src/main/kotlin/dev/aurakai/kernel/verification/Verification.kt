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
 * Establishes evaluated hypotheses, results, veto status, reason, input state digest, and timestamp.
 */
data class VerificationReceipt(
    val id: ReceiptId,
    val timestamp: KernelTimestamp,
    val evaluatedHypothesisIds: List<HypothesisId>,
    val results: Map<HypothesisId, VerificationStatus>,
    val vetoExecuted: Boolean,
    val vetoReason: String?,
    val inputDigest: String
)

/**
 * Epistemic Verification Engine (Constitutional Gate).
 * Applies deterministic rules against existing observations and hypotheses.
 *
 * Invariants:
 * - Verification must NOT mutate input hypotheses.
 * - Verification must NOT create ASH or evidence.
 * - Verification must NOT modify provenance.
 * - Verification must NOT promote hypotheses into facts.
 * - Contradiction triggers falsification veto (vetoExecuted = true).
 */
class VerificationEngine {

    fun verify(
        hypotheses: List<Hypothesis>,
        observations: List<AshRecord>,
        timestamp: KernelTimestamp = KernelTimestamp(System.currentTimeMillis())
    ): VerificationReceipt {
        val results = mutableMapOf<HypothesisId, VerificationStatus>()
        var vetoExecuted = false
        val vetoReasons = mutableListOf<String>()

        val obsMap = observations.associateBy { it.id }

        for (h in hypotheses) {
            // Check for contradiction / counter-evidence
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

        val receiptId = ReceiptId("rcpt-${timestamp.epochMillis}-${hypotheses.hashCode()}")
        val inputDigest = computeDigest(hypotheses, observations)

        return VerificationReceipt(
            id = receiptId,
            timestamp = timestamp,
            evaluatedHypothesisIds = hypotheses.map { it.id },
            results = results,
            vetoExecuted = vetoExecuted,
            vetoReason = if (vetoExecuted) vetoReasons.joinToString("; ") else null,
            inputDigest = inputDigest
        )
    }

    private fun computeDigest(hypotheses: List<Hypothesis>, observations: List<AshRecord>): String {
        var acc = 0L
        for (h in hypotheses) {
            acc = 31 * acc + h.id.value.hashCode() + h.statement.hashCode()
        }
        for (o in observations) {
            acc = 31 * acc + o.id.value.hashCode() + o.payload.hashCode()
        }
        return acc.toULong().toString(16)
    }
}
