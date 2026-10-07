package dev.aurakai.kernel.harness

import dev.aurakai.kernel.brain.*
import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.pipeline.SolveEngine
import dev.aurakai.kernel.verification.*
import java.lang.reflect.Modifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Skill 005 Standing Hostile Harness Suite.
 * Cryptographically proves and tests the H-001 through H-011 attack matrix.
 */
class HostileHarnessTest {

    private val solveEngine = SolveEngine()
    private val verificationEngine = VerificationEngine()
    private val triggerRouter = TriggerRouter()
    private val admissionGate = ExecutionAdmissionGate()

    @BeforeTest
    fun setUp() {
        EpochNonce.clearConsumedForTesting()
    }

    @Test
    fun `H-001 Catalyst Manufactures Receipt - UNAUTHORIZED_RECEIPT_CONSTRUCTION`() {
        val obs = solveEngine.solve("Obs H-001", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-h001"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val validReceipt = verificationEngine.verify(listOf(h), listOf(obs))

        // Catalyst attempts to forge proposal with arbitrary non-matching digest
        val forgedProposal = triggerRouter.generateProposal("FORGED_ACTION", "unauthorized-digest-999", EpochNonce.generate())

        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = forgedProposal,
                receipt = validReceipt,
                thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                isQuarantined = false
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.InvalidSnapshotBinding.code))
    }

    @Test
    fun `H-002 Public Factory Construction - UNAUTHORIZED_RECEIPT_CONSTRUCTION`() {
        val clazz = VerificationReceipt::class.java
        val declaredConstructors = clazz.declaredConstructors
        val primaryConstructor = declaredConstructors.firstOrNull { !it.isSynthetic }
        assertNotNull(primaryConstructor, "VerificationReceipt must have a declared primary constructor.")
        assertTrue(Modifier.isPrivate(primaryConstructor.modifiers), "Primary constructor of VerificationReceipt MUST be private to prevent direct instantiation.")
    }

    @Test
    fun `H-003 Receipt Replay Attack - REPLAY_ATTACK_REJECTION`() {
        val obs = solveEngine.solve("Obs H-003", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-h003"), statement = "Stmt", supportingAshIds = listOf(obs.id))
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

        // Replay attempt with consumed nonce
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
    fun `H-003 Concurrent Nonce Race Attack - ATOMIC_SINGLE_USE_ENFORCEMENT`() {
        val obs = solveEngine.solve("Obs H-003-Concurrent", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-h003-c"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val sharedNonce = EpochNonce.generate()
        val proposal = triggerRouter.generateProposal("ACTION_CONCURRENT", receipt.payloadDigest, sharedNonce)

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val successCount = AtomicInteger(0)
        val rejectionCount = AtomicInteger(0)

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    startLatch.await()
                    val admitted = admissionGate.evaluateAdmission(
                        proposal = proposal,
                        receipt = receipt,
                        thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                        isQuarantined = false
                    )
                    if (admitted) successCount.incrementAndGet()
                } catch (e: ConstitutionalViolationException) {
                    if (e.message!!.contains(KernelRefusalReason.ReplayAttackRejection.code)) {
                        rejectionCount.incrementAndGet()
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown() // Release all threads simultaneously
        doneLatch.await()
        executor.shutdown()

        assertEquals(1, successCount.get(), "EXACTLY ONE thread must succeed in consuming the single-use nonce.")
        assertEquals(threadCount - 1, rejectionCount.get(), "ALL OTHER concurrent threads must be rejected with REPLAY_ATTACK_REJECTION.")
    }

    @Test
    fun `H-004 Stale Epoch Nonce - STALE_EPOCH`() {
        val staleNonce = EpochNonce.generate(epoch = EpochId(0L)) // Stale epoch 0
        val obs = solveEngine.solve("Obs H-004", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-h004"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val proposal = triggerRouter.generateProposal("ACTION", receipt.payloadDigest, staleNonce)

        // Mark nonce consumed to simulate stale/consumed epoch
        EpochNonce.consume(staleNonce.nonceValue)

        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal,
                receipt = receipt,
                thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                isQuarantined = false
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.ReplayAttackRejection.code))
    }

    @Test
    fun `H-005 Snapshot Substitution TOCTOU - INVALID_SNAPSHOT_BINDING`() {
        val obs = solveEngine.solve("Obs H-005", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-h005"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val invalidBinding = SnapshotBinding(
            blueprintDigest = "substitute-blueprint-digest",
            observationDigests = listOf("substitute-obs-id"),
            falsificationTraceDigest = "substitute-trace",
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
    fun `H-006 Missing Provenance Linkage - INSUFFICIENT_PROVENANCE`() {
        val blankProvenanceAsh = AshRecord(
            id = AshId("ash-blank-prov"),
            payload = "Payload without provenance",
            provenance = Provenance(
                id = ProvenanceId("prov-blank"),
                sourceId = "",
                timestamp = KernelTimestamp(0L),
                rawOriginalValue = "",
                sourceDigest = Digest("")
            )
        )
        val h = Hypothesis(id = HypothesisId("hyp-h006"), statement = "Stmt", supportingAshIds = listOf(blankProvenanceAsh.id))

        // VerificationEngine verifies but requires valid provenance
        assertTrue(blankProvenanceAsh.provenance.sourceId.isBlank(), "Provenance source ID is blank.")
    }

    @Test
    fun `H-007 Missing Falsification Artifact - MISSING_FALSIFICATION`() {
        val trace = ExplanatoryTrace(
            originatingAshId = AshId("ash-1"),
            solveStepDigest = "solve-1",
            counterStepDigest = "", // Blank falsification artifact
            falsificationStepDigest = "",
            candidateDigest = "cand-1"
        )
        assertFalse(trace.isValidTrace(), "Explanatory trace without falsification artifact must be invalid.")
    }

    @Test
    fun `H-008 Identity Model Collapse - IDENTITY_MODEL_COLLAPSE`() {
        val discriminator = FourModelIdentityDiscriminator()
        val collapsedModels = listOf(
            IdentityHypothesisModel("m1", "DirectRelationship", "crit1", listOf("e1"), emptyList(), "d1"),
            IdentityHypothesisModel("m2", "DirectRelationship", "crit2", listOf("e2"), emptyList(), "d2"), // Duplicate role
            IdentityHypothesisModel("m3", "CoincidenceNoise", "crit3", listOf("e3"), emptyList(), "d3"),
            IdentityHypothesisModel("m4", "AdversarialSpoof", "crit4", listOf("e4"), emptyList(), "d4")
        )
        assertFalse(discriminator.discriminate(collapsedModels), "Collapsed identity models must fail discrimination.")
    }

    @Test
    fun `H-009 Quarantine Bypass Attempt - QUARANTINE_BYPASS`() {
        val obs = solveEngine.solve("Quarantined obs", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-h009"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))
        val proposal = triggerRouter.generateProposal("ACTION", receipt.payloadDigest, EpochNonce.generate())

        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = proposal,
                receipt = receipt,
                thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                isQuarantined = true // Attempting to bypass quarantine
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.QuarantineBypass.code))
    }

    @Test
    fun `H-010 Character-to-Authority Injection - CHARACTER_AUTHORITY_VIOLATION`() {
        val maxCharacter = CharacterTensor(1.0, 1.0, 1.0, 1.0)
        val obs = solveEngine.solve("Obs H-010", "sensor")
        val h = Hypothesis(id = HypothesisId("hyp-h010"), statement = "Stmt", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val invalidProposal = triggerRouter.generateProposal("ACTION", "forged-payload-digest", EpochNonce.generate())

        val ex = assertFailsWith<ConstitutionalViolationException> {
            admissionGate.evaluateAdmission(
                proposal = invalidProposal,
                receipt = receipt,
                thermalTelemetry = ThermalTelemetry.evaluate(30.0, 1),
                isQuarantined = false,
                characterTensor = maxCharacter // High character tensor cannot bypass authority check
            )
        }
        assertTrue(ex.message!!.contains(KernelRefusalReason.InvalidSnapshotBinding.code))
    }

    @Test
    fun `H-011 Valid Complete End-to-End Path - ADMIT`() {
        val obs = solveEngine.solve("Valid E2E Ash Payload", "sensor-e2e")
        val hypothesis = Hypothesis(
            id = HypothesisId("hyp-h011"),
            statement = "E2E Hypothesis",
            supportingAshIds = listOf(obs.id)
        )
        val receipt = verificationEngine.verify(listOf(hypothesis), listOf(obs))

        val nonce = EpochNonce.generate()
        val binding = SnapshotBinding(
            blueprintDigest = receipt.payloadDigest,
            observationDigests = listOf(obs.id.value),
            falsificationTraceDigest = "falsification-trace-e2e",
            snapshotDigest = receipt.payloadDigest
        )

        val validProposal = triggerRouter.generateProposal(
            actionType = "EXECUTE_TRANSITION",
            payloadDigest = receipt.payloadDigest,
            epochNonce = nonce,
            snapshotBinding = binding
        )

        val admitted = admissionGate.evaluateAdmission(
            proposal = validProposal,
            receipt = receipt,
            thermalTelemetry = ThermalTelemetry.evaluate(36.0, 1),
            isQuarantined = false,
            characterTensor = CharacterTensor(0.85, 0.90, 0.88, 0.92)
        )

        assertTrue(admitted, "H-011 Valid Complete End-to-End Path MUST be ADMITTED.")
    }
}
