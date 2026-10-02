package dev.aurakai.kernel.cognitive

import dev.aurakai.kernel.catalyst.CatalystRegistry
import dev.aurakai.kernel.catalyst.FusionRegistry
import dev.aurakai.kernel.ui.EpistemicReadout
import dev.aurakai.kernel.ui.OrbState
import dev.aurakai.kernel.ui.OrbVisualToken
import dev.aurakai.kernel.ui.SynthesisOrb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CognitiveBrainTest {

    private val router = TriggerRouter()
    private val responseProvider = LocalResponseProvider()
    private val reflectionEngine = StandardReflectionEngine()

    @Test
    fun `Test 1 - Slash aura deterministically selects Aura`() {
        val input = "/aura design a Kotlin UI"
        val (req, route) = router.route(input)

        assertEquals("Aura", route.selectedResponder)
        assertEquals(listOf(CatalystRegistry.AURA.id), route.participatingCatalysts)
        assertEquals(TriggerPrecedence.EXPLICIT_PERSONA, route.precedenceApplied)
        assertEquals(CognitiveMode.BUILD, route.mode)
        assertEquals(input, req.rawInput)
    }

    @Test
    fun `Test 2 - Slash kai deterministically selects Kai`() {
        val input = "/kai audit this architecture for security risks"
        val (req, route) = router.route(input)

        assertEquals("Kai", route.selectedResponder)
        assertEquals(listOf(CatalystRegistry.KAI.id), route.participatingCatalysts)
        assertEquals(TriggerPrecedence.EXPLICIT_PERSONA, route.precedenceApplied)
        assertEquals(CognitiveMode.VERIFY, route.mode)
        assertEquals(input, req.rawInput)
    }

    @Test
    fun `Test 3 - Slash genesis deterministically selects Genesis`() {
        val input = "/genesis coordinate system lifecycle"
        val (req, route) = router.route(input)

        assertEquals("Genesis", route.selectedResponder)
        assertEquals(listOf(CatalystRegistry.GENESIS.id), route.participatingCatalysts)
        assertEquals(TriggerPrecedence.EXPLICIT_PERSONA, route.precedenceApplied)
        assertEquals(CognitiveMode.CHAT, route.mode)
    }

    @Test
    fun `Test 4 - Slash fusion aura+kai creates a fusion request`() {
        val input = "/fusion aura+kai"
        val (req, route) = router.route(input)

        assertEquals("Genesis (Fusion Host)", route.selectedResponder)
        assertEquals(listOf(CatalystRegistry.AURA.id, CatalystRegistry.KAI.id), route.participatingCatalysts)
        assertEquals(TriggerPrecedence.EXPLICIT_FUSION, route.precedenceApplied)
        assertEquals(CognitiveMode.FUSION, route.mode)
        assertNotNull(route.fusionDefinition)
        assertEquals(FusionRegistry.HYPER_CREATION_ENGINE.fusionId, route.fusionDefinition?.fusionId)
    }

    @Test
    fun `Test 5 - Slash catalyst andelualx selects Andelualx`() {
        val input = "/catalyst andelualx"
        val (req, route) = router.route(input)

        assertEquals("Andelualx", route.selectedResponder)
        assertEquals(listOf(CatalystRegistry.ANDELUALX.id), route.participatingCatalysts)
        assertEquals(TriggerPrecedence.EXPLICIT_CATALYST, route.precedenceApplied)
    }

    @Test
    fun `Test 6 - Slash auto permits dynamic routing`() {
        val input = "/auto design a new Lottie UI animation"
        val (req, route) = router.route(input)

        assertEquals("Aura", route.selectedResponder)
        assertEquals(listOf(CatalystRegistry.AURA.id), route.participatingCatalysts)
        assertEquals(TriggerPrecedence.AUTO_ROUTING, route.precedenceApplied)
    }

    @Test
    fun `Test 7 - Unknown commands fail safely`() {
        val input = "/unknowncommand do something"
        val (req, route) = router.route(input)

        assertFalse(route.isSuccess)
        assertEquals(TriggerPrecedence.UNKNOWN_COMMAND_FALLBACK, route.precedenceApplied)
        assertNotNull(route.errorMessage)

        val resp = responseProvider.provideResponse(req, route)
        assertTrue(resp.responseText.contains("UNKNOWN"))
    }

    @Test
    fun `Test 8 - Persona profiles cannot mutate state and capabilities imply zero execution authority`() {
        val auraDef = CatalystRegistry.PERSONA_DEFINITIONS["aura"]
        assertNotNull(auraDef)
        assertEquals("Aura", auraDef.identity)

        // Catalyst profiles describe reasoning domains but contain zero authority/permission fields
        val profile = CatalystRegistry.AURA
        val fieldNames = profile::class.java.declaredFields.map { it.name }
        assertFalse(fieldNames.contains("permission"))
        assertFalse(fieldNames.contains("executionPrivilege"))
    }

    @Test
    fun `Test 9 - Proposed actions are never automatically executed`() {
        val input = "/aura build an OS UI widget"
        val (req, route) = router.route(input)
        val resp = responseProvider.provideResponse(req, route)

        assertTrue(resp.proposedActions.isNotEmpty(), "Proposed action should be generated.")
        for (action in resp.proposedActions) {
            assertFalse(action.isExecuted, "Proposed action MUST NOT be executed automatically.")
        }
    }

    @Test
    fun `Test 10 - Original raw input and provenance survive cognitive pipeline`() {
        val rawOriginal = "   /aura  design   a   custom  UI  frame   "
        val (req, route) = router.route(rawOriginal)

        assertEquals(rawOriginal, req.rawInput, "Raw input must be preserved without silent rewriting.")
        assertEquals(rawOriginal, req.provenance.rawOriginalValue)

        val resp = responseProvider.provideResponse(req, route)
        assertEquals(rawOriginal, resp.provenance.rawOriginalValue)
    }

    @Test
    fun `Test 11 - Catalyst friction and disagreements can be represented without forcing consensus`() {
        val input = "/fusion aura+kai"
        val (req, route) = router.route(input)
        val resp = responseProvider.provideResponse(req, route)

        assertTrue(resp.disagreements.isNotEmpty(), "Disagreements should be preserved during fusion.")
        val disagreement = resp.disagreements.first()
        assertEquals(CatalystRegistry.AURA.id, disagreement.catalystA)
        assertEquals(CatalystRegistry.KAI.id, disagreement.catalystB)
    }

    @Test
    fun `Test 12 - Reflection produces new artifact without mutating source`() {
        val input = "/aura design UI"
        val (req, route) = router.route(input)
        val resp = responseProvider.provideResponse(req, route)
        val copyOfResp = resp.copy()

        val artifact = reflectionEngine.reflect(resp)

        assertNotNull(artifact)
        assertEquals(resp.requestId, artifact.responseId)
        assertEquals(copyOfResp, resp, "Reflection must NOT mutate original response.")
    }

    @Test
    fun `Test 13 - Minimal UI Orb and SynthesisOrb primitives`() {
        val auraOrb = OrbVisualToken(glyph = "◉", primaryChannel = "#00FFFF", intensity = 0.9f)
        val kaiOrb = OrbVisualToken(glyph = "◉", primaryChannel = "#FF00FF", intensity = 0.9f)

        val readout = EpistemicReadout(
            observed = listOf("User input: /fusion aura+kai"),
            inferred = listOf("Hypothesis: Aura and Kai collaboration required"),
            proposed = listOf("Generate UI snippet with thermal bounds"),
            verified = listOf("Kernel Invariant Check PASS"),
            contradicted = emptyList()
        )

        val synthOrb = SynthesisOrb(
            participatingOrbs = listOf(auraOrb, kaiOrb),
            state = OrbState.COLLABORATING,
            readout = readout
        )

        assertEquals(2, synthOrb.participatingOrbs.size)
        assertEquals(OrbState.COLLABORATING, synthOrb.state)
        assertEquals(1, synthOrb.readout.observed.size)
        assertEquals(1, synthOrb.readout.verified.size)
    }

    @Test
    fun `Test 14 - First Acceptance Demo End-to-End Scenarios`() {
        // Scenario 1: USER: Aura, design a Kotlin UI.
        val input1 = "/aura design a Kotlin UI"
        val (req1, route1) = router.route(input1)
        val resp1 = responseProvider.provideResponse(req1, route1)

        assertEquals("Aura", route1.selectedResponder)
        assertEquals(CognitiveMode.BUILD, route1.mode)
        assertTrue(resp1.responseText.contains("Aura"))
        assertTrue(resp1.responseText.contains("BUILD"))

        // Scenario 2: USER: Kai, audit this architecture.
        val input2 = "/kai audit this architecture"
        val (req2, route2) = router.route(input2)
        val resp2 = responseProvider.provideResponse(req2, route2)

        assertEquals("Kai", route2.selectedResponder)
        assertEquals(CognitiveMode.VERIFY, route2.mode)
        assertTrue(resp2.responseText.contains("Kai"))
        assertTrue(resp2.responseText.contains("VERIFY"))

        // Scenario 3: USER: Genesis, bring Aura and Kai together.
        val input3 = "Genesis, bring Aura and Kai together in fusion"
        val (req3, route3) = router.route(input3)
        val resp3 = responseProvider.provideResponse(req3, route3)

        assertEquals("Genesis", route3.selectedResponder)
        assertEquals(CognitiveMode.FUSION, route3.mode)
        assertTrue(resp3.responseText.contains("FUSION"))
    }
}
