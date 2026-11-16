package com.github.edg_thexu.better_experience.module.autofish;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceFishingSupport;
import com.github.edg_thexu.better_experience.mixed.IFishingHook;
import com.github.edg_thexu.better_experience.mixed.IPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.confluence.mod.common.item.fishing.AbstractFishingPole;

public class FishBugRepetition {

    public static void detect(Player player){
        // 复刻钓鱼bug
        Level level = player.level();
        if (!level.isClientSide && CommonConfig.MULTI_FISH.get()) {
            IPlayer iplayer = (IPlayer) player;
            if (iplayer.betterExperience$getHammerUsingTicks() > 0) {
                iplayer.betterExperience$setHammerUsingTicks(iplayer.betterExperience$getHammerUsingTicks() - 1);
                ItemStack poleStack = player.getMainHandItem();
                FishingHook hook = null;
                try {
                    int luck = EnchantmentHelper.getFishingLuckBonus((ServerLevel) level, poleStack, player);
                    int speed = (int) (EnchantmentHelper.getFishingTimeReduction((ServerLevel) level, poleStack, player) * 20);
                    if (ConfluenceHelper.isLoaded() && poleStack.getItem() instanceof AbstractFishingPole pole) {
                        hook = pole.getHook(poleStack, player, level, luck, speed);
                    } else if (poleStack.getItem() instanceof FishingRodItem) {
                        hook = new FishingHook(player, level, luck, speed);
                    }
                    if (hook != null && ConfluenceHelper.isLoaded()) ConfluenceFishingSupport.startSession(hook, player, poleStack);
                } catch (Exception e) {
                    Better_experience.LOGGER.warn(e.getMessage());
                    return;
                }
                if (hook != null) {
                    ((IFishingHook) hook).betterExperience$setAutoCatch(true);
                    level.addFreshEntity(hook);
                }
            }
        }
    }
}
