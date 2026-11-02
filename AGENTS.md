# Project instructions

- Use Java 21, Minecraft 1.21.1, and NeoForge 21.1.252 or newer.
- Build with `./gradlew build`. Use `./gradlew tasks --all` to inspect tasks and
  `./gradlew runClient --dry-run` to validate the client development run.
- Keep `runClient` and `runServer` working, register the main mod source set, and keep
  MineColonies, Structurize, BlockUI, and Domum Ornamentum on the development runtime classpath.
- Keep code readable under GPL-3.0-or-later. Preserve attribution for UI code adapted from
  `projethub42-creator/minecolonies-vanilla-tweaks`.
- Do not add integrations with the owner's other mods unless requested.
- Keep README content user-facing. Put durable architecture and verification notes in `.codex/`.

## Context efficiency

- Locate files and symbols before reading their contents.
- Inspect relevant sections first; read complete files only when structure requires it.
- Do not repeatedly read unchanged files.
- Exclude build outputs, caches, logs, saves, and generated artifacts from routine searches.
- Consult `.codex/PROJECT.md` before exploring an unfamiliar subsystem and
  `.codex/VERIFICATION.md` when selecting checks.
- Delegate only substantial, bounded work. Researchers return concise paths, symbols,
  dependencies, and risks rather than source dumps.
- Run targeted checks before broader verification and summarize results concisely.
- Use a fresh chat for work unrelated to the current task.

## Delegation

- The main agent plans, coordinates, and integrates using GPT-6.1 Sol with medium reasoning.
- Use the Researcher for bounded discovery and dependency tracing, and the Worker for bounded
  implementation and authorized testing. Do not delegate straightforward work.
- Give every subagent a narrow scope, acceptance criteria, and read-only boundaries or explicit
  file ownership. Never assign overlapping edits or concurrent Gradle/game runs.

