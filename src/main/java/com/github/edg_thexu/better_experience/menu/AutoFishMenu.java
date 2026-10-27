package com.github.edg_thexu.better_experience.menu;

import com.github.edg_thexu.better_experience.init.ModMenus;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.terra_curios.TCHelper;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.item.fishing.BaitItem;
import org.confluence.terra_curio.common.item.curio.BaseCurioItem;

public class AutoFishMenu extends ChestMenu {
    public final ContainerData access;


    public AutoFishMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(30), new SimpleContainerData(2));

    }

    public AutoFishMenu(int containerId, Inventory playerInventory, Container container, ContainerData pAccess) {
        super(ModMenus.AUTO_FISH_MENU.get(), containerId, playerInventory, container, 3);
        checkContainerDataCount(pAccess, 2);
        this.access = pAccess;
        int startX = 64;
        addSlot(new Slot(container, 27, startX, -16) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof FishingRodItem;
            }
        });
        addSlot(new Slot(container, 28, startX + 18, -16) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if(!ConfluenceHelper.isLoaded()){
                    return false;
                }
                return stack.getItem() instanceof org.confluence.mod.common.item.fishing.IBait;
            }
        });
        addSlot(new Slot(container, 29, startX + 36, -16) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if(!TCHelper.isLoaded()) {
                    return false;
                }
                return stack.getItem() instanceof BaseCurioItem;
            }
        });

        addDataSlots(access);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < 27 || index >= 63) {
            if (!moveItemStackTo(stack, 27, 63, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int equipment = 63; equipment < 66 && !stack.isEmpty(); equipment++) {
                if (slots.get(equipment).mayPlace(stack)) moved |= moveItemStackTo(stack, equipment, equipment + 1, false);
            }
            if (!stack.isEmpty()) moved |= moveItemStackTo(stack, 0, 27, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return before;
    }
}
