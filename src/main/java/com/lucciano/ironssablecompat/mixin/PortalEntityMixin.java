package com.lucciano.ironssablecompat.mixin;

import io.redspace.ironsspellbooks.capabilities.magic.PortalManager;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalData;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalEntity;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import com.lucciano.ironssablecompat.helpers.SableUnloadedSubLevelCompat;

import java.util.Optional;
import java.util.UUID;

@Mixin(PortalEntity.class)
public class PortalEntityMixin {

    @Unique
    private Vec3 ironssablecompat$getActualDestination(Vec3 originalDest, Level targetLevel) {
        PortalEntity thisPortal = (PortalEntity) (Object) this;
        PortalData portalData = PortalManager.INSTANCE.getPortalData(thisPortal);
        
        Vec3 dest = originalDest;
        
        if (portalData != null && targetLevel instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) targetLevel;
            UUID otherId = portalData.getConnectedPortalUUID(thisPortal.getUUID());
            if (otherId != null) {
                Entity otherPortal = serverLevel.getEntity(otherId);
                if (otherPortal != null) {
                    Optional<PortalPos> stalePosOpt = portalData.getConnectedPortalPos(thisPortal.getUUID());
                    if (stalePosOpt.isPresent()) {
                        Vec3 stalePos = stalePosOpt.get().pos();
                        Vec3 offset = originalDest.subtract(stalePos);
                        dest = otherPortal.position().add(offset);
                    }
                }
            }
        }
        
        return SableUnloadedSubLevelCompat.getVisibleTeleportPos(targetLevel, dest);
    }

    @Redirect(
        method = "lambda$checkForEntitiesToTeleport$1",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;teleportTo(DDD)V"
        )
    )
    private void redirectPortalTeleport(Entity instance, double x, double y, double z) {
        Vec3 dest = new Vec3(x, y, z);
        dest = ironssablecompat$getActualDestination(dest, instance.level());
        instance.teleportTo(dest.x, dest.y, dest.z);
    }

    @ModifyArgs(
        method = "lambda$checkForEntitiesToTeleport$1",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/portal/DimensionTransition;<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;FFLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)V"
        )
    )
    private void fixDimensionTransitionArgs(Args args) {
        ServerLevel targetLevel = args.get(0);
        Vec3 dest = args.get(1);
        
        dest = ironssablecompat$getActualDestination(dest, targetLevel);
        
        args.set(1, dest);
    }
}