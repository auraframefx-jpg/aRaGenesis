package dev.aurakai.kernel.domain

import kotlinx.serialization.Serializable

@Serializable
data class RuntimeReceipt(
    val receiptId: String,
    val codeHash: String,
    val inputHash: String,
    val environmentHash: String,
    val executionTraceHash: String,
    val attestationToken: String,
    val verifierId: String,
    val sealedAt: Long
)

@Serializable
data class CausalReceipt(
    val receiptId: String,
    val baselineL4ReceiptId: String,
    val interventionL4ReceiptId: String,
    val controlL4ReceiptId: String?,
    val beforeStateHash: String,
    val afterStateHash: String,
    val deltaHash: String,
    val controlDeltaHash: String?,
    val causalClaim: String,
    val sealedAt: Long
)

data class FalsificationResult(
    val survived: Boolean,
    val challengeLog: String
)

data class EvaluatedClaim(
    val claimId: String,
    val claimText: String,
    val targetLevel: EpistemicLevel,
    val hasIndependentEvidence: Boolean,
    val hasIndependentContradiction: Boolean,
    val reliesSolelyOnCanon: Boolean,
    val falsificationResult: FalsificationResult,
    val runtimeReceipts: List<RuntimeReceipt>,
    val causalReceipts: List<CausalReceipt>,
    val hasRuntimeVerification: Boolean = false,
    val hasCausalVerification: Boolean = false
)

enum class OuroborosDecision {
    PROMOTE,
    REFRAME,
    REQUEST_EVIDENCE,
    REJECT
}

data class GateResult(
    val decision: OuroborosDecision,
    val persistence: PersistenceDisposition,
    val effectiveCandidate: EvaluatedClaim
)
