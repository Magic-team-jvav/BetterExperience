package com.github.edg_thexu.better_experience.module.reforge;

import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.init.ModTags;

/**
 * Confluence only treats items in {@code confluence:prefix_accessory_only} (forwarding to {@code curios:accessory})
 * as reforgable accessories, so accessories that live in another Curios slot tag can neither enter the reforge slot
 * nor get a prefix type. Any item Curios can wear carries a {@code curios:*} tag, which is a reliable signal.
 */
public final class ReforgeAccessories {
    private ReforgeAccessories() {}

    public static boolean isAccessory(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try {
            return stack.getItemHolder().tags()
                    .anyMatch(tag -> "curios".equals(tag.location().getNamespace()));
        } catch (IllegalStateException notBound) {
            return false;
        }
    }

    /** 视为可重铸：Confluence 标签以外的饰品，且没有被明确禁止附加词条。 */
    public static boolean isReforgeable(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(ModTags.Items.UNABLE_TO_APPLY_PREFIX) && isAccessory(stack);
    }
}
