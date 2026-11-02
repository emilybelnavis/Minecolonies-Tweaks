# Architecture

## Loading boundaries

- The common entry point registers only server configuration; client initialization uses
  `Dist.CLIENT`.
- Common mixins cover name generation and cook hunger behavior. BlockUI and GUI mixins are
  client-only.

## Client screens

- `BuildingScreen` replaces hut and town-hall controllers with a native frame, item tabs, and
  Locate controls. `NativeBuildingControls` preserves upstream identities, handlers, permissions,
  and network messages while presenting native controls.
- `CitizenScreen` uses a fixed logical-GUI frame and downscales only when the viewport cannot fit
  it. Rendering and input share the same transform; scissor rectangles use absolute coordinates.
- `ResearchScreen` presents an advancements-style tree while forwarding start/reset requests to
  upstream messages. `CutterScreen` reuses the upstream container and selection protocol.
- The legacy BlockUI skin remains a fallback for dialogs outside native replacements.

## HUD and locate tools

- `ResourceHud` calls the upstream Resource Scroll calculator through a client accessor rather
  than duplicating resource arithmetic.
- Preferences scope pins by server/world and dimension. Disconnect clears live caches.
- Locate beams use live citizen data when available, fall back to colony positions, expire, and
  remain dimension-scoped.

## Server behavior

- The cook selects prepared food using upstream nutrition and container behavior. Restaurant
  storage is accessed only nearby with loaded chunks; no food or fuel is created.
- Name lists are immutable validated records. Custom JSON is checked by modification time, invalid
  data falls back to bundled lists, and duplicate resolution is bounded.

When changing these boundaries, update this document and the nearest subsystem `AGENTS.md` only
if the change creates a durable instruction for future work.

