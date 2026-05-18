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
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = TeleportSpell.class, remap = false)
public class TeleportSpellMixin {

    @ModifyVariable(
        method = "solveTeleportDestination",
        remap = false,
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private static Vec3 alignTeleportVec3(Vec3 vec3, Level level, LivingEntity entity, BlockPos blockPos) {
        if (level == null || blockPos == null || vec3 == null) {
            return vec3;
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
                return subLevel.logicalPose().transformPositionInverse(realWorldVec);
            }
        }
        return vec3;
    }
}
