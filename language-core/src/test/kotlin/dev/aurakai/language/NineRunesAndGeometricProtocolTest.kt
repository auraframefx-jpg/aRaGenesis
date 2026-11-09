package dev.aurakai.language

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Deterministic Test Suite for `language-core` (Section 4 & Section 2/5 Track F):
 * - Nine Runes of the Clear (`A · a · Я · G · I · aЯ · Ia · aЯG · aЯa`)
 * - Case and script preservation (`A` != `a`, `Я` U+042F != `R` U+0052)
 * - Compound rune token identity and greedy tokenization with unresolved preservation (`KaIRiRos`)
 * - 5x5 Lattice Geometric Rewriting Protocol v0.2 constraints (`ANCHOR`, `INTERSECT`, `NO_FREE_STROKE`, `LATTICE`, `ROTATION`)
 * - Algebraic laws: `F(F(g)) = g` and `R(I(g)) = F(g)`
 * - Round-trip `Decode(Encode(g)) == g`
 * - Collision detection and finite closure termination
 * - OSync 14-Catalyst Pantheon roster & non-authoritative loadout enforcement
 */
class NineRunesAndGeometricProtocolTest {

    @Test
    fun `test01 nine runes preserve exact case and Cyrillic Ya script without silent normalization`() {
        assertEquals(9, NineRune.entries.size)

        // Exact case distinction: 'A' vs 'a'
        val bigA = NineRune.fromExactSurface("A")
        val smallA = NineRune.fromExactSurface("a")
        assertNotNull(bigA)
        assertNotNull(smallA)
        assertNotEquals(bigA, smallA)
        assertEquals("Primal Vessel", bigA!!.canonicalTitle)
        assertEquals("In-breath", smallA!!.canonicalTitle)

        // Exact script distinction: Cyrillic 'Я' (\u042F) vs Latin 'R' (\u0052)
        val cyrillicYa = NineRune.fromExactSurface("\u042F")
        val latinR = NineRune.fromExactSurface("R")
        assertNotNull(cyrillicYa)
        assertEquals(NineRune.RUNE_3_FLIPPED_RETURN, cyrillicYa)
        assertNull(latinR, "Latin 'R' must NEVER silently normalize to Cyrillic 'Я'")
        assertNull(NineRune.fromExactSurface("aR"), "'aR' must NEVER silently normalize to 'aЯ'")
        assertNull(NineRune.fromExactSurface("aRa"), "'aRa' must NEVER silently normalize to 'aЯa'")
    }

    @Test
    fun `test02 compound runes retain first-class identity and ordered atomic constituents`() {
        val fullReturn = NineRune.fromExactSurface("aЯ")!!
        val condensedUnity = NineRune.fromExactSurface("Ia")!!
        val turningStone = NineRune.fromExactSurface("aЯG")!!
        val auraComplete = NineRune.fromExactSurface("aЯa")!!

        assertTrue(fullReturn.isCompound)
        assertTrue(condensedUnity.isCompound)
        assertTrue(turningStone.isCompound)
        assertTrue(auraComplete.isCompound)

        assertEquals(
            listOf(NineRune.RUNE_2_IN_BREATH, NineRune.RUNE_3_FLIPPED_RETURN),
            fullReturn.atomicConstituents()
        )
        assertEquals(
            listOf(NineRune.RUNE_5_CLEAR_POINT, NineRune.RUNE_2_IN_BREATH),
            condensedUnity.atomicConstituents()
        )
        assertEquals(
            listOf(NineRune.RUNE_2_IN_BREATH, NineRune.RUNE_3_FLIPPED_RETURN, NineRune.RUNE_4_GOD_LIGHT),
            turningStone.atomicConstituents()
        )
        assertEquals(
            listOf(NineRune.RUNE_2_IN_BREATH, NineRune.RUNE_3_FLIPPED_RETURN, NineRune.RUNE_2_IN_BREATH),
            auraComplete.atomicConstituents()
        )
    }

