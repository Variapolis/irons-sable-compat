# Iron's Spells x Sable Compat

> ⚠️ AI-generated hotfix mod — not an official release. This is a temporary
> compatibility patch until Iron's Spells 'n Spellbooks or Sable officially
> integrates Sable Companion support.

Fixes all spells exept portal frames, they don't crash but disconnect if the physics are toggled, and only work to get in the contraptions not off them.

## Tested Versions

| Mod | Version | Notes |
|---|---|---|
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.172 | |
| Iron's Spells 'n Spellbooks | **3.15.6** | Hard-pinned — do NOT auto-update |
| Sable | **1.2.2** | Hard-pinned — do NOT auto-update |
| Create Aeronautics (bundles Sable) | **1.2.1** | |
| Sable Companion API | 1.6.0 | Bundled inside this mod (JiJ) |

> **⚠️ Do NOT auto-update Iron's or Sable.** This mod uses Mixins that target
> specific internal methods. If either mod changes its internals, mixins may
> fail to apply, targeting may break, or server startup may crash.


## Fragility Assumptions

This mod assumes:
- **Sable Companion APIs** (`SableCompanion.INSTANCE.projectOutOfSubLevel`, `getContaining`, `SubLevelAccess.logicalPose()`) exist and behave as documented
- **Iron's Spells internals** (`Utils.handleSpellTeleport`, `Utils.hasLineOfSight`, `PortalSpell.handleEntityPortal`, `PortalEntity.lambda$checkForEntitiesToTeleport$1`, `PortalFrameBlockEntity.serverTick`, `TouchDigSpell.onCast`, `RaycastBuilder.performRaycast`, `ShadowSlashSpell.onCast`) remain stable at the method-signature level
- **Mixin targets** (method names, parameter types, invoke targets) remain unchanged

If either mod updates aggressively:
- ❌ Mixins may fail to apply (crash at startup)
- ❌ Method targeting may break (silent misbehavior)
- ❌ Server startup may crash with `MixinApplyError`

## Disclaimer

- Generated with AI assistance (Claude, Anthropic)
- Not affiliated with iron431 (Iron's Spells) or ryanhcode (Sable)
- Use at your own risk
- **This mod will become obsolete once Iron's Spells or Sable ships an official fix**
