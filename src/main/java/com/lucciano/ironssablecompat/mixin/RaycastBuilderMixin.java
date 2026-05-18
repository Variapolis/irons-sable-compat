package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes ALL Iron's Spells raycast-based spells on Sable ships.
 *
 * Root cause: When a player on a ship casts a raycast spell, the ray
 * start/end are in real-world coordinates. Sable intercepts Level.clip()
 * and returns a BlockHitResult at extreme sub-level coordinates (where
 * the ship blocks actually live). performRaycast() then uses this extreme
 * position to build an entity search AABB, creating a massive bounding box
 * from real-world coords to extreme coords. Sable rejects this as
 * "abnormally large AABB" and the spell fails.
 *
 * Fix: Redirect the Level.clip() call inside performRaycast() to project
 * the returned BlockHitResult location back to real-world coordinates.
 * This ensures the entity search AABB stays reasonable, and all downstream
 * code (particles, visual entities, packets) uses real-world positions.
 */
@Mixin(value = RaycastBuilder.class, remap = false)
public class RaycastBuilderMixin {

    @Shadow @Final private Level level;
    @Shadow @Final private Entity originEntity;

    /**
     * Redirect Level.clip() inside performRaycast() to project the block
     * hit location out of any sub-level back to real-world coordinates.
     */
    @Redirect(
        method = "performRaycast",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"
        )
    )
    private BlockHitResult redirectLevelClip(Level levelInstance, ClipContext clipContext) {
        BlockHitResult original = levelInstance.clip(clipContext);

        if (originEntity != null) {
            Vec3 casterPos = originEntity.getEyePosition();
            Vec3 realWorldCasterPos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, casterPos);
            // If the caster is inside a sub-level, keep it in the sub-level space
            if (realWorldCasterPos.distanceToSqr(casterPos) > 0.01) {
                return original;
            }
        }

        // Project the hit location out of sub-level space
        Vec3 hitLocation = original.getLocation();
        Vec3 projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, hitLocation);

        // If unchanged, not in a sub-level — return as-is
        if (projected.x == hitLocation.x && projected.y == hitLocation.y && projected.z == hitLocation.z) {
            return original;
        }

        // Rebuild with projected real-world coordinates
        // This prevents the entity search AABB from spanning real-world to extreme coords
        return new BlockHitResult(
            projected,
            original.getDirection(),
            BlockPos.containing(projected),
            original.isInside()
        );
    }
}
