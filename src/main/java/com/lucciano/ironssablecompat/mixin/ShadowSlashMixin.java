package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import io.redspace.ironsspellbooks.spells.ender.ShadowSlashSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes Shadow Slash on Sable ships.
 *
 * Root cause: ShadowSlashSpell.onCast() calls Utils.raycastForBlock() which
 * internally calls Level.clip(). On a Sable ship, the BlockHitResult returns
 * extreme sub-level coordinates. The spell then builds an AABB from the
 * player's real-world eye position to the extreme hit location, creating a
 * massive bounding box that Sable rejects as "abnormally large AABB".
 *
 * Fix: Redirect BlockHitResult.getLocation() inside onCast() to project the
 * hit position out of sub-level space back to real-world coordinates.
 */
@Mixin(ShadowSlashSpell.class)
public class ShadowSlashMixin {

    @Redirect(
        method = "onCast",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/BlockHitResult;getLocation()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 redirectHitLocation(BlockHitResult instance, Level level, int spellLevel, LivingEntity entity) {
        Vec3 original = instance.getLocation();
        System.out.println("[IronsSableCompat] ShadowSlashMixin: redirectHitLocation original=" + original + " caster=" + entity.getName().getString());
        
        // Check if the caster is in a sub-level
        Vec3 casterPos = entity.getEyePosition();
        Vec3 realWorldCasterPos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, casterPos);
        double distSqr = realWorldCasterPos.distanceToSqr(casterPos);
        System.out.println("[IronsSableCompat] ShadowSlashMixin: casterPos=" + casterPos + " realWorldCasterPos=" + realWorldCasterPos + " distSqr=" + distSqr);
        
        if (distSqr > 0.01) {
            // Keep in sub-level space
            System.out.println("[IronsSableCompat] ShadowSlashMixin: caster is inside sub-level, returning original: " + original);
            return original;
        }
        
        Vec3 projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, original);
        System.out.println("[IronsSableCompat] ShadowSlashMixin: caster is in real world, projecting hit out to: " + projected);
        return projected;
    }
}
