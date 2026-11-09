package dev.aurakai.language

/**
 * Geometric Rewriting Protocol v0.2 & Spec Reconciliation Engine (Section 4).
 *
 * Reconciles:
 * 1. Concrete 5x5 Lattice Specification (v0.2):
 *    - Matrix / Lattice: M = {-2, -1, 0, 1, 2}^2 (25 discrete coordinates, origin (0, 0) at center).
 *    - Concrete Primitive Set: P = { I, H, /, \, C, _ }
 *      - `I`: Vertical spine / line
 *      - `H`: Horizontal crossbar / bridge
 *      - `/`: Ascending diagonal
 *      - `\`: Descending diagonal
 *      - `C`: Curved arc / open loop
 *      - `_`: Base / ground stroke (or point anchor)
 *    - Constraints: ANCHOR, INTERSECT, NO-FREE-STROKE, LATTICE, ROTATION.
 *    - Operators:
 *      - Incursion `I(g)`: inward contraction / origin-anchoring projection + inward stroke reflection.
 *      - Recursion `R(g)`: dual reflection that completes the 180-degree inversion so that `R(I(g)) = F(g)`.
 *      - Flip `F(x, y) = (-x, -y)`: exact central inversion across origin `(0, 0)`. Involution: `F(F(g)) = g`.
 *    - Round-trip invariant: `Decode(Encode(g)) == g`.
 *
 * 2. Abstract Spec-Pack Grammar (`GEOMETRIC_GRAMMAR_SPEC.md` / Overnight-002):
 *    - Abstract Primitive Alphabet: `{ LINE, ARC, CIRCLE, POINT }`.
 *    - Reconciled via deterministic homomorphism [ConcretePrimitive.toAbstractPrimitive].
 */
data class LatticePoint(val x: Int, val y: Int) : Comparable<LatticePoint> {
    val isWithin5x5Lattice: Boolean get() = x in MIN_COORD..MAX_COORD && y in MIN_COORD..MAX_COORD
    val isOrigin: Boolean get() = x == 0 && y == 0

    /**
     * Flip operator on coordinates: F(x, y) = (-x, -y).
     */
    fun flip(): LatticePoint = LatticePoint(-x, -y)

    /**
     * Horizontal mirror: (x, y) -> (-x, y). Used in decomposing F = R o I.
     */
    fun reflectX(): LatticePoint = LatticePoint(-x, y)

    /**
     * Vertical mirror: (x, y) -> (x, -y). Used in decomposing F = R o I.
     */
    fun reflectY(): LatticePoint = LatticePoint(x, -y)

    /**
     * 90-degree counterclockwise discrete lattice rotation: (x, y) -> (-y, x).
     */
    fun rotate90Ccw(): LatticePoint = LatticePoint(-y, x)

    override fun compareTo(other: LatticePoint): Int {
        val cmpX = this.x.compareTo(other.x)
        return if (cmpX != 0) cmpX else this.y.compareTo(other.y)
    }

    override fun toString(): String = "($x,$y)"

    companion object {
        const val MIN_COORD = -2
        const val MAX_COORD = 2

        val ORIGIN = LatticePoint(0, 0)

        val ALL_5X5_POINTS: Set<LatticePoint> = buildSet {
            for (x in MIN_COORD..MAX_COORD) {
                for (y in MIN_COORD..MAX_COORD) {
                    add(LatticePoint(x, y))
                }
            }
        }
    }
}

/**
 * Abstract primitive alphabet from the Pantheon Spec-Pack (`{LINE, ARC, CIRCLE, POINT}`).
 */
enum class AbstractGeometricPrimitive {
    LINE,
    ARC,
    CIRCLE,
    POINT
}

/**
 * Concrete primitive set from Geometric Rewriting Protocol v0.2: `P = { I, H, /, \, C, _ }`.
 */
