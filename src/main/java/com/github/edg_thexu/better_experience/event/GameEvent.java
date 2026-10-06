package com.github.edg_thexu.better_experience.event;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.mixed.IFishingHook;
import com.github.edg_thexu.better_experience.module.autopotion.ForbiddenConfig;
import com.github.edg_thexu.better_experience.module.boomstaff.ExplodeManager;
import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementManager;
import net.minecraft.resources.ResourceKey;
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
import org.confluence.mod.common.init.ModEffects;
import org.confluence.mod.common.init.ModLootTables;
import org.confluence.mod.mixed.IMinecraftServer;

import java.util.List;

@EventBusSubscriber(modid = Better_experience.MODID)
public class GameEvent {

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ExplodeManager.getInstance().clear();
        SpacePlacementManager.getInstance().clear();
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event){
        ExplodeManager.getInstance().tickHandle();
        SpacePlacementManager.getInstance().tick();

    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void itemFished(ItemFishedEvent event){

        FishingHook hook = event.getHookEntity();
        IFishingHook ihook = (IFishingHook)hook;
        Level level = event.getHookEntity().level();
        if(ihook.betterExperience$isSimulation() && level instanceof ServerLevel serverLevel){
            List<ItemStack> items = event.getDrops();
            if(ConfluenceHelper.isLoaded()) {
                int luck = hook.luck;

                Player player = hook.getPlayerOwner();
                float chance = player != null && player.hasEffect(ModEffects.CRATE) ? 0.25f : 0.1f;
                chance += luck / 30f;

                if (level.random.nextFloat() < chance) {
                    ResourceKey<LootTable> lootTable = IMinecraftServer.isHardmode(level.getServer()) ? ModLootTables.CRATE_HARDMODE : ModLootTables.CRATE;
                    items = serverLevel.getServer().reloadableRegistries().getLootTable(lootTable)
                            .getRandomItems(new LootParams.Builder(serverLevel)
                                    .withParameter(LootContextParams.ORIGIN, hook.position())
                                    .withParameter(LootContextParams.THIS_ENTITY, hook)
                                    .create(LootContextParamSets.GIFT));
                }
            }
            ihook.betterExperience$setItems(items);
            event.setCanceled(true);
        }

    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(ForbiddenConfig.getInstance());

    }


}
