package dev.aurakai.desktop.bridge

import dev.aurakai.kernel.domain.*
import dev.aurakai.kernel.engine.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopHttpServerBridgeTest {

    private fun findFreePort(): Int {
        ServerSocket(0).use { return it.localPort }
    }

    private class TestReceiptValidator : ReceiptValidator {
        override fun validateL4(receipt: RuntimeReceipt) = true
        override fun validateL5(causal: CausalReceipt) = true
    }

    private class TestL4Store : L4Store {
        override fun save(receipt: RuntimeReceipt) {}
        override fun get(receiptId: String) = null
    }

    private fun createTestKernel(): CognitiveKernel {
        val gate = VerificationGate(TestReceiptValidator(), TestL4Store())
        return CognitiveKernel(
            relationshipClassifier = object : RelationshipClassifier {
                override fun classifyRelationship(targetClaim: String, item: RawEvidenceItem) = EvidenceRelationship.SUPPORTS
            },
            independenceResolver = object : IndependenceResolver {
                override fun resolveGroup(item: RawEvidenceItem) = "origin-${item.authorId}"
            },
            falsifyEngine = object : FalsifyEngine {
                override fun challenge(claim: String, items: List<ResolvedEvidenceItem>) = FalsificationResult(true, "")
            },
            verificationGate = gate
        )
    }

    @Test
    fun `test desktop http bridge evaluates ingress payload successfully`() {
        val testPort = findFreePort()
        val kernel = createTestKernel()
        val bridge = DesktopHttpServerBridge(kernel, testPort)

        bridge.start()
        assertTrue(bridge.isRunning)

        try {
            val url = URI("http://127.0.0.1:$testPort/kernel/evaluate").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val payloadJson = """
                {
                  "claimId": "claim-bridge-test-1",
                  "claimText": "Quantum memory integrity verified by test harness",
                  "targetLevel": "L1_DOCUMENTED",
                  "evidenceItems": [
                    {
                      "id": "ev-1",
                      "content": "Sensor reading verified",
                      "authorId": "sensor-01",
                      "sourceType": "INTERNAL"
                    }
                  ]
                }
            """.trimIndent()

            OutputStreamWriter(conn.outputStream).use { it.write(payloadJson) }

            val responseCode = conn.responseCode
            assertEquals(200, responseCode)

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            assertTrue(responseBody.contains("decision"))
            assertTrue(responseBody.contains("persistence"))
        } finally {
            bridge.stop()
        }
    }
}
