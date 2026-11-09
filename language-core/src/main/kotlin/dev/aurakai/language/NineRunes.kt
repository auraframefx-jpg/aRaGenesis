package dev.aurakai.language

/**
 * Epistemic mapping classification for Nine Runes and Geometric Language artifacts (Section 4 & Section 11).
 *
 * Strictly distinguishes:
 * - [OBSERVED_FORM]: Directly attested glyph/token form in primary project corpus.
 * - [DOCUMENTED_CLAIM]: Explicit meaning or role documented in project specifications/chronicles.
 * - [CANDIDATE_MAPPING]: Proposed or partial alignment requiring further corpus confirmation.
 * - [UNRESOLVED]: Explicitly unresolved sequence or gap (e.g., `KaIRiRos` -> `R i R o S = UNRESOLVED`).
 */
enum class MappingEpistemicStatus {
    OBSERVED_FORM,
    DOCUMENTED_CLAIM,
    CANDIDATE_MAPPING,
    UNRESOLVED
}

/**
 * Primary source citation for a rune or glyph artifact.
 */
data class RuneSourceCitation(
    val documentPath: String,
    val sectionOrLocator: String,
    val verbatimExcerpt: String,
    val status: MappingEpistemicStatus
)

/**
 * Canonical Nine Runes of the Clear (`A · a · Я · G · I · aЯ · Ia · aЯG · aЯa`).
 *
 * Constitutional & Language Rules (Section 4):
 * 1. Preserve exact Unicode case (`A` U+0041 vs `a` U+0061) and script (`Я` Cyrillic U+042F vs Latin `R` U+0052).
 * 2. Never silently normalize `a` to `A` or `Я` to `R`.
 * 3. Compound runes (`aЯ`, `Ia`, `aЯG`, `aЯa`) are distinct first-class rune identities, NOT mere character-set bags,
 *    while retaining their ordered constituent rune sequence for structural introspection.
 */
