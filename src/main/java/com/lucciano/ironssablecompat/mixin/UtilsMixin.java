package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = Utils.class, remap = false)
public class UtilsMixin {

    @ModifyArgs(
        method = "handleSpellTeleport",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/events/SpellTeleportEvent;<init>(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lnet/minecraft/world/entity/Entity;DDD)V",
            remap = false
        )
    )
    private static void fixTeleportDestination(Args args) {
        Entity entity = args.get(1);
        double x = args.get(2);
        double y = args.get(3);
        double z = args.get(4);
        Vec3 dest = new Vec3(x, y, z);

        System.out.println("[IronsSableCompat] fixTeleportDestination called! dest=" + dest + " entity=" + entity);

        // Case 1: caster is on a ship - project OUT to real world
        Vec3 realWorldDest = SableCompanion.INSTANCE.projectOutOfSubLevel(entity.level(), dest);

        // Case 2: destination is ON a ship - project INTO it
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(entity.level(), realWorldDest);
        System.out.println("[IronsSableCompat] subLevel=" + subLevel);

        if (subLevel != null) {
            Vec3 local = subLevel.logicalPose().transformPositionInverse(realWorldDest);
            System.out.println("[IronsSableCompat] projected into subLevel: " + local);
            dest = local;
        } else {
            dest = realWorldDest;
        }

        args.set(2, dest.x);
        args.set(3, dest.y);
        args.set(4, dest.z);
    }

    @Inject(
        method = "raycastForBlock",
        remap = false,
        at = @At("RETURN"),
        cancellable = true
    )
    private static void projectRaycastForBlock(Level level, Vec3 start, Vec3 end, ClipContext.Fluid clipContext, CallbackInfoReturnable<BlockHitResult> cir) {
        BlockHitResult original = cir.getReturnValue();
        Vec3 hitLocation = original.getLocation();
        Vec3 projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, hitLocation);

        if (projected.x != hitLocation.x || projected.y != hitLocation.y || projected.z != hitLocation.z) {
            cir.setReturnValue(new BlockHitResult(
                projected,
                original.getDirection(),
                BlockPos.containing(projected),
                original.isInside()
            ));
        }
    }

    @Inject(
        method = "getTargetBlock",
        remap = false,
        at = @At("RETURN"),
        cancellable = true
    )
    private static void projectGetTargetBlock(Level level, LivingEntity entity, ClipContext.Fluid clipContext, double reach, CallbackInfoReturnable<BlockHitResult> cir) {
        BlockHitResult original = cir.getReturnValue();
        Vec3 hitLocation = original.getLocation();
        Vec3 projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, hitLocation);

        if (projected.x != hitLocation.x || projected.y != hitLocation.y || projected.z != hitLocation.z) {
            cir.setReturnValue(new BlockHitResult(
                projected,
                original.getDirection(),
                BlockPos.containing(projected),
                original.isInside()
            ));
        }
    }
}
