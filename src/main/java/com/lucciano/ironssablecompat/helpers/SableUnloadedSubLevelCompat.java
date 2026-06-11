package com.lucciano.ironssablecompat.helpers;

import com.lucciano.ironssablecompat.mixin.accessor.SubLevelHoldingChunkAccessor;
import com.lucciano.ironssablecompat.mixin.accessor.SubLevelHoldingChunkMapAccessor;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.io.File;

public final class SableUnloadedSubLevelCompat {

    private SableUnloadedSubLevelCompat() {}

    public static Vec3 resolveDestination(Level level, Vec3 localStoredPos) {
        SubLevel loaded = Sable.HELPER.getContaining(level, localStoredPos);
        if (loaded != null) {
            return loaded.logicalPose().transformPosition(toStorage(localStoredPos));
        }

        if (level instanceof ServerLevel serverLevel) {
            SubLevelData stored = findStoredSubLevelData(serverLevel, BlockPos.containing(localStoredPos));
            if (stored != null) {
                return transformFromStoredPose(stored, toStorage(localStoredPos));
            }
        }

        return localStoredPos;
    }

    public static Vec3 getVisibleTeleportPos(Level level, Vec3 waystoneTargetPos) {
        SubLevel targetSubLevel = Sable.HELPER.getContaining(level, waystoneTargetPos);
        if (targetSubLevel == null) {
            SubLevelData storedTarget = level instanceof ServerLevel serverLevel
                    ? findStoredSubLevelData(serverLevel, BlockPos.containing(waystoneTargetPos))
                    : null;
            if (storedTarget == null) {
                return waystoneTargetPos;
            }

            return transformFromStoredPose(storedTarget, toStorage(waystoneTargetPos));
        }

        return targetSubLevel.logicalPose().transformPosition(toStorage(waystoneTargetPos));
    }


    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static Vec3 toStorage(Vec3 pos) {
        return new Vec3(pos.x, pos.y - 0.5, pos.z);
    }

    private static Vec3 transformFromStoredPose(SubLevelData data, Vec3 localPos) {
        Vector3d out = data.pose().transformPosition(new Vector3d(localPos.x, localPos.y, localPos.z));
        return new Vec3(out.x, out.y, out.z);
    }

    private static SubLevelData findStoredSubLevelData(ServerLevel level, BlockPos targetPos) {
        ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);
        if (container == null) return null;

        int chunkX = targetPos.getX() >> 4;
        int chunkZ = targetPos.getZ() >> 4;
        if (!container.inBounds(chunkX, chunkZ)) return null;

        int plotX = (chunkX >> container.getLogPlotSize()) - container.getOrigin().x;
        int plotZ = (chunkZ >> container.getLogPlotSize()) - container.getOrigin().y;
        if (plotX < 0 || plotZ < 0) return null;

        if (container.getPlot(chunkX, chunkZ) == null
                && !container.getOccupancy().get(container.getIndex(plotX, plotZ))) {
            return null;
        }

        SubLevelData fromLoaded = findInLoadedHoldingChunks(container, plotX, plotZ);
        if (fromLoaded != null) return fromLoaded;

        return findInRegionFiles(container, plotX, plotZ);
    }

    private static SubLevelData findInLoadedHoldingChunks(ServerSubLevelContainer container,
                                                           int localPlotX, int localPlotZ) {
        SubLevelHoldingChunkMapAccessor mapAccessor =
                (SubLevelHoldingChunkMapAccessor) container.getHoldingChunkMap();

        for (SubLevelHoldingChunk loadedChunk : mapAccessor.ironsSable$getLoadedHoldingChunks().values()) {
            SubLevelHoldingChunkAccessor chunkAccessor = (SubLevelHoldingChunkAccessor) loadedChunk;
            for (HoldingSubLevel holdingSub : chunkAccessor.ironsSable$getLoadedHoldingSubLevels().values()) {
                BlockPos plotPos = readPlotPos(holdingSub.data());
                if (plotPos != null && plotPos.getX() == localPlotX && plotPos.getZ() == localPlotZ) {
                    return holdingSub.data();
                }
            }
        }
        return null;
    }

    private static SubLevelData findInRegionFiles(ServerSubLevelContainer container,
                                                  int localPlotX, int localPlotZ) {
        SubLevelStorage storage = container.getHoldingChunkMap().getStorage();
        File[] regionFiles = storage.getFolder().toFile()
                .listFiles((dir, name) -> name.endsWith(SubLevelRegionFile.FILE_EXTENSION));
        if (regionFiles == null) return null;

        for (File regionFile : regionFiles) {
            String name = regionFile.getName();
            String withoutExt = name.substring(0, name.length() - SubLevelRegionFile.FILE_EXTENSION.length());
            String[] parts = withoutExt.split("\\.");
            if (parts.length != 3) continue;

            int regionX, regionZ;
            try {
                regionX = Integer.parseInt(parts[1]);
                regionZ = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
                continue;
            }

            for (int lx = 0; lx < SubLevelRegionFile.SIDE_LENGTH; lx++) {
                for (int lz = 0; lz < SubLevelRegionFile.SIDE_LENGTH; lz++) {
                    ChunkPos chunkPos = new ChunkPos(
                            regionX * SubLevelRegionFile.SIDE_LENGTH + lx,
                            regionZ * SubLevelRegionFile.SIDE_LENGTH + lz);

                    SubLevelHoldingChunk holdingChunk = storage.attemptLoadHoldingChunk(chunkPos);
                    if (holdingChunk == null) continue;

                    for (SavedSubLevelPointer pointer : holdingChunk.getSubLevelPointers()) {
                        SubLevelData data = storage.attemptLoadSubLevel(chunkPos, pointer);
                        if (data == null) continue;

                        BlockPos plotPos = readPlotPos(data);
                        if (plotPos != null && plotPos.getX() == localPlotX && plotPos.getZ() == localPlotZ) {
                            return data;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos readPlotPos(SubLevelData data) {
        if (!data.fullTag().contains("plot")) return null;
        var plotTag = data.fullTag().getCompound("plot");
        if (!plotTag.contains("plot_x") || !plotTag.contains("plot_z")) return null;
        return new BlockPos(plotTag.getInt("plot_x"), 0, plotTag.getInt("plot_z"));
    }
}