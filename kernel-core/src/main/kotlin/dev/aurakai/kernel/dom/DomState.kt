package dev.aurakai.kernel.dom

import dev.aurakai.kernel.epistemic.AshRecord
import dev.aurakai.kernel.epistemic.ConstitutionalViolationException
import dev.aurakai.kernel.epistemic.Hypothesis
import dev.aurakai.kernel.verification.VerificationEngine
import dev.aurakai.kernel.verification.VerificationReceipt
import dev.aurakai.kernel.verification.VerificationStatus

/**
 * Deterministic State Projection (DOM).
 * DOM represents the state permitted to be exposed to host systems after verification.
 * Invariant: Do NOT create a "verifiedFacts" abstraction in DOM.
 * Verified hypotheses remain hypotheses.
 *
 * Entryway Lock:
 * Primary constructor is internal to prevent direct unverified construction.
 * Bypassing the projection gate via un-validated copy throws ConstitutionalViolationException.
 *
 * @property verifiedHypotheses Snapshot list of verified hypotheses (retaining hypothesis status).
 * @property receipts List of verification receipts validating this state.
 * @property observations Observations supporting this state.
 * @property stateDigest Cryptographic digest representing the exact projected state.
 */
class DomState internal constructor(
    val verifiedHypotheses: List<Hypothesis>,
    val receipts: List<VerificationReceipt>,
    val observations: List<AshRecord>,
    internal val stateDigest: String
) {

    /**
     * Disable un-validated state mutations via copy.
     * Re-validates the proposed state against the ProjectionGate rules.
     */
    fun copy(
        verifiedHypotheses: List<Hypothesis> = this.verifiedHypotheses,
        receipts: List<VerificationReceipt> = this.receipts,
        observations: List<AshRecord> = this.observations
    ): DomState {
        val expectedDigest = VerificationEngine.computePayloadDigest(verifiedHypotheses, observations)
        if (expectedDigest != this.stateDigest || receipts.any { it.vetoExecuted }) {
            throw ConstitutionalViolationException(
                "CONSTITUTIONAL VIOLATION: Direct .copy() state mutation attempted without passing ProjectionGate verification."
            )
        }
        return DomState(
            verifiedHypotheses = verifiedHypotheses.toList(),
            receipts = receipts.toList(),
            observations = observations.toList(),
            stateDigest = expectedDigest
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DomState) return false
        return verifiedHypotheses == other.verifiedHypotheses &&
                receipts == other.receipts &&
                observations == other.observations &&
                stateDigest == other.stateDigest
    }

    override fun hashCode(): Int {
        var result = verifiedHypotheses.hashCode()
        result = 31 * result + receipts.hashCode()
        result = 31 * result + observations.hashCode()
        result = 31 * result + stateDigest.hashCode()
        return result
    }

    override fun toString(): String {
        return "DomState(verifiedHypothesesCount=${verifiedHypotheses.size}, receiptsCount=${receipts.size}, stateDigest='$stateDigest')"
    }

    companion object {
        /**
         * ProjectionGate: The singular permitted entry point for minting DomState.
         * Verifies cryptographic receipt payload digest binding against the exact candidate hypotheses and observations.
         *
         * @param hypotheses Candidate hypotheses to project.
         * @param receipts Verification receipts authorizing projection.
         * @param observations Underlying observations.
         * @return DomState instance.
         * @throws ConstitutionalViolationException if no receipts exist, veto executed, or digest mismatch.
         */
        fun project(
            hypotheses: List<Hypothesis>,
            receipts: List<VerificationReceipt>,
            observations: List<AshRecord>
        ): DomState {
            if (receipts.isEmpty()) {
                throw ConstitutionalViolationException(
                    "CONSTITUTIONAL VIOLATION: Cannot project state into DOM without a valid VerificationReceipt."
                )
            }

            for (r in receipts) {
                if (r.vetoExecuted) {
                    throw ConstitutionalViolationException(
                        "STATE TRANSITION REFUSED: Verification veto executed. Permitted projection halted."
                    )
                }
            }

            val currentDigest = VerificationEngine.computePayloadDigest(hypotheses, observations)

            val matchingReceipt = receipts.find { r ->
                r.payloadDigest == currentDigest || r.inputDigest == currentDigest
            }

            if (matchingReceipt == null) {
                throw ConstitutionalViolationException(
                    "CONSTITUTIONAL VIOLATION: VerificationReceipt digest mismatch. Receipt does not match payload digest of target hypotheses."
                )
            }

            val verifiedIds = receipts.flatMap { r ->
                r.results.filter { it.value == VerificationStatus.VERIFIED }.keys
            }.toSet()

            val permittedHypotheses = hypotheses.filter { verifiedIds.contains(it.id) }

            return DomState(
                verifiedHypotheses = permittedHypotheses.toList(),
                receipts = receipts.toList(),
                observations = observations.toList(),
                stateDigest = currentDigest
            )
        }
    }
}