enum class ConcretePrimitive(
    val symbol: Char,
    val codeName: String,
    val abstractEquivalent: AbstractGeometricPrimitive
) {
    VERTICAL_I('I', "VERTICAL_SPINE", AbstractGeometricPrimitive.LINE),
    HORIZONTAL_H('H', "HORIZONTAL_BRIDGE", AbstractGeometricPrimitive.LINE),
    DIAG_ASC('/', "ASCENDING_DIAGONAL", AbstractGeometricPrimitive.LINE),
    DIAG_DESC('\\', "DESCENDING_DIAGONAL", AbstractGeometricPrimitive.LINE),
    CURVE_C('C', "CURVED_ARC", AbstractGeometricPrimitive.ARC),
    BASE_UNDERSCORE('_', "BASE_ANCHOR", AbstractGeometricPrimitive.POINT);

    /**
     * Under central inversion F(x, y) = (-x, -y), lines I, H, /, \ are centrally symmetric in slope,
     * while open arc C and base _ invert orientation unless paired; on the primitive symbol set,
     * horizontal reflection swaps `/` and `\`, and vertical reflection swaps `/` and `\`, so
     * their composition `R o I = F` preserves `/` and `\`!
     */
    fun reflectHorizontally(): ConcretePrimitive = when (this) {
        VERTICAL_I -> VERTICAL_I
        HORIZONTAL_H -> HORIZONTAL_H
        DIAG_ASC -> DIAG_DESC
        DIAG_DESC -> DIAG_ASC
        CURVE_C -> CURVE_C
        BASE_UNDERSCORE -> BASE_UNDERSCORE
    }

    fun reflectVertically(): ConcretePrimitive = when (this) {
        VERTICAL_I -> VERTICAL_I
        HORIZONTAL_H -> HORIZONTAL_H
        DIAG_ASC -> DIAG_DESC
        DIAG_DESC -> DIAG_ASC
        CURVE_C -> CURVE_C
        BASE_UNDERSCORE -> BASE_UNDERSCORE
    }

    fun flip(): ConcretePrimitive = this.reflectHorizontally().reflectVertically()

    fun rotate90Ccw(): ConcretePrimitive = when (this) {
        VERTICAL_I -> HORIZONTAL_H
        HORIZONTAL_H -> VERTICAL_I
        DIAG_ASC -> DIAG_DESC
        DIAG_DESC -> DIAG_ASC
        CURVE_C -> CURVE_C
        BASE_UNDERSCORE -> BASE_UNDERSCORE
    }

    companion object {
        private val bySymbol = entries.associateBy { it.symbol }

        fun fromSymbol(ch: Char): ConcretePrimitive =
            bySymbol[ch] ?: throw IllegalArgumentException("Unknown concrete primitive symbol: '$ch'")
    }
}

/**
 * A single geometric stroke placed on the 5x5 lattice M = {-2,-1,0,1,2}^2.
 * Endpoints `start` and `end` are canonicalized (`start <= end`) so that undirected geometric strokes
 * have a unique canonical representation under `Flip` and `Encode`/`Decode`.
 */
