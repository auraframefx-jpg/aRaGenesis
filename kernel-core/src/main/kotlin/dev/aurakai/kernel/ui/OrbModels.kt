package dev.aurakai.kernel.ui

import dev.aurakai.kernel.catalyst.Capability
import dev.aurakai.kernel.catalyst.CatalystId
import dev.aurakai.kernel.catalyst.CatalystRole

data class OrbVisualToken(
    val glyph: String,
    val primaryChannel: String,
    val intensity: Float = 1.0f
)

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    RESPONDING,
    COLLABORATING,
    VERIFYING,
    FUSION
}

data class CatalystDefinition(
    val id: CatalystId,
    val name: String,
    val role: CatalystRole,
    val capabilityScope: Set<Capability>,
    val visualToken: OrbVisualToken,
    val profileVersion: String = "1.0.0"
)

/**
 * Epistemic Telemetry Readout for Synthesis Orb.
 * Displays observed, inferred, proposed, verified, and contradicted states.
 */
data class EpistemicReadout(
    val observed: List<String> = emptyList(),
    val inferred: List<String> = emptyList(),
    val proposed: List<String> = emptyList(),
    val verified: List<String> = emptyList(),
    val contradicted: List<String> = emptyList()
)

/**
 * Synthesis Orb UI Primitive.
 * Visual convergence point for multi-catalyst collaboration and Conference Room activity.
 */
data class SynthesisOrb(
    val id: String = "synth-orb",
    val participatingOrbs: List<OrbVisualToken>,
    val state: OrbState = OrbState.IDLE,
    val readout: EpistemicReadout = EpistemicReadout()
)
