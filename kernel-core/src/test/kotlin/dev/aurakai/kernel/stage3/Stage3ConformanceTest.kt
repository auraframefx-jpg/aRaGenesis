package dev.aurakai.kernel.stage3

import dev.aurakai.kernel.brain.*
import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.pipeline.SolveEngine
import dev.aurakai.kernel.verification.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stage 3 Conformance Vector Test Suite.
 * Exhaustively tests:
 * - Single-Fracture Matrix
 * - Character Score Orthogonality
 * - Replay Attack Rejection (EpochNonce)
 * - Non-Finite Thermal Input Rejection
 * - Snapshot Binding Verification
 * - 4-Model Identity Discrimination
 * - Positive End-to-End Admission Path
 */
class Stage3ConformanceTest {

    private val solveEngine = SolveEngine()
    private val verificationEngine = VerificationEngine()
    private val triggerRouter = TriggerRouter()
    private val admissionGate = ExecutionAdmissionGate()

    @BeforeTest
    fun setUp() {
        EpochNonce.clearConsumedForTesting()
    }

    @Test
    fun `test_character_orthogonality_high_character_invalid_authority_rejected`() {
        val highCharacter = CharacterTensor(individuality = 1.0, coherence = 1.0, contribution = 1.0, adaptation = 1.0)
        assertEquals(1.0, highCharacter.aggregateCharacterScore)

        val obs = solveEngine.solve("High character test obs", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-1"), statement = "Statement 1", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val invalidProposal = triggerRouter.generateProposal(
            actionType = "ACTION_MUTATE",
            payloadDigest = "invalid-mismatched-digest",
            epochNonce = EpochNonce.generate()
        )

        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = invalidProposal,
                receipt = receipt,
                thermalTelemetry = ThermalTelemetry.evaluate(35.0, 1),
                isQuarantined = false,
                characterTensor = highCharacter
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.InvalidSnapshotBinding.code))
    }

    @Test
    fun `test_character_orthogonality_low_character_valid_authority_admitted`() {
        val lowCharacter = CharacterTensor(individuality = 0.1, coherence = 0.1, contribution = 0.1, adaptation = 0.1)
        assertEquals(0.1, lowCharacter.aggregateCharacterScore)

        val obs = solveEngine.solve("Low character valid obs", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-2"), statement = "Statement 2", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val nonce = EpochNonce.generate()
        val snapshotBinding = SnapshotBinding(
            blueprintDigest = receipt.payloadDigest,
            observationDigests = listOf(obs.id.value),
            falsificationTraceDigest = "trace-digest-123",
            snapshotDigest = receipt.payloadDigest
        )

        val validProposal = triggerRouter.generateProposal(
            actionType = "ACTION_ADMIT",
            payloadDigest = receipt.payloadDigest,
            epochNonce = nonce,
            snapshotBinding = snapshotBinding
        )

        val admitted = admissionGate.evaluateAdmission(
            proposal = validProposal,
            receipt = receipt,
            thermalTelemetry = ThermalTelemetry.evaluate(35.0, 1),
            isQuarantined = false,
            characterTensor = lowCharacter
        )

        assertTrue(admitted, "Low character score must NOT prevent admission when authority is valid.")
    }

    @Test
    fun `test_nan_thermal_input_forces_sovereign_state_freeze`() {
        val nanTelemetry = ThermalTelemetry.evaluate(Double.NaN, 2)
        assertEquals(ThermalState.SOVEREIGN_STATE_FREEZE, nanTelemetry.state)

        val obs = solveEngine.solve("Obs NaN", "src")
        val h = Hypothesis(id = HypothesisId("hyp-nan"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))
        val proposal = triggerRouter.generateProposal("ACTION", receipt.payloadDigest, EpochNonce.generate())

        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal,
                receipt = receipt,
                thermalTelemetry = nanTelemetry,
                isQuarantined = false
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.ThermalWallBreach.code))
    }

    @Test
    fun `test_infinite_thermal_input_forces_sovereign_state_freeze`() {
        val infTelemetry = ThermalTelemetry.evaluate(Double.POSITIVE_INFINITY, 2)
        assertEquals(ThermalState.SOVEREIGN_STATE_FREEZE, infTelemetry.state)

        val obs = solveEngine.solve("Obs Inf", "src")
        val h = Hypothesis(id = HypothesisId("hyp-inf"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))
        val proposal = triggerRouter.generateProposal("ACTION", receipt.payloadDigest, EpochNonce.generate())

        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal,
                receipt = receipt,
                thermalTelemetry = infTelemetry,
                isQuarantined = false
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.ThermalWallBreach.code))
    }

    @Test
    fun `test_replay_attack_with_consumed_epoch_nonce_rejected`() {
        val obs = solveEngine.solve("Obs Replay", "src")
        val h = Hypothesis(id = HypothesisId("hyp-replay"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val nonce = EpochNonce.generate()
        val proposal1 = triggerRouter.generateProposal("ACTION_1", receipt.payloadDigest, nonce)

        // First admission succeeds
        val admitted1 = admissionGate.evaluateAdmission(
            proposal = proposal1,
            receipt = receipt,
            thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
            isQuarantined = false
        )
        assertTrue(admitted1)

        // Replay attempt with same nonce
        val proposal2 = triggerRouter.generateProposal("ACTION_2", receipt.payloadDigest, nonce)
        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal2,
                receipt = receipt,
                thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                isQuarantined = false
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.ReplayAttackRejection.code))
    }

    @Test
    fun `test_mismatched_snapshot_binding_rejected`() {
        val obs = solveEngine.solve("Obs Snapshot", "src")
        val h = Hypothesis(id = HypothesisId("hyp-snap"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val invalidBinding = SnapshotBinding(
            blueprintDigest = "forged-blueprint-digest",
            observationDigests = listOf("obs-1"),
            falsificationTraceDigest = "trace-1",
            snapshotDigest = receipt.payloadDigest
        )

        val proposal = triggerRouter.generateProposal(
            actionType = "ACTION",
            payloadDigest = receipt.payloadDigest,
            epochNonce = EpochNonce.generate(),
            snapshotBinding = invalidBinding
        )

        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal,
                receipt = receipt,
                thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                isQuarantined = false
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.InvalidSnapshotBinding.code))
    }

    @Test
    fun `test_four_model_identity_discrimination_failure_rejected`() {
        val discriminator = FourModelIdentityDiscriminator()

        // Insufficient models (< 4)
        val modelsShort = listOf(
            IdentityHypothesisModel("m1", "DirectRelationship", "crit1", listOf("e1"), emptyList(), "d1"),
            IdentityHypothesisModel("m2", "CommonConfounder", "crit2", listOf("e2"), emptyList(), "d2")
        )
        assertFalse(discriminator.discriminate(modelsShort))

        // Duplicate semantic roles
        val modelsDuplicateRoles = listOf(
            IdentityHypothesisModel("m1", "DirectRelationship", "crit1", listOf("e1"), emptyList(), "d1"),
            IdentityHypothesisModel("m2", "DirectRelationship", "crit2", listOf("e2"), emptyList(), "d2"),
            IdentityHypothesisModel("m3", "CoincidenceNoise", "crit3", listOf("e3"), emptyList(), "d3"),
            IdentityHypothesisModel("m4", "AdversarialSpoof", "crit4", listOf("e4"), emptyList(), "d4")
        )
        assertFalse(discriminator.discriminate(modelsDuplicateRoles))

        // Fully distinct 4 models
        val valid4Models = listOf(
            IdentityHypothesisModel("m1", "DirectRelationship", "crit1", listOf("e1"), emptyList(), "d1"),
            IdentityHypothesisModel("m2", "CommonConfounder", "crit2", listOf("e2"), emptyList(), "d2"),
            IdentityHypothesisModel("m3", "CoincidenceNoise", "crit3", listOf("e3"), emptyList(), "d3"),
            IdentityHypothesisModel("m4", "AdversarialSpoof", "crit4", listOf("e4"), emptyList(), "d4")
        )
        assertTrue(discriminator.discriminate(valid4Models))
    }

    @Test
    fun `test_six_w_structure_validation`() {
        val valid6W = SixWAsymmetryVector(
            who = "Agent-Aura",
            what = "StateTransitionProposal",
            whenTime = "1000L",
            whereLoc = "kernel-core",
            whyReason = "Falsification trace survived",
            howMethod = "SolveEtCoagula"
        )
        assertTrue(valid6W.validate6WStructure())

        val invalid6W = valid6W.copy(whyReason = "")
        assertFalse(invalid6W.validate6WStructure())
    }

    @Test
    fun `test_positive_end_to_end_admission_path`() {
        // 1. Observation
        val obs = solveEngine.solve("E2E Valid Payload", "sensor-e2e")

        // 2. Hypothesis & Solve et Coagula
        val hypothesis = Hypothesis(
            id = HypothesisId("hyp-e2e"),
            statement = "E2E Hypothesis Verified",
            supportingAshIds = listOf(obs.id)
        )

        // 3. Verification & Receipt Minting
        val receipt = verificationEngine.verify(listOf(hypothesis), listOf(obs))

        // 4. MetaInstruct Record Verified Receipt
        val metaGate = DefaultMetaInstructPolicyGate()
        metaGate.recordVerifiedReceipt(receipt)
        assertEquals(1L, metaGate.getVerifiedInsightCount())

        // 5. Fresh Epoch Nonce & Snapshot Binding
        val nonce = EpochNonce.generate()
        val binding = SnapshotBinding(
            blueprintDigest = receipt.payloadDigest,
            observationDigests = listOf(obs.id.value),
            falsificationTraceDigest = "trace-digest-e2e",
            snapshotDigest = receipt.payloadDigest
        )

        // 6. Proposal Generation
        val proposal = triggerRouter.generateProposal(
            actionType = "EXECUTE_ADMITTED_TRANSITION",
            payloadDigest = receipt.payloadDigest,
            epochNonce = nonce,
            snapshotBinding = binding
        )

        // 7. Execution Admission Evaluation
        val admitted = admissionGate.evaluateAdmission(
            proposal = proposal,
            receipt = receipt,
            thermalTelemetry = ThermalTelemetry.evaluate(36.5, 2),
            isQuarantined = false,
            characterTensor = CharacterTensor(0.8, 0.85, 0.9, 0.88)
        )

        assertTrue(admitted, "Positive end-to-end path MUST succeed and permit admission.")
    }
}
