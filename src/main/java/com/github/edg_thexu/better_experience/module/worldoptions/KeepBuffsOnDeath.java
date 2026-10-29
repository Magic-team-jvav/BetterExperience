package com.github.edg_thexu.better_experience.module.worldoptions;

import com.github.edg_thexu.better_experience.Better_experience;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Re-applies the dead player's beneficial effects on respawn; harmful ones stay lost. */
@EventBusSubscriber(modid = Better_experience.MODID)
public final class KeepBuffsOnDeath {
    private KeepBuffsOnDeath() {}

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (!WorldOptions.enabled(player.level(), WorldOptions.Feature.KEEP_BUFFS)) return;
        for (MobEffectInstance effect : event.getOriginal().getActiveEffects()) {
            if (effect.getEffect().value().isBeneficial()) player.addEffect(new MobEffectInstance(effect));
        }
    }
}
