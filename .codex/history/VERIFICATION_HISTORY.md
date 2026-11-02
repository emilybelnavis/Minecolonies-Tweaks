# Verification history

This file preserves past evidence and task-specific validation budgets. Do not treat an old request
to run or skip tests as authority for a new task.

## Baseline smoke verification — 2026-11-02

- Build and all seven JUnit tests passed on Java 21.
- `runClient` launched with the project mod and all four pinned dependency mods; its final task
  graph also passed `--dry-run` after the smoke source set was introduced.
- `runServerSmoke` completed its dedicated-launcher initialization.
- `runSmoke` passed with `MCT_SERVER_POLICY_PASS`, `MCT_RESOURCE_ADAPTER_PASS`, and
  `MCT_SMOKE_PASS`. Coverage included cooked-food selection and consumption, raw-food avoidance,
  restaurant access boundaries, custom names, and Resource Scroll accessor behavior.
- Settings, BlockUI skin, cutter, research tree, and a locate beam rendered. Screenshot review
  caught and corrected duplicate blur and a group-label key.
- The binary jar contained MIT license and mod/mixin metadata, with no smoke classes or bundled
  dependency jars.

These results did not certify every page, research transaction, HUD lifecycle, or remote-server
crafting scenario.

## Citizen layout update — 0.1.1

The citizen layout was rebuilt to a supplied wide-window reference with integrated navigation and
Locate control. The task requested packaging only and no tests or launches; baseline screenshots
therefore described 0.1.0, not this layout.

## Citizen polish — 0.1.2

Added creative inventory tab sprites, compact scaling, rounded frames, corrected equipment
placeholders, tighter vitals, and citizen-inventory return behavior. The task requested
compile/package only, with no runtime or visual verification.

## Citizen sizing — 0.1.3

Reduced the citizen window scale by 25 percent relative to 0.1.2 and applied the same transform to
input. Moved main-hand and shield slots. The task requested compile/package only.

## Native building screens and GUI scale — 0.1.4

`build`, `runClient --dry-run`, and the extended `runSmoke` passed with `MCT_SMOKE_PASS`.
Screenshots covered native builder and town-hall surfaces, citizen GUI scales 1 and 2, native debug
navigation, and materialized list rows. Cook, name, and Resource Scroll checks also passed.

Visual review found and corrected citizen scissor coordinates. Native input callbacks ignored
unchanged text and tolerated absent filters. Fixtures initialized fields normally received over the
network. The run was not exhaustive for every hut module, permission change, colony travel, or
multiplayer transaction.

