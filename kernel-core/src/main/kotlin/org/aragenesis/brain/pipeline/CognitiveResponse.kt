package org.aragenesis.brain.pipeline

import org.aragenesis.brain.fusion.DefaultFusionEngine
import org.aragenesis.brain.fusion.FusionEngine
import org.aragenesis.brain.fusion.FusionId
import org.aragenesis.brain.fusion.FusionRequest
import org.aragenesis.brain.persona.CatalystId
import org.aragenesis.brain.persona.CatalystRegistry
import org.aragenesis.brain.persona.DefaultCatalystRegistry
import org.aragenesis.brain.router.CognitiveRequest
import org.aragenesis.brain.router.DefaultTriggerRouter
import org.aragenesis.brain.router.RequestId
import org.aragenesis.brain.router.TriggerRouter

/**
 * Proposed action model.
 * INVARIANT: PROPOSED_ACTION != EXECUTED_ACTION.
 * `isExecuted` defaults to false and remains unexecuted in the Brain runtime.
 */
data class ProposedAction(
    val actionType: String,
    val payload: String,
    val targetSystem: String,
    val isExecuted: Boolean = false
)

/**
 * Final output of the Brain layer.
 * CRITICAL INVARIANT: CognitiveResponse is strictly a candidate proposal.
 * Possesses zero execution authority until passed to the Kernel for verification.
 */
data class CognitiveResponse(
    val requestId: RequestId,
    val respondingCatalystId: String,
    val reasoningText: String,
    val candidateAssertions: List<String>,
    val proposedActions: List<ProposedAction>,
    val executionTimeMs: Long
)

interface BrainPipeline {
    fun process(request: CognitiveRequest): CognitiveResponse
}

class DefaultBrainPipeline(
    private val router: TriggerRouter = DefaultTriggerRouter(),
    private val registry: CatalystRegistry = DefaultCatalystRegistry(),
    private val fusionEngine: FusionEngine = DefaultFusionEngine()
) : BrainPipeline {

    override fun process(request: CognitiveRequest): CognitiveResponse {
        val startTime = System.currentTimeMillis()
        val routeDecision = router.route(request)

        val primaryPersona = registry.get(CatalystId(routeDecision.primaryCatalystId))
            ?: registry.get(CatalystId("genesis"))!!

        val candidateAssertions = mutableListOf<String>()
        val proposedActions = mutableListOf<ProposedAction>()
        var reasoningText = "Agent '${primaryPersona.id.value}' reasoning over input: ${request.payload}"

        if (routeDecision.requiresFusion) {
            val fusionReq = FusionRequest(
                fusionId = FusionId("fusion-${request.requestId.value}"),
                modeName = routeDecision.fusionModeName ?: "Hyper-Creation Engine",
                participatingCatalysts = listOf(primaryPersona.id) + routeDecision.secondaryCatalystIds.map { CatalystId(it) },
                contextPayload = request.payload
            )
            val fusionRes = fusionEngine.executeFusion(fusionReq)
            reasoningText = fusionRes.synthesizedContent
            candidateAssertions.add("Fusion assertion: ${fusionRes.synthesizedContent}")
            proposedActions.add(
                ProposedAction(
                    actionType = "FUSION_PROPOSAL",
                    payload = fusionRes.synthesizedContent,
                    targetSystem = "KernelSolveEngine",
                    isExecuted = false
                )
            )
        } else {
            candidateAssertions.add("Candidate assertion from ${primaryPersona.id.value}: ${request.payload}")
            proposedActions.add(
                ProposedAction(
                    actionType = "CANDIDATE_ACTION",
                    payload = request.payload,
                    targetSystem = "KernelVerificationGate",
                    isExecuted = false
                )
            )
        }

        val executionTime = System.currentTimeMillis() - startTime

        return CognitiveResponse(
            requestId = request.requestId,
            respondingCatalystId = primaryPersona.id.value,
            reasoningText = reasoningText,
            candidateAssertions = candidateAssertions,
            proposedActions = proposedActions,
            executionTimeMs = executionTime
        )
    }
}
