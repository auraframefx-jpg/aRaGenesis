package dev.aurakai.kernel.epistemic

@JvmInline
value class KernelTimestamp(val epochMillis: Long)

/**
 * Epistemic Provenance Contract.
 * Preserves source identity, timestamp, and the exact unmodified raw original value.
 * Downstream transformations must never erase or overwrite rawOriginalValue.
 */
data class Provenance(
    val sourceId: String,
    val timestamp: KernelTimestamp,
    val rawOriginalValue: String
)
