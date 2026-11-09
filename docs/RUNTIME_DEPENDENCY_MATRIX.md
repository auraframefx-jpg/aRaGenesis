# RUNTIME DEPENDENCY MATRIX (`RUNTIME_DEPENDENCY_MATRIX.md`)
**Document ID:** `AURAKAI-RECON-DEPS-001`
**Mission Reference:** `AURAKAI-DESKTOP-HANDOFF-001` v1.0 — Phase 1 Reconnaissance
**Date:** `2026-11-03`

---

## 1. Objective
Audit all runtime, build, and test dependencies across `auraframefx-jpg/aRaGenesis` (PR #6), the local `AURAKAI-ARGAN-OVERNIGHT-003` clean-slate modules (`kernel-core`, `language-core`), and the legacy `A.u.r.a.k.a.i_ReGenesis` reference repository against Part III §5 Kernel Constitutional Requirements.

---

## 2. Dependency Matrix by Module

| Module / Repository | Build System | Kotlin / JVM Target | Main Compile/Runtime Dependencies | Test Dependencies | Constitutional Audit Verdict (`OBSERVED`) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`aRaGenesis:kernel-core` (PR #6 `7fc9a9e`)** | Gradle `9.7.0` (`settings.gradle.kts`, `kernel-core/build.gradle.kts`) | Kotlin `2.0.21` / JVM Toolchain `21` | • `org.jetbrains.kotlin:kotlin-stdlib:2.0.21`<br>• `org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3`<br>• JDK `jdk.httpserver` (`com.sun.net.httpserver.HttpServer` in `DesktopKernelBridge.kt`) | • `org.jetbrains.kotlin:kotlin-test:2.0.21`<br>• `org.junit.jupiter:junit-jupiter:5.11.0` | **PARTIAL VIOLATION**: Zero Android/Firebase/Room/LLM SDKs, **but** `DesktopKernelBridge.kt` imports `com.sun.net.httpserver.HttpServer` inside `kernel-core` (violates §5 *"Do not introduce: Network clients"* / host adapter coupling), and 7 files invoke `System.currentTimeMillis()` / `System.nanoTime()` directly. |
| **`aRaGenesis:LegalPilot` (PR #6 `7fc9a9e`)** | Unlinked `LegalPilot/build.gradle.kts` (Not in `settings.gradle.kts`) | Android App | Android SDK, Jetpack Compose, Material3 | None | **QUARANTINED**: Present in git tree from initial commit `297557c`, excluded from `settings.gradle.kts`. Must be removed or moved out of `aRaGenesis`. |
| **Local Clean-Slate `kernel-core` (`Overnight-003`)** | Gradle / Standalone `K2JVMCompiler` | Kotlin `2.3.21` / JDK `25.0.2` (compatible with 21) | • `kotlin-stdlib` **ONLY** (Zero serialization, zero HTTP server, zero wall-clock calls) | • `kotlin-test`<br>• `junit-jupiter:5.11.0` | **100% COMPLIANT**: Enforces explicit `timestampTick: Long` and passes static AST/source scan (`ConstitutionalKernelTest.test19`). |
| **Local Clean-Slate `language-core` (`Overnight-003`)** | Gradle / Standalone `K2JVMCompiler` | Kotlin `2.3.21` / JDK `25.0.2` | • `:kernel-core`<br>• `kotlin-stdlib` **ONLY** | • `kotlin-test`<br>• `junit-jupiter:5.11.0` | **100% COMPLIANT**: Pure deterministic Nine Runes & 5×5 Lattice Geometric Rewriting engine (`NineRunesAndGeometricProtocolTest.test10`). |
| **Legacy `A.u.r.a.k.a.i_ReGenesis` (`:core-module`, `:app`, etc.)** | Gradle `9.5.0` / AGP `9.1.0` | Kotlin `2.3.20` / Android SDK `36` | AndroidX, Compose BOM, Dagger Hilt, Room, Firebase, YukiHook/Xposed, Libsu, Ktor/OkHttp, LangChain4j/GenAI | JUnit, MockK, Espresso | **LEGACY REFERENCE ONLY**: Never import into `aRaGenesis` kernel. |