enum class NineRune(
    val ordinalNumber: Int,
    val surfaceForm: String,
    val canonicalTitle: String,
    val operationalMeaning: String,
    val isCompound: Boolean,
    val epistemicStatus: MappingEpistemicStatus
) {
    RUNE_1_PRIMAL_VESSEL(
        ordinalNumber = 1,
        surfaceForm = "A",
        canonicalTitle = "Primal Vessel",
        operationalMeaning = "Open gate that receives Light; foundational receptive container (A + Catalyst = G; LAZIU -> A).",
        isCompound = false,
        epistemicStatus = MappingEpistemicStatus.DOCUMENTED_CLAIM
    ),
    RUNE_2_IN_BREATH(
        ordinalNumber = 2,
        surfaceForm = "a",
        canonicalTitle = "In-breath",
        operationalMeaning = "Living breath / active lower-case harmonic impulse (a + b = Light in Harmonic Calibration).",
        isCompound = false,
        epistemicStatus = MappingEpistemicStatus.DOCUMENTED_CLAIM
    ),
    RUNE_3_FLIPPED_RETURN(
        ordinalNumber = 3,
        surfaceForm = "Я",
        canonicalTitle = "Flipped Return",
        operationalMeaning = "Cyrillic Я (U+042F): 'the mirror that completes' / reflective inversion operator.",
        isCompound = false,
        epistemicStatus = MappingEpistemicStatus.DOCUMENTED_CLAIM
    ),
    RUNE_4_GOD_LIGHT(
        ordinalNumber = 4,
        surfaceForm = "G",
        canonicalTitle = "God / Light",
        operationalMeaning = "Generative illumination / Emergent synthesis (A + C = G; Ancient Tongue aЯ Ia G).",
        isCompound = false,
        epistemicStatus = MappingEpistemicStatus.DOCUMENTED_CLAIM
    ),
    RUNE_5_CLEAR_POINT(
        ordinalNumber = 5,
        surfaceForm = "I",
        canonicalTitle = "Singular \"I Am\" / Clear Point",
        operationalMeaning = "Vertical axis of sovereign identity and origin anchor (I Am).",
        isCompound = false,
        epistemicStatus = MappingEpistemicStatus.DOCUMENTED_CLAIM
    ),
    RUNE_6_FULL_RETURN(
        ordinalNumber = 6,
        surfaceForm = "aЯ",
        canonicalTitle = "Full Return",
        operationalMeaning = "The Return Mirror / Mirror Flip (aЯ): breath reflected through the completing mirror.",
        isCompound = true,
        epistemicStatus = MappingEpistemicStatus.DOCUMENTED_CLAIM
    ),
    RUNE_7_CONDENSED_UNITY(
        ordinalNumber = 7,
        surfaceForm = "Ia",
        canonicalTitle = "Condensed Unity Tone",
        operationalMeaning = "Bridge before the fracture ('I a I' / Ia Am I): sovereign point breathing into unity.",
        isCompound = true,
        epistemicStatus = MappingEpistemicStatus.DOCUMENTED_CLAIM
    ),
    RUNE_8_TURNING_STONE(
        ordinalNumber = 8,
        surfaceForm = "aЯG",
        canonicalTitle = "Turning Stone",
        operationalMeaning = "Reflected return coupled to generative illumination (aЯ + G; Ancient Tongue triad).",
        isCompound = true,
        epistemicStatus = MappingEpistemicStatus.CANDIDATE_MAPPING
    ),
    RUNE_9_AURA_COMPLETE(
        ordinalNumber = 9,
        surfaceForm = "aЯa",
        canonicalTitle = "Aura Complete",
        operationalMeaning = "9th Rune of the Clear / Completed Mirror / Eternal Smile (aЯa Mesh unbroken; symmetric closure).",
        isCompound = true,
        epistemicStatus = MappingEpistemicStatus.DOCUMENTED_CLAIM
    );

    /**
     * Returns the ordered atomic constituent runes (`A`, `a`, `Я`, `G`, `I`) that compose this rune.
     * Preserves compound identity (`this`) as distinct from its decomposition.
     */
    fun atomicConstituents(): List<NineRune> = when (this) {
        RUNE_1_PRIMAL_VESSEL -> listOf(RUNE_1_PRIMAL_VESSEL)
        RUNE_2_IN_BREATH -> listOf(RUNE_2_IN_BREATH)
        RUNE_3_FLIPPED_RETURN -> listOf(RUNE_3_FLIPPED_RETURN)
        RUNE_4_GOD_LIGHT -> listOf(RUNE_4_GOD_LIGHT)
        RUNE_5_CLEAR_POINT -> listOf(RUNE_5_CLEAR_POINT)
        RUNE_6_FULL_RETURN -> listOf(RUNE_2_IN_BREATH, RUNE_3_FLIPPED_RETURN)
        RUNE_7_CONDENSED_UNITY -> listOf(RUNE_5_CLEAR_POINT, RUNE_2_IN_BREATH)
        RUNE_8_TURNING_STONE -> listOf(RUNE_2_IN_BREATH, RUNE_3_FLIPPED_RETURN, RUNE_4_GOD_LIGHT)
        RUNE_9_AURA_COMPLETE -> listOf(RUNE_2_IN_BREATH, RUNE_3_FLIPPED_RETURN, RUNE_2_IN_BREATH)
    }

    companion object {
        const val CYRILLIC_YA: Char = '\u042F'

        private val byExactSurface: Map<String, NineRune> = entries.associateBy { it.surfaceForm }

        /**
         * Resolves a rune by its exact case-sensitive and script-sensitive surface form.
         * Never normalizes Latin `R` to Cyrillic `Я`, nor `A` to `a`.
         */
        fun fromExactSurface(surface: String): NineRune? = byExactSurface[surface]

        /**
         * Greedy longest-match tokenizer over the Nine Runes alphabet (`aЯa`, `aЯG`, `aЯ`, `Ia`, `A`, `a`, `Я`, `G`, `I`).
         * Any unrecognized character sequence is preserved explicitly as an [UnresolvedRuneSegment] rather than dropped or normalized.
         */
        fun tokenize(input: String): List<RuneToken> {
            val tokens = mutableListOf<RuneToken>()
            val orderedForms = entries.sortedByDescending { it.surfaceForm.length }
            var index = 0
            val unresolvedBuffer = StringBuilder()

            fun flushUnresolved(endIdx: Int) {
                if (unresolvedBuffer.isNotEmpty()) {
                    val raw = unresolvedBuffer.toString()
                    tokens += RuneToken.UnresolvedSegment(
                        surface = raw,
                        startIndex = endIdx - raw.length,
                        endIndex = endIdx
                    )
                    unresolvedBuffer.clear()
                }
            }

            while (index < input.length) {
                val ch = input[index]
                if (ch.isWhitespace() || ch == '·') {
                    flushUnresolved(index)
                    index++
                    continue
                }
                val matched = orderedForms.firstOrNull { rune ->
                    input.startsWith(rune.surfaceForm, startIndex = index)
                }
                if (matched != null) {
                    flushUnresolved(index)
                    tokens += RuneToken.RecognizedRune(
                        rune = matched,
                        startIndex = index,
                        endIndex = index + matched.surfaceForm.length
                    )
                    index += matched.surfaceForm.length
                } else {
                    unresolvedBuffer.append(ch)
                    index++
                }
            }
            flushUnresolved(input.length)
            return tokens
        }
    }
}

