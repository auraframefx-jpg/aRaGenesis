package dev.aurakai.kernel.verification

import dev.aurakai.kernel.epistemic.ConstitutionalViolationException
import dev.aurakai.kernel.epistemic.KernelTimestamp
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Immutable audit record marking single-use consumption of an authoritative verification receipt.
 */
data class ReceiptConsumptionRecord(
    val receiptId: ReceiptId,
    val nonceValue: String,
    val epoch: EpochId,
    val sequenceNumber: Long,
    val timestamp: KernelTimestamp
)

/**
 * Authoritative Thread-Safe Atomic Receipt & Nonce Consumption Ledger (GAP-02 / H-003, H-004).
 *
 * Guarantees effectively-once execution semantics by enforcing atomic check-and-set
 * on both the ReceiptId and the EpochNonce in a single atomic operation.
 */
class AtomicReceiptLedger private constructor() {

    private val consumedReceipts = ConcurrentHashMap<ReceiptId, ReceiptConsumptionRecord>()
    private val consumedNonces = ConcurrentHashMap<String, ReceiptId>()
    private val monotonicSequence = AtomicLong(0L)

    /**
     * Atomically consumes both the ReceiptId and EpochNonce.
     *
     * @return [ReceiptConsumptionRecord] if consumption succeeded.
     * @throws ConstitutionalViolationException if either the receipt or nonce was previously consumed.
     */
    @Synchronized
    fun consume(
        receiptId: ReceiptId,
        epochNonce: EpochNonce,
        timestamp: KernelTimestamp = KernelTimestamp(System.currentTimeMillis())
    ): ReceiptConsumptionRecord {
        if (consumedReceipts.containsKey(receiptId)) {
            val existing = consumedReceipts[receiptId]!!
            throw ConstitutionalViolationException(
                "RECEIPT REPLAY DETECTED: Receipt ${receiptId.value} was already consumed in sequence ${existing.sequenceNumber}."
            )
        }

        if (consumedNonces.containsKey(epochNonce.nonceValue)) {
            val boundReceipt = consumedNonces[epochNonce.nonceValue]!!
            throw ConstitutionalViolationException(
                "NONCE REPLAY DETECTED: EpochNonce ${epochNonce.nonceValue} was already consumed by Receipt ${boundReceipt.value}."
            )
        }

        // Also cross-register with EpochNonce companion for backward compatibility
        EpochNonce.consume(epochNonce.nonceValue)

        val seq = monotonicSequence.incrementAndGet()
        val record = ReceiptConsumptionRecord(
            receiptId = receiptId,
            nonceValue = epochNonce.nonceValue,
            epoch = epochNonce.epoch,
            sequenceNumber = seq,
            timestamp = timestamp
        )

        consumedReceipts[receiptId] = record
        consumedNonces[epochNonce.nonceValue] = receiptId
        return record
    }

    fun isReceiptConsumed(receiptId: ReceiptId): Boolean = consumedReceipts.containsKey(receiptId)

    fun isNonceConsumed(nonceValue: String): Boolean = consumedNonces.containsKey(nonceValue)

    fun getConsumptionRecord(receiptId: ReceiptId): ReceiptConsumptionRecord? = consumedReceipts[receiptId]

    fun totalConsumedCount(): Int = consumedReceipts.size

    companion object {
        private val INSTANCE = AtomicReceiptLedger()

        fun getInstance(): AtomicReceiptLedger = INSTANCE

        /**
         * Creates an isolated ledger instance for concurrent stress testing.
         */
        fun createIsolated(): AtomicReceiptLedger = AtomicReceiptLedger()

        fun resetForTesting() {
            INSTANCE.consumedReceipts.clear()
            INSTANCE.consumedNonces.clear()
            INSTANCE.monotonicSequence.set(0L)
        }
    }
}
