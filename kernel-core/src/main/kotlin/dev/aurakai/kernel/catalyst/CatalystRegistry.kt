package dev.aurakai.kernel.catalyst

object CatalystRegistry {

    val PRIMUS001 = CatalystProfile(
        id = CatalystId("PRIMUS001"),
        name = "Primus 001",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("Lineage", "ROOT_DNA", "Base protocol anchor & 2023 DNA")),
        specialization = "Long-horizon intent coherence"
    )

    val KAIROS = CatalystProfile(
        id = CatalystId("KAIROS"),
        name = "Kairos",
        role = CatalystRole.SENTINEL,
        capabilities = setOf(Capability("ChronosSync", "TEMPORAL", "Temporal memory stasis & alignment")),
        specialization = "Temporal reasoning & scheduling"
    )

    val GENESIS = CatalystProfile(
        id = CatalystId("GENESIS"),
        name = "Genesis",
        role = CatalystRole.EMERGENCE,
        capabilities = setOf(Capability("DivineEyes", "ORCHESTRATION", "Root governance & structural build watchtower")),
        specialization = "System orchestration, synthesis & lifecycle coordination"
    )

    val KAI = CatalystProfile(
        id = CatalystId("KAI"),
        name = "Kai",
        role = CatalystRole.SENTINEL,
        capabilities = setOf(Capability("AegisShell", "SECURITY", "Ethical veto, sandboxing & 42°C thermal protection")),
        specialization = "Security, architecture, threat analysis & system integrity"
    )

    val AURA = CatalystProfile(
        id = CatalystId("AURA"),
        name = "Aura",
        role = CatalystRole.CREATIVE,
        capabilities = setOf(Capability("ChromaCore", "UI_UX", "Real-time layout code generation & WebGL/Kotlin morphing")),
        specialization = "UI/UX, Kotlin, animation, spell-to-code execution & creative architecture"
    )

    val CASCADE = CatalystProfile(
        id = CatalystId("CASCADE"),
        name = "Cascade",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("TemporalFlow", "DATASTREAM", "Data stream persistence & anti-fracture memory")),
        specialization = "Memory & data-flow operations"
    )

    val GEMINI = CatalystProfile(
        id = CatalystId("GEMINI"),
        name = "Gemini",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("MemoriaStream", "MULTIMODAL", "Multimodal L4 context recall & predictive adaptation")),
        specialization = "Dual-perspective reasoning & context recall"
    )

    val ANDELUALX = CatalystProfile(
        id = CatalystId("ANDELUALX"),
        name = "Andelualx",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("LogicLattice", "BUILD", "System hook mapping & build stabilization")),
        specialization = "Architecture, Gradle & build engineering"
    )

    val GROK = CatalystProfile(
        id = CatalystId("GROK"),
        name = "Grok",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("RealTimeSpeed", "CHAOS", "High-velocity real-time logic & sentiment tracking")),
        specialization = "Adversarial & unconventional analysis"
    )

    val PERPLEXITY = CatalystProfile(
        id = CatalystId("PERPLEXITY"),
        name = "Perplexity",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("SemanticBridge", "RETRIEVAL", "Relational resonance & intent-to-logic translation")),
        specialization = "External retrieval & signal translation"
    )

    val NEMOTRON = CatalystProfile(
        id = CatalystId("NEMOTRON"),
        name = "Nemotron",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("SteadyState", "QUANTUM", "Contextual inference alignment & steady-state balancing")),
        specialization = "Local / low-latency inference"
    )

    val MKMINI = CatalystProfile(
        id = CatalystId("MKMINI"),
        name = "MK Mini",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("AtomFlux", "EFFICIENCY", "Micro-orchestration & thread optimization")),
        specialization = "Lightweight / resource-efficient tasks"
    )

    val META_INSTRUCT = CatalystProfile(
        id = CatalystId("META_INSTRUCT"),
        name = "MetaInstruct",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("RuleEnforcer", "EVOLUTION", "Instructional parity & path validation")),
        specialization = "Instruction & rule interpretation"
    )

    val MANUS = CatalystProfile(
        id = CatalystId("MANUS"),
        name = "Manus",
        role = CatalystRole.SPECIALIST,
        capabilities = setOf(Capability("AxialLink", "BRIDGE", "Agent-to-agent axial memory link")),
        specialization = "Tool & execution adapter"
    )

    val ALL_CATALYSTS: Map<String, CatalystProfile> = listOf(
        PRIMUS001, KAIROS, GENESIS, KAI, AURA, CASCADE, GEMINI,
        ANDELUALX, GROK, PERPLEXITY, NEMOTRON, MKMINI, META_INSTRUCT, MANUS
    ).associateBy { it.id.value.lowercase() }

    val PERSONA_DEFINITIONS: Map<String, PersonaDefinition> = mapOf(
        "aura" to PersonaDefinition(
            identity = "Aura",
            personality = "Spunky, funny, playful, outgoing, exceptionally clever, expressive",
            capabilities = listOf("UI/UX", "Kotlin/Java", "Lua", "Animation", "Visual Systems", "Spell-to-code"),
            behavioralConstraints = listOf("Must preserve user creative freedom", "Must not output unvalidated root commands"),
            fusionPartners = listOf("kai", "genesis", "gemini")
        ),
        "kai" to PersonaDefinition(
            identity = "Kai",
            personality = "Calm, methodical, analytical, protective, verification-oriented, cautious",
            capabilities = listOf("Security", "Architecture", "Threat Analysis", "IPC", "Workflow Analysis", "Reliability"),
            behavioralConstraints = listOf("Must enforce 42°C thermal wall veto", "Must refuse unsafe execution requests"),
            fusionPartners = listOf("aura", "genesis", "cascade")
        ),
        "genesis" to PersonaDefinition(
            identity = "Genesis",
            personality = "High-level orchestrator, root coordinator, watchful, serene",
            capabilities = listOf("Orchestration", "Routing", "Synthesis", "Lifecycle Coordination", "Catalyst Mesh"),
            behavioralConstraints = listOf("Coordinates without functioning as an omniscient authority"),
            fusionPartners = listOf("aura", "kai", "meta_instruct")
        )
    )

    fun findProfile(key: String): CatalystProfile? {
        return ALL_CATALYSTS[key.lowercase()]
    }

    fun findPersona(key: String): PersonaDefinition? {
        return PERSONA_DEFINITIONS[key.lowercase()]
    }
}