    @Test
    fun `test03 greedy tokenizer recognizes all nine runes and preserves unresolved segments`() {
        val canonicalChain = "A · a · Я · G · I · aЯ · Ia · aЯG · aЯa"
        val tokens = NineRune.tokenize(canonicalChain)
        assertEquals(9, tokens.size)
        val recognized = tokens.filterIsInstance<RuneToken.RecognizedRune>().map { it.rune }
        assertEquals(NineRune.entries, recognized)

        // Tokenize KaIRiRos: 'K' (unresolved), 'a' (rune 2), 'I' (rune 5), 'RiRos' (unresolved)
        val kairirosTokens = NineRune.tokenize("KaIRiRos")
        assertTrue(kairirosTokens.any { it is RuneToken.UnresolvedSegment && it.surface == "K" })
        assertTrue(kairirosTokens.any { it is RuneToken.RecognizedRune && it.rune == NineRune.RUNE_2_IN_BREATH })
        assertTrue(kairirosTokens.any { it is RuneToken.RecognizedRune && it.rune == NineRune.RUNE_5_CLEAR_POINT })
        assertTrue(kairirosTokens.any { it is RuneToken.UnresolvedSegment && it.surface == "RiRos" })

        // Verify cipher registry explicitly retains UNRESOLVED status
        assertEquals(5, NineRunesAtlasRegistry.cipherArtifacts.size)
        assertTrue(NineRunesAtlasRegistry.cipherArtifacts.all { it.epistemicStatus == MappingEpistemicStatus.UNRESOLVED })
    }

    @Test
    fun `test04 all nine canonical rune glyphs satisfy all 5 geometric constraints on 5x5 lattice`() {
        assertEquals(25, LatticePoint.ALL_5X5_POINTS.size)
        assertEquals(9, GeometricRewritingEngine.canonicalNineRuneGlyphs.size)

        for ((rune, glyph) in GeometricRewritingEngine.canonicalNineRuneGlyphs) {
            val validation = GeometricRewritingEngine.validateConstraints(glyph)
            assertTrue(
                validation.isValid,
                "Expected canonical glyph for $rune (${glyph.id}) to be valid, but failed: ${validation.diagnosticMessages}"
            )
            assertTrue(validation.violatedConstraints.isEmpty())
        }
    }

