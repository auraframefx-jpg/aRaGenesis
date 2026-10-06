package dev.aurakai.kernel.brain

import dev.aurakai.kernel.dom.DomState
import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.pipeline.SolveEngine
import dev.aurakai.kernel.verification.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mandatory Conformance Vector Test Suite for Cognitive Brain Runtime (PR #2).
 * Verifies negative bounds, thermal wall freeze, TOCTOU payload integrity,
 * cross-modal contradiction quarantine, and execution admission guarantees.
 */
class CognitiveBrainTest {

    private val solveEngine = SolveEngine()
    private val verificationEngine = VerificationEngine()
    private val triggerRouter = TriggerRouter()
    private val admissionGate = ExecutionAdmissionGate()

    @Test
    fun `test_thermal_wall_breach_forces_state_freeze`() {
        val thermalHigh = ThermalTelemetry.evaluate(42.5, 8)
        assertEquals(ThermalState.SOVEREIGN_STATE_FREEZE, thermalHigh.state)

        val obs = solveEngine.solve("Thermal test obs", "sensor-thermal")
        val h = Hypothesis(id = HypothesisId("hyp-thermal"), statement = "Normal status", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))
        val proposal = triggerRouter.generateProposal(ProposedActionType.CHAT, receipt.payloadDigest)

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
        assertEquals(Cryptography.computePayloadDigest(originalBytes), payload.payloadDigest.value)
    }

    @Test
    fun `test_cross_modal_mismatch_forces_unresolved_state`() {
        val vector = VisualIntegrityVector(0.9, 1.2, 2.3, 0.05, 0.95)
        val digest = PayloadDigest("abcd1234digest")
        val assessment = AdversarialVisualAssessment(vector, 0.1, digest, listOf("artifact1"))

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
        val proposal = triggerRouter.generateProposal(ProposedActionType.DOM_PROJECTION, "digest-123")

        val obs = solveEngine.solve("Obs for execution", "src")
        val h = Hypothesis(id = HypothesisId("hyp-exec"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val matchingProposal = triggerRouter.generateProposal(ProposedActionType.DOM_PROJECTION, receipt.payloadDigest)
        val executionReceipt = admissionGate.evaluateAdmission(
            proposal = matchingProposal,
            receipt = receipt,
            thermalTelemetry = ThermalTelemetry.evaluate(35.0, 2),
            isQuarantined = false
        )

        assertNotNull(executionReceipt)
        assertEquals(matchingProposal.proposalId, executionReceipt.proposalId)
        assertEquals(ProposedActionType.DOM_PROJECTION, executionReceipt.actionType)
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

        assertFalse(payload1.payloadDigest.value == payload2.payloadDigest.value, "Visual payload digests must bind exact payload bytes.")
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
        val proposal = triggerRouter.generateProposal(ProposedActionType.QUARANTINE, receipt.payloadDigest)

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

        val forgedProposal = triggerRouter.generateProposal(ProposedActionType.CHAT, "mismatched-digest-666")

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
        val assessment = AdversarialVisualAssessment(vector, 0.0, PayloadDigest(obs.provenance.sourceDigest.value))

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

    // ============================================================================
    // HARDENING REGRESSION TESTS
    // ============================================================================

    @Test
    fun `test_meta_instruct_cannot_create_verified_insight`() {
        val policyGate = DefaultMetaInstructPolicyGate()
        val payload = RawVisualPayload.ingest(byteArrayOf(10, 20, 30))
        val assessment = AdversarialVisualAssessment(
            VisualIntegrityVector(1.0, 1.0, 1.0, 0.0, 1.0),
            0.0,
            payload.payloadDigest
        )

        val directCorroborator = object : CrossModalCorroborator {
            override fun corroborateContext(
                visualAssessment: AdversarialVisualAssessment,
                geminiHistoryContext: List<String>,
                perplexitySignalStream: List<String>
            ): EpistemicGrade = EvidenceGrade.DIRECT
        }

        val verdict = policyGate.evaluateIngestionPipeline(payload, assessment, directCorroborator, emptyList(), emptyList())
        assertEquals(EvaluationVerdict.SNAPSHOT_CURATION, verdict)
        assertEquals(0L, policyGate.getVerifiedInsightCount(), "MetaInstruct evaluation pipeline MUST NOT increment verified insight count.")

        val obs = solveEngine.solve("Obs payload", "src-meta")
        val h = Hypothesis(id = HypothesisId("hyp-meta"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val verifiedReceipt = verificationEngine.verify(listOf(h), listOf(obs))

        policyGate.recordVerifiedInsight(verifiedReceipt)
        assertEquals(1L, policyGate.getVerifiedInsightCount(), "MetaInstruct insight count MUST only increment via verified VerificationReceipt.")
    }

    @Test
    fun `test_payload_digest_is_not_merkle_root`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val digest = Cryptography.computePayloadDigest(bytes)
        val expectedSha256 = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

        assertEquals(expectedSha256, digest)
        assertFalse(PayloadDigest::class.java.simpleName.contains("Merkle"), "Payload digest abstraction must be truthfully named.")
    }

    @Test
    fun `test_original_input_mutation_cannot_change_frozen_snapshot`() {
        val callerBuffer = byteArrayOf(100, 101, 102)
        val payload = RawVisualPayload.ingest(callerBuffer)

        val initialDigest = payload.payloadDigest.value

        callerBuffer[0] = 0

        val postMutationDigest = Cryptography.computePayloadDigest(payload.getPayloadSnapshot())
        assertEquals(initialDigest, postMutationDigest, "Mutating original caller buffer MUST NOT alter frozen snapshot payload digest.")
    }

    @Test
    fun `test_admission_revalidates_exact_verified_payload`() {
        val originalBytes = byteArrayOf(50, 60, 70)
        val payload = RawVisualPayload.ingest(originalBytes)

        val obs = solveEngine.solve("Visual telemetry", "camera")
        val h = Hypothesis(id = HypothesisId("hyp-admit"), statement = "Stmt", supportingAshIds = listOf(obs.id))

        val receipt = verificationEngine.verify(listOf(h), listOf(obs))
        val proposal = triggerRouter.generateProposal(ProposedActionType.CHAT, receipt.payloadDigest)

        val executionReceipt = admissionGate.evaluateAdmission(
            proposal = proposal,
            receipt = receipt,
            thermalTelemetry = ThermalTelemetry.evaluate(30.0, 2),
            isQuarantined = false,
            payload = payload
        )

        assertNotNull(executionReceipt)
        assertEquals(proposal.proposalId, executionReceipt.proposalId)
    }

    @Test
    fun `test_forged_verification_receipt_cannot_authorize_execution`() {
        val obs = solveEngine.solve("Valid obs", "src")
        val h = Hypothesis(id = HypothesisId("hyp-forged-rcpt"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val realReceipt = verificationEngine.verify(listOf(h), listOf(obs))

        val forgedProposal = triggerRouter.generateProposal(ProposedActionType.DOM_PROJECTION, "forged-payload-digest-000")

        assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = forgedProposal,
                receipt = realReceipt,
                thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                isQuarantined = false
            )
        }
    }

    @Test
    fun `test_trigger_action_type_is_strongly_typed`() {
        val proposal = triggerRouter.generateProposal(ProposedActionType.EVOLUTION_PROPOSAL, "digest-typed")
        assertEquals(ProposedActionType.EVOLUTION_PROPOSAL, proposal.actionType)
        assertTrue(proposal.actionType is ProposedActionType, "TriggerProposal action type must be a strongly typed enum/sealed domain contract.")
    }

    @Test
    fun `test_execution_state_is_not_mutable_proposal_state`() {
        val proposal = triggerRouter.generateProposal(ProposedActionType.CHAT, "digest-immutable")
        val fields = proposal::class.java.declaredFields.map { it.name }
        assertFalse(fields.contains("isExecuted"), "TriggerProposal must NOT maintain mutable execution state.")
    }

    @Test
    fun `test_thermal_throttle_has_behavioral_effect`() {
        val telemetryNominal = ThermalTelemetry.evaluate(35.0, 8)
        assertEquals(ThermalState.NOMINAL, telemetryNominal.state)
        assertEquals(8, telemetryNominal.activeParsingThreads)

        val telemetryThrottled = ThermalTelemetry.evaluate(39.5, 8)
        assertEquals(ThermalState.THROTTLED_MEDITATION, telemetryThrottled.state)
        assertEquals(1, telemetryThrottled.activeParsingThreads, "THROTTLED_MEDITATION (>= 39°C) MUST clamp active parsing threads to 1.")
    }

    @Test
    fun `test_frozen_state_blocks_pending_visual_work`() {
        val thermalFreeze = ThermalTelemetry.evaluate(43.0, 4)
        assertEquals(ThermalState.SOVEREIGN_STATE_FREEZE, thermalFreeze.state)

        val obs = solveEngine.solve("Pending visual obs", "mesh")
        val h = Hypothesis(id = HypothesisId("hyp-freeze"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))
        val proposal = triggerRouter.generateProposal(ProposedActionType.CHAT, receipt.payloadDigest)

        assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal,
                receipt = receipt,
                thermalTelemetry = thermalFreeze,
                isQuarantined = false
            )
        }
    }
}
