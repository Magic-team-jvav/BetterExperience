package com.github.edg_thexu.better_experience.module.autopotion;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.concurrent.atomic.AtomicBoolean;

public final class PlayerAttribute {
	private static final ResourceLocation FALL_DISTANCE =
			Better_experience.space("fall_distance_modifier");
	private static final AtomicBoolean RELOAD_PENDING = new AtomicBoolean();

	private PlayerAttribute() {}

	public static void requestReload() {
		RELOAD_PENDING.set(true);
	}

	public static void applyAdditionalAttributes(ServerPlayer player) {
		var attribute = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
		if (attribute == null) {
			return;
		}
		attribute.removeModifier(FALL_DISTANCE);
		attribute.addTransientModifier(new AttributeModifier(
				FALL_DISTANCE, CommonConfig.ADDITIONAL_FALL_DISTANCE.get(),
				AttributeModifier.Operation.ADD_VALUE));
	}

	public static void applyPendingReload(MinecraftServer server) {
		if (!RELOAD_PENDING.getAndSet(false)) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			applyAdditionalAttributes(player);
		}
	}
}
