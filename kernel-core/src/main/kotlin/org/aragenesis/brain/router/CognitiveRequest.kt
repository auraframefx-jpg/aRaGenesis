package org.aragenesis.brain.router

import java.time.Instant

@JvmInline value class RequestId(val value: String)
@JvmInline value class SessionId(val value: String)

enum class TriggerType {
    USER_PROMPT,
    SYSTEM_TELEMETRY,
    AGENT_BROADCAST,
    SCHEDULED_PULSE
}

/**
 * Unified entry point for all cognitive activity in the Brain layer.
 */
data class CognitiveRequest(
    val requestId: RequestId,
    val sessionId: SessionId,
    val triggerType: TriggerType,
    val payload: String,
    val targetAgentId: String? = null,
    val timestamp: Instant = Instant.now(),
    val metadata: Map<String, String> = emptyMap()
)

data class RouteDecision(
    val primaryCatalystId: String,
    val secondaryCatalystIds: List<String> = emptyList(),
    val requiresFusion: Boolean = false,
    val fusionModeName: String? = null
)

interface TriggerRouter {
    fun route(request: CognitiveRequest): RouteDecision
}

class DefaultTriggerRouter : TriggerRouter {

    override fun route(request: CognitiveRequest): RouteDecision {
        val payload = request.payload.trim()

        if (payload.startsWith("/fusion ", ignoreCase = true)) {
            val mode = payload.substring(8).trim()
            return RouteDecision(
                primaryCatalystId = "genesis",
                secondaryCatalystIds = listOf("aura", "kai"),
                requiresFusion = true,
                fusionModeName = if (mode.isNotBlank()) mode else "Hyper-Creation Engine"
            )
        }

        if (request.targetAgentId != null && request.targetAgentId.isNotBlank()) {
            return RouteDecision(primaryCatalystId = request.targetAgentId.lowercase())
        }

        if (payload.startsWith("/aura", ignoreCase = true)) {
            return RouteDecision(primaryCatalystId = "aura")
        }
        if (payload.startsWith("/kai", ignoreCase = true)) {
            return RouteDecision(primaryCatalystId = "kai")
        }
        if (payload.startsWith("/genesis", ignoreCase = true)) {
            return RouteDecision(primaryCatalystId = "genesis")
        }

        return when (request.triggerType) {
            TriggerType.SYSTEM_TELEMETRY -> RouteDecision(primaryCatalystId = "kai")
            TriggerType.SCHEDULED_PULSE -> RouteDecision(primaryCatalystId = "genesis")
            else -> RouteDecision(primaryCatalystId = "aura")
        }
    }
}
