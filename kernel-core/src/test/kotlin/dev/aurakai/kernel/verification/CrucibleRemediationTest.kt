package dev.aurakai.kernel.verification

import dev.aurakai.kernel.brain.*
import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.pipeline.SolveEngine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

/**
 * Phase 2 Crucible Remediation Verification Test Suite.
 * Validates fixes for GAP-01 (TOCTOU & Authorized Token), GAP-02 (Atomic CAS Ledger & Concurrency),
 * and GAP-04 (Pure In-Memory AshLedger).
 */
class CrucibleRemediationTest {

    private val solveEngine = SolveEngine()
    private val verificationEngine = VerificationEngine()
    private val triggerRouter = TriggerRouter()
    private val admissionGate = ExecutionAdmissionGate()

    @BeforeTest
    fun setUp() {
        EpochNonce.clearConsumedForTesting()
        AtomicReceiptLedger.resetForTesting()
        AshLedger.resetForTesting()
        QuarantineRegistry.resetForTesting()
        AuthoritativeThermalMonitor.resetForTesting()
        KernelEpochManager.resetForTesting()
    }

    @Test
    fun `test_ash_ledger_rejects_payload_mutation_on_identical_ash_id`() {
        val ledger = AshLedger.createIsolated()
        val obs1 = solveEngine.solve("Sensor Observation A", "camera-1")
        assertTrue(ledger.appendObservation(obs1))

        // Re-appending exact same record is idempotent
        assertFalse(ledger.appendObservation(obs1))

        // Attempting to overwrite existing AshId with modified payload must fail
        val mutatedObs = AshRecord(obs1.id, "Mutated Sensor Observation", obs1.provenance, obs1.telemetryType)
        assertFailsWith<ConstitutionalViolationException> {
            ledger.appendObservation(mutatedObs)
        }
    }

    @Test
    fun `test_ash_ledger_records_quarantine_scars_and_cumulative_degradation`() {
        val ledger = AshLedger.createIsolated()
        assertEquals(0L, ledger.getCumulativeDegradationScore())

        val scar = QuarantineScar(
            scarId = "scar-001",
            fromEpoch = EpochId(1L),
            contradictionDigest = "digest-contra",
            falsificationArtifactDigest = "digest-artifact",
            previousStateDigest = "digest-prev",
            newStateDigest = "digest-new",
            reasonCode = "CONTRADICTION_VETO",
            timestamp = KernelTimestamp(1000L)
        )

        assertTrue(ledger.recordQuarantineScar(scar))
        assertEquals(1, ledger.scarCount())
        assertEquals(10L, ledger.getCumulativeDegradationScore())

        ledger.recordTransition(
            EpistemicTransition(
                transitionId = "trans-001",
                fromDigest = "digest-prev",
                toDigest = "digest-new",
                reasonCode = "THERMAL_THROTTLE",
                severity = 3,
                timestamp = KernelTimestamp(1050L)
            )
        )
        assertEquals(13L, ledger.getCumulativeDegradationScore())
    }

    @Test
    fun `test_atomic_receipt_ledger_under_16_threads_64_attempts_guarantees_single_winner`() {
        val ledger = AtomicReceiptLedger.createIsolated()
        val receiptId = ReceiptId("receipt-concurrency-001")
        val nonce = EpochNonce.generate(EpochId(1L))

        val threadCount = 16
        val attemptsPerThread = 4
        val totalAttempts = threadCount * attemptsPerThread // 64 attempts

        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalAttempts)

        val winCount = AtomicInteger(0)
        val rejectionCount = AtomicInteger(0)

        for (i in 0 until totalAttempts) {
            executor.submit {
                try {
                    startLatch.await()
                    ledger.consume(receiptId, nonce)
                    winCount.incrementAndGet()
                } catch (e: ConstitutionalViolationException) {
                    rejectionCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        assertEquals(1, winCount.get(), "EXACTLY ONE attempt out of 64 concurrent contenders must consume the receipt.")
        assertEquals(totalAttempts - 1, rejectionCount.get(), "ALL 63 other concurrent attempts must be rejected.")
        assertTrue(ledger.isReceiptConsumed(receiptId))
        assertTrue(ledger.isNonceConsumed(nonce.nonceValue))
    }

    @Test
    fun `test_execution_admission_gate_issues_authorized_token_and_rejects_receipt_reuse`() {
        val obs = solveEngine.solve("Obs for Token Test", "sensor-token")
        val h = Hypothesis(id = HypothesisId("hyp-token"), statement = "Statement verified", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val nonce1 = KernelEpochManager.issueAuthoritativeNonce()
        val binding1 = SnapshotBinding(
            blueprintDigest = receipt.payloadDigest,
            observationDigests = listOf(obs.id.value),
            falsificationTraceDigest = "trace-token-1",
            snapshotDigest = receipt.payloadDigest
        )
        val proposal1 = triggerRouter.generateProposal("ACTION_ONE", receipt.payloadDigest, nonce1, binding1)

        // First attempt returns an AuthorizedActionToken
        val decision1 = admissionGate.authorize(proposal1, receipt)
        assertIs<AdmissionDecision.Authorized>(decision1)
        assertEquals("ACTION_ONE", decision1.token.actionType)
        assertEquals(receipt.payloadDigest, decision1.token.payloadDigest)
        assertEquals(1L, decision1.token.consumptionRecord.sequenceNumber)

        // Second attempt with FRESH nonce but SAME receipt MUST be rejected (GAP-02 Receipt Double-Spending)
        val nonce2 = KernelEpochManager.issueAuthoritativeNonce()
        val binding2 = SnapshotBinding(
            blueprintDigest = receipt.payloadDigest,
            observationDigests = listOf(obs.id.value),
            falsificationTraceDigest = "trace-token-2",
            snapshotDigest = receipt.payloadDigest
        )
        val proposal2 = triggerRouter.generateProposal("ACTION_TWO", receipt.payloadDigest, nonce2, binding2)

        val decision2 = admissionGate.authorize(proposal2, receipt)
        assertIs<AdmissionDecision.Denied>(decision2)
        assertEquals(KernelRefusalReason.ReplayAttackRejection, decision2.refusalReason)
        assertTrue(decision2.reason.contains("RECEIPT REPLAY DETECTED"))
    }
}
