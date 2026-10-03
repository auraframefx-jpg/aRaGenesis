package dev.aurakai.kernel.dom

import dev.aurakai.kernel.epistemic.AshRecord
import dev.aurakai.kernel.epistemic.ConstitutionalViolationException
import dev.aurakai.kernel.epistemic.Hypothesis
import dev.aurakai.kernel.verification.VerificationEngine
import dev.aurakai.kernel.verification.VerificationReceipt
import dev.aurakai.kernel.verification.VerificationStatus

data class KnowledgeSnapshot(val serializedState: ByteArray)

/**
 * Deterministic State Projection (DOM).
 * DOM represents the state permitted to be exposed to host systems after verification.
 *
 * Invariant:
 * RAW DATA -> DomState is forbidden.
 * RAW DATA -> VERIFICATION -> RECEIPT -> DOM PROJECTION is the only legal control flow.
 */
sealed interface DomState {

    class Projected private constructor(
        val verifiedHypotheses: List<Hypothesis>,
        val receipts: List<VerificationReceipt>,
        val observations: List<AshRecord>,
        internal val stateDigest: String
    ) : DomState {

        fun copy(
            verifiedHypotheses: List<Hypothesis> = this.verifiedHypotheses,
            receipts: List<VerificationReceipt> = this.receipts,
            observations: List<AshRecord> = this.observations
        ): Projected {
            val expectedDigest = VerificationEngine.computePayloadDigest(verifiedHypotheses, observations)
            if (expectedDigest != this.stateDigest || receipts.any { it.vetoExecuted } || receipts.any { it.payloadDigest != expectedDigest && it.inputDigest != expectedDigest }) {
                throw ConstitutionalViolationException(
                    "CONSTITUTIONAL VIOLATION: Direct .copy() state mutation attempted without passing ProjectionGate verification."
                )
            }
            return Projected(
                verifiedHypotheses = verifiedHypotheses.toList(),
                receipts = receipts.toList(),
                observations = observations.toList(),
                stateDigest = expectedDigest
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Projected) return false
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
            return "DomState.Projected(verifiedHypothesesCount=${verifiedHypotheses.size}, receiptsCount=${receipts.size}, stateDigest='$stateDigest')"
        }

        companion object {
            private fun createProjected(
                verifiedHypotheses: List<Hypothesis>,
                receipts: List<VerificationReceipt>,
                observations: List<AshRecord>,
                stateDigest: String
            ): Projected {
                return Projected(
                    verifiedHypotheses = verifiedHypotheses.toList(),
                    receipts = receipts.toList(),
                    observations = observations.toList(),
                    stateDigest = stateDigest
                )
            }

            internal fun create(
                verifiedHypotheses: List<Hypothesis>,
                receipts: List<VerificationReceipt>,
                observations: List<AshRecord>,
                stateDigest: String
            ): Projected = createProjected(verifiedHypotheses, receipts, observations, stateDigest)
        }
    }

    data object Empty : DomState

    companion object {
        fun project(
            hypotheses: List<Hypothesis>,
            receipts: List<VerificationReceipt>,
            observations: List<AshRecord>
        ): Projected {
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

            val inputDigest = VerificationEngine.computePayloadDigest(hypotheses, observations)

            val invalidReceipt = receipts.find { r ->
                r.payloadDigest != inputDigest && r.inputDigest != inputDigest
            }

            if (invalidReceipt != null) {
                throw ConstitutionalViolationException(
                    "CONSTITUTIONAL VIOLATION: VerificationReceipt digest mismatch. Receipt does not match payload digest of target hypotheses."
                )
            }

            val verifiedIds = receipts.flatMap { r ->
                r.results.filter { it.value == VerificationStatus.VERIFIED }.keys
            }.toSet()

            val permittedHypotheses = hypotheses.filter { verifiedIds.contains(it.id) }
            val projectedDigest = VerificationEngine.computePayloadDigest(permittedHypotheses, observations)

            return Projected.create(
                verifiedHypotheses = permittedHypotheses,
                receipts = receipts,
                observations = observations,
                stateDigest = projectedDigest
            )
        }
    }
}
