package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TouchDigSpell.class)
public class TouchDigSpellMixin {

    @Redirect(
        method = "onCast",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/BlockHitResult;getLocation()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 redirectHitLocation(BlockHitResult instance, Level world, int spellLevel, LivingEntity entity) {
        Vec3 original = instance.getLocation();
        
        // If the player is in sub-level, keep it in sub-level space!
        Vec3 casterPos = entity.getEyePosition();
        Vec3 realWorldCasterPos = SableCompanion.INSTANCE.projectOutOfSubLevel(world, casterPos);
        if (realWorldCasterPos.distanceToSqr(casterPos) > 0.01) {
            return original;
        }

        // Project the block hit location (which may be extreme) to Real-World so it matches the player's space for particle distance calculation
        return SableCompanion.INSTANCE.projectOutOfSubLevel(world, original);
    }

    @Redirect(
        method = "onCast",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;subtract(DDD)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 redirectParticleLocation(Vec3 instance, double x, double y, double z, Level world, int spellLevel, LivingEntity entity) {
        Vec3 original = instance.subtract(x, y, z);
        
        // If the player is in sub-level, keep it in sub-level space!
        Vec3 casterPos = entity.getEyePosition();
        Vec3 realWorldCasterPos = SableCompanion.INSTANCE.projectOutOfSubLevel(world, casterPos);
        if (realWorldCasterPos.distanceToSqr(casterPos) > 0.01) {
            return original;
        }

        // Project the player's eye position (which may be extreme if standing on a ship) to Real-World
        return SableCompanion.INSTANCE.projectOutOfSubLevel(world, original);
    }
}
