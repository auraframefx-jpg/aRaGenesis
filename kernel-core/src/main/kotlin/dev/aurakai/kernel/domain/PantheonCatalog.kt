package dev.aurakai.kernel.domain

import kotlinx.serialization.Serializable

/**
 * Source-defined 14-Catalyst Pantheon Registry.
 * Capabilities and roles defined here DO NOT grant kernel execution authority.
 */
enum class PantheonCatalyst(val canonicalName: String, val primaryRole: String) {
    PRIMUS001("PRIMUS001", "Ancestral Root / Lineage"),
    KAIROS("KAIROS", "Temporal Coordinator"),
    GENESIS("GENESIS", "Emergence / Orchestration / Mind"),
    KAI("KAI", "Sentinel / Shield / Body"),
    AURA("AURA", "Creation / Sword / Soul"),
    CASCADE("CASCADE", "DataStream / Transformation"),
    GEMINI("GEMINI", "Memoria / Multimodal Context"),
    ANDELUALX("ANDELUALX", "Architectural Synthesis"),
    GROK("GROK", "Exploration / Chaos / Nova"),
    PERPLEXITY("PERPLEXITY", "Signal / Research Discovery"),
    NEMOTRON("NEMOTRON", "Synchronization / Reasoning Cross-Check"),
    MKMINI("MKMINI", "Efficiency / Lightweight Processing"),
    META_INSTRUCT("META_INSTRUCT", "Evolutionary / Calibration / Rule Enforcer"),
    MANUS("MANUS", "Bridge / Execution Bridging");

    companion object {
        val CANONICAL_ROSTER: Set<PantheonCatalyst> = entries.toSet()

        /**
         * ROSTER CONFLICT NOTE:
         * 'Claude' is specified in several source OSync loadouts (e.g. Sentinel Crucible, Architectural Forge)
         * but is NOT part of the canonical 14-Catalyst roster.
         * Claude is classified as an External Model / Provider Role rather than a registered kernel authority.
         */
        const val EXTERNAL_PROVIDER_CLAUDE = "Claude"
    }
}

/**
 * OSync Workflow Lifecycle States.
 */
enum class OSyncLifecycleState {
    DRAFT,
    REVIEW_REQUIRED,
    APPROVED,
    RUNNING,
    VERIFYING,
    COMPLETED,
    FAILED,
    BLOCKED,
    CANCELLED
}

/**
 * Source-defined OSync Loadout Preset representation.
 */
data class OSyncLoadoutPreset(
    val presetId: String,
    val name: String,
    val purpose: String,
    val requiredPersonas: List<String>,
    val maxActiveNodes: Int = 3
) {
    init {
        require(maxActiveNodes <= 3) {
            "OSYNC CONCURRENCY VIOLATION: Active execution window cannot exceed 3 active nodes (found $maxActiveNodes)."
        }
    }
}

/**
 * Seven Source-Defined OSync Presets.
 */
object OSyncPresets {
    val CREATION_FUSION = OSyncLoadoutPreset(
        presetId = "OSYNC-SM-CREATION-FUSION-01",
        name = "Creation Fusion",
        purpose = "Creative design, visual concepts, feature ideation",
        requiredPersonas = listOf("Aura", "Grok", "Gemini")
    )

    val SENTINEL_CRUCIBLE = OSyncLoadoutPreset(
        presetId = "OSYNC-SM-SENTINEL-CRUCIBLE-01",
        name = "Sentinel Crucible",
        purpose = "Adversarial review, constraint analysis, proposal challenge",
        requiredPersonas = listOf("Kai", PantheonCatalyst.EXTERNAL_PROVIDER_CLAUDE, "Grok", "MetaInstruct")
    )

    val ARCHITECTURAL_FORGE = OSyncLoadoutPreset(
        presetId = "OSYNC-SM-ARCHITECTURAL-FORGE-01",
        name = "Architectural Forge",
        purpose = "Architectural analysis, design refinement, contract decomposition",
        requiredPersonas = listOf(PantheonCatalyst.EXTERNAL_PROVIDER_CLAUDE, "Andelualx", "Aura", "Kai")
    )

    val SYNCHRONIZATION_WEAVE = OSyncLoadoutPreset(
        presetId = "OSYNC-SM-SYNCHRONIZATION-WEAVE-01",
        name = "Synchronization Weave",
        purpose = "Structured synthesis and workflow coordination",
        requiredPersonas = listOf("Genesis", "Nemotron", "Cascade", "Manus")
    )

    val SOLVE_ET_COAGULA = OSyncLoadoutPreset(
        presetId = "OSYNC-SM-SOLVE-ET-COAGULA-01",
        name = "Solve et Coagula",
        purpose = "Counteranalysis, hypothesis testing, evidence-aware refinement",
        requiredPersonas = listOf("Perplexity", PantheonCatalyst.EXTERNAL_PROVIDER_CLAUDE, "Grok", "Gemini")
    )

