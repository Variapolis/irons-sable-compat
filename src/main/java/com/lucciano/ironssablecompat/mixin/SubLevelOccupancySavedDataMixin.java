package com.lucciano.ironssablecompat.mixin;

import com.lucciano.ironssablecompat.mixin.accessor.SubLevelContainerAccess;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.storage.SubLevelOccupancySavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(SubLevelOccupancySavedData.class)
public class SubLevelOccupancySavedDataMixin {

    @Shadow
    private ServerLevel level;

    private static final String KEY_POSES = "ironssablecompat:last_known_poses";
    private static final String KEY_UUIDS = "ironssablecompat:last_known_uuids";

    @Inject(method = "save", at = @At("RETURN"))
    private void onSave(CompoundTag tag, HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return;

        SubLevelContainerAccess access = (SubLevelContainerAccess) container;
        Pose3d[] poses = access.ironssablecompat$getLastKnownPoses();
        UUID[] uuids = access.ironssablecompat$getLastKnownUuids();
        if (poses == null || uuids == null) return;

        ListTag posesList = new ListTag();
        for (int i = 0; i < poses.length; i++) {
            if (poses[i] != null) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("i", i);
                entry.putDouble("px", poses[i].position().x());
                entry.putDouble("py", poses[i].position().y());
                entry.putDouble("pz", poses[i].position().z());
                entry.putDouble("ox", poses[i].orientation().x());
                entry.putDouble("oy", poses[i].orientation().y());
                entry.putDouble("oz", poses[i].orientation().z());
                entry.putDouble("ow", poses[i].orientation().w());
                entry.putDouble("rx", poses[i].rotationPoint().x());
                entry.putDouble("ry", poses[i].rotationPoint().y());
                entry.putDouble("rz", poses[i].rotationPoint().z());
                entry.putDouble("sx", poses[i].scale().x());
                entry.putDouble("sy", poses[i].scale().y());
                entry.putDouble("sz", poses[i].scale().z());
                entry.putUUID("u", uuids[i]);
                posesList.add(entry);
            }
        }
        tag.put(KEY_POSES, posesList);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private static void onLoad(ServerLevel level, CompoundTag tag, CallbackInfoReturnable<SubLevelOccupancySavedData> cir) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || !tag.contains(KEY_POSES, Tag.TAG_LIST)) return;

        SubLevelContainerAccess access = (SubLevelContainerAccess) container;
        Pose3d[] poses = access.ironssablecompat$getLastKnownPoses();
        UUID[] uuids = access.ironssablecompat$getLastKnownUuids();
        if (poses == null || uuids == null) return;

        ListTag posesList = tag.getList(KEY_POSES, Tag.TAG_COMPOUND);
        for (int i = 0; i < posesList.size(); i++) {
            CompoundTag entry = posesList.getCompound(i);
            int index = entry.getInt("i");
            if (index < 0 || index >= poses.length) continue;

            org.joml.Vector3d pos = new org.joml.Vector3d(
                entry.getDouble("px"), entry.getDouble("py"), entry.getDouble("pz"));
            org.joml.Quaterniond orient = new org.joml.Quaterniond(
                entry.getDouble("ox"), entry.getDouble("oy"), entry.getDouble("oz"), entry.getDouble("ow"));
            org.joml.Vector3d rotPoint = new org.joml.Vector3d(
                entry.getDouble("rx"), entry.getDouble("ry"), entry.getDouble("rz"));
            org.joml.Vector3d scale = new org.joml.Vector3d(
                entry.getDouble("sx"), entry.getDouble("sy"), entry.getDouble("sz"));

            poses[index] = new Pose3d(pos, orient, rotPoint, scale);
            uuids[index] = entry.getUUID("u");
        }
    }
}
