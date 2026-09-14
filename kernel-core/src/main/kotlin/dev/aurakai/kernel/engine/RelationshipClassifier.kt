package dev.aurakai.kernel.engine

import dev.aurakai.kernel.domain.*

class ContentDrivenRelationshipClassifier : RelationshipClassifier {

    override fun classifyRelationship(
        targetClaim: String,
        item: RawEvidenceItem
    ): EvidenceRelationship {
        val contentLower = item.rawContent.lowercase()

        // 1. Check for hard counterevidence / contradiction markers
        val contradictionMarkers = listOf(
            "is absent",
            "does not define",
            "not present",
            "contradicts",
            "fails to",
            "no call-site",
            "missing"
        )
        if (contradictionMarkers.any { contentLower.contains(it) }) {
            return EvidenceRelationship.CONTRADICTS
        }

        // 2. Check for corroborating support markers
        val supportMarkers = listOf(
            "defines",
            "supports",
            "implemented",
            "verified"
        )
        if (supportMarkers.any { contentLower.contains(it) }) {
            return EvidenceRelationship.SUPPORTS
        }

        return EvidenceRelationship.UNRESOLVED
    }
}
