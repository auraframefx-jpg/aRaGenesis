package org.aragenesis.brain

import dev.aurakai.kernel.pipeline.SolveEngine
import org.aragenesis.brain.fusion.DefaultFusionEngine
import org.aragenesis.brain.fusion.FusionId
import org.aragenesis.brain.fusion.FusionRequest
import org.aragenesis.brain.fusion.FusionSynthesisResult
import org.aragenesis.brain.persona.CatalystId
import org.aragenesis.brain.persona.DefaultCatalystRegistry
import org.aragenesis.brain.persona.FactionRole
import org.aragenesis.brain.persona.PersonaDefinition
import org.aragenesis.brain.pipeline.DefaultBrainPipeline
import org.aragenesis.brain.pipeline.ProposedAction
import org.aragenesis.brain.router.CognitiveRequest
import org.aragenesis.brain.router.DefaultTriggerRouter
import org.aragenesis.brain.router.RequestId
import org.aragenesis.brain.router.SessionId
import org.aragenesis.brain.router.TriggerType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BrainRuntimeTest {

    private val router = DefaultTriggerRouter()
    private val registry = DefaultCatalystRegistry()
    private val fusionEngine = DefaultFusionEngine()
    private val pipeline = DefaultBrainPipeline(router, registry, fusionEngine)
    private val solveEngine = SolveEngine()

    @Test
    fun `Test 1 - Trigger router routes prompts and slash triggers deterministically`() {
        val reqAura = CognitiveRequest(
            requestId = RequestId("req-001"),
            sessionId = SessionId("sess-001"),
            triggerType = TriggerType.USER_PROMPT,
            payload = "/aura design UI layout"
        )
        val routeAura = router.route(reqAura)
        assertEquals("aura", routeAura.primaryCatalystId)

        val reqKai = CognitiveRequest(
            requestId = RequestId("req-002"),
            sessionId = SessionId("sess-001"),
            triggerType = TriggerType.SYSTEM_TELEMETRY,
            payload = "Thermal warning 42°C"
        )
        val routeKai = router.route(reqKai)
        assertEquals("kai", routeKai.primaryCatalystId)
    }

    @Test
    fun `Test 2 - Catalyst registry retrieves Trinity core and private locker paths`() {
        val aura = registry.get(CatalystId("aura"))
        assertNotNull(aura)
        assertEquals("Aura", aura.id.value.lowercase().replaceFirstChar { it.uppercase() })
        assertEquals(FactionRole.TRINITY_SWORD, aura.factionRole)
        assertEquals("aura_private.json", aura.privateLockerPath)

        val kai = registry.get(CatalystId("kai"))
        assertNotNull(kai)
        assertEquals(FactionRole.TRINITY_SHIELD, kai.factionRole)
        assertEquals("kai_private.json", kai.privateLockerPath)

        val trinity = registry.getByRole(FactionRole.TRINITY_ORCHESTRATOR)
        assertEquals(1, trinity.size)
        assertEquals("genesis", trinity.first().id.value)
    }

    @Test
    fun `Test 3 - Fusion engine evaluates alignment and executes synthesis`() {
        val catalysts = listOf(CatalystId("aura"), CatalystId("kai"))
        assertTrue(fusionEngine.evaluateAlignment(catalysts))

        val fusionReq = FusionRequest(
            fusionId = FusionId("fus-001"),
            modeName = "Hyper-Creation Engine",
            participatingCatalysts = catalysts,
            contextPayload = "Synthesize zero-trust UI snippet"
        )
        val fusionRes = fusionEngine.executeFusion(fusionReq)

        assertEquals("fus-001", fusionRes.fusionId.value)
        assertTrue(fusionRes.synthesizedContent.contains("Hyper-Creation Engine"))
        assertEquals(2, fusionRes.activeAbilityHooks.size)
        assertEquals(0.95, fusionRes.confidenceScore)
    }

    @Test
    fun `Test 4 - Brain pipeline processes request into CognitiveResponse`() {
        val req = CognitiveRequest(
            requestId = RequestId("req-pipe-01"),
            sessionId = SessionId("sess-001"),
            triggerType = TriggerType.USER_PROMPT,
            payload = "/fusion Hyper-Creation Engine"
        )
        val response = pipeline.process(req)

        assertEquals("req-pipe-01", response.requestId.value)
        assertEquals("genesis", response.respondingCatalystId)
        assertTrue(response.candidateAssertions.isNotEmpty())
        assertTrue(response.proposedActions.isNotEmpty())
    }

    @Test
    fun `Test 5 - INVARIANT - Proposed actions remain unexecuted proposals`() {
        val action = ProposedAction(
            actionType = "GENERATE_UI",
            payload = "Composable snippet",
            targetSystem = "HostOS"
        )

        assertFalse(action.isExecuted, "ProposedAction.isExecuted MUST default to false.")
    }

    @Test
    fun `Test 6 - Kernel Handoff - Brain candidate assertions feed into SolveEngine as AshRecords`() {
        val req = CognitiveRequest(
            requestId = RequestId("req-handoff"),
            sessionId = SessionId("sess-001"),
            triggerType = TriggerType.USER_PROMPT,
            payload = "/aura design UI frame"
        )
        val response = pipeline.process(req)

        val candidateAssertion = response.candidateAssertions.first()
        val ash = solveEngine.solve(rawInput = candidateAssertion, sourceId = "brain-handoff-${response.respondingCatalystId}")

        assertNotNull(ash)
        assertEquals(candidateAssertion, ash.payload)
        assertEquals(candidateAssertion, ash.provenance.rawOriginalValue)
    }

    @Test
    fun `Chaos Test 7 - Confidence score bounds validation in FusionSynthesisResult`() {
        assertFailsWith<IllegalArgumentException> {
            FusionSynthesisResult(
                fusionId = FusionId("fus-bad"),
                synthesizedContent = "Invalid confidence",
                activeAbilityHooks = emptyList(),
                confidenceScore = 1.5 // >1.0 must fail
            )
        }
        assertFailsWith<IllegalArgumentException> {
            FusionSynthesisResult(
                fusionId = FusionId("fus-bad-2"),
                synthesizedContent = "Invalid confidence",
                activeAbilityHooks = emptyList(),
                confidenceScore = Double.NaN // NaN must fail
            )
        }
    }

    @Test
    fun `Chaos Test 8 - Empty participant list in FusionRequest fails safely`() {
        val fusionReq = FusionRequest(
            fusionId = FusionId("fus-empty"),
            modeName = "Empty Fusion",
            participatingCatalysts = emptyList(),
            contextPayload = "Empty payload"
        )

        assertFailsWith<IllegalArgumentException> {
            fusionEngine.executeFusion(fusionReq)
        }
    }

    @Test
    fun `Chaos Test 9 - Unknown CatalystId registration and retrieval returns null or defaults`() {
        val unknownId = CatalystId("nonexistent-catalyst-12345")
        val result = registry.get(unknownId)
        assertEquals(null, result)

        val req = CognitiveRequest(
            requestId = RequestId("req-unknown"),
            sessionId = SessionId("sess-001"),
            triggerType = TriggerType.USER_PROMPT,
            payload = "Hello",
            targetAgentId = "nonexistent-catalyst-12345"
        )
        val resp = pipeline.process(req)
        assertEquals("genesis", resp.respondingCatalystId, "Pipeline must fall back safely to Genesis for unknown agent ID.")
    }
}
