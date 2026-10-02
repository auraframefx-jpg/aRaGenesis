package dev.aurakai.kernel.cognitive

import dev.aurakai.kernel.catalyst.CatalystRegistry
import dev.aurakai.kernel.epistemic.KernelTimestamp

interface ResponseProvider {
    fun provideResponse(request: CognitiveRequest, routingResult: RoutingResult): CognitiveResponse
}

/**
 * Local Deterministic Response Provider.
 * Constructs structured CognitiveResponse outputs including reasoning metadata and proposed actions.
 * INVARIANT: Stops strictly at proposals. Never executes host/system actions.
 */
class LocalResponseProvider : ResponseProvider {

    override fun provideResponse(request: CognitiveRequest, routingResult: RoutingResult): CognitiveResponse {
        val timestamp = KernelTimestamp(System.currentTimeMillis())

        if (!routingResult.isSuccess) {
            return CognitiveResponse(
                requestId = request.requestId,
                responder = "SystemGuard",
                participatingCatalysts = emptyList(),
                responseText = routingResult.errorMessage ?: "UNKNOWN COMMAND ERROR",
                reasoningMetadata = mapOf("status" to "ERROR", "precedence" to routingResult.precedenceApplied.name),
                disagreements = emptyList(),
                proposedActions = emptyList(),
                provenance = request.provenance
            )
        }

        val responder = routingResult.selectedResponder
        val responseText = when (request.mode) {
            CognitiveMode.BUILD -> "Route → $responder | Mode → BUILD | Capability → UI/Kotlin | ResponseProvider → Active"
            CognitiveMode.ANALYZE -> "Route → $responder | Mode → ANALYZE | Capability → SECURITY/ARCHITECTURE | ResponseProvider → Active"
            CognitiveMode.VERIFY -> "Route → $responder | Mode → VERIFY | Capability → ETHICAL_VETO/SANDBOX | ResponseProvider → Active"
            CognitiveMode.FUSION -> "Route → $responder | Fusion → ${routingResult.fusionDefinition?.name ?: "Multi-Catalyst"} | Mode → FUSION | ResponseProvider → Active"
            else -> "Route → $responder | Mode → CHAT | Capability → CONVERSATIONAL | ResponseProvider → Active"
        }

        val proposedActions = if (request.mode == CognitiveMode.BUILD || request.mode == CognitiveMode.EXECUTE) {
            listOf(
                ProposedAction(
                    actionId = "prop-${timestamp.epochMillis}-01",
                    targetComponent = "UIFramework",
                    commandDescription = "Generate Kotlin composable layout snippet for $responder",
                    isExecuted = false // Must remain false
                )
            )
        } else {
            emptyList()
        }

        val disagreements = if (request.mode == CognitiveMode.FUSION && routingResult.participatingCatalysts.size >= 2) {
            listOf(
                Disagreement(
                    catalystA = CatalystRegistry.AURA.id,
                    catalystB = CatalystRegistry.KAI.id,
                    topic = "Execution Speed vs Thermal Wall Verification",
                    rationaleA = "Aura proposes rapid inline dynamic layout morphing.",
                    rationaleB = "Kai requires prior static analysis under 42°C Thermal Wall bounds."
                )
            )
        } else {
            emptyList()
        }

        return CognitiveResponse(
            requestId = request.requestId,
            responder = responder,
            participatingCatalysts = routingResult.participatingCatalysts,
            responseText = responseText,
            reasoningMetadata = mapOf(
                "mode" to request.mode.name,
                "precedence" to routingResult.precedenceApplied.name,
                "inputPreserved" to request.rawInput
            ),
            disagreements = disagreements,
            proposedActions = proposedActions,
            provenance = request.provenance
        )
    }
}
