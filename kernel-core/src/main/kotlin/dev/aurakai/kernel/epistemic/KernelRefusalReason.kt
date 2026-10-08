package dev.aurakai.kernel.epistemic

/**
 * Versioned, sealed KernelRefusalReason hierarchy.
 * Prevents callers from inventing generic or un-versioned refusal reasons.
 */
sealed class KernelRefusalReason(
    val code: String,
    val description: String,
    val version: String = "1.0.0"
) {
    object UnauthorizedReceiptConstruction : KernelRefusalReason(
        code = "UNAUTHORIZED_RECEIPT_CONSTRUCTION",
        description = "Verification receipt was constructed outside the authoritative VerificationEngine boundary."
    )

    object ReplayAttackRejection : KernelRefusalReason(
        code = "REPLAY_ATTACK_REJECTION",
        description = "Candidate or epoch nonce has already been consumed or replayed."
    )

    object StaleEpoch : KernelRefusalReason(
        code = "STALE_EPOCH",
        description = "Epoch associated with proposal or receipt does not match the active kernel epoch."
    )

    object InvalidSnapshotBinding : KernelRefusalReason(
        code = "INVALID_SNAPSHOT_BINDING",
        description = "Action snapshot digest does not cryptographically match the verified blueprint snapshot digest."
    )

    object InsufficientProvenance : KernelRefusalReason(
        code = "INSUFFICIENT_PROVENANCE",
        description = "Observation or candidate lacks complete, un-truncated provenance lineage."
    )

    object MissingFalsification : KernelRefusalReason(
        code = "MISSING_FALSIFICATION",
        description = "Candidate explanation lacks an explicit, discriminating falsification trace."
    )

    object IdentityModelCollapse : KernelRefusalReason(
        code = "IDENTITY_MODEL_COLLAPSE",
        description = "Identity reasoning models failed 4-model semantic and cryptographic discrimination."
    )

    object NonDiscriminatingTrace : KernelRefusalReason(
        code = "NON_DISCRIMINATING_TRACE",
        description = "Explanation trace is non-discriminating or fabricated post-conclusion."
    )

    object CharacterAuthorityViolation : KernelRefusalReason(
        code = "CHARACTER_AUTHORITY_VIOLATION",
        description = "Character development tensor was illegally attempted as execution authority."
    )

    object UnboundLineageException : KernelRefusalReason(
        code = "UNBOUND_LINEAGE_EXCEPTION",
        description = "State transition lineage is unbound or missing mandatory scar linkage."
    )

    object QuarantineBypass : KernelRefusalReason(
        code = "QUARANTINE_BYPASS",
        description = "Quarantined state attempted execution admission without authenticated reconciliation."
    )

    object ThermalWallBreach : KernelRefusalReason(
        code = "THERMAL_WALL_BREACH",
        description = "Substrate temperature or non-finite thermal telemetry breached sovereign state freeze boundary (>= 42°C)."
    )

    object ConstitutionMismatch : KernelRefusalReason(
        code = "CONSTITUTION_MISMATCH",
        description = "Proposal or receipt policy digest does not match active kernel constitutional digest."
    )

    data class CustomRefusal(val customCode: String, val customDesc: String) : KernelRefusalReason(
        code = customCode,
        description = customDesc
    )
}
