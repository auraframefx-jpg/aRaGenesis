package dev.aurakai.kernel.verification

import dev.aurakai.kernel.epistemic.AshId
import dev.aurakai.kernel.epistemic.AshRecord
import dev.aurakai.kernel.epistemic.ConstitutionalViolationException
import dev.aurakai.kernel.epistemic.KernelTimestamp
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Epistemic Degraded Transition Event.
 * Records monotonic state degradation or contradiction detection in the kernel substrate.
 */
data class EpistemicTransition(
    val transitionId: String,
    val fromDigest: String,
    val toDigest: String,
    val reasonCode: String,
    val severity: Int, // 1 (Minor) to 5 (Critical Contradiction / Veto)
    val timestamp: KernelTimestamp
)

/**
 * Authoritative Pure In-Memory Epistemic Observation & Ash Ledger (GAP-04 / H-008).
 *
 * Enforces constitutional kernel invariants:
 * 1. Zero external I/O, Room, Android, or cloud dependencies.
 * 2. Append-only, monotonically advancing event histories.
 * 3. Immutable defensive copies on read.
 * 4. Authoritative recording of QuarantineScars and EpistemicTransitions.
 */
class AshLedger private constructor() {

    private val observations = ConcurrentHashMap<AshId, AshRecord>()
    private val observationOrder = CopyOnWriteArrayList<AshId>()

    private val quarantineScars = ConcurrentHashMap<String, QuarantineScar>()
    private val scarOrder = CopyOnWriteArrayList<String>()

    private val transitions = CopyOnWriteArrayList<EpistemicTransition>()
    private val cumulativeDegradationScore = AtomicLong(0L)

    /**
     * Appends an authoritative observation to the ledger.
     * Rejects duplicate AshId mutations to enforce immutability.
     */
    fun appendObservation(record: AshRecord): Boolean {
        val existing = observations.putIfAbsent(record.id, record)
        if (existing == null) {
            observationOrder.add(record.id)
            return true
        }
        if (existing != record) {
            throw ConstitutionalViolationException(
                "ASH LEDGER MUTATION DETECTED: Observation ${record.id.value} already exists with a different payload."
            )
        }
        return false
    }

    /**
     * Appends an immutable QuarantineScar into the permanent lineage.
     */
    fun recordQuarantineScar(scar: QuarantineScar): Boolean {
        val existing = quarantineScars.putIfAbsent(scar.scarId, scar)
        if (existing == null) {
            scarOrder.add(scar.scarId)
            cumulativeDegradationScore.addAndGet(10L)
            return true
        }
        return false
    }

    /**
     * Records an epistemic state degradation transition.
     */
    fun recordTransition(transition: EpistemicTransition) {
        transitions.add(transition)
        cumulativeDegradationScore.addAndGet(transition.severity.toLong())
    }

    fun getObservation(id: AshId): AshRecord? = observations[id]

    fun hasObservation(id: AshId): Boolean = observations.containsKey(id)

    fun getAllObservations(): List<AshRecord> {
        return Collections.unmodifiableList(observationOrder.mapNotNull { observations[it] })
    }

    fun getQuarantineScars(): List<QuarantineScar> {
        return Collections.unmodifiableList(scarOrder.mapNotNull { quarantineScars[it] })
    }

    fun getTransitions(): List<EpistemicTransition> {
        return Collections.unmodifiableList(transitions.toList())
    }

    fun getCumulativeDegradationScore(): Long = cumulativeDegradationScore.get()

    fun observationCount(): Int = observations.size

    fun scarCount(): Int = quarantineScars.size

    companion object {
        private val INSTANCE = AshLedger()

        fun getInstance(): AshLedger = INSTANCE

        /**
         * Creates an isolated in-memory ledger instance for test environments.
         */
        fun createIsolated(): AshLedger = AshLedger()

        fun resetForTesting() {
            INSTANCE.observations.clear()
            INSTANCE.observationOrder.clear()
            INSTANCE.quarantineScars.clear()
            INSTANCE.scarOrder.clear()
            INSTANCE.transitions.clear()
            INSTANCE.cumulativeDegradationScore.set(0L)
        }
    }
}
