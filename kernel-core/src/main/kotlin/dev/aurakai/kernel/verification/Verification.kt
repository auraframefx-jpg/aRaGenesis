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
 * Cryptographically bound to the complete deterministic content digest of evaluated hypotheses and observations.
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

            // Veto triggers ONLY when an explicit contradiction is established
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
