package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.client.ChronopilotInformation;
import net.minecraft.client.player.LocalPlayer;
import org.confluence.terra_curio.client.handler.InformationHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = InformationHandler.class, remap = false)
public abstract class ChronopilotInformationMixin {
    @Inject(method = "handle", at = @At("TAIL"))
    private static void betterExperience$nearbyClock(LocalPlayer player, CallbackInfo ci) {
        ChronopilotInformation.addTime(player);
    }
}
