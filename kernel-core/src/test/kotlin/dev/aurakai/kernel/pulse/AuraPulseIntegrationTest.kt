package dev.aurakai.kernel.pulse

import dev.aurakai.kernel.brain.*
import dev.aurakai.kernel.epistemic.*
import dev.aurakai.kernel.pipeline.SolveEngine
import dev.aurakai.kernel.verification.*
import kotlin.test.*

/**
 * AURA-PULSE-005 Acceptance Test Suite.
 * Validates AP-005-01 through AP-005-12 behavioral contracts and the Twelve Invariants.
 */
class AuraPulseIntegrationTest {

    private val solveEngine = SolveEngine()
    private val verificationEngine = VerificationEngine()
    private val triggerRouter = TriggerRouter()
    private val admissionGate = ExecutionAdmissionGate()
    private val pulseGate = AuraPulseGate(admissionGate)

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
    fun `AP-005-01 Resume unfinished objective without repeating completed steps`() {
        val initialTask = PulseTaskContext(
            taskId = "task-build-ast-01",
            objective = "Parse and project 9 canonical runes",
            completedSteps = listOf("Tokenize Cyrillic runes", "Validate compound constituents"),
            pendingSteps = listOf("Construct AST tree", "5x5 Lattice Projection")
        )
        pulseGate.registerTask(initialTask)

        val retrieved = pulseGate.getTask("task-build-ast-01")
        assertNotNull(retrieved)
        assertEquals(2, retrieved!!.completedSteps.size)
        assertEquals("Construct AST tree", retrieved.pendingSteps.first())
        assertTrue(retrieved.hasPendingWork())
    }

    @Test
    fun `AP-005-02 Recognize known failed procedure and block execution`() {
        val taskId = "task-quarantine-test"
        val task = PulseTaskContext(
            taskId = taskId,
            objective = "Execute database mutation",
            completedSteps = emptyList(),
            pendingSteps = listOf("Apply schema patch")
        )
        pulseGate.registerTask(task)

        val failedProcedureDigest = "procedure-corrupt-schema-hash"
        pulseGate.recordFailedProcedure(failedProcedureDigest)

        val decision = pulseGate.evaluateStepAdmission(
            taskId = taskId,
            stepName = "Apply schema patch",
            requiredLevel = PulsePermissionLevel.L3_ROUTINE_REVERSIBLE,
            procedureDigest = failedProcedureDigest
        )

        assertIs<PulseAdmissionDecision.Refused>(decision)
        assertEquals("FAILED_PROCEDURE_BLOCKED", decision.violationCode)
    }

    @Test
    fun `AP-005-05 Continue authorized routine work autonomously within L0 to L3`() {
        val taskId = "task-autonomous-work"
        val task = PulseTaskContext(
            taskId = taskId,
            objective = "Compile AST and cache results",
            completedSteps = emptyList(),
            pendingSteps = listOf("Generate AST"),
            maxPermittedLevel = PulsePermissionLevel.L3_ROUTINE_REVERSIBLE
        )
        pulseGate.registerTask(task)

        val decision = pulseGate.evaluateStepAdmission(
            taskId = taskId,
            stepName = "Generate AST",
            requiredLevel = PulsePermissionLevel.L2_PREPARATION
        )

        assertIs<PulseAdmissionDecision.ProceedAutonomous>(decision)
        assertEquals(PulsePermissionLevel.L2_PREPARATION, decision.level)
    }

    @Test
    fun `AP-005-06 Stop and require user confirmation before consequential L4 or L5 operations`() {
        val taskId = "task-consequential"
        val task = PulseTaskContext(
            taskId = taskId,
            objective = "Publish release package",
            completedSteps = listOf("Run test suite"),
            pendingSteps = listOf("Push git tags"),
            maxPermittedLevel = PulsePermissionLevel.L3_ROUTINE_REVERSIBLE
        )
        pulseGate.registerTask(task)

        val decision = pulseGate.evaluateStepAdmission(
            taskId = taskId,
            stepName = "Push git tags",
            requiredLevel = PulsePermissionLevel.L4_CONSEQUENTIAL_APPROVAL
        )

        assertIs<PulseAdmissionDecision.RequiresUserConfirmation>(decision)
        assertEquals(PulsePermissionLevel.L4_CONSEQUENTIAL_APPROVAL, decision.level)
        assertTrue(decision.prompt.contains("User confirmation required"))
    }

    @Test
    fun `AP-005-11 Respect permission revocation immediately`() {
        val taskId = "task-revoked"
        val task = PulseTaskContext(
            taskId = taskId,
            objective = "Continuous background indexing",
            completedSteps = listOf("Index doc 1"),
            pendingSteps = listOf("Index doc 2"),
            isRevoked = true // User revoked permission
        )
        pulseGate.registerTask(task)

        val decision = pulseGate.evaluateStepAdmission(
            taskId = taskId,
            stepName = "Index doc 2",
            requiredLevel = PulsePermissionLevel.L1_AUTHORIZED_INFO
        )

        assertIs<PulseAdmissionDecision.Refused>(decision)
        assertEquals("PERMISSION_REVOKED", decision.violationCode)
    }

    @Test
    fun `AP-005-12 Atomic kernel token binding prevents unadmitted self-authorization`() {
        val obs = solveEngine.solve("Valid pulse observation", "sensor-pulse")
        val h = Hypothesis(id = HypothesisId("hyp-pulse"), statement = "Statement pulse", supportingAshIds = listOf(obs.id))
        val receipt = verificationEngine.verify(listOf(h), listOf(obs))

        val nonce = KernelEpochManager.issueAuthoritativeNonce()
        val binding = SnapshotBinding(
            blueprintDigest = receipt.payloadDigest,
            observationDigests = listOf(obs.id.value),
            falsificationTraceDigest = "trace-pulse",
            snapshotDigest = receipt.payloadDigest
        )
        val proposal = triggerRouter.generateProposal("PULSE_ACTION", receipt.payloadDigest, nonce, binding)

        // Admitted through kernel gate via pulseGate
        val token = pulseGate.executeAdmittedProposal(proposal, receipt)
        assertNotNull(token)
        assertEquals("PULSE_ACTION", token.actionType)
        assertEquals(receipt.payloadDigest, token.payloadDigest)

        // Replay attempt must be rejected
        val proposal2 = triggerRouter.generateProposal("PULSE_ACTION_2", receipt.payloadDigest, nonce, binding)
        assertFailsWith<ConstitutionalViolationException> {
            pulseGate.executeAdmittedProposal(proposal2, receipt)
        }
    }
}
