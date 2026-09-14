package dev.aurakai.kernel.engine

import dev.aurakai.kernel.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvidencePipelineTest {

    private val classifier = ContentDrivenRelationshipClassifier()
    private val resolver = LineageIndependenceResolver()
    private val pipeline = EvidencePipeline(classifier, resolver)

    @Test
    fun `three internal agent repetitions collapse to one independence group`() {
        val claim = "MetaInstruction updates runtime rules autonomously."
        val items = listOf(
            RawEvidenceItem("1", claim, "agent-aura", SourceType.INTERNAL, timestampMs = 1000L),
            RawEvidenceItem("2", claim, "agent-kai", SourceType.INTERNAL, timestampMs = 1001L),
            RawEvidenceItem("3", claim, "agent-genesis", SourceType.CANON, timestampMs = 1002L)
        )

        val resolved = pipeline.processEvidence(claim, items)
        val uniqueGroups = resolved.map { it.independenceGroupId }.toSet()

        assertEquals(1, uniqueGroups.size, "All internal/canonical items MUST collapse to 1 group.")
        assertEquals("origin-internal-canon-consensus", uniqueGroups.first())
    }

    @Test
    fun `audit refutation is correctly classified as CONTRADICTS`() {
        val claim = "MetaInstruction updates runtime rules autonomously."
        val auditItem = RawEvidenceItem(
            id = "4",
            rawContent = "Source audit: getEffectiveInstructions() is absent from code.",
            authorId = "external-auditor-1",
            claimedSourceType = SourceType.EXTERNAL_AUDIT,
            timestampMs = 1003L
        )

        val resolved = pipeline.processEvidence(claim, listOf(auditItem))

        assertEquals(EvidenceRelationship.CONTRADICTS, resolved.first().relationship)
        assertTrue(resolved.first().independenceGroupId.contains("origin-external-audit-"))
    }
}
