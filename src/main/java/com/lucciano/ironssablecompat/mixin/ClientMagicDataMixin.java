package com.lucciano.ironssablecompat.mixin;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientMagicData.class, remap = false)
public class ClientMagicDataMixin {

    @Inject(
        method = "getSyncedSpellData",
        remap = false,
        at = @At("HEAD"),
        cancellable = true
    )
    private static void fixNullMagicData(LivingEntity livingEntity, CallbackInfoReturnable<SyncedSpellData> cir) {
        if (livingEntity instanceof IMagicEntity abstractSpellCastingMob) {
            try {
                var magicData = abstractSpellCastingMob.getMagicData();
                if (magicData == null) {
                    cir.setReturnValue(new SyncedSpellData(null));
                }
            } catch (Exception e) {
                // Defensive fallback
                cir.setReturnValue(new SyncedSpellData(null));
            }
        }
    }
}
