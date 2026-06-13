package com.lucciano.ironssablecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lucciano.ironssablecompat.helpers.SableUnloadedSubLevelCompat;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import io.redspace.ironsspellbooks.block.portal_frame.PortalFrameBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(PortalFrameBlockEntity.class)
public class PortalFrameBlockEntityTeleportMixin {

    @WrapOperation(
        method = "lambda$teleport$1",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z")
    )
    private static boolean resolveSameDimTeleport(Entity entity, ServerLevel level, double x, double y, double z,
                                                   Set<?> set, float yRot, float xRot, Operation<Boolean> original) {
        if (SubLevelContainer.getContainer(level) == null)
            return original.call(entity, level, x, y, z, set, yRot, xRot);
        Vec3 resolved = SableUnloadedSubLevelCompat.resolveDestination(level, new Vec3(x, y, z));
        return original.call(entity, level, resolved.x, resolved.y, resolved.z, set, yRot, xRot);
    }

    @WrapOperation(
        method = "lambda$teleport$1",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;")
    )
    private static Entity resolveCrossDimTeleport(Entity entity, DimensionTransition transition,
                                                   Operation<Entity> original) {
        if (SubLevelContainer.getContainer(entity.level()) == null)
            return original.call(entity, transition);
        Vec3 resolved = SableUnloadedSubLevelCompat.resolveDestination(
                transition.newLevel(), transition.pos());
        DimensionTransition fixed = new DimensionTransition(
                transition.newLevel(), resolved, transition.speed(),
                transition.yRot(), transition.xRot(),
                transition.postDimensionTransition());
        return original.call(entity, fixed);
    }
}
