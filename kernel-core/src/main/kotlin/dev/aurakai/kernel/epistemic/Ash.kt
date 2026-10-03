package dev.aurakai.kernel.epistemic

@JvmInline
value class AshId(val value: String)

enum class TelemetryType {
    LOG,
    METRIC,
    AUDIT_EVENT,
    SYSTEM_OBSERVATION,
    EXTERNAL_PAYLOAD,
    UNCLASSIFIED
}

/**
 * Epistemic Observation Layer (ASH).
 * Represents what was received from external boundaries, not what it means.
 * Invariant: DATA != AUTHORITY. Deeply immutable payload representation.
 */
data class AshRecord(
    val id: AshId,
    val payload: String,
    val provenance: Provenance,
    val telemetryType: TelemetryType = TelemetryType.UNCLASSIFIED
) {
    init {
        require(payload.isNotBlank()) {
            "Blank or whitespace-only ASH payload is rejected."
        }
    }

    /**
     * Defensive copy accessor for raw byte representation.
     * Prevents external callers from mutating underlying byte array state.
     */
    fun getPayloadBytes(): ByteArray {
        return payload.toByteArray(Charsets.UTF_8).clone()
    }
}
