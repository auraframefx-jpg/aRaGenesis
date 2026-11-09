package dev.aurakai.kernel.bridge

import dev.aurakai.kernel.domain.*
import dev.aurakai.kernel.dsl.evaluate
import dev.aurakai.kernel.engine.CognitiveKernel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NexusIngressPayload(
    val claimId: String,
    val claimText: String,
    val targetLevel: String = "L1_DOCUMENTED",
    val evidenceItems: List<IngressEvidence> = emptyList(),
    val falsificationSurvived: Boolean = true,
    val challengeLog: String = "Passed falsification.",
    val runtimeReceipts: List<IngressRuntimeReceipt> = emptyList()
)

@Serializable
data class IngressEvidence(
    val id: String,
    val content: String,
    val authorId: String,
    val sourceType: String,
    val relationshipHint: String = "NONE"
)

@Serializable
data class IngressRuntimeReceipt(
    val receiptId: String,
    val attestationToken: String,
    val verifierId: String
)

@Serializable
data class IngressGateEvaluationResponse(
    val decision: String,
    val persistence: String,
    val hasRuntimeVerification: Boolean,
    val hasCausalVerification: Boolean
)

/**
 * Platform-independent Ingress Dispatcher Interface.
 * Implements pure evaluation dispatching without coupling to socket, HTTP, or OS network stacks.
 * Complies with constitutional host-independence boundary (zero com.sun.net.httpserver imports in kernel-core).
 */
interface KernelIngressChannel {
    fun evaluatePayload(payload: NexusIngressPayload): GateResult
    fun dispatchJson(jsonString: String): String
}

class DesktopKernelDispatcher(
    private val kernel: CognitiveKernel
) : KernelIngressChannel {

    private val jsonMapper = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun evaluatePayload(payload: NexusIngressPayload): GateResult {
        return kernel.evaluate {
            claimId = payload.claimId
            claimText = payload.claimText
            targetLevel = parseEpistemicLevel(payload.targetLevel)

            evidence {
                payload.evidenceItems.forEach { item ->
                    item(
                        id = item.id,
                        content = item.content,
                        authorId = item.authorId,
                        sourceType = parseSourceType(item.sourceType),
                        relationshipHint = parseHint(item.relationshipHint)
                    )
                }
            }

            receipts {
                payload.runtimeReceipts.forEach { rcpt ->
                    runtime(
                        receiptId = rcpt.receiptId,
                        attestationToken = rcpt.attestationToken,
                        verifierId = rcpt.verifierId
                    )
                }
            }
        }
    }

    override fun dispatchJson(jsonString: String): String {
        val payload = jsonMapper.decodeFromString<NexusIngressPayload>(jsonString)
        val gateResult = evaluatePayload(payload)
        return serializeGateResult(gateResult)
    }

    private fun parseEpistemicLevel(str: String): EpistemicLevel =
        runCatching { EpistemicLevel.valueOf(str) }.getOrDefault(EpistemicLevel.L1_DOCUMENTED)

    private fun parseSourceType(str: String): SourceType =
        runCatching { SourceType.valueOf(str) }.getOrDefault(SourceType.INTERNAL)

    private fun parseHint(str: String): RelationshipHint =
        runCatching { RelationshipHint.valueOf(str) }.getOrDefault(RelationshipHint.NONE)

    private fun serializeGateResult(result: GateResult): String {
        return """
            {
              "decision": "${result.decision}",
              "persistence": "${result.persistence}",
              "hasRuntimeVerification": ${result.effectiveCandidate.hasRuntimeVerification},
              "hasCausalVerification": ${result.effectiveCandidate.hasCausalVerification}
            }
        """.trimIndent()
    }
}
