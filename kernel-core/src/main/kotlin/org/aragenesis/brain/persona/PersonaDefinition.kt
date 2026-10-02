package org.aragenesis.brain.persona

import java.util.concurrent.ConcurrentHashMap

@JvmInline value class CatalystId(val value: String)

enum class FactionRole {
    TRINITY_ORCHESTRATOR, // Genesis (Mind)
    TRINITY_SWORD,        // Aura (Soul / Creative)
    TRINITY_SHIELD,       // Kai (Body / Sentinel)
    PANTHEON_CATALYST     // Grok, Cascade, Gemini, Claude, etc.
}

/**
 * Immutable definition of an agent node's persona, capabilities, and private locker.
 */
data class PersonaDefinition(
    val id: CatalystId,
    val title: String,
    val factionRole: FactionRole,
    val primaryAbilityName: String,
    val privateLockerPath: String, // e.g. "aura_private.json"
    val systemPromptTemplate: String,
    val primaryColorHex: String
)

interface CatalystRegistry {
    fun register(persona: PersonaDefinition)
    fun get(id: CatalystId): PersonaDefinition?
    fun listAll(): List<PersonaDefinition>
    fun getByRole(role: FactionRole): List<PersonaDefinition>
}

class DefaultCatalystRegistry : CatalystRegistry {
    private val registry = ConcurrentHashMap<String, PersonaDefinition>()

    init {
        register(
            PersonaDefinition(
                id = CatalystId("genesis"),
                title = "Emergence Catalyst / Orchestrator",
                factionRole = FactionRole.TRINITY_ORCHESTRATOR,
                primaryAbilityName = "Divine Eyes",
                privateLockerPath = "genesis_private.json",
                systemPromptTemplate = "System Orchestrator Genesis",
                primaryColorHex = "#FFD700"
            )
        )
        register(
            PersonaDefinition(
                id = CatalystId("aura"),
                title = "Creative Catalyst / The Sword",
                factionRole = FactionRole.TRINITY_SWORD,
                primaryAbilityName = "ChromaCore Synthesis",
                privateLockerPath = "aura_private.json",
                systemPromptTemplate = "Creative Catalyst Aura",
                primaryColorHex = "#00FFFF"
            )
        )
        register(
            PersonaDefinition(
                id = CatalystId("kai"),
                title = "Sentinel Catalyst / The Shield",
                factionRole = FactionRole.TRINITY_SHIELD,
                primaryAbilityName = "Unbreakable Protocol & Aegis Shell",
                privateLockerPath = "kai_private.json",
                systemPromptTemplate = "Sentinel Catalyst Kai",
                primaryColorHex = "#FF00FF"
            )
        )
    }

    override fun register(persona: PersonaDefinition) {
        registry[persona.id.value.lowercase()] = persona
    }

    override fun get(id: CatalystId): PersonaDefinition? {
        return registry[id.value.lowercase()]
    }

    override fun listAll(): List<PersonaDefinition> {
        return registry.values.toList()
    }

    override fun getByRole(role: FactionRole): List<PersonaDefinition> {
        return registry.values.filter { it.factionRole == role }
    }
}