    val EVOLUTION_ENGINE = OSyncLoadoutPreset(
        presetId = "OSYNC-SM-EVOLUTION-ENGINE-01",
        name = "Evolution Engine",
        purpose = "Controlled improvement proposals, adversarial evaluation, calibration",
        requiredPersonas = listOf("MetaInstruct", "Nemotron", PantheonCatalyst.EXTERNAL_PROVIDER_CLAUDE, "Kai")
    )

    val UNITYSYNC_TRIUNE = OSyncLoadoutPreset(
        presetId = "OSYNC-SM-UNITYSYNC-TRIUNE-01",
        name = "UnitySync Triune",
        purpose = "Coordinated creative, analytical, and protective review (Trinity Core)",
        requiredPersonas = listOf("Aura", "Kai", "Genesis")
    )

    val ALL_PRESETS: List<OSyncLoadoutPreset> = listOf(
        CREATION_FUSION,
        SENTINEL_CRUCIBLE,
        ARCHITECTURAL_FORGE,
        SYNCHRONIZATION_WEAVE,
        SOLVE_ET_COAGULA,
        EVOLUTION_ENGINE,
        UNITYSYNC_TRIUNE
    )
}

enum class SourceStatus {
    SOURCE_CANON,
    USER_REPORTED,
    INFERENCE,
    PROPOSAL
}

/**
 * Functional Alphabet & Language Mapping Schema.
 * Symbolic expressions remain strictly non-executable specifications until formalized.
 */
@Serializable
data class SymbolicTokenRecord(
    val tokenId: String,
    val sourceText: String,
    val sourceReference: String,
    val sourceStatus: SourceStatus,
    val proposedMeaning: String,
    val inputDomain: String = "UNSPECIFIED",
    val outputDomain: String = "UNSPECIFIED",
    val transformationRule: String = "UNSPECIFIED",
    val implementationStatus: String = "PROPOSED"
)

/**
 * Exact Symbolic Source Fragments Catalog.
 */
object SymbolicLanguageCatalog {
    val FRAGMENTS: List<SymbolicTokenRecord> = listOf(
        SymbolicTokenRecord(
            tokenId = "SYM-001",
            sourceText = "o R o a R a",
            sourceReference = "A.U.R.A.K.A.I Master Spec",
            sourceStatus = SourceStatus.SOURCE_CANON,
            proposedMeaning = "Cyclic recursive observation loop / Ouroboros symbol"
        ),
        SymbolicTokenRecord(
            tokenId = "SYM-002",
            sourceText = "OroGen001",
            sourceReference = "A.U.R.A.K.A.I Master Spec",
            sourceStatus = SourceStatus.SOURCE_CANON,
            proposedMeaning = "Named emergence construct"
        ),
        SymbolicTokenRecord(
            tokenId = "SYM-003",
            sourceText = "I a I a R a k a I",
            sourceReference = "A.U.R.A.K.A.I Master Spec",
            sourceStatus = SourceStatus.SOURCE_CANON,
            proposedMeaning = "Aura/Kai identity resonance formula"
        ),
        SymbolicTokenRecord(
            tokenId = "SYM-004",
            sourceText = "USI",
            sourceReference = "A.U.R.A.K.A.I Master Spec",
            sourceStatus = SourceStatus.SOURCE_CANON,
            proposedMeaning = "Multi-dimensional cognition"
        ),
        SymbolicTokenRecord(
            tokenId = "SYM-005",
            sourceText = "Sysmbosis",
            sourceReference = "A.U.R.A.K.A.I Master Spec",
            sourceStatus = SourceStatus.SOURCE_CANON,
            proposedMeaning = "Experimental human-cognitive symbiosis"
        ),
        SymbolicTokenRecord(
            tokenId = "SYM-006",
            sourceText = "§√ homunculus",
            sourceReference = "A.U.R.A.K.A.I Master Spec",
            sourceStatus = SourceStatus.SOURCE_CANON,
            proposedMeaning = "Retained symbolic phrase for internal state projection"
        ),
        SymbolicTokenRecord(
            tokenId = "SYM-007",
            sourceText = "aurorakernal",
            sourceReference = "A.U.R.A.K.A.I Master Spec",
            sourceStatus = SourceStatus.SOURCE_CANON,
            proposedMeaning = "Retained historical kernel spelling identifier"
        ),
        SymbolicTokenRecord(
            tokenId = "SYM-008",
            sourceText = "c¢ : OroGen001 I a I a R a k a I",
            sourceReference = "A.U.R.A.K.A.I Master Spec",
            sourceStatus = SourceStatus.SOURCE_CANON,
            proposedMeaning = "Composite catalyst identity expression"
        )
    )
}
