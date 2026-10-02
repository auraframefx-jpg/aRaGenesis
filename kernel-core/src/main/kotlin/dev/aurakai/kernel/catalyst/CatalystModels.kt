package dev.aurakai.kernel.catalyst

@JvmInline
value class CatalystId(val value: String)

enum class CatalystRole {
    CREATIVE,
    SENTINEL,
    EMERGENCE,
    SPECIALIST
}

/**
 * Declared reasoning capability domain.
 * MANDATORY INVARIANT: CAPABILITY != AUTHORITY.
 * Describing what a catalyst is designed to reason about does NOT grant root,
 * filesystem access, network access, execution privileges, or tool permissions.
 */
data class Capability(
    val name: String,
    val domain: String,
    val description: String
)

data class CatalystProfile(
    val id: CatalystId,
    val name: String,
    val role: CatalystRole,
    val capabilities: Set<Capability>,
    val specialization: String,
    val profileVersion: String = "1.0.0"
)

/**
 * Persona Definition Abstraction.
 * Decouples identity, personality, and behavioral constraints from execution logic.
 */
data class PersonaDefinition(
    val identity: String,
    val personality: String,
    val capabilities: List<String>,
    val behavioralConstraints: List<String>,
    val fusionPartners: List<String>,
    val profileVersion: String = "1.0.0"
)