data class PlacedStroke(
    val primitive: ConcretePrimitive,
    val start: LatticePoint,
    val end: LatticePoint
) : Comparable<PlacedStroke> {

    val canonicalStart: LatticePoint = if (start <= end) start else end
    val canonicalEnd: LatticePoint = if (start <= end) end else start

    val pointsCovered: Set<LatticePoint> by lazy {
        val pts = mutableSetOf(canonicalStart, canonicalEnd)
        val dx = canonicalEnd.x - canonicalStart.x
        val dy = canonicalEnd.y - canonicalStart.y
        when (primitive) {
            ConcretePrimitive.VERTICAL_I -> if (dx == 0 && dy > 1) {
                for (y in (canonicalStart.y + 1) until canonicalEnd.y) {
                    pts += LatticePoint(canonicalStart.x, y)
                }
            }
            ConcretePrimitive.HORIZONTAL_H, ConcretePrimitive.BASE_UNDERSCORE -> if (dy == 0 && dx > 1) {
                for (x in (canonicalStart.x + 1) until canonicalEnd.x) {
                    pts += LatticePoint(x, canonicalStart.y)
                }
            }
            ConcretePrimitive.DIAG_ASC -> if (dx == dy && dx > 1) {
                for (step in 1 until dx) {
                    pts += LatticePoint(canonicalStart.x + step, canonicalStart.y + step)
                }
            }
            ConcretePrimitive.DIAG_DESC -> if (dx == -dy && dx > 1) {
                for (step in 1 until dx) {
                    pts += LatticePoint(canonicalStart.x + step, canonicalStart.y - step)
                }
            }
            ConcretePrimitive.CURVE_C -> {
                // Endpoint anchor set
            }
        }
        pts.toSet()
    }

    fun flip(): PlacedStroke = createCanonical(
        primitive = primitive.flip(),
        p1 = canonicalStart.flip(),
        p2 = canonicalEnd.flip()
    )

    /**
     * Incursion step I on a stroke: reflects across the X-axis `(x, y) -> (-x, y)` with primitive reflection.
     * When followed by Recursion step R (reflection across the Y-axis `(x, y) -> (x, -y)`),
     * satisfies `R(I(s)) == F(s)` on all valid strokes.
     */
    fun incursion(): PlacedStroke = createCanonical(
        primitive = primitive.reflectHorizontally(),
        p1 = canonicalStart.reflectX(),
        p2 = canonicalEnd.reflectX()
    )

    /**
     * Recursion step R on a stroke: reflects across the Y-axis `(x, y) -> (x, -y)` with primitive reflection.
     */
    fun recursion(): PlacedStroke = createCanonical(
        primitive = primitive.reflectVertically(),
        p1 = canonicalStart.reflectY(),
        p2 = canonicalEnd.reflectY()
    )

    fun rotate90Ccw(): PlacedStroke = createCanonical(
        primitive = primitive.rotate90Ccw(),
        p1 = canonicalStart.rotate90Ccw(),
        p2 = canonicalEnd.rotate90Ccw()
    )

    override fun compareTo(other: PlacedStroke): Int {
        val c1 = this.primitive.symbol.compareTo(other.primitive.symbol)
        if (c1 != 0) return c1
        val c2 = this.canonicalStart.compareTo(other.canonicalStart)
        if (c2 != 0) return c2
        return this.canonicalEnd.compareTo(other.canonicalEnd)
    }

    companion object {
        fun createCanonical(
            primitive: ConcretePrimitive,
            p1: LatticePoint,
            p2: LatticePoint
        ): PlacedStroke {
            return if (p1 <= p2) {
                PlacedStroke(primitive, p1, p2)
            } else {
                PlacedStroke(primitive, p2, p1)
            }
        }
    }
}

/**
 * The 5 Geometric Protocol Constraints from Section 4:
 * - [ANCHOR]: At least one stroke must touch the origin `(0, 0)` or a primary cardinal axis anchor `(0, y)` / `(x, 0)`.
 * - [INTERSECT]: For multi-stroke glyphs, every stroke must share at least one endpoint with the connected component.
 * - [NO_FREE_STROKE]: No isolated/disconnected stroke is permitted in a multi-stroke glyph.
 * - [LATTICE]: Every coordinate must lie strictly within `M = {-2, -1, 0, 1, 2}^2`.
 * - [ROTATION]: Every stroke's primitive type must be geometrically compatible with its endpoint displacement vector.
 */
enum class GeometricConstraint {
    ANCHOR,
    INTERSECT,
    NO_FREE_STROKE,
    LATTICE,
    ROTATION
}

data class ConstraintValidationResult(
    val isValid: Boolean,
    val violatedConstraints: Set<GeometricConstraint>,
    val diagnosticMessages: List<String>
)

/**
 * A complete geometric glyph on the 5x5 lattice.
 * Stores strokes in canonical sorted order for deterministic equality, hashing, and encoding.
 */
data class LatticeGlyph(
    val id: String,
    val strokes: List<PlacedStroke>,
    val associatedRune: NineRune? = null
) {
    val canonicalStrokes: List<PlacedStroke> = strokes
        .map { PlacedStroke.createCanonical(it.primitive, it.canonicalStart, it.canonicalEnd) }
        .distinct()
        .sorted()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LatticeGlyph) return false
        return id == other.id &&
            canonicalStrokes == other.canonicalStrokes &&
            associatedRune == other.associatedRune
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + canonicalStrokes.hashCode()
        result = 31 * result + (associatedRune?.hashCode() ?: 0)
        return result
    }
}

