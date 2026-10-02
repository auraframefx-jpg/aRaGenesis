package dev.aurakai.kernel.epistemic

/**
 * Unique value identifier for a hypothesis.
 */
@JvmInline
value class HypothesisId(val value: String)

/**
 * Taxonomy of evidence strength supporting a hypothesis.
 */
enum class EvidenceGrade {
    DIRECT,
    CORROBORATED,
    PLAUSIBLE,
    WEAK,
    CONTRADICTED
}

/**
 * Cryptographic or attestation status of the provenance of supporting evidence.
 */
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
 *
 * @property id Unique hypothesis identifier.
 * @property statement Textual statement or claim of the hypothesis.
 * @property supportingAshIds Snapshot list of supporting observation IDs.
 * @property counterEvidenceAshIds Snapshot list of counter-evidence observation IDs.
 * @property evidenceGrade Assessment of evidence strength.
 * @property provenanceStatus Attestation level of evidence provenance.
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

    /**
     * Computes a deterministic content digest string for this hypothesis.
     * Incorporates id, statement, sorted supporting ASH IDs, sorted counter-evidence ASH IDs,
     * evidence grade, and provenance status.
     */
    fun computeDigest(): String {
        val sortedSupp = supportingAsh.map { it.value }.sorted().joinToString(",")
        val sortedCount = counterAsh.map { it.value }.sorted().joinToString(",")
        val raw = "${id.value}|$statement|$sortedSupp|$sortedCount|${evidenceGrade.name}|${provenanceStatus.name}"

        var acc = 0L
        for (ch in raw) {
            acc = 31 * acc + ch.code
        }
        return acc.toULong().toString(16)
    }
}
