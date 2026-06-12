package com.lucciano.ironssablecompat.mixin.accessor;

import dev.ryanhcode.sable.companion.math.Pose3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(targets = "dev.ryanhcode.sable.api.sublevel.SubLevelContainer")
public interface SubLevelContainerAccess {

    @Accessor("ironssablecompat$lastKnownPoses")
    Pose3d[] ironssablecompat$getLastKnownPoses();

    @Accessor("ironssablecompat$lastKnownPoses")
    void ironssablecompat$setLastKnownPoses(Pose3d[] poses);

    @Accessor("ironssablecompat$lastKnownUuids")
    UUID[] ironssablecompat$getLastKnownUuids();

    @Accessor("ironssablecompat$lastKnownUuids")
    void ironssablecompat$setLastKnownUuids(UUID[] uuids);
}