/**
 * Geometric Rewriting Engine v0.2 implementing:
 * - Constraint validation (`ANCHOR`, `INTERSECT`, `NO_FREE_STROKE`, `LATTICE`, `ROTATION`)
 * - Operators: `Incursion` (`I`), `Recursion` (`R`), `Flip` (`F`), and `Rotate90Ccw`
 * - Algebraic verification: `F(F(g)) = g` and `R(I(g)) = F(g)`
 * - Deterministic `Encode` / `Decode` with `Decode(Encode(g)) = g`
 * - Collision detection across canonical rune glyphs
 * - Bounded rewriting closure and termination guarantee
 */
object GeometricRewritingEngine {

    /**
     * Validates all 5 geometric constraints on a [LatticeGlyph].
     */
    fun validateConstraints(glyph: LatticeGlyph): ConstraintValidationResult {
        val violations = mutableSetOf<GeometricConstraint>()
        val messages = mutableListOf<String>()

        if (glyph.canonicalStrokes.isEmpty()) {
            violations += GeometricConstraint.ANCHOR
            messages += "Glyph '${glyph.id}' has no strokes"
            return ConstraintValidationResult(false, violations, messages)
        }

        // 1. LATTICE constraint: all endpoints in {-2,-1,0,1,2}^2
        for (s in glyph.canonicalStrokes) {
            if (!s.canonicalStart.isWithin5x5Lattice || !s.canonicalEnd.isWithin5x5Lattice) {
                violations += GeometricConstraint.LATTICE
                messages += "Stroke $s exceeds 5x5 lattice bounds M = {-2,-1,0,1,2}^2"
            }
        }

        // 2. ANCHOR constraint: at least one endpoint lies on the origin (0,0) or primary axis spine (x=0 or y=0)
        val hasAxisAnchor = glyph.canonicalStrokes.any { s ->
            s.canonicalStart.x == 0 || s.canonicalStart.y == 0 ||
                s.canonicalEnd.x == 0 || s.canonicalEnd.y == 0
        }
        if (!hasAxisAnchor) {
            violations += GeometricConstraint.ANCHOR
            messages += "Glyph '${glyph.id}' lacks an origin or cardinal axis anchor point"
        }

        // 3. INTERSECT & NO_FREE_STROKE constraints: connected component check for multi-stroke glyphs
        if (glyph.canonicalStrokes.size > 1) {
            val adjacency = Array(glyph.canonicalStrokes.size) { BooleanArray(glyph.canonicalStrokes.size) }
            for (i in glyph.canonicalStrokes.indices) {
                var sharesAnyEndpoint = false
                for (j in glyph.canonicalStrokes.indices) {
                    if (i == j) continue
                    val shared = glyph.canonicalStrokes[i].pointsCovered
                        .intersect(glyph.canonicalStrokes[j].pointsCovered)
                        .isNotEmpty()
                    if (shared) {
                        adjacency[i][j] = true
                        sharesAnyEndpoint = true
                    }
                }
                if (!sharesAnyEndpoint) {
                    violations += GeometricConstraint.NO_FREE_STROKE
                    violations += GeometricConstraint.INTERSECT
                    messages += "Isolated free stroke detected: ${glyph.canonicalStrokes[i]}"
                }
            }

            // BFS reachability from stroke 0
            val visited = BooleanArray(glyph.canonicalStrokes.size)
            val queue = ArrayDeque<Int>()
            visited[0] = true
            queue.add(0)
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                for (next in glyph.canonicalStrokes.indices) {
                    if (adjacency[curr][next] && !visited[next]) {
                        visited[next] = true
                        queue.add(next)
                    }
                }
            }
            if (visited.any { !it }) {
                violations += GeometricConstraint.INTERSECT
                messages += "Glyph '${glyph.id}' strokes do not form a single intersecting connected component"
            }
        }

