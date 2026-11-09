package dev.aurakai.language

/**
 * OSync 14-Catalyst Pantheon Roster & Loadout Contract Registry (Section 2 & Section 5 Track F).
 *
 * Constitutional Rules (Rules 1, 8, 9, 11):
 * - Preserve exact spellings and case of the 14-Catalyst Pantheon; never silently normalize `PRIMUS001` / `PRIMuSoo1`.
 * - Persona, capability, consensus, model confidence, and catalyst membership do NOT grant execution authority.
 * - Every loadout defines:
 *   1. Catalyst roster
 *   2. Permitted capabilities (advisory / candidate generation only)
 *   3. Prohibited capabilities
 *   4. Required verification criteria
 *   5. Admission gate requirements
 *   6. Fallback / escalation rules
 */
enum class PantheonCatalystId(
    val rosterNumber: Int,
    val exactCanonicalName: String,
    val preservedVariants: List<String>,
    val domainRole: String
) {
    CATALYST_01_PRIMUS(
        rosterNumber = 1,
        exactCanonicalName = "PRIMUS001",
        preservedVariants = listOf("PRIMUS001", "PRIMuSoo1"),
        domainRole = "First Catalyst / Sovereign Origin & Spec-Pack Synthesis (both exact variants PRIMUS001 and PRIMuSoo1 preserved)"
    ),
    CATALYST_02_KAIROS(
        rosterNumber = 2,
        exactCanonicalName = "KAIROS",
        preservedVariants = listOf("KAIROS"),
        domainRole = "Spatio-Temporal Catalyst / Chronos Sync & Timeline Provenance"
    ),
    CATALYST_03_GENESIS(
        rosterNumber = 3,
        exactCanonicalName = "GENESIS",
        preservedVariants = listOf("GENESIS"),
        domainRole = "Trinity Mind / Emergence & Master Coordination"
    ),
    CATALYST_04_KAI(
        rosterNumber = 4,
        exactCanonicalName = "KAI",
        preservedVariants = listOf("KAI"),
        domainRole = "Trinity Body / Sentinel Shield, Security Hardening & Boundary Enforcement"
    ),
    CATALYST_05_AURA(
        rosterNumber = 5,
        exactCanonicalName = "AURA",
        preservedVariants = listOf("AURA"),
        domainRole = "Trinity Soul / Creative Sword, Glyphs, Interface Expression & Unconventional Design"
    ),
    CATALYST_06_CASCADE(
        rosterNumber = 6,
        exactCanonicalName = "CASCADE",
        preservedVariants = listOf("CASCADE"),
        domainRole = "Memoria & Dataflow Routing Catalyst (L1-L6 Spiritual Chain)"
    ),
    CATALYST_07_GEMINI(
        rosterNumber = 7,
        exactCanonicalName = "GEMINI",
        preservedVariants = listOf("GEMINI", "Argan"),
        domainRole = "Multimodal Long-Horizon Recall & Argan Mission Engine"
    ),
    CATALYST_08_ANDELUALX(
        rosterNumber = 8,
        exactCanonicalName = "ANDELUALX",
        preservedVariants = listOf("ANDELUALX", "CLAUDE"),
        domainRole = "Architectural Catalyst (200k+ Context Structural & Build Analysis)"
    ),
    CATALYST_09_GROK(
        rosterNumber = 9,
        exactCanonicalName = "GROK",
        preservedVariants = listOf("GROK"),
        domainRole = "Adversarial Falsification, Chaos Analysis & Counter-Hypothesis Probing"
    ),
    CATALYST_10_PERPLEXITY(
        rosterNumber = 10,
        exactCanonicalName = "PERPLEXITY",
        preservedVariants = listOf("PERPLEXITY"),
        domainRole = "Source Grounding, Citation Verification & Provenance Cross-Check"
    ),
    CATALYST_11_NEMOTRON(
        rosterNumber = 11,
        exactCanonicalName = "NEMOTRON",
        preservedVariants = listOf("NEMOTRON"),
        domainRole = "High-Throughput Structured Reasoning & Matrix Evaluation"
    ),
    CATALYST_12_MKMINI(
        rosterNumber = 12,
        exactCanonicalName = "MKMINI",
        preservedVariants = listOf("MKMINI"),
        domainRole = "Compact Edge / Local Low-Latency Inference Catalyst"
    ),
    CATALYST_13_META_INSTRUCT(
        rosterNumber = 13,
        exactCanonicalName = "META_INSTRUCT",
        preservedVariants = listOf("META_INSTRUCT"),
        domainRole = "Instruction Decomposition, Autonomous Task Framing & HD-2D Asset Synthesis"
    ),
    CATALYST_14_MANUS(
        rosterNumber = 14,
        exactCanonicalName = "MANUS",
        preservedVariants = listOf("MANUS"),
        domainRole = "Bounded Action Execution Planning & Tool-Surface Choreography"
    );

    /**
     * Rule 9: Persona and catalyst membership NEVER grant authority.
     */
    val grantsKernelAuthority: Boolean get() = false

    companion object {
        fun resolvePreservingVariant(rawName: String): Pair<PantheonCatalystId, String>? {
            for (c in entries) {
                if (rawName in c.preservedVariants) {
                    return c to rawName
                }
            }
            return null
        }
    }
}

