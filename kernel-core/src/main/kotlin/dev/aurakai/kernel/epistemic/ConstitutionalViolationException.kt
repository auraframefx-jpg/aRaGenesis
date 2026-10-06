package dev.aurakai.kernel.epistemic

/**
 * Thrown when an attempt is made to bypass constitutional kernel invariants
 * (e.g. state projection bypass, receipt digest mismatch, or unverified state injection).
 */
class ConstitutionalViolationException(message: String) : RuntimeException(message)
