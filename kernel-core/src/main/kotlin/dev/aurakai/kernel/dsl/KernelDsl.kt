package dev.aurakai.kernel.dsl

import dev.aurakai.kernel.domain.*
import dev.aurakai.kernel.engine.CognitiveKernel

class ClaimEvaluationBuilder(
    var claimId: String = "claim-${System.currentTimeMillis()}",
    var claimText: String = "",
    var targetLevel: EpistemicLevel = EpistemicLevel.L1_DOCUMENTED
) {
    private val rawEvidenceList = mutableListOf<RawEvidenceItem>()
    private val runtimeReceiptList = mutableListOf<RuntimeReceipt>()
    private val causalReceiptList = mutableListOf<CausalReceipt>()
    private var falsificationSurvived: Boolean = true
    private var challengeLogText: String = "No explicit contradiction found."

    fun evidence(block: EvidenceBuilder.() -> Unit) {
        val builder = EvidenceBuilder()
        builder.block()
        rawEvidenceList.addAll(builder.items)
    }

    fun receipts(block: ReceiptBuilder.() -> Unit) {
        val builder = ReceiptBuilder()
        builder.block()
        runtimeReceiptList.addAll(builder.runtimeReceipts)
        causalReceiptList.addAll(builder.causalReceipts)
    }

    internal fun buildNodeResult(nodeId: String = "dsl-nexus-node"): NodeResult =
        NodeResult(
            nodeId = nodeId,
            output = claimText,
            evidence = rawEvidenceList,
            runtimeReceipts = runtimeReceiptList,
            causalReceipts = causalReceiptList
        )
}

class EvidenceBuilder {
    internal val items = mutableListOf<RawEvidenceItem>()

    fun item(
        id: String,
        content: String,
        authorId: String,
        sourceType: SourceType,
        relationshipHint: RelationshipHint = RelationshipHint.NONE
    ) {
        items.add(
            RawEvidenceItem(
                id = id,
                rawContent = content,
                authorId = authorId,
                claimedSourceType = sourceType,
                relationshipHint = relationshipHint,
                timestampMs = System.currentTimeMillis()
            )
        )
    }
}

class ReceiptBuilder {
    internal val runtimeReceipts = mutableListOf<RuntimeReceipt>()
    internal val causalReceipts = mutableListOf<CausalReceipt>()

    fun runtime(
        receiptId: String,
        codeHash: String = "code-hash",
        inputHash: String = "input-hash",
        environmentHash: String = "env-hash",
        executionTraceHash: String = "trace-hash",
        attestationToken: String,
        verifierId: String
    ) {
        runtimeReceipts.add(
            RuntimeReceipt(
                receiptId = receiptId,
                codeHash = codeHash,
                inputHash = inputHash,
                environmentHash = environmentHash,
                executionTraceHash = executionTraceHash,
                attestationToken = attestationToken,
                verifierId = verifierId,
                sealedAt = System.currentTimeMillis()
            )
        )
    }

    fun causal(
        receiptId: String,
        baselineL4ReceiptId: String,
        interventionL4ReceiptId: String,
        controlL4ReceiptId: String? = null,
        beforeStateHash: String,
        afterStateHash: String,
        deltaHash: String,
        causalClaim: String
    ) {
        causalReceipts.add(
            CausalReceipt(
                receiptId = receiptId,
                baselineL4ReceiptId = baselineL4ReceiptId,
                interventionL4ReceiptId = interventionL4ReceiptId,
                controlL4ReceiptId = controlL4ReceiptId,
                beforeStateHash = beforeStateHash,
                afterStateHash = afterStateHash,
                deltaHash = deltaHash,
                controlDeltaHash = null,
                causalClaim = causalClaim,
                sealedAt = System.currentTimeMillis()
            )
        )
    }
}

/**
 * Entry point for Alchemical Kernel evaluation DSL.
 */
fun CognitiveKernel.evaluate(
    block: ClaimEvaluationBuilder.() -> Unit
): dev.aurakai.kernel.domain.GateResult {
    val builder = ClaimEvaluationBuilder()
    builder.block()

    val nodeResult = builder.buildNodeResult()

    return this.ingest(
        targetClaimId = builder.claimId,
        targetClaimText = builder.claimText,
        targetLevel = builder.targetLevel,
        untrustedNodeResults = listOf(nodeResult)
    )
}
