package com.github.edg_thexu.better_experience.module.potionbag;

import com.github.edg_thexu.better_experience.data.component.PotionBagContents;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import net.minecraft.world.item.ItemStack;

public final class PotionBagStorage {
    private PotionBagStorage() {}

    public static PotionBagContents read(ItemStack bag) {
        return bag.getOrDefault(ModDataComponentTypes.POTION_BAG_CONTENTS.get(), PotionBagContents.EMPTY);
    }

    public static void write(ItemStack bag, PotionBagContents contents) {
        bag.set(ModDataComponentTypes.POTION_BAG_CONTENTS.get(), contents);
    }

    public static int insert(ItemStack bag, ItemStack input, int requested) {
        if (!PotionEffectRules.isPotion(input) || requested <= 0) return 0;
        var previous = read(bag);
        int index = previous.indexOf(input);
        int oldCount = index < 0 ? 0 : previous.entries().get(index).count();
        int accepted = Math.min(Math.min(requested, input.getCount()), Integer.MAX_VALUE - oldCount);
        var updated = previous.insert(input, accepted);
        if (updated == previous) return 0;
        write(bag, updated);
        input.shrink(accepted);
        return accepted;
    }
}
