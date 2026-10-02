package dev.aurakai.kernel.pipeline

import dev.aurakai.kernel.epistemic.*

/**
 * SOLVE Engine Implementation.
 * Breaks external boundary into immutable ASH observations.
 *
 * SOLVE responsibilities:
 * 1. Receive raw string input
 * 2. Reject blank input
 * 3. Preserve the exact raw original value in provenance
 * 4. Assign an AshId
 * 5. Attach provenance
 * 6. Classify observation telemetry type
 * 7. Return immutable AshRecord
 *
 * SOLVE does NOT parse commands, invoke AI, infer intent, verify claims,
 * create hypotheses, or execute embedded instructions.
 */
class SolveEngine {

    fun solve(
        rawInput: String,
        sourceId: String,
        timestamp: KernelTimestamp = KernelTimestamp(System.currentTimeMillis()),
        telemetryType: TelemetryType = TelemetryType.UNCLASSIFIED
    ): AshRecord {
        require(rawInput.isNotBlank()) {
            "Blank or whitespace-only input rejected by SOLVE engine."
        }

        // Preserve exact raw value in provenance without transformation or normalization
        val provenance = Provenance(
            sourceId = sourceId,
            timestamp = timestamp,
            rawOriginalValue = rawInput
        )

        val id = AshId("ash-${digest(rawInput)}-$sourceId-${timestamp.epochMillis}")

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
