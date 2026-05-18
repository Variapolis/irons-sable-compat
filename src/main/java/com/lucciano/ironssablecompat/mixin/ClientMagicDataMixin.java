package com.lucciano.ironssablecompat.mixin;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientMagicData.class, remap = false)
public class ClientMagicDataMixin {

    @Inject(method = "getSyncedSpellData", at = @At("HEAD"), cancellable = true)
    private static void onGetSyncedSpellData(LivingEntity livingEntity, CallbackInfoReturnable<SyncedSpellData> cir) {
        if (livingEntity instanceof IMagicEntity abstractSpellCastingMob) {
            try {
                if (abstractSpellCastingMob.getMagicData() == null || abstractSpellCastingMob.getMagicData().getSyncedData() == null) {
                    cir.setReturnValue(new SyncedSpellData(livingEntity));
                }
            } catch (Exception e) {
                cir.setReturnValue(new SyncedSpellData(livingEntity));
            }
        }
    }
}
