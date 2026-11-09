# BACKGROUND AGENT & DAEMON RECONNAISSANCE (`BACKGROUND_AGENT_RECONNAISSANCE.md`)
**Document ID:** `AURAKAI-RECON-AGENTS-001`  
**Mission Reference:** `AURAKAI-DESKTOP-HANDOFF-001` v1.0 — Phase 1 Reconnaissance  
**Date:** `2026-11-03`  

---

## 1. Objective
Inventory active agent processes, background daemons, MCP servers, local model services, and agent configuration directories across the desktop host.

---

## 2. Observed Process & Daemon State (`OBSERVED`)
- **Active Process Scan (`Get-Process`)**:
  - No rogue background Python, Ollama, local LLM server, or orphaned Kotlin/Java agent processes were running persistently outside the active IDE/Antigravity session (`task-37`).
  - The single-use Gradle 9.7.0 daemon (`pid 11532` on JDK `21.0.10`) started for `:kernel-core:test` in `task-126` and terminated cleanly upon build completion (`Daemon will be stopped at the end of the build`).
  - Git `fsmonitor--daemon.ipc` in `c:\Users\AuraF\A.u.r.a.k.a.i_ReGenesis\.git` returned a benign IPC socket warning inside the sandbox (`error: fsmonitor_ipc__send_query: unspecified error on '.git/fsmonitor--daemon.ipc'`), which does not affect read-only Git operations.

## 3. Configured MCP Servers & Agent Directories (`OBSERVED`)
| Integration / Directory | Location | Status & Capabilities | Epistemic Label |
| :--- | :--- | :--- | :--- |
| **Antigravity Agent Harness** | `C:\Users\AuraF\.gemini\antigravity` | Active primary desktop agent environment; manages `brain/<conversation-id>`, `scratch/`, and skills (`agy-customizations`, `antigravity-guide`, `automation`, `generative_ui`, `migrate-workflows`, `plugin`, `ui-plugin-navigation`). | `OBSERVED` |
| **MCP Servers (Lazy-Loaded)** | `C:\Users\AuraF\.gemini\antigravity\mcp\` | 3 configured MCP servers:<br>1. `cloudrun` (`list_projects`, `deploy_local_folder`, etc.)<br>2. `firebase-mcp-server` (`firebase_get_project`, `developerknowledge_search_documents`, etc.)<br>3. `genkit-mcp-server` (`lookup_genkit_docs`, `list_flows`, `run_flow`, etc.) | `OBSERVED` |
| **Agent Dot-Directories in `C:\Users\AuraF\`** | `.antigravity`, `.claude`, `.claude-server-commander`, `.claude-worktrees`, `.codeium`, `.copilot`, `.cursor`, `.gemini`, `.genkit`, `.grok`, `.mcp-auth`, `.tooluniverse`, `.windsurf` | Present on host filesystem; sibling directories outside workspace are sandbox-isolated unless accessed via unsandboxed commands. | `OBSERVED` |
| **121-Agent Civilization Matrix & 700 Autonomous Instances** | Referenced in `special.md` & Handoff §2 | Architectural design concepts and user-reported deployment intentions. Zero evidence of 121 or 700 live background agent processes running on the host. | `REPORTED` / `UNVERIFIED` (Design Canon Only) |
