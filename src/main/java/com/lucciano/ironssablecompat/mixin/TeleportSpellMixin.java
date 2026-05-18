package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TeleportSpell.class, remap = false)
public class TeleportSpellMixin {

    @Inject(
        method = "solveTeleportDestination",
        remap = false,
        at = @At("HEAD"),
        cancellable = true
    )
    private static void alignSolveTeleportDestination(Level level, LivingEntity entity, BlockPos blockPos, Vec3 vec3, CallbackInfoReturnable<Vec3> cir) {
        if (level == null || blockPos == null || vec3 == null || entity == null) {
            return;
        }

        // Project vec3 to real world first (if it's not already)
        Vec3 realWorldVec = SableCompanion.INSTANCE.projectOutOfSubLevel(level, vec3);

        // Check if blockPos is in a sublevel
        Vec3 blockVec = Vec3.atCenterOf(blockPos);
        Vec3 projectedBlockVec = SableCompanion.INSTANCE.projectOutOfSubLevel(level, blockVec);

        if (projectedBlockVec.distanceToSqr(blockVec) > 0.01) {
            // blockPos is in sublevel space! 
            // We want vec3 to be in the exact same sublevel space.
            SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, projectedBlockVec);
            if (subLevel != null) {
                Vec3 sublevelVec = subLevel.logicalPose().transformPositionInverse(realWorldVec);
                
                // Re-execute solveTeleportDestination logic inside the sublevel space
                Vec3 bbOffset = entity.getForward().normalize().multiply(entity.getBbWidth() / 3, 0, entity.getBbHeight() / 3);
                Vec3 bbImpact = sublevelVec.subtract(bbOffset);

                double ledgeY = level.clip(new net.minecraft.world.level.ClipContext(
                    Vec3.atBottomCenterOf(blockPos).add(0, 3, 0),
                    Vec3.atBottomCenterOf(blockPos),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    net.minecraft.world.phys.shapes.CollisionContext.empty()
                )).getLocation().y;

                boolean isAir = level.getBlockState(new BlockPos(new net.minecraft.core.Vec3i(blockPos.getX(), (int) ledgeY, blockPos.getZ())).above()).isAir();
                boolean los = level.clip(new net.minecraft.world.level.ClipContext(
                    bbImpact,
                    bbImpact.add(0, ledgeY - blockPos.getY(), 0),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    entity
                )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;

                if (isAir && los && Math.abs(ledgeY - blockPos.getY()) <= 3) {
                    cir.setReturnValue(new Vec3(blockPos.getX() + .5, ledgeY + 0.001, blockPos.getZ() + .5));
                    return;
                }
                
                cir.setReturnValue(level.clip(new net.minecraft.world.level.ClipContext(
                    bbImpact,
                    bbImpact.add(0, -entity.getBbHeight(), 0),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    entity
                )).getLocation().add(0, 0.001, 0));
            }
        }
    }
}
