package com.lucciano.ironssablecompat.mixin;

import com.lucciano.ironssablecompat.helpers.SableUnloadedSubLevelCompat;
import io.redspace.ironsspellbooks.capabilities.magic.PortalManager;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalData;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.util.UUID;

@Mixin(PortalEntity.class)
public class PortalEntityMixin {
    @Unique
    private Vec3 ironssablecompat$getDestination(PortalData portalData, ServerLevel level, UUID portalUUID) {
        UUID otherId = portalData.getConnectedPortalUUID(portalUUID);
        if (otherId != null) {
            Entity otherPortal = level.getEntity(otherId);
            if (otherPortal != null) {
                return otherPortal.position();
            }
        }
        return null;
    }

    @Redirect(
        method = "lambda$checkForEntitiesToTeleport$1",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;teleportTo(DDD)V"
        )
    )
    private void redirectPortalTeleport(Entity instance, double x, double y, double z) {
        PortalEntity thisPortal = (PortalEntity) (Object) this;
        PortalData portalData = PortalManager.INSTANCE.getPortalData(thisPortal);
        if (portalData != null && instance.level() instanceof ServerLevel serverLevel) {
            Vec3 dest = ironssablecompat$getDestination(portalData, serverLevel, thisPortal.getUUID());
            if (dest != null) {
                Vec3 resolved = SableUnloadedSubLevelCompat.resolveDestination(instance.level(), dest);
                instance.teleportTo(resolved.x, resolved.y, resolved.z);
                return;
            }
        }
        instance.teleportTo(x, y, z);
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
        PortalEntity thisPortal = (PortalEntity) (Object) this;
        PortalData portalData = PortalManager.INSTANCE.getPortalData(thisPortal);
        if (portalData != null) {
            Vec3 dest = ironssablecompat$getDestination(portalData, targetLevel, thisPortal.getUUID());
            if (dest != null) {
                Vec3 resolved = SableUnloadedSubLevelCompat.resolveDestination(targetLevel, dest);
                args.set(1, resolved);
            }
        }
    }
}