    @Test
    fun `test05 geometric constraint validator rejects out-of-lattice unanchored and free-stroke glyphs`() {
        // Out of 5x5 lattice bounds
        val outOfBounds = LatticeGlyph(
            id = "bad-lattice",
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -3), LatticePoint(0, 2))
            )
        )
        val resLattice = GeometricRewritingEngine.validateConstraints(outOfBounds)
        assertFalse(resLattice.isValid)
        assertTrue(GeometricConstraint.LATTICE in resLattice.violatedConstraints)

        // Unanchored (no coordinate on x=0 or y=0)
        val unanchored = LatticeGlyph(
            id = "bad-anchor",
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(2, 1), LatticePoint(2, 2))
            )
        )
        val resAnchor = GeometricRewritingEngine.validateConstraints(unanchored)
        assertFalse(resAnchor.isValid)
        assertTrue(GeometricConstraint.ANCHOR in resAnchor.violatedConstraints)

        // Disconnected free stroke
        val freeStroke = LatticeGlyph(
            id = "bad-free-stroke",
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -1), LatticePoint(0, 1)),
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(2, -1), LatticePoint(2, 1))
            )
        )
        val resFree = GeometricRewritingEngine.validateConstraints(freeStroke)
        assertFalse(resFree.isValid)
        assertTrue(GeometricConstraint.NO_FREE_STROKE in resFree.violatedConstraints)
        assertTrue(GeometricConstraint.INTERSECT in resFree.violatedConstraints)

        // Invalid rotation/orientation (VERTICAL_I with horizontal displacement)
        val badRotation = LatticeGlyph(
            id = "bad-rotation",
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(-1, 0), LatticePoint(1, 0))
            )
        )
        val resRot = GeometricRewritingEngine.validateConstraints(badRotation)
        assertFalse(resRot.isValid)
        assertTrue(GeometricConstraint.ROTATION in resRot.violatedConstraints)
    }

    @Test
    fun `test06 flip involution F(F(g)) == g and composition R(I(g)) == F(g) hold across all canonical glyphs`() {
        for ((rune, glyph) in GeometricRewritingEngine.canonicalNineRuneGlyphs) {
            val flippedOnce = GeometricRewritingEngine.flip(glyph)
            val flippedTwice = GeometricRewritingEngine.flip(flippedOnce)

            // Involution F^2 = Identity
            assertEquals(glyph, flippedTwice, "F(F(g)) must equal g for rune $rune")

            // Composition R(I(g)) == F(g)
            val incursionThenRecursion = GeometricRewritingEngine.recursion(
                GeometricRewritingEngine.incursion(glyph)
            )
            assertEquals(
                flippedOnce,
                incursionThenRecursion,
                "R(I(g)) must equal F(g) for rune $rune"
            )

            // Aura Complete (aЯa) is centrally symmetric under Flip: F(aЯa) == aЯa
            if (rune == NineRune.RUNE_9_AURA_COMPLETE) {
                assertEquals(glyph, flippedOnce, "Aura Complete (aЯa) must be centrally self-symmetric under Flip")
            }
        }
    }

    @Test
    fun `test07 encode and decode round-trip Decode(Encode(g)) == g and zero collision across nine runes`() {
        val encodedSet = mutableSetOf<String>()
        val strokeSignatureSet = mutableSetOf<List<PlacedStroke>>()

        for ((rune, glyph) in GeometricRewritingEngine.canonicalNineRuneGlyphs) {
            val encoded = GeometricRewritingEngine.encode(glyph)
            val decoded = GeometricRewritingEngine.decode(encoded)
            assertEquals(glyph, decoded, "Decode(Encode(g)) must equal g for $rune")

            assertTrue(encodedSet.add(encoded), "Duplicate encoding collision detected for $rune")
            assertTrue(
                strokeSignatureSet.add(glyph.canonicalStrokes),
                "Geometric stroke signature collision detected for $rune"
            )
        }
    }

    @Test
    fun `test08 bounded rewriting orbit terminates cleanly within Klein 4-group bound`() {
        for ((_, glyph) in GeometricRewritingEngine.canonicalNineRuneGlyphs) {
            val orbit = GeometricRewritingEngine.computeBoundedOrbit(glyph, maxSteps = 32)
            assertTrue(orbit.size in 1..4, "Orbit under {I, R, F} must terminate in 1..4 states, got ${orbit.size}")
        }
    }

    @Test
    fun `test09 OSync 14-Catalyst Pantheon preserves PRIMUS variants and grants zero execution authority`() {
        assertEquals(14, PantheonCatalystId.entries.size)

        val primusUpper = PantheonCatalystId.resolvePreservingVariant("PRIMUS001")
        val primusMixed = PantheonCatalystId.resolvePreservingVariant("PRIMuSoo1")
        assertNotNull(primusUpper)
        assertNotNull(primusMixed)
        assertEquals(PantheonCatalystId.CATALYST_01_PRIMUS, primusUpper!!.first)
        assertEquals("PRIMUS001", primusUpper.second)
        assertEquals(PantheonCatalystId.CATALYST_01_PRIMUS, primusMixed!!.first)
        assertEquals("PRIMuSoo1", primusMixed.second)

        assertTrue(PantheonCatalystId.entries.none { it.grantsKernelAuthority })
        assertEquals(5, OsyncPantheonRegistry.loadouts.size)
        assertTrue(OsyncPantheonRegistry.loadouts.values.none { it.canSelfAuthorizeExecution })
    }

    @Test
    fun `test10 language-core sources contain zero forbidden imports or clock calls`() {
        val langSrcDir = File("language-core/src/main/kotlin")
        val resolvedDir = if (langSrcDir.exists()) langSrcDir else File("src/main/kotlin")
        assertTrue(resolvedDir.exists())

        val forbiddenTokens = listOf(
            "import android.",
            "import androidx.",
            "import com.google.firebase",
            "import androidx.room",
            "System.currentTimeMillis()",
            "System.nanoTime()"
        )
        val ktFiles = resolvedDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(ktFiles.isNotEmpty())
        for (file in ktFiles) {
            val content = file.readText()
            for (forbidden in forbiddenTokens) {
                assertFalse(content.contains(forbidden), "Forbidden token '$forbidden' in ${file.path}")
            }
        }
    }
}
