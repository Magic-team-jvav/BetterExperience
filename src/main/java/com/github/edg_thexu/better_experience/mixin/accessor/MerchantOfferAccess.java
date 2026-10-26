package com.github.edg_thexu.better_experience.mixin.accessor;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantOffer.class)
public interface MerchantOfferAccess {
    @Accessor("demand") void betterExperience$setDemand(int demand);
}
