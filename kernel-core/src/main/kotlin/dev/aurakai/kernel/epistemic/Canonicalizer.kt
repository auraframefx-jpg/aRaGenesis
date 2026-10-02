package dev.aurakai.kernel.epistemic

import java.security.MessageDigest

/**
 * Deterministic Canonicalization Engine (Canonicalize(x)).
 * Implements unambiguous, injective canonical string formatting and SHA-256 digest computation
 * for binding verification receipts to exact evaluated hypotheses and observations.
 *
 * Rules:
 * - Null fields: Encoded as "\u0000NULL\u0000"
 * - Unit Delimiter: "\u001F"
 * - Record Delimiter: "\u001E"
 * - Collections: Lexicographically sorted by ID / value prior to joining
 * - Character Encoding: UTF-8
 */
object Canonicalizer {

    private const val UNIT_SEP = "\u001F"
    private const val RECORD_SEP = "\u001E"
    private const val NULL_TAG = "\u0000NULL\u0000"

    fun canonicalizeHypothesis(h: Hypothesis): String {
        val sortedSupp = h.supportingEvidence
            .sortedBy { it.ashId.value }
            .joinToString(RECORD_SEP) { "${it.ashId.value}$UNIT_SEP${it.relation.name}" }

        val sortedCount = h.counterEvidence
            .sortedBy { it.evidence.ashId.value }
            .joinToString(RECORD_SEP) { "${it.evidence.ashId.value}$UNIT_SEP${it.assessment.name}" }

        val nullHypStr = h.nullHypothesisId?.value ?: NULL_TAG

        return listOf(
            "HYP_ID:$UNIT_SEP${h.id.value}",
            "STMT:$UNIT_SEP${h.statement}",
            "SUPP:$UNIT_SEP$sortedSupp",
            "COUNT:$UNIT_SEP$sortedCount",
            "NULL_HYP:$UNIT_SEP$nullHypStr",
            "GRADE:$UNIT_SEP${h.evidenceGrade.name}",
            "PROV_STAT:$UNIT_SEP${h.provenanceStatus.name}",
            "STATUS:$UNIT_SEP${h.status.name}"
        ).joinToString(RECORD_SEP)
    }

    fun canonicalizeObservation(obs: AshRecord): String {
        val sortedParents = obs.provenance.parents
            .map { it.value }
            .sorted()
            .joinToString(",")

        return listOf(
            "OBS_ID:$UNIT_SEP${obs.id.value}",
            "PAYLOAD:$UNIT_SEP${obs.payload}",
            "SRC:$UNIT_SEP${obs.provenance.sourceId}",
            "RAW:$UNIT_SEP${obs.provenance.rawOriginalValue}",
            "TIMESTAMP:$UNIT_SEP${obs.provenance.timestamp.epochMillis}",
            "PARENTS:$UNIT_SEP$sortedParents"
        ).joinToString(RECORD_SEP)
    }

    fun computeDigest(hypotheses: List<Hypothesis>, observations: List<AshRecord>): Digest {
        val sortedHypotheses = hypotheses.sortedBy { it.id.value }
            .joinToString("\u001D") { canonicalizeHypothesis(it) }

        val sortedObservations = observations.sortedBy { it.id.value }
            .joinToString("\u001D") { canonicalizeObservation(it) }

        val canonicalString = "HYPOTHESES_BEGIN\u001C$sortedHypotheses\u001CHYPOTHESES_END\u001C" +
                "OBSERVATIONS_BEGIN\u001C$sortedObservations\u001COBSERVATIONS_END"

        val bytes = MessageDigest.getInstance("SHA-256").digest(canonicalString.toByteArray(Charsets.UTF_8))
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return Digest(hex)
    }
}
