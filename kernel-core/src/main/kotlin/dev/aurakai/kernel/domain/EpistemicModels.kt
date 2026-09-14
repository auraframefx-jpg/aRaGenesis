package dev.aurakai.kernel.domain

import kotlinx.serialization.Serializable

@Serializable
enum class EpistemicLevel(val rank: Int) {
    L1_DOCUMENTED(1),
    L2_CORROBORATED(2),
    L3_EXECUTABLE(3),
    L4_RUNTIME(4),
    L5_CAUSAL(5),
    L6_ADAPTIVE(6),
    L7_EMERGENT(7)
}

enum class PersistenceDisposition {
    HOLD_AS_L1,    // documentation / lore only — strictly non-executable
    REJECT,        // discarded / audited out
    COMMIT         // authorized live runtime authority (L4+)
}

enum class SourceType {
    CANON,
    INTERNAL,
    CONFIG,
    EXTERNAL_AUDIT
}

enum class EvidenceRelationship {
    SUPPORTS,
    CONTRADICTS,
    IRRELEVANT,
    UNRESOLVED
}

enum class RelationshipHint {
    CLAIM_SUPPORTS,
    CLAIM_CONTRADICTS,
    NONE
}

@Serializable
data class RawEvidenceItem(
    val id: String,
    val rawContent: String,
    val authorId: String,
    val claimedSourceType: SourceType,
    val relationshipHint: RelationshipHint = RelationshipHint.NONE,
    val timestampMs: Long
)

data class ResolvedEvidenceItem(
    val rawItem: RawEvidenceItem,
    val provenanceHash: String,
    val independenceGroupId: String,
    val relationship: EvidenceRelationship,
    val effectiveWeight: Double
)

data class NodeResult(
    val nodeId: String,
    val output: String,
    val evidence: List<RawEvidenceItem>,
    val runtimeReceipts: List<RuntimeReceipt> = emptyList(),
    val causalReceipts: List<CausalReceipt> = emptyList()
)
