package dev.aurakai.kernel.engine

import dev.aurakai.kernel.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerificationGateTest {

    private class TestReceiptValidator(
        private val validL4ReceiptIds: Set<String> = emptySet(),
        private val validL5ReceiptIds: Set<String> = emptySet()
    ) : ReceiptValidator {
        override fun validateL4(receipt: RuntimeReceipt): Boolean =
            validL4ReceiptIds.contains(receipt.receiptId)

        override fun validateL5(causal: CausalReceipt): Boolean =
            validL5ReceiptIds.contains(causal.receiptId)
    }

    private class TestL4Store : L4Store {
        val savedReceipts = mutableMapOf<String, RuntimeReceipt>()
        override fun save(receipt: RuntimeReceipt) {
            savedReceipts[receipt.receiptId] = receipt
        }
        override fun get(receiptId: String): RuntimeReceipt? = savedReceipts[receiptId]
    }

    @Test
    fun `failed falsification must veto promotion and force HOLD_AS_L1`() {
        val l4Store = TestL4Store()
        val validator = TestReceiptValidator(validL4ReceiptIds = setOf("rcpt-l4-001"))
        val gate = VerificationGate(validator, l4Store)

        val candidateWithFailedFalsification = EvaluatedClaim(
            claimId = "claim-001",
            claimText = "MetaInstruction autonomously updates runtime rules from live experience.",
            targetLevel = EpistemicLevel.L4_RUNTIME,
            hasIndependentEvidence = true,
            hasIndependentContradiction = true,
            reliesSolelyOnCanon = false,
            falsificationResult = FalsificationResult(
                survived = false,
                challengeLog = "Direct contradiction detected."
            ),
            runtimeReceipts = listOf(
                RuntimeReceipt("rcpt-l4-001", "hash1", "hash2", "hash3", "hash4", "token", "verifier", 1000L)
            ),
            causalReceipts = emptyList()
        )

        val gateResult = gate.evaluate(candidateWithFailedFalsification)

        assertEquals(OuroborosDecision.REFRAME, gateResult.decision)
        assertEquals(PersistenceDisposition.HOLD_AS_L1, gateResult.persistence)
        assertFalse(gateResult.effectiveCandidate.hasRuntimeVerification)
        assertTrue(l4Store.savedReceipts.isEmpty(), "Unverified receipts must NOT be saved.")
    }
}
