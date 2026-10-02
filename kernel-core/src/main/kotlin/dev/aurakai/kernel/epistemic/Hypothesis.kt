package dev.aurakai.kernel.epistemic

@JvmInline
value class HypothesisId(val value: String)

enum class EvidenceGrade {
    DIRECT,
    CORROBORATED,
    PLAUSIBLE,
    WEAK,
    CONTRADICTED
}

enum class ProvenanceStatus {
    UNVERIFIED,
    SOURCE_ATTESTED,
    CRYPTOGRAPHICALLY_VERIFIED
}

enum class EvidenceRelation {
    SUPPORTS,
    COUNTERS,
    CONTEXT
}

data class EvidenceRef(
    val ashId: AshId,
    val relation: EvidenceRelation = EvidenceRelation.SUPPORTS
)

enum class CounterAssessment {
    CONTRADICTORY,
    ALTERNATIVE_EXPLANATION,
    DATA_QUALITY_CONCERN,
    TEMPORAL_CONFLICT,
    IDENTITY_CONFLICT,
    UNRESOLVED
}

data class CounterEvidence(
    val evidence: EvidenceRef,
    val assessment: CounterAssessment = CounterAssessment.UNRESOLVED
)

enum class HypothesisStatus {
    PROPOSED,
    SUPPORTED,
    CONTRADICTED,
    CALIBRATED,
    UNKNOWN
}

/**
 * Epistemic Hypothesis Layer (PHOENIX).
 * Represents structured propositions derived from observations.
 *
 * Invariants:
 * - Hypothesis != Observation
 * - Hypothesis != Fact
 * - Null Hypothesis (H0) is modeled as a HypothesisId reference, preserving H0 != H1 and H0 != Evidence.
 */
data class Hypothesis(
    val id: HypothesisId,
    val statement: String,
    val supportingAshIds: List<AshId> = emptyList(),
    val counterEvidenceAshIds: List<AshId> = emptyList(),
    val supportingEvidence: List<EvidenceRef> = supportingAshIds.map { EvidenceRef(it, EvidenceRelation.SUPPORTS) },
    val counterEvidence: List<CounterEvidence> = counterEvidenceAshIds.map { CounterEvidence(EvidenceRef(it, EvidenceRelation.COUNTERS), CounterAssessment.UNRESOLVED) },
    val nullHypothesisId: HypothesisId? = null,
    val evidenceGrade: EvidenceGrade = EvidenceGrade.PLAUSIBLE,
    val provenanceStatus: ProvenanceStatus = ProvenanceStatus.UNVERIFIED,
    val status: HypothesisStatus = HypothesisStatus.PROPOSED
) {
    val supportingAsh: List<AshId> = supportingAshIds.toList()
    val counterAsh: List<AshId> = counterEvidenceAshIds.toList()
    val suppEvidence: List<EvidenceRef> = supportingEvidence.toList()
    val countEvidence: List<CounterEvidence> = counterEvidence.toList()

    fun computeDigest(): String {
        val sortedSupp = suppEvidence.map { "${it.ashId.value}:${it.relation.name}" }.sorted().joinToString(",")
        val sortedCount = countEvidence.map { "${it.evidence.ashId.value}:${it.assessment.name}" }.sorted().joinToString(",")
        val nullHypStr = nullHypothesisId?.value ?: "NONE"
        val raw = "${id.value}|$statement|$sortedSupp|$sortedCount|$nullHypStr|${evidenceGrade.name}|${provenanceStatus.name}|${status.name}"

        var acc = 0L
        for (ch in raw) {
            acc = 31 * acc + ch.code
        }
        return acc.toULong().toString(16)
    }
}
