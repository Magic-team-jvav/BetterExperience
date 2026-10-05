package com.github.edg_thexu.better_experience.module.autopotion;

import com.github.edg_thexu.better_experience.data.component.ItemContainerComponent;
import com.github.edg_thexu.better_experience.utils.ModUtils;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ProjectAuditTest {
    public static void main(String[] args) {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ItemStack first = new ItemStack(Items.STONE, 60);
        ItemStack second = new ItemStack(Items.STONE, 10);
        require(ModUtils.mergeItemStacks(second, first) == 4 && second.getCount() == 6, "partial merge");
        second.set(DataComponents.CUSTOM_NAME, Component.literal("Keep me"));
        require(ModUtils.mergeItemStacks(second, new ItemStack(Items.STONE)) == 0, "components preserved");
        require(ModUtils.mergeItemStacks(first, first) == 0, "self merge");

        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < 18; i++) slots.add(ItemStack.EMPTY);
        slots.set(7, second.copy());
        ItemContainerComponent bag = new ItemContainerComponent(ItemContainerContents.fromItems(slots), true, 18);
        require(bag.getItems().get(0).isEmpty() && bag.getItems().get(7).getCount() == 6, "sparse slots preserved");
        bag.getItems().get(7).setCount(1);
        require(bag.getItems().get(7).getCount() == 6, "defensive item copies");
        require(bag.isAutoCollect() && !bag.withAutoCollect(false).isAutoCollect(), "immutable flag");
        ItemStack oversized = new ItemStack(Items.STONE, 100);
        List<ItemStack> empty = new ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY));
        require(ModUtils.tryPlaceBackItemStackToItemStacks(oversized, empty)
                && empty.get(0).getCount() == 64 && empty.get(1).getCount() == 36, "stack limits");

        var speed = MobEffects.MOVEMENT_SPEED.value();
        ForbiddenConfig config = new ForbiddenConfig(Set.of(Items.STONE), List.of(
                new ForbiddenConfig.EffectAmp(speed, 2),
                new ForbiddenConfig.EffectAmp(speed, 1)), Set.of("example"));
        require(config.isEffectForbidden(speed, 1) && !config.isEffectForbidden(speed, 0), "duplicate effects");
        ForbiddenConfig.handleServer(config);
        require(ForbiddenConfig.getInstance().isItemForbidden(Items.STONE), "config synchronization");
        ForbiddenConfig.handleServer(new ForbiddenConfig(Set.of(), List.of(), Set.of()));
        require(!ForbiddenConfig.getInstance().isItemForbidden(Items.STONE)
                && !ForbiddenConfig.getInstance().isEffectForbidden(speed, 3)
                && !ForbiddenConfig.getInstance().isModForbidden("example"), "reload removes stale values");
        System.out.println("Project audit regression checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
