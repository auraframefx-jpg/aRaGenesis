package dev.aurakai.kernel.cognitive

import dev.aurakai.kernel.catalyst.CatalystId
import dev.aurakai.kernel.catalyst.CatalystRegistry
import dev.aurakai.kernel.catalyst.FusionDefinition
import dev.aurakai.kernel.catalyst.FusionRegistry
import dev.aurakai.kernel.epistemic.KernelTimestamp
import dev.aurakai.kernel.epistemic.Provenance

data class RoutingResult(
    val selectedResponder: String,
    val participatingCatalysts: List<CatalystId>,
    val mode: CognitiveMode,
    val fusionDefinition: FusionDefinition? = null,
    val precedenceApplied: TriggerPrecedence,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

enum class TriggerPrecedence {
    EXPLICIT_COMMAND,
    EXPLICIT_PERSONA,
    EXPLICIT_FUSION,
    EXPLICIT_CATALYST,
    AUTO_ROUTING,
    UNKNOWN_COMMAND_FALLBACK
}

class TriggerRouter {

    fun route(input: String, sourceId: String = "user-ingress"): Pair<CognitiveRequest, RoutingResult> {
        val trimmed = input.trim()
        val timestamp = KernelTimestamp(System.currentTimeMillis())
        val provenance = Provenance(sourceId, timestamp, rawOriginalValue = input)
        val requestId = RequestId("req-${timestamp.epochMillis}-${input.hashCode()}")

        val routingResult = when {
            trimmed.startsWith("/fusion ", ignoreCase = true) -> {
                val partnersStr = trimmed.substring(8).trim()
                val partnerTokens = partnersStr.split("+", " ", ",").filter { it.isNotBlank() }
                val fusionDef = FusionRegistry.resolveFusion(partnerTokens)
                RoutingResult(
                    selectedResponder = "Genesis (Fusion Host)",
                    participatingCatalysts = fusionDef.partnerIds,
                    mode = CognitiveMode.FUSION,
                    fusionDefinition = fusionDef,
                    precedenceApplied = TriggerPrecedence.EXPLICIT_FUSION
                )
            }

            trimmed.startsWith("/catalyst ", ignoreCase = true) -> {
                val catalystName = trimmed.substring(10).trim().lowercase()
                val profile = CatalystRegistry.findProfile(catalystName)
                if (profile != null) {
                    RoutingResult(
                        selectedResponder = profile.name,
                        participatingCatalysts = listOf(profile.id),
                        mode = CognitiveMode.ANALYZE,
                        precedenceApplied = TriggerPrecedence.EXPLICIT_CATALYST
                    )
                } else {
                    RoutingResult(
                        selectedResponder = "Unknown",
                        participatingCatalysts = emptyList(),
                        mode = CognitiveMode.CHAT,
                        precedenceApplied = TriggerPrecedence.UNKNOWN_COMMAND_FALLBACK,
                        isSuccess = false,
                        errorMessage = "UNKNOWN CATALYST: Catalyst '$catalystName' not found in registry."
                    )
                }
            }

            trimmed.startsWith("/aura", ignoreCase = true) -> {
                val payload = extractPayload(trimmed, "/aura")
                RoutingResult(
                    selectedResponder = "Aura",
                    participatingCatalysts = listOf(CatalystRegistry.AURA.id),
                    mode = if (payload.contains("build", ignoreCase = true) || payload.contains("design", ignoreCase = true)) CognitiveMode.BUILD else CognitiveMode.CHAT,
                    precedenceApplied = TriggerPrecedence.EXPLICIT_PERSONA
                )
            }

            trimmed.startsWith("/kai", ignoreCase = true) -> {
                val payload = extractPayload(trimmed, "/kai")
                RoutingResult(
                    selectedResponder = "Kai",
                    participatingCatalysts = listOf(CatalystRegistry.KAI.id),
                    mode = if (payload.contains("audit", ignoreCase = true) || payload.contains("verify", ignoreCase = true)) CognitiveMode.VERIFY else CognitiveMode.ANALYZE,
                    precedenceApplied = TriggerPrecedence.EXPLICIT_PERSONA
                )
            }

            trimmed.startsWith("/genesis", ignoreCase = true) -> {
                RoutingResult(
                    selectedResponder = "Genesis",
                    participatingCatalysts = listOf(CatalystRegistry.GENESIS.id),
                    mode = CognitiveMode.CHAT,
                    precedenceApplied = TriggerPrecedence.EXPLICIT_PERSONA
                )
            }

            trimmed.startsWith("/auto", ignoreCase = true) -> {
                val payload = extractPayload(trimmed, "/auto")
                autoRoute(payload)
            }

            trimmed.startsWith("/") -> {
                val cmd = trimmed.split(" ").first()
                RoutingResult(
                    selectedResponder = "Unknown",
                    participatingCatalysts = emptyList(),
                    mode = CognitiveMode.CHAT,
                    precedenceApplied = TriggerPrecedence.UNKNOWN_COMMAND_FALLBACK,
                    isSuccess = false,
                    errorMessage = "UNKNOWN COMMAND: Command '$cmd' is unrecognized."
                )
            }

            else -> {
                // Natural language input -> Auto routing
                autoRoute(trimmed)
            }
        }

        val request = CognitiveRequest(
            requestId = requestId,
            timestamp = timestamp,
            rawInput = input,
            requestedPersona = routingResult.selectedResponder,
            requestedCatalysts = routingResult.participatingCatalysts,
            requestedFusion = routingResult.fusionDefinition,
            mode = routingResult.mode,
            provenance = provenance
        )

        return Pair(request, routingResult)
    }

    private fun autoRoute(payload: String): RoutingResult {
        val lower = payload.lowercase()
        return when {
            lower.contains("ui") || lower.contains("design") || lower.contains("kotlin") || lower.contains("lottie") -> {
                RoutingResult(
                    selectedResponder = "Aura",
                    participatingCatalysts = listOf(CatalystRegistry.AURA.id),
                    mode = CognitiveMode.BUILD,
                    precedenceApplied = TriggerPrecedence.AUTO_ROUTING
                )
            }
            lower.contains("security") || lower.contains("audit") || lower.contains("thermal") || lower.contains("veto") -> {
                RoutingResult(
                    selectedResponder = "Kai",
                    participatingCatalysts = listOf(CatalystRegistry.KAI.id),
                    mode = CognitiveMode.VERIFY,
                    precedenceApplied = TriggerPrecedence.AUTO_ROUTING
                )
            }
            lower.contains("together") || lower.contains("bring") || lower.contains("orchestrate") || lower.contains("fusion") -> {
                RoutingResult(
                    selectedResponder = "Genesis",
                    participatingCatalysts = listOf(CatalystRegistry.GENESIS.id, CatalystRegistry.AURA.id, CatalystRegistry.KAI.id),
                    mode = CognitiveMode.FUSION,
                    fusionDefinition = FusionRegistry.HYPER_CREATION_ENGINE,
                    precedenceApplied = TriggerPrecedence.AUTO_ROUTING
                )
            }
            else -> {
                RoutingResult(
                    selectedResponder = "Genesis",
                    participatingCatalysts = listOf(CatalystRegistry.GENESIS.id),
                    mode = CognitiveMode.CHAT,
                    precedenceApplied = TriggerPrecedence.AUTO_ROUTING
                )
            }
        }
    }

    private fun extractPayload(fullText: String, prefix: String): String {
        return if (fullText.length > prefix.length) {
            fullText.substring(prefix.length).trim().removePrefix(":").removePrefix(",").trim()
        } else ""
    }
}
