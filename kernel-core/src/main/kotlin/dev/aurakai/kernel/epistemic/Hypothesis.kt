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
 * Encapsulates immutable snapshot collections of supporting and counter-evidence refs.
 */
class Hypothesis private constructor(
    val id: HypothesisId,
    val statement: String,
    supportingAshIds: List<AshId>,
    counterEvidenceAshIds: List<AshId>,
    supportingEvidence: List<EvidenceRef>,
    counterEvidence: List<CounterEvidence>,
    val nullHypothesisId: HypothesisId?,
    val evidenceGrade: EvidenceGrade,
    val provenanceStatus: ProvenanceStatus,
    val status: HypothesisStatus
) {
    val supportingAshIds: List<AshId> = supportingAshIds.toList()
    val counterEvidenceAshIds: List<AshId> = counterEvidenceAshIds.toList()
    val supportingEvidence: List<EvidenceRef> = supportingEvidence.toList()
    val counterEvidence: List<CounterEvidence> = counterEvidence.toList()

    val supportingAsh: List<AshId> get() = supportingAshIds
    val counterAsh: List<AshId> get() = counterEvidenceAshIds
    val suppEvidence: List<EvidenceRef> get() = supportingEvidence
    val countEvidence: List<CounterEvidence> get() = counterEvidence

    fun copy(
        id: HypothesisId = this.id,
        statement: String = this.statement,
        supportingAshIds: List<AshId> = this.supportingAshIds,
        counterEvidenceAshIds: List<AshId> = this.counterEvidenceAshIds,
        supportingEvidence: List<EvidenceRef> = this.supportingEvidence,
        counterEvidence: List<CounterEvidence> = this.counterEvidence,
        nullHypothesisId: HypothesisId? = this.nullHypothesisId,
        evidenceGrade: EvidenceGrade = this.evidenceGrade,
        provenanceStatus: ProvenanceStatus = this.provenanceStatus,
        status: HypothesisStatus = this.status
    ): Hypothesis {
        return create(
            id = id,
            statement = statement,
            supportingAshIds = supportingAshIds,
            counterEvidenceAshIds = counterEvidenceAshIds,
            supportingEvidence = supportingEvidence,
            counterEvidence = counterEvidence,
            nullHypothesisId = nullHypothesisId,
            evidenceGrade = evidenceGrade,
            provenanceStatus = provenanceStatus,
            status = status
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hypothesis) return false
        return id == other.id &&
                statement == other.statement &&
                supportingAshIds == other.supportingAshIds &&
                counterEvidenceAshIds == other.counterEvidenceAshIds &&
                supportingEvidence == other.supportingEvidence &&
                counterEvidence == other.counterEvidence &&
                nullHypothesisId == other.nullHypothesisId &&
                evidenceGrade == other.evidenceGrade &&
                provenanceStatus == other.provenanceStatus &&
                status == other.status
    }

    override fun hashCode(): Int {
        var res = id.hashCode()
        res = 31 * res + statement.hashCode()
        res = 31 * res + supportingAshIds.hashCode()
        res = 31 * res + counterEvidenceAshIds.hashCode()
        res = 31 * res + supportingEvidence.hashCode()
        res = 31 * res + counterEvidence.hashCode()
        res = 31 * res + (nullHypothesisId?.hashCode() ?: 0)
        res = 31 * res + evidenceGrade.hashCode()
        res = 31 * res + provenanceStatus.hashCode()
        res = 31 * res + status.hashCode()
        return res
    }

    override fun toString(): String {
        return "Hypothesis(id='${id.value}', statement='$statement', evidenceGrade=$evidenceGrade, status=$status)"
    }

    fun computeDigest(): String {
        return Canonicalizer.canonicalizeHypothesis(this)
    }

    companion object {
        fun create(
            id: HypothesisId,
            statement: String,
            supportingAshIds: List<AshId> = emptyList(),
            counterEvidenceAshIds: List<AshId> = emptyList(),
            supportingEvidence: List<EvidenceRef> = supportingAshIds.map { EvidenceRef(it, EvidenceRelation.SUPPORTS) },
            counterEvidence: List<CounterEvidence> = counterEvidenceAshIds.map { CounterEvidence(EvidenceRef(it, EvidenceRelation.COUNTERS), CounterAssessment.UNRESOLVED) },
            nullHypothesisId: HypothesisId? = null,
            evidenceGrade: EvidenceGrade = EvidenceGrade.PLAUSIBLE,
            provenanceStatus: ProvenanceStatus = ProvenanceStatus.UNVERIFIED,
            status: HypothesisStatus = HypothesisStatus.PROPOSED
        ): Hypothesis {
            return Hypothesis(
                id = id,
                statement = statement,
                supportingAshIds = supportingAshIds.toList(),
                counterEvidenceAshIds = counterEvidenceAshIds.toList(),
                supportingEvidence = supportingEvidence.toList(),
                counterEvidence = counterEvidence.toList(),
                nullHypothesisId = nullHypothesisId,
                evidenceGrade = evidenceGrade,
                provenanceStatus = provenanceStatus,
                status = status
            )
        }

        operator fun invoke(
            id: HypothesisId,
            statement: String,
            supportingAshIds: List<AshId> = emptyList(),
            counterEvidenceAshIds: List<AshId> = emptyList(),
            supportingEvidence: List<EvidenceRef> = supportingAshIds.map { EvidenceRef(it, EvidenceRelation.SUPPORTS) },
            counterEvidence: List<CounterEvidence> = counterEvidenceAshIds.map { CounterEvidence(EvidenceRef(it, EvidenceRelation.COUNTERS), CounterAssessment.UNRESOLVED) },
            nullHypothesisId: HypothesisId? = null,
            evidenceGrade: EvidenceGrade = EvidenceGrade.PLAUSIBLE,
            provenanceStatus: ProvenanceStatus = ProvenanceStatus.UNVERIFIED,
            status: HypothesisStatus = HypothesisStatus.PROPOSED
        ): Hypothesis {
            return create(
                id = id,
                statement = statement,
                supportingAshIds = supportingAshIds,
                counterEvidenceAshIds = counterEvidenceAshIds,
                supportingEvidence = supportingEvidence,
                counterEvidence = counterEvidence,
                nullHypothesisId = nullHypothesisId,
                evidenceGrade = evidenceGrade,
                provenanceStatus = provenanceStatus,
                status = status
            )
        }
    }
}
