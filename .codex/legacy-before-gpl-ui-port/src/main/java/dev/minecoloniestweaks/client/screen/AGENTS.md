# Client screen instructions

- Keep screens in Minecraft GUI coordinates. Apply one consistent transform to rendering,
  hit-testing, scrolling, portraits, and scissor bounds.
- Preserve upstream handlers, parent/list identities, permissions, containers, and network messages.
- Native replacements must not draw hidden BlockUI panes or decorative assets. Keep the legacy
  skin as the fallback for dialogs that are not replaced.
- Check specialized pages and controls before generalizing shared screen behavior.

