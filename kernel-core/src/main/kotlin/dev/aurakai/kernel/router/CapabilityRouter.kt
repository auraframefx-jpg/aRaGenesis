package dev.aurakai.kernel.router

data class CapabilityRequirement(
    val evidenceAnalysis: Boolean = false,
    val securityAnalysis: Boolean = false,
    val causalAnalysis: Boolean = false,
    val creativeGeneration: Boolean = false,
    val runtimeExecution: Boolean = false,
    val adversarialChallenge: Boolean = false
)

enum class ExecutionMode {
    SEQUENTIAL_CHALLENGE,
    PARALLEL_SAFE
}

data class DispatchPlan(
    val taskId: String,
    val requiredNodes: Set<String>,
    val executionMode: ExecutionMode
)

class CapabilityRouter(
    private val nodeRegistry: Map<String, CapabilityRequirement>
) {
    fun route(taskId: String, requirement: CapabilityRequirement): DispatchPlan {
        val matchingNodes = nodeRegistry.filter { (_, nodeCapabilities) ->
            nodeCapabilities.satisfies(requirement)
        }.keys

        require(matchingNodes.isNotEmpty()) {
            "DISPATCH FAILURE: No registered node satisfies the required capability set for task $taskId."
        }

        val mode = if (requirement.adversarialChallenge || requirement.runtimeExecution) {
            ExecutionMode.PARALLEL_SAFE
        } else {
            ExecutionMode.SEQUENTIAL_CHALLENGE
        }

        return DispatchPlan(
            taskId = taskId,
            requiredNodes = matchingNodes,
            executionMode = mode
        )
    }

    private fun CapabilityRequirement.satisfies(req: CapabilityRequirement): Boolean {
        return (!req.evidenceAnalysis || this.evidenceAnalysis) &&
               (!req.securityAnalysis || this.securityAnalysis) &&
               (!req.causalAnalysis || this.causalAnalysis) &&
               (!req.creativeGeneration || this.creativeGeneration) &&
               (!req.runtimeExecution || this.runtimeExecution) &&
               (!req.adversarialChallenge || this.adversarialChallenge)
    }
}
