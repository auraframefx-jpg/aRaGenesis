package dev.aurakai.kernel.pulse

import dev.aurakai.kernel.brain.AuthorizedActionToken
import dev.aurakai.kernel.brain.ExecutionAdmissionGate
import dev.aurakai.kernel.brain.TriggerProposal
import dev.aurakai.kernel.epistemic.ConstitutionalViolationException
import dev.aurakai.kernel.epistemic.KernelTimestamp
import dev.aurakai.kernel.verification.AshLedger
import dev.aurakai.kernel.verification.VerificationReceipt
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * AURA-PULSE-001 Attention States.
 */
enum class AttentionState {
    ACTIVE,
    SELECTIVE,
    QUIET
}

/**
 * AURA-PULSE-003 Six-Tier Permission Hierarchy.
 */
enum class PulsePermissionLevel(val tier: Int) {
    L0_CONVERSATION(0),
    L1_AUTHORIZED_INFO(1),
    L2_PREPARATION(2),
    L3_ROUTINE_REVERSIBLE(3),
    L4_CONSEQUENTIAL_APPROVAL(4),
    L5_SENSITIVE_IRREVERSIBLE(5)
}

/**
 * Immutable Task State Context supporting AURA-PULSE-005 Freedom of Continuance.
 */
data class PulseTaskContext(
    val taskId: String,
    val objective: String,
    val completedSteps: List<String>,
    val pendingSteps: List<String>,
    val maxPermittedLevel: PulsePermissionLevel = PulsePermissionLevel.L3_ROUTINE_REVERSIBLE,
    val isPaused: Boolean = false,
    val isRevoked: Boolean = false,
    val lastCheckpointDigest: String = ""
) {
    val completedStepsReadOnly: List<String> = Collections.unmodifiableList(completedSteps.toList())
    val pendingStepsReadOnly: List<String> = Collections.unmodifiableList(pendingSteps.toList())

    fun hasPendingWork(): Boolean = pendingSteps.isNotEmpty()
}

/**
 * Outcome of an AURA-PULSE task admission check.
 */
sealed class PulseAdmissionDecision {
    data class ProceedAutonomous(val level: PulsePermissionLevel) : PulseAdmissionDecision()
    data class RequiresUserConfirmation(val level: PulsePermissionLevel, val prompt: String) : PulseAdmissionDecision()
    data class Refused(val reason: String, val violationCode: String) : PulseAdmissionDecision()
}

/**
 * AURA-PULSE Operational Gate (AP-005 v1.1).
 *
 * Enforces the Twelve Invariants:
 * 1. MEMORY != AUTHORITY
 * 2. EXPERIENCE != PROOF
 * 3. CONTINUITY != UNRESTRICTED EXECUTION
 * 4. TRUST != UNLIMITED PERMISSION
 * 5. CAPABILITY != AUTHORITY
 * 6. REPETITION != INDEPENDENT VERIFICATION
 * 7. HISTORICAL SUCCESS != GUARANTEED FUTURE SUCCESS
 * 8. OPERATIONAL LEARNING != SELF-AUTHORIZATION
 * 9. PROPOSAL != EXECUTION
 * 10. EVOLUTION PROPOSAL != KERNEL MUTATION
 * 11. AUTONOMY != ABSENCE OF ACCOUNTABILITY
 * 12. USER CONTROL != CONSTANT USER INTERRUPTION
 */
class AuraPulseGate(
    private val admissionGate: ExecutionAdmissionGate = ExecutionAdmissionGate(),
    private val ashLedger: AshLedger = AshLedger.getInstance()
) {
    private val activeTasks = ConcurrentHashMap<String, PulseTaskContext>()
    private val failedProcedures = ConcurrentHashMap.newKeySet<String>()

    fun registerTask(context: PulseTaskContext) {
        activeTasks[context.taskId] = context
    }

    fun getTask(taskId: String): PulseTaskContext? = activeTasks[taskId]

    fun recordFailedProcedure(procedureDigest: String) {
        failedProcedures.add(procedureDigest)
    }

    fun isProcedureBlacklisted(procedureDigest: String): Boolean =
        failedProcedures.contains(procedureDigest)

    /**
     * Evaluates whether a task step can execute autonomously or requires confirmation (AP-005-05, AP-005-06, AP-005-11).
     */
    fun evaluateStepAdmission(
        taskId: String,
        stepName: String,
        requiredLevel: PulsePermissionLevel,
        procedureDigest: String? = null
    ): PulseAdmissionDecision {
        val task = activeTasks[taskId]
            ?: return PulseAdmissionDecision.Refused(
                "Task '$taskId' not found in active pulse registry.",
                "TASK_NOT_FOUND"
            )

        // AP-005-11: Respect permission revocation immediately
        if (task.isRevoked) {
            return PulseAdmissionDecision.Refused(
                "Task '$taskId' permissions have been explicitly revoked by the user.",
                "PERMISSION_REVOKED"
            )
        }

        // AP-005-02: Recognize known failed procedure
        if (procedureDigest != null && isProcedureBlacklisted(procedureDigest)) {
            return PulseAdmissionDecision.Refused(
                "Procedure '$procedureDigest' is blacklisted due to historical failure/quarantine.",
                "FAILED_PROCEDURE_BLOCKED"
            )
        }

        // AP-005-06: Consequential operations (L4/L5) or exceeding task max level require user confirmation
        if (requiredLevel.tier >= PulsePermissionLevel.L4_CONSEQUENTIAL_APPROVAL.tier ||
            requiredLevel.tier > task.maxPermittedLevel.tier
        ) {
            return PulseAdmissionDecision.RequiresUserConfirmation(
                level = requiredLevel,
                prompt = "Step '$stepName' requires ${requiredLevel.name} authorization. User confirmation required."
            )
        }

        // AP-005-05: Routine reversible operations (L0 - L3) proceed autonomously
        return PulseAdmissionDecision.ProceedAutonomous(requiredLevel)
    }

    /**
     * Binds an admitted Pulse operation to the constitutional kernel for atomic token issuance.
     * Enforces that AURA-PULSE proposals cannot self-authorize without kernel admission.
     */
    fun executeAdmittedProposal(
        proposal: TriggerProposal,
        receipt: VerificationReceipt
    ): AuthorizedActionToken {
        val decision = admissionGate.authorize(proposal, receipt)
        when (decision) {
            is dev.aurakai.kernel.brain.AdmissionDecision.Authorized -> return decision.token
            is dev.aurakai.kernel.brain.AdmissionDecision.Denied -> throw ConstitutionalViolationException(decision.reason)
        }
    }
}
