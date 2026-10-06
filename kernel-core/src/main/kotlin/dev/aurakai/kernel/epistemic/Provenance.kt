package dev.aurakai.kernel.epistemic

import java.util.Collections

@JvmInline
value class KernelTimestamp(val epochMillis: Long)

@JvmInline
value class ProvenanceId(val value: String)

@JvmInline
value class Digest(val value: String)

/**
 * Epistemic Provenance Contract.
 * Preserves source identity, timestamp, lineage parent graph, and exact unmodified raw original value.
 */
data class Provenance(
    val id: ProvenanceId,
    val sourceId: String,
    val timestamp: KernelTimestamp,
    val rawOriginalValue: String,
    val sourceUri: String = "uri://kernel/$sourceId",
    val methodology: String = "SOULSCRIPT_SOLVE",
    val parentGraph: List<ProvenanceId> = emptyList(),
    val sourceDigest: Digest = Digest(rawOriginalValue.hashCode().toULong().toString(16))
) {
    val parents: List<ProvenanceId> = Collections.unmodifiableList(parentGraph.toList().sortedBy { it.value })

    init {
        // Enforce unmodifiable wrapper check on parentGraph in toString / copy operations if needed
    }

    fun computeDigest(): String {
        val sortedParents = parents.map { it.value }.joinToString(",")
        val raw = "${id.value}|$sourceId|${timestamp.epochMillis}|$rawOriginalValue|$sourceUri|$methodology|$sortedParents|${sourceDigest.value}"
        var acc = 0L
        for (ch in raw) {
            acc = 31 * acc + ch.code
        }
        return acc.toULong().toString(16)
    }
}
