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
 * Retains references to both supporting and counter-evidence ASH records.
 * EvidenceGrade and ProvenanceStatus are strictly independent.
 */
data class Hypothesis(
    val id: HypothesisId,
    val statement: String,
    val supportingAshIds: List<AshId> = emptyList(),
    val counterEvidenceAshIds: List<AshId> = emptyList(),
    val evidenceGrade: EvidenceGrade = EvidenceGrade.PLAUSIBLE,
    val provenanceStatus: ProvenanceStatus = ProvenanceStatus.UNVERIFIED
)
