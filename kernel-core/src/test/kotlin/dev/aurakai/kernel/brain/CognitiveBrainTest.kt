package dev.aurakai.kernel.brain

import dev.aurakai.kernel.dom.DomState
import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.pipeline.SolveEngine
import dev.aurakai.kernel.verification.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mandatory Conformance Vector Test Suite for Cognitive Brain Runtime (PR #2 / Stage 3).
 * Verifies negative bounds, thermal wall freeze, TOCTOU payload integrity,
 * cross-modal contradiction quarantine, and execution admission guarantees.
 */
class CognitiveBrainTest {

    private val solveEngine = SolveEngine()
    private val verificationEngine = VerificationEngine()
    private val triggerRouter = TriggerRouter()
    private val admissionGate = ExecutionAdmissionGate()

    @BeforeTest
    fun setUp() {
        EpochNonce.clearConsumedForTesting()
    }

    @Test
    fun `test_thermal_wall_breach_forces_state_freeze`() {
        val thermalHigh = ThermalTelemetry.evaluate(42.5, 8)
        assertEquals(ThermalState.SOVEREIGN_STATE_FREEZE, thermalHigh.state)

        val obs = solveEngine.solve("Thermal test obs", "sensor-thermal")
        val h = Hypothesis(id = HypothesisId("hyp-thermal"), statement = "Normal status", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))
        val proposal = triggerRouter.generateProposal("ACTION_EXECUTE", receipt.payloadDigest, EpochNonce.generate())

        assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal,
                receipt = receipt,
                thermalTelemetry = thermalHigh,
                isQuarantined = false
            )
        }
    }

    @Test
    fun `test_visual_payload_toctou_mutation_rejection`() {
        val originalBytes = byteArrayOf(0x10, 0x20, 0x30, 0x40)
        val timestamp = KernelTimestamp(1000L)
        val payload = RawVisualPayload.ingest(originalBytes, timestamp)

        val retrievedSnapshot = payload.getPayloadSnapshot()
        retrievedSnapshot[0] = 0x99.toByte() // Mutate local snapshot

        val freshSnapshot = payload.getPayloadSnapshot()
        assertEquals(0x10.toByte(), freshSnapshot[0], "RawVisualPayload must reject post-capture TOCTOU mutation.")
        assertEquals(Cryptography.computeMerkleRoot(originalBytes), payload.merkleRoot.value)
    }

    @Test
    fun `test_cross_modal_mismatch_forces_unresolved_state`() {
        val vector = VisualIntegrityVector(0.9, 1.2, 2.3, 0.05, 0.95)
        val root = MerkleRootHash("abcd1234root")
        val assessment = AdversarialVisualAssessment(vector, 0.1, root, listOf("artifact1"))

        val mismatchCorroborator = object : CrossModalCorroborator {
            override fun corroborateContext(
                visualAssessment: AdversarialVisualAssessment,
                geminiHistoryContext: List<String>,
                perplexitySignalStream: List<String>
            ): EpistemicGrade {
                return EvidenceGrade.CONTRADICTED
            }
        }

        val policyGate = DefaultMetaInstructPolicyGate()
        val verdict = policyGate.evaluateIngestionPipeline(
            payload = RawVisualPayload.ingest(byteArrayOf(1, 2, 3)),
            assessment = assessment,
            corroborator = mismatchCorroborator,
            geminiContext = listOf("Location: Room A"),
            perplexityStream = listOf("Location: Outdoors")
        )

        assertEquals(EvaluationVerdict.UNRESOLVED_QUARANTINE, verdict)
    }

    @Test
    fun `test_proposed_action_never_equals_executed_action`() {
        val proposal = triggerRouter.generateProposal("PURGE_CACHE", "digest-123", EpochNonce.generate())
        assertFalse(proposal.isExecuted, "TriggerProposal must never be constructed as executed.")

        val executedProposal = proposal.copy(isExecuted = true)
        val obs = solveEngine.solve("Obs for execution", "src")
        val h = Hypothesis(id = HypothesisId("hyp-exec"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = executedProposal,
                receipt = receipt,
                thermalTelemetry = ThermalTelemetry.evaluate(35.0, 2),
                isQuarantined = false
            )
        }
    }

    @Test
    fun `test_meta_instruct_cannot_directly_mutate_kernel`() {
        val policyGate = DefaultMetaInstructPolicyGate(verifiedInsightCount = 50L)

        assertFailsWith<IllegalArgumentException> {
            policyGate.proposeSubstrateEvolution()
        }

        val kernelClasses = listOf(
            "dev.aurakai.kernel.epistemic.AshRecord",
            "dev.aurakai.kernel.verification.VerificationReceipt",
            "dev.aurakai.kernel.dom.DomState"
        )
        for (cls in kernelClasses) {
            val clazz = Class.forName(cls)
            val mutableFields = clazz.declaredFields.filter { java.lang.reflect.Modifier.isPublic(it.modifiers) && !java.lang.reflect.Modifier.isFinal(it.modifiers) }
            assertTrue(mutableFields.isEmpty(), "Kernel class $cls must contain zero mutable public fields.")
        }
    }

    @Test
    fun `test_evolution_threshold_creates_proposal_not_mutation`() {
        val policyGate = DefaultMetaInstructPolicyGate(verifiedInsightCount = 100L)
        val proposal = policyGate.proposeSubstrateEvolution()

        assertNotNull(proposal)
        assertEquals(100L, proposal.verifiedInsightCount)
        assertFalse(proposal.isExecuted, "100 verified insights creates a proposal, NOT an automatic kernel mutation.")
    }

    @Test
    fun `test_visual_digest_binds_exact_payload`() {
        val bytes1 = byteArrayOf(1, 2, 3, 4, 5)
        val bytes2 = byteArrayOf(1, 2, 3, 4, 6)

        val payload1 = RawVisualPayload.ingest(bytes1)
        val payload2 = RawVisualPayload.ingest(bytes2)

        assertFalse(payload1.merkleRoot.value == payload2.merkleRoot.value, "Visual payload digests must bind exact payload bytes.")
    }

    @Test
    fun `test_visual_observation_cannot_become_verified_fact_without_verification`() {
        val obs = solveEngine.solve("Visual Integrity Vector: 0.95", "visual-mesh")
        val h = Hypothesis(id = HypothesisId("hyp-vis"), statement = "Visual geometry is authentic", supportingAshIds = listOf(obs.id))

        assertFailsWith<ConstitutionalViolationException> {
            DomState.project(listOf(h), emptyList(), listOf(obs))
        }
    }

    @Test
    fun `test_quarantined_state_cannot_reach_execution_admission`() {
        val obs = solveEngine.solve("Quarantined signal", "src")
        val h = Hypothesis(id = HypothesisId("hyp-quar"), statement = "Quarantined statement", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))
        val proposal = triggerRouter.generateProposal("ACTION", receipt.payloadDigest, EpochNonce.generate())

        assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal,
                receipt = receipt,
                thermalTelemetry = ThermalTelemetry.evaluate(32.0, 1),
                isQuarantined = true
            )
        }
    }

    @Test
    fun `test_caller_supplied_receipt_cannot_bypass_verification`() {
        val obs = solveEngine.solve("Valid observation", "src")
        val h = Hypothesis(id = HypothesisId("hyp-caller"), statement = "Statement", supportingAshIds = listOf(obs.id))
        val validReceipt = verificationEngine.verify(listOf(h), listOf(obs))

        val forgedProposal = triggerRouter.generateProposal("FORGED_ACTION", "mismatched-digest-666", EpochNonce.generate())

        assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = forgedProposal,
                receipt = validReceipt,
                thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                isQuarantined = false
            )
        }
    }

    @Test
    fun `test_provenance_survives_recursive_calibration`() {
        val rawValue = "UNTOUCHED_IMAGE_TELEMETRY_RAW_999"
        val obs = solveEngine.solve(rawValue, "camera-sensor")

        assertEquals(rawValue, obs.provenance.rawOriginalValue)
        assertEquals(rawValue, obs.payload)

        val policyGate = DefaultMetaInstructPolicyGate()
        val vector = VisualIntegrityVector(0.88, 1.1, 2.1, 0.04, 0.92)
        val assessment = AdversarialVisualAssessment(vector, 0.0, MerkleRootHash(obs.provenance.sourceDigest.value))

        val corroborator = object : CrossModalCorroborator {
            override fun corroborateContext(
                visualAssessment: AdversarialVisualAssessment,
                geminiHistoryContext: List<String>,
                perplexitySignalStream: List<String>
            ): EpistemicGrade = EvidenceGrade.DIRECT
        }

        policyGate.evaluateIngestionPipeline(
            payload = RawVisualPayload.ingest(byteArrayOf(9, 9, 9)),
            assessment = assessment,
            corroborator = corroborator,
            geminiContext = emptyList(),
            perplexityStream = emptyList()
        )

        assertEquals(rawValue, obs.provenance.rawOriginalValue, "Provenance rawOriginalValue MUST survive recursive calibration.")
    }
}
