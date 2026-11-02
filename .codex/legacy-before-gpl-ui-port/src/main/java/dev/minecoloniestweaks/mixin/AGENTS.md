# Mixin instructions

- Treat targets as pinned, version-specific upstream APIs. Verify target owners, names, descriptors,
  and client/server loading before changing a mixin.
- Keep client-only mixins below `mixin/client` and out of common/server loading paths.
- Prefer accessors and invokers over duplicated upstream behavior. Preserve upstream permissions,
  protocols, and calculations.
- Update `minecoloniestweaks.mixins.json` when adding, removing, or relocating mixins.

