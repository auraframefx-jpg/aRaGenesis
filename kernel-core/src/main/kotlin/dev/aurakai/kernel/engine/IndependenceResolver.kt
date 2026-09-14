package dev.aurakai.kernel.engine

import dev.aurakai.kernel.domain.*

class LineageIndependenceResolver : IndependenceResolver {

    override fun resolveGroup(item: RawEvidenceItem): String {
        return when (item.claimedSourceType) {
            // INVARIANT: All internal agents and canonical entries collapse to 1 group
            SourceType.CANON, SourceType.INTERNAL -> "origin-internal-canon-consensus"
            SourceType.EXTERNAL_AUDIT -> "origin-external-audit-${item.authorId}"
            SourceType.CONFIG -> "origin-config-lineage-${item.authorId}"
        }
    }
}
