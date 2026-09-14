package dev.aurakai.kernel.engine

import dev.aurakai.kernel.domain.*

class VerificationGate(
    private val validator: ReceiptValidator,
    private val l4Store: L4Store
) {
    /**
     * The singular state-commit ingress point.
     * Enforces strict epistemic barriers and the central invariant:
     * No claim may gain authority solely through repetition or consensus.
     */
    fun evaluate(candidate: EvaluatedClaim): GateResult {
        // Absolute Veto: Failed Falsification
        if (!candidate.falsificationResult.survived) {
            return GateResult(
                decision = if (candidate.hasIndependentContradiction) {
                    OuroborosDecision.REFRAME
                } else {
                    OuroborosDecision.REQUEST_EVIDENCE
                },
                persistence = PersistenceDisposition.HOLD_AS_L1,
                effectiveCandidate = candidate
            )
        }

        // Derivation of Verification Flags from Validated Receipts
        val verifiedL4 = candidate.runtimeReceipts.any { validator.validateL4(it) }
        val verifiedL5 = candidate.causalReceipts.any { validator.validateL5(it) }

        // Store valid receipts only after proof succeeds
        if (verifiedL4) {
            candidate.runtimeReceipts.forEach { l4Store.save(it) }
        }

        val effective = candidate.copy(
            hasRuntimeVerification = verifiedL4,
            hasCausalVerification = verifiedL5
        )

        return evaluatePromotion(effective)
    }

    private fun evaluatePromotion(candidate: EvaluatedClaim): GateResult {
        // Central Invariant: Level-aware requirement logic
        val canPromote = when (candidate.targetLevel) {
            EpistemicLevel.L1_DOCUMENTED, EpistemicLevel.L2_CORROBORATED -> 
                candidate.hasIndependentEvidence
            
            EpistemicLevel.L3_EXECUTABLE -> 
                candidate.hasIndependentEvidence && candidate.falsificationResult.survived
            
            EpistemicLevel.L4_RUNTIME -> 
                candidate.hasRuntimeVerification
            
            EpistemicLevel.L5_CAUSAL -> 
                candidate.hasRuntimeVerification && candidate.hasCausalVerification
            
            EpistemicLevel.L6_ADAPTIVE, EpistemicLevel.L7_EMERGENT -> 
                candidate.hasRuntimeVerification && candidate.hasCausalVerification && candidate.falsificationResult.survived
        }

        return if (canPromote) {
            GateResult(
                decision = OuroborosDecision.PROMOTE,
                persistence = PersistenceDisposition.COMMIT,
                effectiveCandidate = candidate
            )
        } else {
            GateResult(
                decision = OuroborosDecision.REQUEST_EVIDENCE,
                persistence = PersistenceDisposition.HOLD_AS_L1,
                effectiveCandidate = candidate
            )
        }
    }
}

interface ReceiptValidator {
    fun validateL4(receipt: RuntimeReceipt): Boolean
    fun validateL5(causal: CausalReceipt): Boolean
}

interface L4Store {
    fun save(receipt: RuntimeReceipt)
    fun get(receiptId: String): RuntimeReceipt?
}
