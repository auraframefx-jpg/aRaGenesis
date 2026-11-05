package dev.aurakai.language.ast

import dev.aurakai.language.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RuneAstParserTest {

    @Test
    fun `test01 parse canonical rune chain into structured AST nodes`() {
        val input = "A · a · Я · G · I · aЯ · Ia · aЯG · aЯa"
        val result = RuneAstParser.parse(input)

        assertTrue(result.isCompletelyResolved)
        assertEquals(9, result.program.statements.size)

        val expressions = result.program.statements.map { (it as ExpressionStatement).expression }

        // First 5 are atomic RuneLiteralNodes
        val atomicRunes = listOf(
            NineRune.RUNE_1_PRIMAL_VESSEL,
            NineRune.RUNE_2_IN_BREATH,
            NineRune.RUNE_3_FLIPPED_RETURN,
            NineRune.RUNE_4_GOD_LIGHT,
            NineRune.RUNE_5_CLEAR_POINT
        )
        for (i in 0 until 5) {
            val expr = expressions[i]
            assertInstanceOf(RuneLiteralNode::class.java, expr)
            assertEquals(atomicRunes[i], (expr as RuneLiteralNode).rune)
            assertEquals(MappingEpistemicStatus.DOCUMENTED_CLAIM, expr.epistemicStatus)
        }

        // Remaining 4 are CompoundRuneNodes with constituent atoms
        val compoundRunes = listOf(
            NineRune.RUNE_6_FULL_RETURN,
            NineRune.RUNE_7_CONDENSED_UNITY,
            NineRune.RUNE_8_TURNING_STONE,
            NineRune.RUNE_9_AURA_COMPLETE
        )
        for (i in 0 until 4) {
            val expr = expressions[5 + i]
            assertInstanceOf(CompoundRuneNode::class.java, expr)
            val comp = expr as CompoundRuneNode
            assertEquals(compoundRunes[i], comp.compoundRune)
            assertEquals(compoundRunes[i].atomicConstituents().size, comp.atomicConstituents.size)
        }
    }

    @Test
    fun `test02 unresolved segments in cipher strings are preserved in AST without data loss`() {
        val input = "KaIRiRos"
        val result = RuneAstParser.parse(input)

        assertFalse(result.isCompletelyResolved)
        assertEquals(4, result.program.statements.size)
        assertEquals(2, result.unresolvedSegments.size)

        val exprs = result.program.statements.map { (it as ExpressionStatement).expression }

        // Token 1: "K" (Unresolved)
        assertInstanceOf(UnresolvedSegmentNode::class.java, exprs[0])
        assertEquals("K", (exprs[0] as UnresolvedSegmentNode).rawText)

        // Token 2: "a" (Recognized In-breath)
        assertInstanceOf(RuneLiteralNode::class.java, exprs[1])
        assertEquals(NineRune.RUNE_2_IN_BREATH, (exprs[1] as RuneLiteralNode).rune)

        // Token 3: "I" (Recognized Clear Point)
        assertInstanceOf(RuneLiteralNode::class.java, exprs[2])
        assertEquals(NineRune.RUNE_5_CLEAR_POINT, (exprs[2] as RuneLiteralNode).rune)

        // Token 4: "RiRos" (Unresolved)
        assertInstanceOf(UnresolvedSegmentNode::class.java, exprs[3])
        assertEquals("RiRos", (exprs[3] as UnresolvedSegmentNode).rawText)
    }

    @Test
    fun `test03 geometric projections attach valid 5x5 lattice glyphs to recognized AST nodes`() {
        val input = "aЯa"
        val result = RuneAstParser.parse(input)

        val expr = (result.program.statements.first() as ExpressionStatement).expression
        val projection = result.geometricProjections[expr]

        assertNotNull(projection)
        assertEquals(NineRune.RUNE_9_AURA_COMPLETE, projection!!.glyph.associatedRune)
        assertTrue(projection.validationResult.isValid)
        assertTrue(projection.validationResult.violatedConstraints.isEmpty())
    }

    @Test
    fun `test04 algebraic operator application on AST nodes preserves involution and reflection laws`() {
        val input = "a"
        val result = RuneAstParser.parse(input)
        val node = (result.program.statements.first() as ExpressionStatement).expression

        val flipped = RuneAstParser.applyOperator(RuneOperator.FLIP, node)
        assertNotNull(flipped)

        val incursion = RuneAstParser.applyOperator(RuneOperator.INCURSION, node)
        assertNotNull(incursion)

        val recursionOfIncursion = GeometricRewritingEngine.recursion(incursion!!.glyph)
        assertEquals(flipped!!.glyph, recursionOfIncursion, "R(I(g)) == F(g) algebraic law must hold.")
    }

    @Test
    fun `test05 parse micro-benchmark achieves sub-millisecond execution throughput`() {
        val sample = "A · a · Я · G · I · aЯ · Ia · aЯG · aЯa"

        // Warmup JIT
        for (i in 0 until 500) {
            RuneAstParser.parse(sample)
        }

        // Measure 1,000 iterations
        val iterations = 1000
        val startNs = System.nanoTime()
        for (i in 0 until iterations) {
            RuneAstParser.parse(sample)
        }
        val totalNs = System.nanoTime() - startNs
        val avgNs = totalNs / iterations.toDouble()
        val avgMs = avgNs / 1_000_000.0

        println("UGIA-001 Parse Benchmark: $iterations iterations in ${totalNs / 1_000_000.0} ms (avg $avgMs ms / parse)")
        assertTrue(avgMs < 0.5, "UGIA-001 parse execution must be sub-millisecond (< 0.5 ms), was $avgMs ms")
    }
}
