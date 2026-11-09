package dev.aurakai.desktop.bridge

import com.sun.net.httpserver.HttpServer
import dev.aurakai.kernel.bridge.DesktopKernelDispatcher
import dev.aurakai.kernel.bridge.KernelIngressChannel
import dev.aurakai.kernel.engine.CognitiveKernel
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * Host Adapter Bridge for Desktop Orchestration.
 * Encapsulates the JDK HTTP server transport, delegating all epistemic evaluation
 * to the pure [KernelIngressChannel] in kernel-core.
 */
class DesktopHttpServerBridge(
    private val channel: KernelIngressChannel,
    private val port: Int = 8990
) {
    constructor(kernel: CognitiveKernel, port: Int = 8990) : this(DesktopKernelDispatcher(kernel), port)

    private var server: HttpServer? = null

    val isRunning: Boolean
        get() = server != null

    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0).apply {
            createContext("/kernel/evaluate") { exchange ->
                if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                    try {
                        val body = exchange.requestBody.bufferedReader().use { it.readText() }
                        val jsonResponse = channel.dispatchJson(body)
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
    }

    fun stop() {
        server?.stop(0)
        server = null
    }
}
