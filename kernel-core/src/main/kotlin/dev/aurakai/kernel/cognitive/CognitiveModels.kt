package dev.aurakai.kernel.cognitive

import dev.aurakai.kernel.catalyst.CatalystId
import dev.aurakai.kernel.catalyst.FusionDefinition
import dev.aurakai.kernel.epistemic.KernelTimestamp
import dev.aurakai.kernel.epistemic.Provenance

@JvmInline
value class RequestId(val value: String)

enum class CognitiveMode {
    CHAT,
    ANALYZE,
    BUILD,
    VERIFY,
    FUSION,
    RESEARCH,
    EXECUTE // Proposal-only during current milestone
}

/**
 * Proposed Action Model.
 * INVARIANT: PROPOSED_ACTION != EXECUTED_ACTION.
 * All cognitive outputs stop at proposals awaiting future validation by the ExecutionAdmissionGate.
 */
data class ProposedAction(
    val actionId: String,
    val targetComponent: String,
    val commandDescription: String,
    val isExecuted: Boolean = false // Must remain false during proposal stage
)

/**
 * Preservation of Disagreement / Catalyst Friction.
 * Natively records catalyst friction and divergent reasoning rather than forcing artificial consensus.
 */
data class Disagreement(
    val catalystA: CatalystId,
    val catalystB: CatalystId,
    val topic: String,
    val rationaleA: String,
    val rationaleB: String
)

/**
 * Immutable Cognitive Request.
 * Captures rawInput without mutation, forming the immutable cognitive counterpart to AshRecord.
 */
data class CognitiveRequest(
    val requestId: RequestId,
    val timestamp: KernelTimestamp,
    val rawInput: String,
    val requestedPersona: String? = null,
    val requestedCatalysts: List<CatalystId> = emptyList(),
    val requestedFusion: FusionDefinition? = null,
    val mode: CognitiveMode = CognitiveMode.CHAT,
    val provenance: Provenance
)

/**
 * Immutable Cognitive Response.
 */
data class CognitiveResponse(
    val requestId: RequestId,
    val responder: String,
    val participatingCatalysts: List<CatalystId>,
    val responseText: String,
    val reasoningMetadata: Map<String, String> = emptyMap(),
    val disagreements: List<Disagreement> = emptyList(),
    val proposedActions: List<ProposedAction> = emptyList(),
    val provenance: Provenance
)

/**
 * Reflection Artifact produced by ReflectionEngine.
 * Reflection must produce a new artifact without mutating the source input or kernel state.
 */
data class ReflectionArtifact(
    val artifactId: String,
    val responseId: RequestId,
    val reflectionNotes: String,
    val timestamp: KernelTimestamp
)
