package com.github.edg_thexu.better_experience.module.locator;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.attachment.ExtraInventory;
import org.confluence.mod.common.attachment.PlayerPiggyBankContainer;
import org.confluence.mod.util.PlayerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

/** Plans both the debit and its change before modifying any of the player's coin containers. */
public final class LocatorPayment {
    public static final int GOLD_COST = 1;
    private static final long COPPER_COST = GOLD_COST * 10_000L;
    private static final long[] COIN_VALUES = {1_000_000, 10_000, 100, 1};

    public enum Result { PAID, INSUFFICIENT_FUNDS, NO_CHANGE_SPACE }
    record Slot(Container container, int index) {}

    private LocatorPayment() {}

    public static boolean canAfford(Player player) {
        if (player.isCreative()) return true;
        long balance = 0;
        for (var slot : coinSlots(player)) {
            var stack = slot.container.getItem(slot.index);
            balance += coinValue(stack) * stack.getCount();
        }
        return balance >= COPPER_COST;
    }

    private static List<Slot> coinSlots(Player player) {
        var slots = new ArrayList<Slot>();
        var bank = PlayerPiggyBankContainer.of(player);
        var extra = ExtraInventory.of(player);
        for (int i = 0; i < bank.getContainerSize(); i++) slots.add(new Slot(bank, i));
        for (int i = 0; i < ExtraInventory.SIZE_COINS; i++) slots.add(new Slot(extra, ExtraInventory.COINS_START + i));
        for (int i = 0; i < player.getInventory().items.size(); i++) slots.add(new Slot(player.getInventory(), i));
        return slots;
    }

    public static Result pay(ServerPlayer player) {
        if (player.isCreative()) return Result.PAID;
        var result = pay(coinSlots(player), COPPER_COST, LocatorPayment::coinValue, value -> {
            for (int i = 0; i < COIN_VALUES.length; i++) {
                if (COIN_VALUES[i] == value) return PlayerUtils.INDEX_2_COIN.apply(3 - i).getDefaultInstance();
            }
            throw new IllegalArgumentException("Unknown coin denomination: " + value);
        });
        if (result == Result.PAID) ExtraInventory.of(player).sync(player);
        return result;
    }

    private static long coinValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int index = PlayerUtils.COIN_2_INDEX.applyAsInt(stack.getItem());
        return index < 0 || index >= COIN_VALUES.length ? 0 : COIN_VALUES[index];
    }

    static Result pay(List<Slot> slots, long cost, ToLongFunction<ItemStack> coinValue, LongFunction<ItemStack> coin) {
        if (cost <= 0) throw new IllegalArgumentException("Coin cost must be positive");
        var planned = new ArrayList<ItemStack>(slots.size());
        long balance = 0;
        for (var slot : slots) {
            var stack = slot.container.getItem(slot.index).copy();
            planned.add(stack);
            balance += coinValue.applyAsLong(stack) * stack.getCount();
        }
        if (balance < cost) return Result.INSUFFICIENT_FUNDS;
        long remaining = cost;
        for (var stack : planned) {
            long value = coinValue.applyAsLong(stack);
            if (value == 0) continue;
            int count = (int) Math.min(stack.getCount(), remaining / value);
            stack.shrink(count);
            remaining -= count * value;
            if (remaining == 0) break;
        }
        if (remaining > 0) {
            int smallest = -1;
            long smallestValue = Long.MAX_VALUE;
            for (int i = 0; i < planned.size(); i++) {
                var stack = planned.get(i);
                long value = coinValue.applyAsLong(stack);
                if (!stack.isEmpty() && value > 0 && value < smallestValue) {
                    smallest = i;
                    smallestValue = value;
                }
            }
            if (smallest < 0) return Result.INSUFFICIENT_FUNDS;
            planned.get(smallest).shrink(1);
            remaining -= smallestValue;
        }
        long change = -remaining;
        for (long value : COIN_VALUES) {
            int count = (int) (change / value);
            if (count == 0) continue;
            var prototype = coin.apply(value);
            // Smaller coins were exhausted before breaking the smallest remaining denomination.
            for (int i = 0; i < planned.size() && count > 0; i++) {
                if (!planned.get(i).isEmpty()) continue;
                var slot = slots.get(i);
                if (!slot.container.canPlaceItem(slot.index, prototype)) continue;
                int insert = Math.min(count, slot.container.getMaxStackSize(prototype));
                if (insert <= 0) continue;
                planned.set(i, prototype.copyWithCount(insert));
                count -= insert;
            }
            if (count > 0) return Result.NO_CHANGE_SPACE;
            change %= value;
        }
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            var current = slot.container.getItem(slot.index);
            var next = planned.get(i);
            if (ItemStack.matches(current, next)) continue;
            if (ItemStack.isSameItemSameComponents(current, next)) {
                // A mod may already allow larger stacks here; setItem can clamp an existing stack on debit.
                current.setCount(next.getCount());
                slot.container.setChanged();
            } else {
                slot.container.setItem(slot.index, next);
            }
        }
        return Result.PAID;
    }
}
