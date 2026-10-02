package dev.aurakai.kernel.dom

import dev.aurakai.kernel.epistemic.AshRecord
import dev.aurakai.kernel.epistemic.Hypothesis
import dev.aurakai.kernel.verification.VerificationReceipt
import dev.aurakai.kernel.verification.VerificationStatus

/**
 * Deterministic State Projection (DOM).
 * DOM represents the state permitted to be exposed to host systems after verification.
 * Invariant: Do NOT create a "verifiedFacts" abstraction in DOM.
 * Verified hypotheses remain hypotheses.
 */
data class DomState(
    val verifiedHypotheses: List<Hypothesis>,
    val receipts: List<VerificationReceipt>,
    val observations: List<AshRecord>
) {
    companion object {
        /**
         * Project state into DOM after verification gate evaluation.
         * Refuses transition if any receipt contains a veto.
         */
        fun project(
            hypotheses: List<Hypothesis>,
            receipts: List<VerificationReceipt>,
            observations: List<AshRecord>
        ): DomState {
            val vetoOccurred = receipts.any { it.vetoExecuted }
            require(!vetoOccurred) {
                "STATE TRANSITION REFUSED: Verification veto executed. Permitted projection halted."
            }

            val verifiedIds = receipts.flatMap { r ->
                r.results.filter { it.value == VerificationStatus.VERIFIED }.keys
            }.toSet()

            val permittedHypotheses = hypotheses.filter { verifiedIds.contains(it.id) }

            return DomState(
                verifiedHypotheses = permittedHypotheses,
                receipts = receipts,
                observations = observations
            )
        }
    }
}
