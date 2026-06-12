package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalPos;
import io.redspace.ironsspellbooks.spells.ender.PortalSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PortalSpell.class)
public class PortalSpellMixin {

    private static Vec3 worldToExtreme(Level level, Vec3 pos) {
        Vec3 projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos);
        if (projected.distanceToSqr(pos) > 0.01)
            return pos;

        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, pos);
        if (subLevel == null)
            subLevel = SableCompanion.INSTANCE.getContaining(level, pos.add(0, -0.5, 0));
        if (subLevel != null)
            return subLevel.logicalPose().transformPositionInverse(pos);

        return pos;
    }

    @ModifyArgs(
        method = "handleEntityPortal",
        at = @At(
            value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/spells/ender/PortalSpell;setupPortalEntity(Lnet/minecraft/world/level/Level;Lio/redspace/ironsspellbooks/entity/spells/portal/PortalData;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;F)Lio/redspace/ironsspellbooks/entity/spells/portal/PortalEntity;"
        )
    )
    private void fixSetupPortalEntityArgs(Args args) {
        Level level = args.get(0);
        Vec3 spawnPos = args.get(3);
        Vec3 dest = worldToExtreme(level, spawnPos);
        args.set(3, dest);
    }

    @ModifyArgs(
        method = "handleEntityPortal",
        at = @At(
            value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/entity/spells/portal/PortalPos;of(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/phys/Vec3;F)Lio/redspace/ironsspellbooks/entity/spells/portal/PortalPos;"
        )
    )
    private void fixPortalPosOfArgs(Args args,
                                     RecastInstance recastInstance,
                                     Level level,
                                     int i,
                                     LivingEntity livingEntity,
                                     CastSource castSource,
                                     MagicData magicData,
                                     Player player,
                                     ServerLevel serverLevel,
                                     BlockHitResult blockHitResult) {
        Vec3 pos = args.get(1);
        Vec3 dest = worldToExtreme(level, pos);
        args.set(1, dest);
    }
}
