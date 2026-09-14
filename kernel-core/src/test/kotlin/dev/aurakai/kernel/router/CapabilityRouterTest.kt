package dev.aurakai.kernel.router

import kotlin.test.Test
import kotlin.test.assertEquals

class CapabilityRouterTest {

    private val registry = mapOf(
        "aura-creative" to CapabilityRequirement(
            creativeGeneration = true,
            evidenceAnalysis = true
        ),
        "kai-adversary" to CapabilityRequirement(
            adversarialChallenge = true,
            securityAnalysis = true
        )
    )

    private val router = CapabilityRouter(registry)

    @Test
    fun `simple evidence task activates minimal required node set`() {
        val requirement = CapabilityRequirement(evidenceAnalysis = true)
        val plan = router.route("task-001", requirement)

        assertEquals(setOf("aura-creative"), plan.requiredNodes)
        assertEquals(ExecutionMode.SEQUENTIAL_CHALLENGE, plan.executionMode)
    }
}
