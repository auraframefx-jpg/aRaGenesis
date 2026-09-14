package dev.aurakai.kernel.bridge

import com.sun.net.httpserver.HttpServer
import dev.aurakai.kernel.domain.*
import dev.aurakai.kernel.dsl.evaluate
import dev.aurakai.kernel.engine.CognitiveKernel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.util.concurrent.Executors

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

class DesktopKernelBridge(
    private val kernel: CognitiveKernel,
    private val port: Int = 8990
) {
    private var server: HttpServer? = null
    private val jsonMapper = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0).apply {
            createContext("/kernel/evaluate") { exchange ->
                if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                    try {
                        val body = exchange.requestBody.bufferedReader().use { it.readText() }
                        val payload = jsonMapper.decodeFromString<NexusIngressPayload>(body)

                        val gateResult = kernel.evaluate {
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

                        val jsonResponse = serializeGateResult(gateResult)
                        val bytes = jsonResponse.encodeToByteArray()
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.use { it.write(bytes) }
                    } catch (e: Exception) {
                        val errorBytes = """{"error": "${e.message?.replace("\"", "'")}"}""".encodeToByteArray()
                        exchange.sendResponseHeaders(400, errorBytes.size.toLong())
                        exchange.responseBody.use { it.write(errorBytes) }
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1)
                }
            }
            executor = Executors.newFixedThreadPool(4)
            start()
        }
        println("DesktopKernelBridge active on http://127.0.0.1:$port/kernel/evaluate")
    }

    fun stop() {
        server?.stop(0)
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
