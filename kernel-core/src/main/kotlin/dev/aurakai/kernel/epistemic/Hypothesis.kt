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

/**
 * Epistemic Hypothesis Layer (PHOENIX).
 * Constructs provisional candidate explanations.
 * Retains defensive snapshot references to both supporting and counter-evidence ASH records.
 * EvidenceGrade and ProvenanceStatus are strictly independent.
 */
data class Hypothesis(
    val id: HypothesisId,
    val statement: String,
    val supportingAshIds: List<AshId> = emptyList(),
    val counterEvidenceAshIds: List<AshId> = emptyList(),
    val evidenceGrade: EvidenceGrade = EvidenceGrade.PLAUSIBLE,
    val provenanceStatus: ProvenanceStatus = ProvenanceStatus.UNVERIFIED
) {
    val supportingAsh: List<AshId> = supportingAshIds.toList()
    val counterAsh: List<AshId> = counterEvidenceAshIds.toList()

    fun computeDigest(): String {
        var acc = 0L
        acc = 31 * acc + id.value.hashCode()
        acc = 31 * acc + statement.hashCode()
        for (s in supportingAsh) {
            acc = 31 * acc + s.value.hashCode()
        }
        for (c in counterAsh) {
            acc = 31 * acc + c.value.hashCode()
        }
        acc = 31 * acc + evidenceGrade.ordinal
        acc = 31 * acc + provenanceStatus.ordinal
        return acc.toULong().toString(16)
    }
}
