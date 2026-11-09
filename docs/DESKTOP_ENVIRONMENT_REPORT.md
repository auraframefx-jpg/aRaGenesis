# DESKTOP ENVIRONMENT REPORT (`DESKTOP_ENVIRONMENT_REPORT.md`)
**Document ID:** `AURAKAI-RECON-ENV-001`  
**Mission Reference:** `AURAKAI-DESKTOP-HANDOFF-001` v1.0 — Phase 1 Reconnaissance  
**Date:** `2026-11-03`  
**Host Machine:** `AURAKAI`  

---

## 1. Objective
Establish the physical operating environment, hardware topology, installed compiler/build toolchains, runtime services, and sandbox boundaries on the desktop development host without modifying production code or installing packages.

## 2. Source
- `.NET` / PowerShell `[System.Environment]` & environment variable probes (`task-32`, `task-40`, `task-107`, `task-126`).
- Direct filesystem traversal of `C:\`, `C:\Program Files\Java`, `C:\Program Files\Android`, `C:\Users\AuraF\.gradle`, and `c:\Users\AuraF\A.u.r.a.k.a.i_ReGenesis\local.properties`.

---

## 3. Observations & Evidence-Labeled Environment Inventory

### 3.1 Operating System & Hardware
| Component | Value / Specification | Epistemic Label | Source / Evidence |
| :--- | :--- | :--- | :--- |
| **Operating System** | Microsoft Windows NT `10.0.26300.0` (64-bit, `AMD64`) *(Note: Handoff §11 template listed Debian/Linux; actual host is Windows 11 Build 26300)* | `OBSERVED` | `[System.Environment]::OSVersion.VersionString` |
| **Processor** | `AMD64 Family 25 Model 33 Stepping 2, AuthenticAMD` (Zen 3 architecture, **16 Logical Processors**) | `OBSERVED` | `$env:PROCESSOR_IDENTIFIER`, `[System.Environment]::ProcessorCount` |
| **WMI / CIM Provider** | `Win32_OperatingSystem` & `Win32_Processor` CIM queries blocked in standard shell (`Access to a CIM resource was not available to the client`) | `BLOCKED` | `Get-CimInstance` exit diagnostics |
| **Filesystem Drives** | `C:\` (Primary OS, toolchains, workspaces) and `E:\` (Secondary volume) | `OBSERVED` | `Get-PSDrive -PSProvider FileSystem` |

### 3.2 Development Toolchains & Runtimes
| Tool / SDK | Version & Installation Path | Epistemic Label | Source / Evidence |
| :--- | :--- | :--- | :--- |
| **Git** | `git version 2.45.1.windows.1` | `OBSERVED` | `git --version` |
| **Python** | `Python 3.14.2` (`C:\Python314\python.exe`) + workspace `.venv` (`c:\Users\AuraF\A.u.r.a.k.a.i_ReGenesis\.venv`) | `OBSERVED` | `python --version`, root directory listing |
| **System `JAVA_HOME` (JDK 25)** | Oracle Java SE `25.0.2` LTS (`build 25.0.2+10-LTS-69`, `C:\Program Files\Java\jdk-25.0.2`) | `OBSERVED` | `java -version`, `$env:JAVA_HOME` |
| **Gradle Toolchain JDK 21 (Adoptium)** | Eclipse Adoptium OpenJDK `21.0.10+7-LTS` (`C:\Users\AuraF\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2`) | `OBSERVED` | Gradle toolchain auto-detection (`task-126`) |
| **Android OpenJDK 21 (Microsoft)** | Microsoft OpenJDK `21.0.8+9-LTS` (`C:\Program Files\Android\openjdk\jdk-21.0.8`) | `OBSERVED` | `task-107` filesystem scan |
| **Gradle Toolchain JDK 25 / 26** | Eclipse Adoptium `25+36-LTS` & `26+35` (`C:\Users\AuraF\.gradle\jdks\`) | `OBSERVED` | `task-107` & `task-122` |
| **Android Studio JBR** | `C:\Program Files\Android\Android Studio\jbr\bin\javac.exe` | `OBSERVED` | `task-107` |
| **Kotlin Compilers (Cached)** | `kotlin-compiler-embeddable` versions **`2.0.21`**, **`2.1.0-RC2`**, **`2.3.21`**, and **`2.4.0`** in `C:\Users\AuraF\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\` | `OBSERVED` | `task-40` |
| **Gradle Wrapper Distributions** | `gradle-9.1-bin`, `gradle-9.2-rc-1-bin`, `gradle-9.5-rc-3-bin`, `gradle-9.6.0-milestone-2-bin`, **`gradle-9.7.0-bin`** in `C:\Users\AuraF\.gradle\wrapper\dists\` | `OBSERVED` | `task-32` |
| **Android SDK** | `C:\androidsdk` (configured via `sdk.dir=C\:\\androidsdk` in `local.properties`) | `OBSERVED` | `local.properties` & `C:\` listing |

### 3.3 Sandbox & Execution Boundaries
| Boundary | Observed Behavior | Epistemic Label |
| :--- | :--- | :--- |
| **Standard Sandbox (`BypassSandbox: false`)** | Full read/write access to active workspace (`c:\Users\AuraF\A.u.r.a.k.a.i_ReGenesis`), `C:\Users\AuraF\Downloads`, and `<appDataDir>\brain\247eb90a-469d-464d-8952-40d30250b8a3`. Outbound network/DNS (`getaddrinfo()`), loopback socket IPC to forked Gradle daemons, and sibling user profile folders (`Desktop`, `Documents`, `OneDrive`, `CrossDevice`) are restricted by sandbox policy. | `OBSERVED` |
| **Unsandboxed Host Execution (`BypassSandbox: true`)** | Full GitHub network clone (`git clone https://github.com/auraframefx-jpg/aRaGenesis.git`) and native Gradle 9.7.0 + JUnit Jupiter 5.11.0 execution succeeded with exit code `0` (`task-126`). | `OBSERVED` |
