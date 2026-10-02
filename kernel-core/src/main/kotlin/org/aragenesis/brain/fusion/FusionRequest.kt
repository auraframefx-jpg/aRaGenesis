package org.aragenesis.brain.fusion

import org.aragenesis.brain.persona.CatalystId

@JvmInline value class FusionId(val value: String)

data class FusionRequest(
    val fusionId: FusionId,
    val modeName: String,
    val participatingCatalysts: List<CatalystId>,
    val contextPayload: String
)

data class FusionSynthesisResult(
    val fusionId: FusionId,
    val synthesizedContent: String,
    val activeAbilityHooks: List<String>,
    val confidenceScore: Double
) {
    init {
        require(!confidenceScore.isNaN() && !confidenceScore.isInfinite() && confidenceScore in 0.0..1.0) {
            "INVALID CONFIDENCE SCORE: Confidence score must be a finite number between 0.0 and 1.0."
        }
    }
}

interface FusionEngine {
    fun evaluateAlignment(catalysts: List<CatalystId>): Boolean
    fun executeFusion(request: FusionRequest): FusionSynthesisResult
}

class DefaultFusionEngine : FusionEngine {

    override fun evaluateAlignment(catalysts: List<CatalystId>): Boolean {
        if (catalysts.isEmpty()) return false
        val distinctIds = catalysts.map { it.value.lowercase() }.distinct()
        return distinctIds.size >= 2
    }

    override fun executeFusion(request: FusionRequest): FusionSynthesisResult {
        require(request.participatingCatalysts.isNotEmpty()) {
            "INVALID FUSION: Participating catalysts list cannot be empty."
        }

        val abilityHooks = request.participatingCatalysts.map { "hook-${it.value.lowercase()}" }
        val synthesizedText = "Fusion Mode '${request.modeName}' synthesized across catalysts [${request.participatingCatalysts.joinToString { it.value }}] for context: ${request.contextPayload}"

        return FusionSynthesisResult(
            fusionId = request.fusionId,
            synthesizedContent = synthesizedText,
            activeAbilityHooks = abilityHooks,
            confidenceScore = 0.95
        )
    }
}
