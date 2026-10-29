package com.github.edg_thexu.better_experience.event;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.mixed.IFishingHook;
import com.github.edg_thexu.better_experience.module.autopotion.ForbiddenConfig;
import com.github.edg_thexu.better_experience.module.boomstaff.ExplodeManager;
import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementManager;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidStaffManager;
import com.github.edg_thexu.better_experience.module.fluidstaff.FluidStaffManager;
import net.minecraft.resources.ResourceKey;
import com.github.edg_thexu.better_experience.module.locator.StructureLocatorManager;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = Better_experience.MODID)
public class GameEvent {

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ExplodeManager.getInstance().clear();
        SpacePlacementManager.getInstance().clear();
        FluidStaffManager.getInstance().clear();
        StructureLocatorManager.getInstance().clear();
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event){
        ExplodeManager.getInstance().tickHandle();
        SpacePlacementManager.getInstance().tick();
        FluidStaffManager.getInstance().tick();
        StructureLocatorManager.getInstance().tick();

    }

    @SubscribeEvent
    public static void locatorLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StructureLocatorManager.getInstance().synchronize(player);
            VoidStaffManager.synchronize(player);
        }
    }

    @SubscribeEvent
    public static void voidStaffRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) VoidStaffManager.synchronize(player);
    }

    @SubscribeEvent
    public static void locatorDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StructureLocatorManager.getInstance().cancel(player.getUUID());
            StructureLocatorManager.getInstance().synchronize(player);
            VoidStaffManager.synchronize(player);
        }
    }

    @SubscribeEvent
    public static void locatorLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        StructureLocatorManager.getInstance().cancel(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void itemFished(ItemFishedEvent event){

        FishingHook hook = event.getHookEntity();
        IFishingHook ihook = (IFishingHook)hook;
        Level level = event.getHookEntity().level();
        if(ihook.betterExperience$isSimulation() && level instanceof ServerLevel serverLevel){
            List<ItemStack> items = event.getDrops();
            ihook.betterExperience$setItems(items);
            event.setCanceled(true);
        }

    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(ForbiddenConfig.getInstance());

    }


}