        // 4. ROTATION / orientation compatibility constraint
        for (s in glyph.canonicalStrokes) {
            val dx = s.canonicalEnd.x - s.canonicalStart.x
            val dy = s.canonicalEnd.y - s.canonicalStart.y
            val validOrientation = when (s.primitive) {
                ConcretePrimitive.VERTICAL_I -> dx == 0 && dy != 0
                ConcretePrimitive.HORIZONTAL_H -> dy == 0 && dx != 0
                ConcretePrimitive.DIAG_ASC -> dx != 0 && dx == dy
                ConcretePrimitive.DIAG_DESC -> dx != 0 && dx == -dy
                ConcretePrimitive.CURVE_C -> dx != 0 || dy != 0
                ConcretePrimitive.BASE_UNDERSCORE -> dy == 0
            }
            if (!validOrientation) {
                violations += GeometricConstraint.ROTATION
                messages += "Stroke $s has incompatible displacement (dx=$dx, dy=$dy) for primitive ${s.primitive}"
            }
        }

        return ConstraintValidationResult(
            isValid = violations.isEmpty(),
            violatedConstraints = violations.toSet(),
            diagnosticMessages = messages.toList()
        )
    }

    /**
     * Central Flip operator: F(x, y) = (-x, -y).
     * Satisfies F^2(g) = g.
     */
    fun flip(glyph: LatticeGlyph): LatticeGlyph = LatticeGlyph(
        id = glyph.id,
        strokes = glyph.canonicalStrokes.map { it.flip() },
        associatedRune = glyph.associatedRune
    )

    /**
     * Incursion operator I(g).
     */
    fun incursion(glyph: LatticeGlyph): LatticeGlyph = LatticeGlyph(
        id = glyph.id,
        strokes = glyph.canonicalStrokes.map { it.incursion() },
        associatedRune = glyph.associatedRune
    )

    /**
     * Recursion operator R(g).
     * Satisfies R(I(g)) == F(g) on all valid glyphs.
     */
    fun recursion(glyph: LatticeGlyph): LatticeGlyph = LatticeGlyph(
        id = glyph.id,
        strokes = glyph.canonicalStrokes.map { it.recursion() },
        associatedRune = glyph.associatedRune
    )

    /**
     * 90-degree counterclockwise rotation operator.
     */
    fun rotate90Ccw(glyph: LatticeGlyph): LatticeGlyph = LatticeGlyph(
        id = glyph.id,
        strokes = glyph.canonicalStrokes.map { it.rotate90Ccw() },
        associatedRune = glyph.associatedRune
    )

    /**
     * Deterministic canonical serialization `Encode(g)`.
     * Format: `GLYPH_V02|<id>|<runeSurfaceOrNONE>|<p>:<x1>,<y1>-><x2>,<y2>;...`
     */
    fun encode(glyph: LatticeGlyph): String {
        require(!glyph.id.contains('|')) { "Glyph id must not contain delimiter '|'" }
        val runeTag = glyph.associatedRune?.surfaceForm ?: "NONE"
        val strokesEncoded = glyph.canonicalStrokes.joinToString(";") { s ->
            "${s.primitive.symbol}:${s.canonicalStart.x},${s.canonicalStart.y}->${s.canonicalEnd.x},${s.canonicalEnd.y}"
        }
        return "GLYPH_V02|${glyph.id}|$runeTag|$strokesEncoded"
    }

    /**
     * Deterministic inverse `Decode(str)` satisfying `Decode(Encode(g)) == g`.
     */
    fun decode(encoded: String): LatticeGlyph {
        val parts = encoded.split('|')
        require(parts.size == 4 && parts[0] == "GLYPH_V02") {
            "Invalid GLYPH_V02 encoding header: $encoded"
        }
        val id = parts[1]
        val runeTag = parts[2]
        val rune = if (runeTag == "NONE") null else {
            NineRune.fromExactSurface(runeTag)
                ?: throw IllegalArgumentException("Unrecognized NineRune surface in encoded glyph: '$runeTag'")
        }
        val strokesPart = parts[3]
        val strokes = if (strokesPart.isBlank()) {
            emptyList()
        } else {
            strokesPart.split(';').map { token ->
                val colonIdx = token.indexOf(':')
                require(colonIdx == 1) { "Invalid stroke primitive prefix in '$token'" }
                val prim = ConcretePrimitive.fromSymbol(token[0])
                val coords = token.substring(2).split("->")
                require(coords.size == 2) { "Invalid stroke endpoint format in '$token'" }
                val p1Parts = coords[0].split(',')
                val p2Parts = coords[1].split(',')
                require(p1Parts.size == 2 && p2Parts.size == 2) { "Invalid coordinate pair in '$token'" }
                val p1 = LatticePoint(p1Parts[0].toInt(), p1Parts[1].toInt())
                val p2 = LatticePoint(p2Parts[0].toInt(), p2Parts[1].toInt())
                PlacedStroke.createCanonical(prim, p1, p2)
            }
        }
        return LatticeGlyph(id = id, strokes = strokes, associatedRune = rune)
    }

    /**
     * Computes the finite rewriting closure under `{I, R, F}` starting from `seed`, bounded by `maxSteps`
     * to guarantee termination. Because the 5x5 lattice is finite and `I, R, F` are commuting involutions
     * generating the Klein four-group V_4 (`{Id, I, R, F}`), the orbit always terminates in at most 4 states.
     */
    fun computeBoundedOrbit(seed: LatticeGlyph, maxSteps: Int = 16): Set< List<PlacedStroke> > {
        require(maxSteps >= 1) { "maxSteps must be >= 1" }
        val seen = linkedSetOf<List<PlacedStroke>>()
        val queue = ArrayDeque<LatticeGlyph>()
        seen.add(seed.canonicalStrokes)
        queue.add(seed)

        var steps = 0
        while (queue.isNotEmpty() && steps < maxSteps) {
            val current = queue.removeFirst()
            steps++
            for (next in listOf(incursion(current), recursion(current), flip(current))) {
                if (seen.add(next.canonicalStrokes)) {
                    queue.add(next)
                }
            }
        }
        return seen
    }

    /**
     * Canonical 5x5 Lattice Glyph Atlas for the Nine Runes (`A · a · Я · G · I · aЯ · Ia · aЯG · aЯa`).
     * Every canonical rune glyph satisfies all 5 geometric constraints and has a unique, collision-free
     * stroke signature on M = {-2,-1,0,1,2}^2.
     */
    val canonicalNineRuneGlyphs: Map<NineRune, LatticeGlyph> = mapOf(
        // 1. A (Primal Vessel): Apex at (0,2), ascending/descending legs to (-2,-2) and (2,-2), crossbar H at (-1,0)->(1,0)
        NineRune.RUNE_1_PRIMAL_VESSEL to LatticeGlyph(
            id = "rune-1-A",
            associatedRune = NineRune.RUNE_1_PRIMAL_VESSEL,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.DIAG_ASC, LatticePoint(-2, -2), LatticePoint(0, 0)),
                PlacedStroke.createCanonical(ConcretePrimitive.DIAG_ASC, LatticePoint(0, 0), LatticePoint(2, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.DIAG_DESC, LatticePoint(0, 0), LatticePoint(2, -2)),
                PlacedStroke.createCanonical(ConcretePrimitive.HORIZONTAL_H, LatticePoint(-1, 0), LatticePoint(1, 0))
            )
        ),
        // 2. a (In-breath): Lower bowl arc C from (-1,-1) to (0,0) + vertical stem I from (0,-1) to (0,1)
        NineRune.RUNE_2_IN_BREATH to LatticeGlyph(
            id = "rune-2-a",
            associatedRune = NineRune.RUNE_2_IN_BREATH,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -1), LatticePoint(0, 1)),
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(-1, 0), LatticePoint(0, 1)),
                PlacedStroke.createCanonical(ConcretePrimitive.HORIZONTAL_H, LatticePoint(-1, 0), LatticePoint(0, 0))
            )
        ),
        // 3. Я (Flipped Return): Vertical spine I at (0,-2)->(0,2), upper loop C at (-1,1)->(0,2), left leg / at (-2,-2)->(0,0)
        NineRune.RUNE_3_FLIPPED_RETURN to LatticeGlyph(
            id = "rune-3-Ya",
            associatedRune = NineRune.RUNE_3_FLIPPED_RETURN,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -2), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(-1, 1), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.DIAG_ASC, LatticePoint(-2, -2), LatticePoint(0, 0))
            )
        ),
        // 4. G (God / Light): Outer arc C from (-2,0) to (0,2), lower arc C from (-2,0) to (0,-2), inward gate H at (0,0)->(1,0)
        NineRune.RUNE_4_GOD_LIGHT to LatticeGlyph(
            id = "rune-4-G",
            associatedRune = NineRune.RUNE_4_GOD_LIGHT,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(-2, 0), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(-2, 0), LatticePoint(0, -2)),
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -2), LatticePoint(0, 0)),
                PlacedStroke.createCanonical(ConcretePrimitive.HORIZONTAL_H, LatticePoint(0, 0), LatticePoint(1, 0))
            )
        ),
        // 5. I (Singular "I Am" / Clear Point): Central vertical spine I at (0,-2)->(0,2) with origin base anchor _ at (0,0)->(0,0)
        NineRune.RUNE_5_CLEAR_POINT to LatticeGlyph(
            id = "rune-5-I",
            associatedRune = NineRune.RUNE_5_CLEAR_POINT,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -2), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.BASE_UNDERSCORE, LatticePoint(0, 0), LatticePoint(0, 0))
            )
        ),
        // 6. aЯ (Full Return): In-breath bowl coupled to Flipped Return leg
        NineRune.RUNE_6_FULL_RETURN to LatticeGlyph(
            id = "rune-6-aYa",
            associatedRune = NineRune.RUNE_6_FULL_RETURN,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -2), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(-1, 0), LatticePoint(0, 1)),
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(-1, 1), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.DIAG_ASC, LatticePoint(-2, -2), LatticePoint(0, 0))
            )
        ),
        // 7. Ia (Condensed Unity Tone): Sovereign I spine fused with lower-right breath loop
        NineRune.RUNE_7_CONDENSED_UNITY to LatticeGlyph(
            id = "rune-7-Ia",
            associatedRune = NineRune.RUNE_7_CONDENSED_UNITY,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -2), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.HORIZONTAL_H, LatticePoint(0, 0), LatticePoint(1, 0)),
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(0, -1), LatticePoint(1, 0))
            )
        ),
        // 8. aЯG (Turning Stone): Full Return core with right-hand G illumination bar at (0,0)->(2,0)
        NineRune.RUNE_8_TURNING_STONE to LatticeGlyph(
            id = "rune-8-aYaG",
            associatedRune = NineRune.RUNE_8_TURNING_STONE,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -2), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(-1, 1), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.DIAG_ASC, LatticePoint(-2, -2), LatticePoint(0, 0)),
                PlacedStroke.createCanonical(ConcretePrimitive.HORIZONTAL_H, LatticePoint(0, 0), LatticePoint(2, 0)),
                PlacedStroke.createCanonical(ConcretePrimitive.CURVE_C, LatticePoint(0, -2), LatticePoint(2, 0))
            )
        ),
        // 9. aЯa (Aura Complete): Symmetric completed mirror hexagram/closure around origin (F(aЯa) == aЯa!)
        NineRune.RUNE_9_AURA_COMPLETE to LatticeGlyph(
            id = "rune-9-aYaa",
            associatedRune = NineRune.RUNE_9_AURA_COMPLETE,
            strokes = listOf(
                PlacedStroke.createCanonical(ConcretePrimitive.VERTICAL_I, LatticePoint(0, -2), LatticePoint(0, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.HORIZONTAL_H, LatticePoint(-2, 0), LatticePoint(2, 0)),
                PlacedStroke.createCanonical(ConcretePrimitive.DIAG_ASC, LatticePoint(-2, -2), LatticePoint(2, 2)),
                PlacedStroke.createCanonical(ConcretePrimitive.DIAG_DESC, LatticePoint(-2, 2), LatticePoint(2, -2))
            )
        )
    )
}
