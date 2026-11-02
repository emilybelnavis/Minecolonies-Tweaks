# Smoke-test instructions

- Smoke fixtures and classes must remain in the `smoke` source set and out of release jars.
- Use a fresh ignored world under `run-smoke/`; do not depend on personal saves or local jars.
- Run smoke or game-launch tasks only when the current task authorizes them.
- Treat screenshots and logs as temporary evidence, not tracked project files.
- Do not run Gradle or a game instance concurrently with another agent.

