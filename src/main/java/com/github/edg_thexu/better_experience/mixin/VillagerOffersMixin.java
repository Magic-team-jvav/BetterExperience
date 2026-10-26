package com.github.edg_thexu.better_experience.mixin;

import com.github.edg_thexu.better_experience.module.worldoptions.ConvenienceRules;
import com.github.edg_thexu.better_experience.module.worldoptions.WorldOptions;
import com.github.edg_thexu.better_experience.mixin.accessor.MerchantOfferAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractVillager.class)
public abstract class VillagerOffersMixin {
    @ModifyReturnValue(method = "getOffers", at = @At("RETURN"))
    private MerchantOffers betterExperience$fairOffers(MerchantOffers offers) {
        var level = ((AbstractVillager) (Object) this).level();
        boolean fair = WorldOptions.enabled(level, WorldOptions.Feature.FAIR_TRADES);
        boolean curses = WorldOptions.enabled(level, WorldOptions.Feature.NO_CURSED_LOOT);
        if (fair || curses) {
            for (var offer : offers) {
                if (fair) {
                    ((MerchantOfferAccess) offer).betterExperience$setDemand(Math.min(0, offer.getDemand()));
                    offer.setSpecialPriceDiff(Math.min(0, offer.getSpecialPriceDiff()));
                }
                if (curses) ConvenienceRules.stripCurses(offer.getResult());
            }
        }
        return offers;
    }
}
