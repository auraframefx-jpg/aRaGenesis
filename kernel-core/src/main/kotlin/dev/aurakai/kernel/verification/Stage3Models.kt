package dev.aurakai.kernel.verification

import dev.aurakai.kernel.epistemic.KernelTimestamp
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@JvmInline
value class EpochId(val value: Long)

/**
 * Kernel-owned single-use Epoch Nonce for anti-replay enforcement.
 */
@ConsistentCopyVisibility
data class EpochNonce private constructor(
    val epoch: EpochId,
    val nonceValue: String,
    val timestamp: KernelTimestamp
) {
    companion object {
        private val consumedNonces = ConcurrentHashMap.newKeySet<String>()

        internal fun generate(epoch: EpochId = EpochId(1L), timestamp: KernelTimestamp = KernelTimestamp(System.currentTimeMillis())): EpochNonce {
            val digestBytes = MessageDigest.getInstance("SHA-256").digest(
                "${epoch.value}:${timestamp.epochMillis}:${System.nanoTime()}".toByteArray()
            )
            val nonceHex = digestBytes.joinToString("") { "%02x".format(it) }
            return EpochNonce(epoch, nonceHex, timestamp)
        }

        fun isConsumed(nonceHex: String): Boolean = consumedNonces.contains(nonceHex)

        internal fun consume(nonceHex: String): Boolean {
            return consumedNonces.add(nonceHex)
        }

        fun clearConsumedForTesting() {
            consumedNonces.clear()
        }
    }
}

/**
 * Immutable cryptographic binding linking action proposal directly to verified blueprint snapshot.
 */
data class SnapshotBinding(
    val blueprintDigest: String,
    val observationDigests: List<String>,
    val falsificationTraceDigest: String,
    val snapshotDigest: String
) {
    val observationDigestsReadOnly: List<String> = Collections.unmodifiableList(observationDigests.toList())

    init {
        require(blueprintDigest.isNotBlank()) { "Blueprint digest cannot be blank." }
        require(snapshotDigest.isNotBlank()) { "Snapshot digest cannot be blank." }
    }
}

/**
 * Immutable Quarantine Scar recording state rejection into permanent Ouroboros lineage.
 */
data class QuarantineScar(
    val scarId: String,
    val fromEpoch: EpochId,
    val contradictionDigest: String,
    val falsificationArtifactDigest: String,
    val previousStateDigest: String,
    val newStateDigest: String,
    val reasonCode: String,
    val timestamp: KernelTimestamp
)
