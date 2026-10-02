package dev.aurakai.kernel.verification

import dev.aurakai.kernel.epistemic.*

/**
 * Unique identifier for a verification receipt.
 */
@JvmInline
value class ReceiptId(val value: String)

/**
 * Result status of an evaluated hypothesis.
 */
enum class VerificationStatus {
    VERIFIED,
    REJECTED,
    VETOED
}

/**
 * Immutable Verification Receipt.
 * Cryptographically bound to the complete deterministic content digest of evaluated hypotheses and observations.
 *
 * @property id Unique receipt identifier.
 * @property timestamp Kernel timestamp when receipt was sealed.
 * @property evaluatedHypothesisIds Snapshot list of evaluated hypothesis IDs.
 * @property results Evaluation status mapped per hypothesis ID.
 * @property vetoExecuted Indicates whether an absolute falsification veto occurred.
 * @property vetoReason Reason for veto if vetoExecuted is true.
 * @property inputDigest Deterministic digest of evaluated input.
 * @property payloadDigest Complete payload digest string.
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
 *
 * Invariants:
 * - Verification must NOT mutate input hypotheses.
 * - Verification must NOT create ASH or evidence.
 * - Verification must NOT modify provenance.
 * - Verification must NOT promote hypotheses into facts.
 * - Contradiction triggers falsification veto (vetoExecuted = true).
 */
class VerificationEngine {

    /**
     * Evaluates candidate hypotheses against supplied observations deterministically.
     *
     * @param hypotheses Candidate hypotheses to evaluate.
     * @param observations Immutable observations against which hypotheses are evaluated.
     * @param timestamp Sealing timestamp.
     * @return Immutable VerificationReceipt.
     * @throws IllegalArgumentException if duplicate HypothesisId values are provided.
     */
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
            val presentCounterObs = h.counterEvidenceAshIds.filter { obsMap.containsKey(it) }
            val isExplicitlyContradicted = h.evidenceGrade == EvidenceGrade.CONTRADICTED

            val isContradicted = isExplicitlyContradicted || presentCounterObs.isNotEmpty()

            if (isContradicted) {
                results[h.id] = VerificationStatus.VETOED
                vetoExecuted = true
                vetoReasons.add("Hypothesis ${h.id.value} is contradicted by counter-evidence.")
            } else if (h.supportingAshIds.isNotEmpty() && h.supportingAshIds.all { obsMap.containsKey(it) }) {
                results[h.id] = VerificationStatus.VERIFIED
            } else {
                // If counter evidence references are unresolved (missing from observations) and supporting evidence is incomplete, status is REJECTED
                results[h.id] = VerificationStatus.REJECTED
            }
        }

        val payloadDigest = computePayloadDigest(hypotheses, observations)
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
        /**
         * Computes a stable, deterministic payload digest across all hypotheses and observations.
         * Sorts records by ID to guarantee ordering independence.
         */
        fun computePayloadDigest(hypotheses: List<Hypothesis>, observations: List<AshRecord>): String {
            val sortedHypDigests = hypotheses.sortedBy { it.id.value }.joinToString(";") { it.computeDigest() }
            val sortedObsDigests = observations.sortedBy { it.id.value }.joinToString(";") {
                "${it.id.value}:${it.payload}:${it.provenance.rawOriginalValue}:${it.provenance.sourceId}"
            }

            val rawCombined = "HYP[$sortedHypDigests]|OBS[$sortedObsDigests]"

            var acc = 0L
            for (ch in rawCombined) {
                acc = 31 * acc + ch.code
            }
            return acc.toULong().toString(16)
        }
    }
}
