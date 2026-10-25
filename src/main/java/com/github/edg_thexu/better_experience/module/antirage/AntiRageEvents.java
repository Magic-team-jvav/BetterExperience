package com.github.edg_thexu.better_experience.module.antirage;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.init.ModAttachments;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.mixin.accessor.LevelSettingsAccess;
import com.github.edg_thexu.better_experience.mixin.accessor.PrimaryLevelDataAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.minecraft.world.damagesource.DamageSource;
import org.confluence.mod.mixed.Immunity;

@EventBusSubscriber(modid = Better_experience.MODID)
public final class AntiRageEvents {
    private AntiRageEvents() {}

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CommonConfig.ANTI_RAGE_MODE.get()) return;
        var state = player.getData(ModAttachments.ANTI_RAGE);
        if (!state.recordDeath()) return;
        if (state.deaths() == 3) {
            // Apply before this death drops the player's inventory.
            player.server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, player.server);
            enableSingleplayerCheats(player.server);
            player.sendSystemMessage(Component.translatable("better_experience.anti_rage.assistance"));
        }
    }

    private static void enableSingleplayerCheats(MinecraftServer server) {
        // Dedicated servers retain their existing OP/permission policy.
        if (!server.isSingleplayer()) return;
        if (server.getWorldData() instanceof PrimaryLevelDataAccess access) {
            ((LevelSettingsAccess) (Object) access.betterExperience$getSettings()).betterExperience$setAllowCommands(true);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (server.isSingleplayerOwner(player.getGameProfile())) {
                    server.getPlayerList().sendPlayerPermissionLevel(player);
                    server.getCommands().sendCommands(player);
                    player.sendSystemMessage(Component.translatable("better_experience.anti_rage.cheats"));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)
                || !CommonConfig.ANTI_RAGE_MODE.get()) return;
        var state = player.getData(ModAttachments.ANTI_RAGE);
        state.respawn(now(player), player.gameMode.getGameModeForPlayer());
        if (state.hasTemporaryCreative() && !state.creativeExpired(now(player))) {
            player.setGameMode(GameType.CREATIVE);
            player.sendSystemMessage(Component.translatable("better_experience.anti_rage.creative"));
        } else if (state.deaths() > 3) {
            player.sendSystemMessage(Component.translatable("better_experience.anti_rage.protection",
                    state.deaths(), state.deaths() * 10,
                    CommonConfig.ANTI_RAGE_EXTRA_INVULNERABILITY_SECONDS.get()));
        } else if (state.deaths() > 0) {
            player.sendSystemMessage(Component.translatable("better_experience.anti_rage.progress", state.deaths()));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.hasData(ModAttachments.ANTI_RAGE)) return;
        var state = player.getData(ModAttachments.ANTI_RAGE);
        boolean disabled = !CommonConfig.ANTI_RAGE_MODE.get();
        if (disabled || state.creativeExpired(now(player))) {
            GameType original = state.reset();
            if (original != null) {
                // Do not overwrite a subsequent explicit game-mode change.
                if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) player.setGameMode(original);
                player.sendSystemMessage(Component.translatable("better_experience.anti_rage.reset"));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CommonConfig.ANTI_RAGE_MODE.get()
                || !player.isAlive() || bypassesProtection(event.getSource())
                || (event.getNewDamage() <= 0 && event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0)
                || !player.hasData(ModAttachments.ANTI_RAGE)
                || !player.getData(ModAttachments.ANTI_RAGE).protectedAt(now(player))) return;
        int extra = (int) Math.round(CommonConfig.ANTI_RAGE_EXTRA_INVULNERABILITY_SECONDS.get() * 20);
        // Vanilla's cooldown gates damage only while invulnerableTime > 10.
        int base = Math.max(0, event.getPostAttackInvulnerabilityTicks() - 10);
        if (ConfluenceHelper.isLoaded()) {
            Immunity cause = Immunity.getCause(event.getSource());
            if (cause != null) base = Math.max(base, cause.confluence$getImmunityDuration(event.getSource()));
        }
        player.getData(ModAttachments.ANTI_RAGE).extendPostHitProtection(now(player), base, extra);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CommonConfig.ANTI_RAGE_MODE.get()
                && CommonConfig.ANTI_RAGE_EXTRA_INVULNERABILITY_SECONDS.get() > 0
                && !bypassesProtection(event.getSource()) && player.hasData(ModAttachments.ANTI_RAGE)
                && player.getData(ModAttachments.ANTI_RAGE).postHitProtectedAt(now(player))) {
            // Check before the damage pipeline, including Confluence's per-source immunity.
            // A later overwrite of vanilla invulnerableTime cannot remove this protection.
            event.setInvulnerable(true);
        }
    }

    private static boolean bypassesProtection(DamageSource source) {
        // Confluence dynamically makes source.is(BYPASSES_COOLDOWN) true when
        // using its own per-source immunity. That does not mean the damage type
        // bypasses protection: read the actual registry tag for this check.
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.typeHolder().is(DamageTypeTags.BYPASSES_COOLDOWN);
    }
}
