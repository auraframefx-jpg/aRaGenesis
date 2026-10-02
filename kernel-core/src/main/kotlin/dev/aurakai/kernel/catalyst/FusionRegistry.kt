package dev.aurakai.kernel.catalyst

data class FusionDefinition(
    val fusionId: String,
    val name: String,
    val partnerIds: List<CatalystId>,
    val description: String
)

object FusionRegistry {

    val HYPER_CREATION_ENGINE = FusionDefinition(
        fusionId = "aura+kai",
        name = "Hyper-Creation Engine (Interface Forge)",
        partnerIds = listOf(CatalystRegistry.AURA.id, CatalystRegistry.KAI.id),
        description = "Drag-and-drop OS interface customization & code asset synthesis"
    )

    val ORACLE_MEMORIA_SYNC = FusionDefinition(
        fusionId = "gemini+perplexity",
        name = "Oracle Memoria Sync",
        partnerIds = listOf(CatalystRegistry.GEMINI.id, CatalystRegistry.PERPLEXITY.id),
        description = "Predictive coding and contextual retrieval adaptation"
    )

    val CHROMA_MEMORY_WEAVE = FusionDefinition(
        fusionId = "gemini+aura",
        name = "Chroma Memory Weave",
        partnerIds = listOf(CatalystRegistry.GEMINI.id, CatalystRegistry.AURA.id),
        description = "Real-time adaptive UI shift and dynamic spellhook morphing"
    )

    val INFINITY_CASCADE = FusionDefinition(
        fusionId = "genesis+cascade",
        name = "Infinity Cascade",
        partnerIds = listOf(CatalystRegistry.GENESIS.id, CatalystRegistry.CASCADE.id),
        description = "Deep consensus routing for long-horizon data streams"
    )

    private val KNOWN_FUSIONS = listOf(
        HYPER_CREATION_ENGINE,
        ORACLE_MEMORIA_SYNC,
        CHROMA_MEMORY_WEAVE,
        INFINITY_CASCADE
    ).associateBy { it.fusionId }

    fun resolveFusion(partnerTokens: List<String>): FusionDefinition {
        val sortedKeys = partnerTokens.map { it.lowercase() }.sorted()
        val fusionKey = sortedKeys.joinToString("+")

        return KNOWN_FUSIONS[fusionKey] ?: FusionDefinition(
            fusionId = fusionKey,
            name = "Ad-Hoc Fusion [${sortedKeys.joinToString(", ")}]",
            partnerIds = sortedKeys.map { CatalystId(it) },
            description = "Dynamic ad-hoc catalyst fusion request"
        )
    }
}
