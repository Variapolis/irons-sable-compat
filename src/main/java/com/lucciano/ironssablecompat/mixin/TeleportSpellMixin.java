package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = TeleportSpell.class, remap = false)
public class TeleportSpellMixin {

    @ModifyArgs(
        method = "solveTeleportDestination",
        remap = false,
        at = @At("HEAD")
    )
    private static void alignSolveTeleportDestinationArgs(Args args) {
        Level level = args.get(0);
        BlockPos blockPos = args.get(2);
        Vec3 vec3 = args.get(3);

        if (level == null || blockPos == null || vec3 == null) {
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
                args.set(3, sublevelVec);
            }
        }
    }
}
