package com.lucciano.ironssablecompat.mixin;

import com.lucciano.ironssablecompat.helpers.SableUnloadedSubLevelCompat;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Resolve teleport destination from extreme→world coords at teleport time.
 * The lambda computes dest = connectedPos.pos() + (0, entity.y - this.y, 0).
 * connectedPos.pos() is in extreme coords (set by PortalSpell.handleEntityPortal).
 * We resolve the final destination Vec3 before teleporting.
 */
@Mixin(PortalEntity.class)
public class PortalEntityMixin {

    @Redirect(
        method = "lambda$checkForEntitiesToTeleport$1",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;teleportTo(DDD)V"
        )
    )
    private void redirectPortalTeleport(Entity instance, double x, double y, double z) {
        Level level = instance.level();
        Vec3 resolved = SableUnloadedSubLevelCompat.resolveDestination(level, new Vec3(x, y, z));
        instance.teleportTo(resolved.x, resolved.y, resolved.z);
    }

    @ModifyArgs(
        method = "lambda$checkForEntitiesToTeleport$1",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/portal/DimensionTransition;<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;FFLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)V"
        )
    )
    private void fixDimensionTransitionArgs(Args args) {
        ServerLevel level = args.get(0);
        Vec3 dest = args.get(1);
        Vec3 resolved = SableUnloadedSubLevelCompat.resolveDestination(level, dest);
        args.set(1, resolved);
    }
}
