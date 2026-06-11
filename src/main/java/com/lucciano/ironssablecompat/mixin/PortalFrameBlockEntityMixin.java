package com.lucciano.ironssablecompat.mixin;

import io.redspace.ironsspellbooks.block.portal_frame.PortalFrameBlockEntity;
import io.redspace.ironsspellbooks.capabilities.magic.PortalManager;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalData;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.lucciano.ironssablecompat.helpers.SableUnloadedSubLevelCompat;
import java.util.UUID;

@Mixin(PortalFrameBlockEntity.class)
public class PortalFrameBlockEntityMixin {

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onServerTick(Level level, BlockPos pos, BlockState blockState, PortalFrameBlockEntity entity, CallbackInfo ci) {
        UUID uuid = entity.getUUID();
        if (uuid != null) {
            PortalData portalData = PortalManager.INSTANCE.getPortalData(uuid);
            if (portalData != null) {
                boolean isPrimary = uuid.equals(portalData.portalEntityId1);
                PortalPos currentPos = isPrimary ? portalData.globalPos1 : portalData.globalPos2;
                
                Vec3 extremeLoc = entity.getPortalLocation().add(0, 0.1, 0);
                Vec3 worldLoc = SableUnloadedSubLevelCompat.getVisibleTeleportPos(level, extremeLoc);
                
                if (currentPos == null || currentPos.pos().distanceToSqr(worldLoc) > 0.01) {
                    float rotation = currentPos != null ? currentPos.rotation() : 0f;
                    PortalPos newPos = PortalPos.of(level.dimension(), worldLoc, rotation);      
                    
                    if (isPrimary) {
                        portalData.globalPos1 = newPos;
                    } else {
                        portalData.globalPos2 = newPos;
                    }
                }
            }
        }
    }
}