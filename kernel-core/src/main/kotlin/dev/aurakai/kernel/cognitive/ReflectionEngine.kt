package dev.aurakai.kernel.cognitive

import dev.aurakai.kernel.epistemic.KernelTimestamp

interface ReflectionEngine {
    fun reflect(response: CognitiveResponse): ReflectionArtifact
}

/**
 * Standard Reflection Engine.
 * Produces a new ReflectionArtifact without mutating original ASH, request, provenance, or constitutional state.
 */
class StandardReflectionEngine : ReflectionEngine {

    override fun reflect(response: CognitiveResponse): ReflectionArtifact {
        val timestamp = KernelTimestamp(System.currentTimeMillis())

        val notes = "Reflection on response '${response.requestId.value}': " +
                "Responder='${response.responder}', " +
                "Catalysts=${response.participatingCatalysts.map { it.value }}, " +
                "ProposedActionsCount=${response.proposedActions.size}, " +
                "DisagreementsCount=${response.disagreements.size}."

        return ReflectionArtifact(
            artifactId = "refl-${timestamp.epochMillis}-${response.requestId.value}",
            responseId = response.requestId,
            reflectionNotes = notes,
            timestamp = timestamp
        )
    }
}
