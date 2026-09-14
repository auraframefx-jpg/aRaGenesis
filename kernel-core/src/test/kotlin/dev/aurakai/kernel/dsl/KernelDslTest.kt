package dev.aurakai.kernel.dsl

import dev.aurakai.kernel.domain.*
import dev.aurakai.kernel.engine.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KernelDslTest {

    private class TestCognitiveKernel(
        private val gate: VerificationGate
    ) : CognitiveKernel(
        object : RelationshipClassifier {
            override fun classifyRelationship(targetClaim: String, item: RawEvidenceItem) = EvidenceRelationship.SUPPORTS
        },
        object : IndependenceResolver {
            override fun resolveGroup(item: RawEvidenceItem) = "origin-canon-core"
        },
        object : FalsifyEngine {
            override fun challenge(claim: String, items: List<ResolvedEvidenceItem>) = FalsificationResult(true, "")
        },
        gate
    )

    private class TestReceiptValidator : ReceiptValidator {
        override fun validateL4(receipt: RuntimeReceipt) = true
        override fun validateL5(causal: CausalReceipt) = true
    }

    private class TestL4Store : L4Store {
        override fun save(receipt: RuntimeReceipt) {}
        override fun get(receiptId: String) = null
    }

    @Test
    fun `DSL evaluation promotes claim to COMMIT`() {
        val gate = VerificationGate(TestReceiptValidator(), TestL4Store())
        val kernel = TestCognitiveKernel(gate)

        val result = kernel.evaluate {
            claimId = "CLAIM-001"
            claimText = "Test Claim"
            targetLevel = EpistemicLevel.L1_DOCUMENTED

            evidence {
                item("ev-01", "Support", "author", SourceType.CANON)
                item("ev-02", "Support", "author", SourceType.CANON)
            }
        }

        assertEquals(OuroborosDecision.PROMOTE, result.decision)
        assertEquals(PersistenceDisposition.COMMIT, result.persistence)
    }
}
