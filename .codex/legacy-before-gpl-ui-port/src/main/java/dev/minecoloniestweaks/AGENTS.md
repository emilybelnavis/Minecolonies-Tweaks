# Java source instructions

- Keep Minecraft client classes out of common and server entry points.
- Common/server behavior must remain safe on a dedicated server. Client initialization belongs
  behind the NeoForge client distribution boundary.
- Preserve upstream MineColonies permissions, messages, lifecycle, and authoritative server state.
- Prefer small changes against the pinned dependency APIs; record cross-cutting architecture
  decisions in `.codex/PROJECT.md`.