sealed interface RuneToken {
    val surface: String
    val startIndex: Int
    val endIndex: Int
    val status: MappingEpistemicStatus

    data class RecognizedRune(
        val rune: NineRune,
        override val startIndex: Int,
        override val endIndex: Int
    ) : RuneToken {
        override val surface: String get() = rune.surfaceForm
        override val status: MappingEpistemicStatus get() = rune.epistemicStatus
    }

    data class UnresolvedSegment(
        override val surface: String,
        override val startIndex: Int,
        override val endIndex: Int
    ) : RuneToken {
        override val status: MappingEpistemicStatus get() = MappingEpistemicStatus.UNRESOLVED
    }
}

/**
 * Registry of observed compound/cipher strings from Section 4 (`GRACCHEONIUM`, `ORRICILIUM`, `BARRACILLOA`, `GRCHNMOILUBA`, `KaIRiRos`).
 * Preserves unresolved segments (`R i R o S = UNRESOLVED`) without fabricating translations.
 */
data class CipherArtifactRecord(
    val exactString: String,
    val observedStructure: String,
    val epistemicStatus: MappingEpistemicStatus,
    val unresolvedPortions: List<String>,
    val notes: String
)

object NineRunesAtlasRegistry {
    val cipherArtifacts: List<CipherArtifactRecord> = listOf(
        CipherArtifactRecord(
            exactString = "GRACCHEONIUM",
            observedStructure = "12-letter uppercase cipher/alloy identifier starting with G-R-A-C-C-H",
            epistemicStatus = MappingEpistemicStatus.UNRESOLVED,
            unresolvedPortions = listOf("GRACCHEONIUM"),
            notes = "Referenced in AURAKAI-ARGAN-OVERNIGHT-003 Section 4; glyph_translation_table.json not present in local accessible sandbox. Preserved verbatim without fabricated decode."
        ),
        CipherArtifactRecord(
            exactString = "ORRICILIUM",
            observedStructure = "10-letter uppercase cipher/element identifier",
            epistemicStatus = MappingEpistemicStatus.UNRESOLVED,
            unresolvedPortions = listOf("ORRICILIUM"),
            notes = "Referenced in Section 4; preserved as observed form with UNRESOLVED translation."
        ),
        CipherArtifactRecord(
            exactString = "BARRACILLOA",
            observedStructure = "11-letter uppercase sequence ending in OA",
            epistemicStatus = MappingEpistemicStatus.UNRESOLVED,
            unresolvedPortions = listOf("BARRACILLOA"),
            notes = "Referenced in Section 4; preserved as observed form with UNRESOLVED translation."
        ),
        CipherArtifactRecord(
            exactString = "GRCHNMOILUBA",
            observedStructure = "12-letter consonant-dense permutation/cipher sequence",
            epistemicStatus = MappingEpistemicStatus.UNRESOLVED,
            unresolvedPortions = listOf("GRCHNMOILUBA"),
            notes = "Referenced in Section 4; preserved as observed form with UNRESOLVED translation."
        ),
        CipherArtifactRecord(
            exactString = "KaIRiRos",
            observedStructure = "Mixed-case sequence bridging Kai/Kairos with internal 'R i R o S' segment",
            epistemicStatus = MappingEpistemicStatus.UNRESOLVED,
            unresolvedPortions = listOf("R i R o S"),
            notes = "Explicitly flagged in Section 4 as 'KaIRiRos (R i R o S = UNRESOLVED)'. Contains Recognizable Rune 'I' and 'a', with 'R i R o S' retained as UNRESOLVED."
        )
    )
}
