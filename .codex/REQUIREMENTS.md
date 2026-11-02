# Accepted scope

Build an independently versioned MIT MineColonies GUI and quality-of-life addon. Use this
repository as the project directory; no personal-mod integrations. Maintain clean Java,
portable Gradle setup, a useful `.gitignore`, concise user documentation, and Codex context
under `.codex/`.

## Screens

- Citizens: overview, skills, inventory, requests, happiness, family, dialogue, work diagnosis.
- Town hall: dashboard, actions, statistics, permissions, alliances, colony map, quest log, banner.
- Every hut: home, workers, settings, statistics, recipes, minimum stock, tasks, item lists,
  build options. Include builder, warehouse, guard tower, mine, houses, restaurant, farmer,
  plantation, graveyard, enchanter, post box, barracks, and nether mine pages.
- University: research trees presented like vanilla advancements.
- Structurize: blueprint browser, styles, build tool, scan, shape, tag, undo tools.
- Domum Ornamentum: Architect's Cutter with named groups and a larger variant grid.
- Clipboard, field blocks, rally banner, and remaining dialogs.
- Each screen can be switched off independently.

## Tools and behaviour

- Permanent Resource HUD with the same counts as Resource Scrolls. Hold a scroll and press
  J to pin/unpin; P arranges panels; H toggles visibility.
- Coloured locate beams for citizens, huts, and graves, plus direction and distance.
- K settings organized into Screens, Resource HUD (Position and size, Content), and Beams.
- Bundled and custom male/female first-name and surname lists for new citizens. Locales:
  en_ca, en_us, en_gb, fr_ca, fr_fr, zh_cn_latn, zh_tw_latn. Chinese pools are English
  romanizations, with surname-first ordering.
- Fix starving restaurant cooks: eat the best prepared food in their inventory or restaurant,
  bypassing residence-level requirements. If none is cooked, keep cooking instead of waiting
  for someone to serve them. Return to ordinary work after eating.
- GUI remains client-only. Server installation supports authoritative citizen names and cook
  behaviour; client tools consume server-synchronized MineColonies data. Both integrated and
  dedicated-server environments must remain loadable.

## Version target

Minecraft 1.21.1; NeoForge 21.1.252 or newer; MineColonies 1.1.1406-snapshot; latest compatible
Structurize, BlockUI, and Domum Ornamentum, pinned to reproducible versions in Gradle.

See `PROJECT.md` for the context index, `ARCHITECTURE.md` for current design, and
`VERIFICATION.md` for current checks. Historical results live under `history/`.
