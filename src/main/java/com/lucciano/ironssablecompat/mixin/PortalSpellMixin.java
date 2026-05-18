package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import io.redspace.ironsspellbooks.spells.ender.PortalSpell;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PortalSpell.class)
public class PortalSpellMixin {

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
        
        Vec3 projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, spawnPos);
        Vec3 dest = spawnPos;
        
        if (projected.distanceToSqr(spawnPos) > 0.01) {
            // It's already an extreme coordinate on a ship, leave it alone!
            dest = spawnPos;
        } else {
            SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, spawnPos);
            if (subLevel == null) {
                // The portal spawns slightly above the floor. If it's just outside the bounding box, check slightly below!
                subLevel = SableCompanion.INSTANCE.getContaining(level, spawnPos.add(0, -0.5, 0));
            }
            if (subLevel != null) {
                // If it's real world but inside a ship's bounds, spawn it inside the ship
                dest = subLevel.logicalPose().transformPositionInverse(spawnPos);
            }
        }
        
        System.out.println("[IronsSableCompat] PortalEntity spawning at: " + dest + " (original spawnPos: " + spawnPos + ")");
        
        args.set(3, dest);
    }
    
    // We intentionally DO NOT modify PortalPos.of(). It should save the Real-World coordinates
    // so our PortalEntityMixin can correctly intercept and transform them dynamically.
}
