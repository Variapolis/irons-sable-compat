package com.lucciano.ironssablecompat.mixin;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public class LevelMixin {
    @ModifyVariable(
        method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private AABB projectGetEntitiesAABB(AABB box) {
        Level level = (Level) (Object) this;
        Vec3 min = SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vec3(box.minX, box.minY, box.minZ));
        Vec3 max = SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vec3(box.maxX, box.maxY, box.maxZ));
        return new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
    }
}