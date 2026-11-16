package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceFishingSupport;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.entity.fishing.FishingSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FishingSession.class, remap = false)
public abstract class MachineFishingSessionMixin {
    @ModifyReturnValue(method = "heldRod", at = @At("RETURN"))
    private static ItemStack betterExperience$machineRod(ItemStack original, Player player) {
        var machine = ConfluenceFishingSupport.current(player);
        return machine == null ? original : machine.rod();
    }
}
