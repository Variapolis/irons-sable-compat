package com.lucciano.ironssablecompat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(SubLevelContainer.class)
public class SubLevelContainerMixin {

    @Unique
    private Pose3d[] ironssablecompat$lastKnownPoses;

    @Unique
    private UUID[] ironssablecompat$lastKnownUuids;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initLastKnown(Level level, int logSideLength, int logPlotSize, int originX, int originZ, CallbackInfo ci) {
        SubLevelContainer self = (SubLevelContainer) (Object) this;
        int size = (1 << self.getLogSideLength()) * (1 << self.getLogSideLength());
        ironssablecompat$lastKnownPoses = new Pose3d[size];
        ironssablecompat$lastKnownUuids = new UUID[size];
    }

    @Inject(method = "allocateSubLevel", at = @At("TAIL"))
    private void onAllocate(UUID uuid, int x, int z, Pose3d pose, CallbackInfoReturnable<SubLevel> cir) {
        SubLevelContainer self = (SubLevelContainer) (Object) this;
        int index = self.getIndex(x, z);
        ironssablecompat$lastKnownPoses[index] = new Pose3d(pose);
        ironssablecompat$lastKnownUuids[index] = uuid;
    }

    @Inject(method = "removeSubLevel(IILdev/ryanhcode/sable/sublevel/storage/SubLevelRemovalReason;)V", at = @At("HEAD"))
    private void onRemoveHead(int x, int z, SubLevelRemovalReason reason, CallbackInfo ci) {
        SubLevelContainer self = (SubLevelContainer) (Object) this;
        int index = self.getIndex(x, z);
        if (reason == SubLevelRemovalReason.REMOVED) {
            ironssablecompat$lastKnownPoses[index] = null;
            ironssablecompat$lastKnownUuids[index] = null;
        } else {
            SubLevel sub = self.getSubLevel(x, z);
            if (sub != null) {
                ironssablecompat$lastKnownPoses[index] = new Pose3d(sub.logicalPose());
            }
        }
    }
}
