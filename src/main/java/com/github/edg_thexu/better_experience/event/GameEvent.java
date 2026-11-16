package com.github.edg_thexu.better_experience.event;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.mixed.IFishingHook;
import com.github.edg_thexu.better_experience.module.autopotion.ForbiddenConfig;
import com.github.edg_thexu.better_experience.module.autopotion.PlayerAttribute;
import com.github.edg_thexu.better_experience.module.boomstaff.ExplodeManager;
import com.github.edg_thexu.better_experience.module.fluidstaff.FluidStaffManager;
import com.github.edg_thexu.better_experience.module.locator.StructureLocatorManager;
import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Better_experience.MODID)
public class GameEvent {

	@SubscribeEvent
	public static void serverStopped(ServerStoppedEvent event) {
		ExplodeManager.getInstance().clear();
		SpacePlacementManager.getInstance().clear();
		FluidStaffManager.getInstance().clear();
		StructureLocatorManager.getInstance().clear();
		ForbiddenConfig.clearServer();
	}

	@SubscribeEvent
	public static void serverTick$Post(ServerTickEvent.Post event) {
		PlayerAttribute.applyPendingReload(event.getServer());
		ExplodeManager.getInstance().tickHandle();
		SpacePlacementManager.getInstance().tick();
		FluidStaffManager.getInstance().tick();
		StructureLocatorManager.getInstance().tick();
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void itemFished(ItemFishedEvent event) {
		var hook = event.getHookEntity();
		if (hook instanceof IFishingHook simulation && simulation.betterExperience$isSimulation()
				&& hook.level() instanceof ServerLevel) {
			simulation.betterExperience$setItems(event.getDrops());
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void addReloadListener(AddReloadListenerEvent event) {
		event.addListener(ForbiddenConfig.getInstance());

	}
}
