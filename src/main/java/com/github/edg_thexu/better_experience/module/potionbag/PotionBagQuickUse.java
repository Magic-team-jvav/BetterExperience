package com.github.edg_thexu.better_experience.module.potionbag;

import com.github.edg_thexu.better_experience.item.PotionBag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.attachment.PlayerVoidVaultContainer;

import java.util.ArrayList;
import java.util.Map;

/** Adds bag prototypes to native potion selection, then commits consumption to their source. */
public final class PotionBagQuickUse {
    private PotionBagQuickUse() {}

    public static Iterable<ItemStack> candidates(Player player, Iterable<ItemStack> carried,
                                                 Map<ItemStack, ItemStack> sources) {
        var items = new ArrayList<ItemStack>();
        carried.forEach(items::add);
        if (player.getOffhandItem().getItem() instanceof PotionBag) items.add(player.getOffhandItem());
        // Iterate a snapshot: added prototypes must never be treated as carried bags.
        for (ItemStack bag : new ArrayList<>(items)) {
            if (!(bag.getItem() instanceof PotionBag)) continue;
            for (var entry : PotionBagStorage.read(bag).entries()) {
                if (!entry.enabled()) continue;
                ItemStack sample = entry.item();
                items.add(sample);
                sources.put(sample, bag);
            }
        }
        return items;
    }

    public static void consume(Player player, ItemStack bag, ItemStack sample) {
        if (player.level().isClientSide) return;
        var contents = PotionBagStorage.read(bag);
        int index = contents.indexOf(sample);
        if (index < 0 || !contents.entries().get(index).enabled()) return;
        ItemStack result = sample.copyWithCount(1).finishUsingItem(player.level(), player);
        if (!player.hasInfiniteMaterials()) {
            PotionBagStorage.write(bag, contents.remove(index, 1));
            if (!result.isEmpty() && !player.getInventory().add(result)) player.drop(result, false);
            player.getInventory().setChanged();
            var vault = PlayerVoidVaultContainer.of(player);
            for (ItemStack item : vault.getItems()) {
                if (item == bag) {
                    vault.setChanged();
                    break;
                }
            }
        }
    }
}
