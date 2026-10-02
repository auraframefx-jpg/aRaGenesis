package dev.aurakai.kernel.pipeline

import dev.aurakai.kernel.epistemic.*
import java.util.concurrent.atomic.AtomicLong

/**
 * SOLVE Engine Implementation.
 * Breaks external boundary into immutable ASH observations.
 */
class SolveEngine {

    companion object {
        private val sequence = AtomicLong(0)
    }

    fun solve(
        rawInput: String,
        sourceId: String,
        timestamp: KernelTimestamp = KernelTimestamp(System.currentTimeMillis()),
        telemetryType: TelemetryType = TelemetryType.UNCLASSIFIED
    ): AshRecord {
        require(rawInput.isNotBlank()) {
            "Blank or whitespace-only input rejected by SOLVE engine."
        }

        val seq = sequence.incrementAndGet()
        val provId = ProvenanceId("prov-$sourceId-${timestamp.epochMillis}-$seq")

        val provenance = Provenance(
            id = provId,
            sourceId = sourceId,
            timestamp = timestamp,
            rawOriginalValue = rawInput,
            sourceUri = "uri://kernel/$sourceId",
            methodology = "SOULSCRIPT_SOLVE",
            parentGraph = emptyList(),
            sourceDigest = Digest(digest(rawInput))
        )

        val id = AshId("ash-${digest(rawInput)}-$sourceId-${timestamp.epochMillis}-$seq")

        return AshRecord(
            id = id,
            payload = rawInput,
            provenance = provenance,
            telemetryType = telemetryType
        )
    }

    private fun digest(input: String): String {
        var h = 0L
        for (ch in input) {
            h = 31 * h + ch.code
        }
        return h.toULong().toString(16)
    }
}
