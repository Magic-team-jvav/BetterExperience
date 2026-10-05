package com.github.edg_thexu.better_experience.event;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.attachment.EnderChestAttachment;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.init.ModAttachments;
import com.github.edg_thexu.better_experience.module.autofish.FishBugRepetition;
import com.github.edg_thexu.better_experience.module.autopotion.ForbiddenConfig;
import com.github.edg_thexu.better_experience.module.autopotion.PlayerAttribute;
import com.github.edg_thexu.better_experience.module.autopotion.PlayerInventoryManager;
import com.github.edg_thexu.better_experience.networks.c2s.PotionApplyPacketC2S;
import com.github.edg_thexu.better_experience.networks.s2c.ClientBoundConfigPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Better_experience.MODID)
public class PlayerEvents {
    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        PlayerInventoryManager.getInstance().detect(player);

        if (player.level() instanceof ServerLevel sl) {
            PlayerAttribute.notifyDirty(sl);
            FishBugRepetition.detect(player);
        }
    }

    @SubscribeEvent
    public static void entityJoinLevel(EntityJoinLevelEvent event) {
        // 欢迎语
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.connection.tickCount == 0)
                player.sendSystemMessage(Component.translatable("better_experience.welcome_message"));
            EnderChestAttachment.syncAll(player);
//            player.getInventory().add(ModItems.MAGIC_BOOM_STAFF.toStack());
            ClientBoundConfigPacket.sync(player);
            PacketDistributor.sendToPlayer(player, new PotionApplyPacketC2S(player.getData(ModAttachments.AUTO_POTION)));

        }
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            ForbiddenConfig.sync(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 同步所有相关容器
            EnderChestAttachment.syncAll(player);
            if (CommonConfig.FILL_LIFE_ON_RESPAWN.get())
                player.setHealth(player.getMaxHealth());
            PlayerAttribute.applyAdditionalAttributes(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed((float) (event.getNewSpeed() * CommonConfig.BLOCK_BREAK_SPEED_MULTIPLIER.get()));
    }
}
