package dev.aurakai.testrunner

import dev.aurakai.language.NineRunesAndGeometricProtocolTest
import org.junit.jupiter.api.Test

object DeterministicTestRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        println("================================================================================")
        println("A.U.R.A.K.A.I. — ARGAN OVERNIGHT-003 DETERMINISTIC TEST HARNESS")
        println("Java Runtime : ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
        println("Kotlin Stdlib: ${KotlinVersion.CURRENT}")
        println("================================================================================")

        val suites = listOf<Any>(
            NineRunesAndGeometricProtocolTest()
        )

        var totalPassed = 0
        var totalFailed = 0
        val failures = mutableListOf<String>()

        for (suite in suites) {
            val suiteClass = suite::class.java
            println("\n[SUITE] ${suiteClass.name}")
            val methods = suiteClass.declaredMethods
                .filter { it.isAnnotationPresent(Test::class.java) }
                .sortedBy { it.name }

            for (method in methods) {
                val startNs = System.nanoTime()
                try {
                    method.invoke(suite)
                    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
                    totalPassed++
                    println(String.format(java.util.Locale.ROOT, "  [PASS] %s (%.2f ms)", method.name, elapsedMs))
                } catch (e: Throwable) {
                    val cause = e.cause ?: e
                    totalFailed++
                    val msg = "${suiteClass.simpleName}.${method.name}: ${cause::class.java.simpleName} - ${cause.message}"
                    failures += msg
                    println("  [FAIL] $msg")
                    cause.printStackTrace(System.out)
                }
            }
        }

        println("\n================================================================================")
        println("SUMMARY: Passed=$totalPassed | Failed=$totalFailed | Total=${totalPassed + totalFailed}")
        println("================================================================================")

        if (totalFailed > 0) {
            System.exit(1)
        }
    }
}
