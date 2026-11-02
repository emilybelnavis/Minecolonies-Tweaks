# MineColonies Tweaks

An independent MineColonies addon with vanilla-style interfaces, a pinned resource HUD,
locate beams, custom citizen names, and a restaurant cook hunger fix.

## Requirements

Minecraft **1.21.1**, NeoForge **21.1.252+**, MineColonies **1.1.1406-snapshot**,
Structurize **1.0.837+**, BlockUI **1.0.212+**, and Domum Ornamentum **1.0.237-snapshot+**.
Install the addon and dependencies in your game's `mods` folder.

The screen skin, research map, cutter browser, HUD, and beams run on the client. Install
on the server for custom citizen names and the cook fix; single-player includes both sides.
This addon does not bundle its dependencies or integrate with other personal mods.

## Controls

| Key | Action |
| --- | --- |
| **J** | Pin or unpin the builder linked to a held Resource Scroll |
| **P** | Arrange panels; right click a panel to unpin it |
| **H** | Show or hide resource panels |
| **K** | Settings: Screens, Resource HUD, and Beams |

Citizen and hut windows have **Locate** controls; citizen windows also have a work diagnosis.
Use **Beams → Locate** to select colony targets, or locate the citizen, hut, or grave you are
looking at. Research maps support dragging and zooming. The cutter has named groups and
a larger, paginated variant grid.

Screen switches are independent. Resource counts use MineColonies' Resource Scroll calculation.
Pins and positions are saved per world or server in `config/minecoloniestweaks-client.json`.

## Citizen names and cooking

Server options live in the world's `serverconfig/minecoloniestweaks-server.toml`.
Name pools: `en_ca`, `en_us`, `en_gb`, `fr_ca`, `fr_fr`, `zh_cn_latn` (Pinyin), and
`zh_tw_latn` (traditional Chinese names in English romanization).
Names apply to new citizens; existing names remain intact.

Override a pool with `config/minecoloniestweaks/names/<locale>.json`:

```json
{
  "male": ["Louis", "Gabriel"],
  "female": ["Alice", "Florence"],
  "surnames": ["Tremblay", "Roy"],
  "surnameFirst": false
}
```

The cook eats the best prepared meal in their inventory or nearby restaurant storage,
even when their residence normally requires better food. With no prepared meal, the cook
continues cooking instead of waiting to be served. Ingredients and fuel are still required.

## Development

Install **JDK 21**, then use the included Gradle wrapper. Dependencies download automatically.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

Jars are written to `build/libs/`. `./gradlew runSmoke` runs development-only gameplay and
render checks in a disposable world. Portable project context is in `.codex/`.

## License

[GNU GPL v3 or later](LICENSE). UI components are adapted from
[MineColonies Vanilla Tweaks](https://github.com/projethub42-creator/minecolonies-vanilla-tweaks),
also licensed under GPL-3.0-or-later. Dependencies retain their respective licenses. Unofficial;
not affiliated with LDT Team.
