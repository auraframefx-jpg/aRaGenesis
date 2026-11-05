package dev.aurakai.language.ast

import dev.aurakai.language.*

/**
 * Common AST Node interface for UGIA-001 (Section 4 & Section 5 / §16).
 */
sealed interface RuneAstNode {
    val span: IntRange
}

/**
 * Root Program AST node containing parsed statement sequence.
 */
data class ProgramNode(
    val statements: List<RuneStatementNode>,
    override val span: IntRange
) : RuneAstNode

sealed interface RuneStatementNode : RuneAstNode

data class ExpressionStatement(
    val expression: RuneExpressionNode,
    override val span: IntRange
) : RuneStatementNode

/**
 * Epistemic-aware expression node in the Rune grammar.
 */
sealed interface RuneExpressionNode : RuneAstNode {
    val epistemicStatus: MappingEpistemicStatus
}

/**
 * Atomic single rune literal (`A`, `a`, `Я`, `G`, `I`).
 */
data class RuneLiteralNode(
    val rune: NineRune,
    override val span: IntRange
) : RuneExpressionNode {
    override val epistemicStatus: MappingEpistemicStatus get() = rune.epistemicStatus
}

/**
 * Compound rune node (`aЯ`, `Ia`, `aЯG`, `aЯa`) retaining atomic constituent sequence.
 */
data class CompoundRuneNode(
    val compoundRune: NineRune,
    val atomicConstituents: List<RuneLiteralNode>,
    override val span: IntRange
) : RuneExpressionNode {
    override val epistemicStatus: MappingEpistemicStatus get() = compoundRune.epistemicStatus
}

/**
 * Explicitly preserved unresolved string segment (e.g., `KaIRiRos` -> `RiRos`).
 */
data class UnresolvedSegmentNode(
    val rawText: String,
    override val span: IntRange
) : RuneExpressionNode {
    override val epistemicStatus: MappingEpistemicStatus get() = MappingEpistemicStatus.UNRESOLVED
}

/**
 * Geometric Transformation Operator applied to a rune expression.
 */
enum class RuneOperator(val symbol: String, val description: String) {
    INCURSION("I()", "Incursion operator (X-axis reflection / inward contraction)"),
    RECURSION("R()", "Recursion operator (Y-axis reflection completing central inversion)"),
    FLIP("F()", "Central inversion operator F(x, y) = (-x, -y)"),
    ROTATE_CCW("ROT()", "90-degree CCW discrete lattice rotation")
}

data class OperatorApplicationNode(
    val operator: RuneOperator,
    val operand: RuneExpressionNode,
    override val span: IntRange
) : RuneExpressionNode {
    override val epistemicStatus: MappingEpistemicStatus get() = operand.epistemicStatus
}

/**
 * Complete Geometric Projection on the 5x5 discrete lattice M = {-2..2}^2.
 */
data class GeometricProjection(
    val glyph: LatticeGlyph,
    val validationResult: ConstraintValidationResult
)

/**
 * Full output of the 5-Stage UGIA-001 Pipeline.
 * Pure and deterministic with zero platform or clock dependencies.
 */
data class AstPipelineResult(
    val program: ProgramNode,
    val geometricProjections: Map<RuneExpressionNode, GeometricProjection>,
    val unresolvedSegments: List<UnresolvedSegmentNode>
) {
    val isCompletelyResolved: Boolean get() = unresolvedSegments.isEmpty()
}

/**
 * 5-Stage UGIA-001 Rune AST Parser & Geometric Projection Engine:
 * 1. Lexical Tokenization (NineRune.tokenize)
 * 2. Syntax Grammar Analysis
 * 3. AST Tree Construction
 * 4. Semantic Constraint Validation (ANCHOR, INTERSECT, NO_FREE_STROKE, LATTICE, ROTATION)
 * 5. Discrete 5x5 Lattice Projection
 */
object RuneAstParser {

    fun parse(input: String): AstPipelineResult {
        // Stage 1: Lexical Tokenization with case and script preservation
        val tokens = NineRune.tokenize(input)

        // Stage 2 & 3: Syntax Analysis and AST Node Construction
        val statements = mutableListOf<RuneStatementNode>()
        val unresolved = mutableListOf<UnresolvedSegmentNode>()
        val projections = mutableMapOf<RuneExpressionNode, GeometricProjection>()

        for (token in tokens) {
            when (token) {
                is RuneToken.RecognizedRune -> {
                    val expr: RuneExpressionNode = if (token.rune.isCompound) {
                        var currentOffset = token.startIndex
                        val constituents = token.rune.atomicConstituents().map { atomic ->
                            val atomLen = atomic.surfaceForm.length
                            val node = RuneLiteralNode(atomic, currentOffset until (currentOffset + atomLen))
                            currentOffset += atomLen
                            node
                        }
                        CompoundRuneNode(token.rune, constituents, token.startIndex until token.endIndex)
                    } else {
                        RuneLiteralNode(token.rune, token.startIndex until token.endIndex)
                    }
                    statements.add(ExpressionStatement(expr, token.startIndex until token.endIndex))

                    // Stage 4 & 5: Semantic Constraint Validation & 5x5 Geometric Projection
                    val canonicalGlyph = GeometricRewritingEngine.canonicalNineRuneGlyphs[token.rune]
                    if (canonicalGlyph != null) {
                        val validation = GeometricRewritingEngine.validateConstraints(canonicalGlyph)
                        projections[expr] = GeometricProjection(canonicalGlyph, validation)
                    }
                }
                is RuneToken.UnresolvedSegment -> {
                    val unres = UnresolvedSegmentNode(token.surface, token.startIndex until token.endIndex)
                    statements.add(ExpressionStatement(unres, token.startIndex until token.endIndex))
                    unresolved.add(unres)
                }
            }
        }

        val program = ProgramNode(statements, 0 until input.length)

        return AstPipelineResult(
            program = program,
            geometricProjections = projections,
            unresolvedSegments = unresolved
        )
    }

    /**
     * Applies a geometric operator to an AST expression, generating a transformed LatticeGlyph.
     */
    fun applyOperator(operator: RuneOperator, node: RuneExpressionNode): GeometricProjection? {
        val baseRune = when (node) {
            is RuneLiteralNode -> node.rune
            is CompoundRuneNode -> node.compoundRune
            else -> return null
        }
        val canonicalGlyph = GeometricRewritingEngine.canonicalNineRuneGlyphs[baseRune] ?: return null

        val transformedGlyph = when (operator) {
            RuneOperator.INCURSION -> GeometricRewritingEngine.incursion(canonicalGlyph)
            RuneOperator.RECURSION -> GeometricRewritingEngine.recursion(canonicalGlyph)
            RuneOperator.FLIP -> GeometricRewritingEngine.flip(canonicalGlyph)
            RuneOperator.ROTATE_CCW -> GeometricRewritingEngine.rotate90Ccw(canonicalGlyph)
        }

        val validation = GeometricRewritingEngine.validateConstraints(transformedGlyph)
        return GeometricProjection(transformedGlyph, validation)
    }
}
