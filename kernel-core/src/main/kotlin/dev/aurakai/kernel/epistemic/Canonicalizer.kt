package dev.aurakai.kernel.epistemic

import java.security.MessageDigest

/**
 * Deterministic Canonicalization Engine (Canonicalize(x)).
 * Implements unambiguous, injective length-prefixed canonical string formatting and SHA-256 digest computation.
 */
object Canonicalizer {

    private fun encodeField(value: String?): String {
        if (value == null) return "0:NULL"
        return "${value.length}:$value"
    }

    private fun encodeList(list: List<String>): String {
        val encodedElements = list.joinToString(";") { encodeField(it) }
        return "${list.size}:${encodeField(encodedElements)}"
    }

    fun canonicalizeHypothesis(h: Hypothesis): String {
        val sortedSuppAsh = h.supportingAshIds
            .map { it.value }
            .sorted()
        val encodedSuppAsh = encodeList(sortedSuppAsh)

        val sortedSuppEv = h.supportingEvidence
            .map { "${it.ashId.value}:${it.relation.name}" }
            .sorted()
        val encodedSuppEv = encodeList(sortedSuppEv)

        val sortedCountEv = h.counterEvidence
            .map { "${it.evidence.ashId.value}:${it.evidence.relation.name}:${it.assessment.name}" }
            .sorted()
        val encodedCountEv = encodeList(sortedCountEv)

        val nullHypStr = encodeField(h.nullHypothesisId?.value)

        return listOf(
            "HYP_ID=${encodeField(h.id.value)}",
            "STMT=${encodeField(h.statement)}",
            "SUPP_ASH=$encodedSuppAsh",
            "SUPP_EV=$encodedSuppEv",
            "COUNT_EV=$encodedCountEv",
            "NULL_HYP=$nullHypStr",
            "GRADE=${encodeField(h.evidenceGrade.name)}",
            "PROV_STAT=${encodeField(h.provenanceStatus.name)}",
            "STATUS=${encodeField(h.status.name)}"
        ).joinToString("|")
    }

    fun canonicalizeObservation(obs: AshRecord): String {
        val sortedParents = obs.provenance.parents
            .map { it.value }
            .sorted()
        val encodedParents = encodeList(sortedParents)

        return listOf(
            "OBS_ID=${encodeField(obs.id.value)}",
            "PAYLOAD=${encodeField(obs.payload)}",
            "TELEMETRY=${encodeField(obs.telemetryType.name)}",
            "PROV_ID=${encodeField(obs.provenance.id.value)}",
            "SRC=${encodeField(obs.provenance.sourceId)}",
            "RAW=${encodeField(obs.provenance.rawOriginalValue)}",
            "TIMESTAMP=${encodeField(obs.provenance.timestamp.epochMillis.toString())}",
            "URI=${encodeField(obs.provenance.sourceUri)}",
            "METHODOLOGY=${encodeField(obs.provenance.methodology)}",
            "SRC_DIGEST=${encodeField(obs.provenance.sourceDigest.value)}",
            "PARENTS=$encodedParents"
        ).joinToString("|")
    }

    fun computeDigest(hypotheses: List<Hypothesis>, observations: List<AshRecord>): Digest {
        val sortedHypotheses = hypotheses.sortedBy { it.id.value }
            .map { canonicalizeHypothesis(it) }
        val encodedHypotheses = encodeList(sortedHypotheses)

        val sortedObservations = observations.sortedBy { it.id.value }
            .map { canonicalizeObservation(it) }
        val encodedObservations = encodeList(sortedObservations)

        val canonicalString = "HYPOTHESES_BEGIN|${encodeField(encodedHypotheses)}|HYPOTHESES_END||" +
                "OBSERVATIONS_BEGIN|${encodeField(encodedObservations)}|OBSERVATIONS_END"

        val bytes = MessageDigest.getInstance("SHA-256").digest(canonicalString.toByteArray(Charsets.UTF_8))
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return Digest(hex)
    }
}