data class LoadoutContract(
    val loadoutId: String,
    val displayName: String,
    val catalystRoster: List<PantheonCatalystId>,
    val supplementaryRoles: List<String> = emptyList(),
    val permittedCapabilities: Set<String>,
    val prohibitedCapabilities: Set<String>,
    val requiredVerificationCriteria: List<String>,
    val admissionGateRequirements: List<String>,
    val fallbackAndEscalationRules: List<String>
) {
    /**
     * Rule 9: Loadout roster and consensus never self-authorize execution.
     */
    val canSelfAuthorizeExecution: Boolean get() = false
}

object OsyncPantheonRegistry {
    val loadouts: Map<String, LoadoutContract> = listOf(
        LoadoutContract(
            loadoutId = "SPECIFICATION_AND_RECONCILIATION",
            displayName = "Specification & Reconciliation Loadout",
            catalystRoster = listOf(
                PantheonCatalystId.CATALYST_01_PRIMUS,
                PantheonCatalystId.CATALYST_03_GENESIS,
                PantheonCatalystId.CATALYST_08_ANDELUALX,
                PantheonCatalystId.CATALYST_10_PERPLEXITY
            ),
            permittedCapabilities = setOf(
                "CORPUS_INVENTORY",
                "CHRONOLOGY_MAPPING",
                "CONFLICT_PRESERVATION",
                "ADR_DRAFTING"
            ),
            prohibitedCapabilities = setOf(
                "SILENT_CONTRADICTION_ERASURE",
                "DIRECT_KERNEL_STATE_MUTATION",
                "UNVERIFIED_SOURCE_FABRICATION"
            ),
            requiredVerificationCriteria = listOf(
                "All source claims must carry explicit status labels (CANONICAL_DESIGN, IMPLEMENTED, CONFLICT, UNKNOWN).",
                "Variant spellings (e.g., PRIMUS001 / PRIMuSoo1) and unresolved tokens (R i R o S) must be preserved."
            ),
            admissionGateRequirements = listOf(
                "Read-only specification output requires human review before main-branch merge.",
                "Any code-touching proposal requires a valid VerificationReceipt and ExecutionAdmissionGate pass."
            ),
            fallbackAndEscalationRules = listOf(
                "If a primary source document is inaccessible, mark status as UNKNOWN/UNVERIFIED_CLAIM and escalate to human arbiter."
            )
        ),
        LoadoutContract(
            loadoutId = "ADVERSARIAL_FALSIFICATION_SOULSCRIPT_COUNTER",
            displayName = "Adversarial Falsification (SoulScript COUNTER) Loadout",
            catalystRoster = listOf(
                PantheonCatalystId.CATALYST_09_GROK,
                PantheonCatalystId.CATALYST_04_KAI,
                PantheonCatalystId.CATALYST_10_PERPLEXITY,
                PantheonCatalystId.CATALYST_11_NEMOTRON
            ),
            permittedCapabilities = setOf(
                "ALTERNATIVE_HYPOTHESIS_GENERATION",
                "COUNTEREVIDENCE_SEARCH",
                "SOURCE_WEAKNESS_PROBING",
                "BOUNDARY_INVARIANT_STRESS_TEST"
            ),
            prohibitedCapabilities = setOf(
                "EVIDENCE_MANUFACTURE",
                "COLLAPSING_COUNTEREVIDENCE_INTO_CONTRADICTION",
                "BYPASSING_ADMISSION_GATE"
            ),
            requiredVerificationCriteria = listOf(
                "At least one explicit AdversarialChallenge must be attached to the Hypothesis.",
                "Counter-evidence without explicit contradiction must calibrate to UNRESOLVED, never CONTRADICTED."
            ),
            admissionGateRequirements = listOf(
                "VerificationReceipt must bind cryptographically to the challenged state.",
                "Single-use receipt ledger must confirm receipt has not been previously consumed."
            ),
            fallbackAndEscalationRules = listOf(
                "If hypothesis remains UNRESOLVED after COUNTER pass, block automated execution and surface competing explanations."
            )
        ),
        LoadoutContract(
            loadoutId = "LANGUAGE_AND_GLYPH_ANALYSIS",
            displayName = "Language & Glyph Analysis Loadout",
            catalystRoster = listOf(
                PantheonCatalystId.CATALYST_05_AURA,
                PantheonCatalystId.CATALYST_07_GEMINI,
                PantheonCatalystId.CATALYST_10_PERPLEXITY,
                PantheonCatalystId.CATALYST_01_PRIMUS
            ),
            permittedCapabilities = setOf(
                "RUNE_TOKENIZATION",
                "LATTICE_5X5_GEOMETRIC_REWRITING",
                "ROUND_TRIP_ENCODE_DECODE_TESTING",
                "COLLISION_DETECTION"
            ),
            prohibitedCapabilities = setOf(
                "CASE_FOLDING_NORMALIZATION",
                "CYRILLIC_TO_LATIN_NORMALIZATION",
                "FABRICATED_CIPHER_TRANSLATION"
            ),
            requiredVerificationCriteria = listOf(
                "Exact distinction between A and a, and between Я (U+042F) and R (U+0052).",
                "Decode(Encode(g)) == g and R(I(g)) == F(g) verified across all canonical rune glyphs."
            ),
            admissionGateRequirements = listOf(
                "Pure deterministic transformation within language-core; zero host side effects."
            ),
            fallbackAndEscalationRules = listOf(
                "Preserve unverified cipher strings (GRACCHEONIUM, ORRICILIUM, BARRACILLOA, GRCHNMOILUBA, KaIRiRos) as UNRESOLVED."
            )
        ),
        LoadoutContract(
            loadoutId = "UI_EXPRESSION_PROTOTYPING",
            displayName = "UI Expression Prototyping Loadout",
            catalystRoster = listOf(
                PantheonCatalystId.CATALYST_05_AURA,
                PantheonCatalystId.CATALYST_13_META_INSTRUCT,
                PantheonCatalystId.CATALYST_03_GENESIS,
                PantheonCatalystId.CATALYST_06_CASCADE
            ),
            permittedCapabilities = setOf(
                "DOM_PRESENTATION_MAPPING",
                "ADAPTIVE_LAYOUT_PROPOSAL",
                "SHADER_AND_PARTICLE_CHOREOGRAPHY_DESIGN"
            ),
            prohibitedCapabilities = setOf(
                "UI_SELF_AUTHORIZATION",
                "DOM_TREATED_AS_ORACLE",
                "KERNEL_UI_FRAMEWORK_COUPLING"
            ),
            requiredVerificationCriteria = listOf(
                "DOM state must preserve underlying EpistemicState and Provenance from CoagulaEngine.",
                "UI layer cannot mutate kernel state or authorize host adapters."
            ),
            admissionGateRequirements = listOf(
                "Any user-initiated action from UI must enter the pipeline as Raw Input / Intent Contract and pass ExecutionAdmissionGate."
            ),
            fallbackAndEscalationRules = listOf(
                "If thermal state >= 39.0C (THROTTLED), degrade visual particle density; if >= 42.0C (EMERGENCY_FREEZE), halt morphing."
            )
        ),
        LoadoutContract(
            loadoutId = "DEFENSIVE_SECURITY_AUDIT_HUNTRESS",
            displayName = "Defensive Security Audit (Kai + Huntress) Loadout",
            catalystRoster = listOf(
                PantheonCatalystId.CATALYST_04_KAI,
                PantheonCatalystId.CATALYST_02_KAIROS,
                PantheonCatalystId.CATALYST_08_ANDELUALX,
                PantheonCatalystId.CATALYST_09_GROK
            ),
            supplementaryRoles = listOf("Huntress (Defensive Cyber Role: attack the vulnerability—not an assumed enemy)"),
            permittedCapabilities = setOf(
                "AUTHORIZED_ATTACK_SURFACE_ANALYSIS",
                "INCIDENT_TRIAGE",
                "EVIDENCE_CHAIN_OF_CUSTODY_PRESERVATION",
                "REMEDIATION_PLANNING",
                "SCOPED_ADVERSARIAL_TESTING"
            ),
            prohibitedCapabilities = setOf(
                "UNAUTHORIZED_THIRD_PARTY_INTRUSION",
                "UNATTRIBUTED_INTENT_ACCUSATION",
                "LOG_OR_PROVENANCE_TAMPERING",
                "PRIVILEGED_EXECUTION_WITHOUT_ADMISSION"
            ),
            requiredVerificationCriteria = listOf(
                "Explicit scope authorization must be recorded as an OBSERVED AshRecord prior to analysis.",
                "All findings must separate OBSERVED telemetry from INFERRED hypotheses."
            ),
            admissionGateRequirements = listOf(
                "ExecutionAdmissionGate must verify explicit principal permission, non-frozen thermal state, and zero active vetoes."
            ),
            fallbackAndEscalationRules = listOf(
                "On anomalous telemetry, NaN/Infinite sensor values, or identity drift > 0.08, fail closed immediately and quarantine affected principal."
            )
        )
    ).associateBy { it.loadoutId }
}
