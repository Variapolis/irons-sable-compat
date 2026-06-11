package com.lucciano.ironssablecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.companion.SableCompanion;
import io.redspace.ironsspellbooks.block.portal_frame.PortalFrameBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PortalFrameBlock.class)
public class PortalFrameBlockMixin {
    @WrapOperation(
        method = "entityInside",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/VoxelShape;move(DDD)Lnet/minecraft/world/phys/shapes/VoxelShape;")
    )
    private VoxelShape ironssablecompat$projectShapeMove(VoxelShape shape, double x, double y, double z,
                                                          Operation<VoxelShape> original,
                                                          @Local(argsOnly = true) Level level,
                                                          @Local(argsOnly = true) BlockPos pos,
                                                          @Local(argsOnly = true) Entity entity) {
        Vec3 entityPos = entity.position();
        Vec3 projectedEntityPos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, entityPos);
        boolean entityInSubLevel = projectedEntityPos.distanceToSqr(entityPos) > 0.01;

        if (entityInSubLevel) {
            return original.call(shape, x, y, z);
        }

        Vec3 p = SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos.getBottomCenter());
        return original.call(shape, p.x, p.y, p.z);
    }
}
