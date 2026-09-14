package dev.aurakai.kernel.engine

import dev.aurakai.kernel.domain.*

class EvidencePipeline(
    private val classifier: RelationshipClassifier,
    private val resolver: IndependenceResolver
) {

    fun processEvidence(
        targetClaim: String,
        rawItems: List<RawEvidenceItem>
    ): List<ResolvedEvidenceItem> {
        return rawItems.map { raw ->
            val relationship = classifier.classifyRelationship(targetClaim, raw)
            val groupId = resolver.resolveGroup(raw)

            // Weight calculation based on relationship and source type
            val weight = when (raw.claimedSourceType) {
                SourceType.EXTERNAL_AUDIT -> 0.95
                SourceType.CONFIG -> 0.30
                SourceType.CANON, SourceType.INTERNAL -> 0.25
            }

            ResolvedEvidenceItem(
                rawItem = raw,
                provenanceHash = raw.id, // In production, this would be a real hash
                independenceGroupId = groupId,
                relationship = relationship,
                effectiveWeight = weight
            )
        }
    }
}
