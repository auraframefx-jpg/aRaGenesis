package dev.aurakai.kernel.epistemic

import java.security.MessageDigest
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
        val raw = "${id.value.length}:${id.value}|${sourceId.length}:$sourceId|${timestamp.epochMillis}|$rawOriginalValue|${sourceUri.length}:$sourceUri|${methodology.length}:$methodology|${sortedParents.length}:$sortedParents|${sourceDigest.value.length}:${sourceDigest.value}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
