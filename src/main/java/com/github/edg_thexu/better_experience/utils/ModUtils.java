package com.github.edg_thexu.better_experience.utils;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class ModUtils {
    private ModUtils() {}

    /** Moves compatible items into a nonempty stack and returns the transferred count. */
    public static int mergeItemStacks(ItemStack source, ItemStack target) {
        if (source == target || source.isEmpty() || target.isEmpty()
                || !ItemStack.isSameItemSameComponents(source, target)) {
            return 0;
        }
        int transfer = Math.min(source.getCount(), Math.max(0, target.getMaxStackSize() - target.getCount()));
        target.grow(transfer);
        source.shrink(transfer);
        return transfer;
    }

    public static boolean tryAddItemStackToItemStacks(ItemStack item, List<ItemStack> items) {
        for (ItemStack current : items) {
            if (item.isEmpty()) return true;
            mergeItemStacks(item, current);
        }
        return item.isEmpty();
    }

    public static boolean tryPlaceBackItemStackToItemStacks(ItemStack item, List<ItemStack> items) {
        if (tryAddItemStackToItemStacks(item, items)) return true;
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).isEmpty()) {
                int transfer = Math.min(item.getCount(), item.getMaxStackSize());
                items.set(index, item.copyWithCount(transfer));
                item.shrink(transfer);
                if (item.isEmpty()) return true;
            }
        }
        return false;
    }

    public static void unionItemStacks(List<ItemStack> items) {
        for (int index = 0; index < items.size(); index++) {
            ItemStack current = items.get(index);
            if (current.isEmpty()) continue;
            for (int next = index + 1; next < items.size() && current.getCount() < current.getMaxStackSize(); next++) {
                mergeItemStacks(items.get(next), current);
            }
        }
    }
}
