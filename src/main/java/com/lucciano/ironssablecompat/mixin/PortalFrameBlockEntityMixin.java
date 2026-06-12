package com.lucciano.ironssablecompat.mixin;
import com.lucciano.ironssablecompat.helpers.SableUnloadedSubLevelCompat;
import io.redspace.ironsspellbooks.block.portal_frame.PortalFrameBlockEntity;
import io.redspace.ironsspellbooks.capabilities.magic.PortalManager;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalData;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;
@Mixin(PortalFrameBlockEntity.class)
public class PortalFrameBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onServerTick(Level level, BlockPos pos, BlockState blockState, PortalFrameBlockEntity entity, CallbackInfo ci) {
        UUID uuid = entity.getUUID();
        if (uuid != null && level instanceof ServerLevel serverLevel) {
            PortalData portalData = PortalManager.INSTANCE.getPortalData(uuid);
            if (portalData != null) {
                boolean isPrimary = uuid.equals(portalData.portalEntityId1);
                PortalPos currentPos = isPrimary ? portalData.globalPos1 : portalData.globalPos2;
                Vec3 extremeLoc = entity.getPortalLocation().add(0, 0.1, 0);
                Vec3 worldLoc = SableUnloadedSubLevelCompat.resolveDestination(level, extremeLoc);
                float rotation = currentPos != null ? currentPos.rotation() : 0f;
                PortalPos newPos = PortalPos.of(level.dimension(), worldLoc, rotation);
                if (isPrimary) {
                    portalData.globalPos1 = newPos;
                } else {
                    portalData.globalPos2 = newPos;
                }
                // Keep portal entities in sync with the frame when the ship moves
                teleportPortalEntity(serverLevel, portalData.portalEntityId1, extremeLoc);
                teleportPortalEntity(serverLevel, portalData.portalEntityId2, extremeLoc);
            }
        }
    }
    private static void teleportPortalEntity(ServerLevel level, UUID portalEntityId, Vec3 pos) {
        if (portalEntityId != null) {
            Entity e = level.getEntity(portalEntityId);
            if (e != null) {
                e.teleportTo(pos.x, pos.y, pos.z);
            }
        }
    }
}
