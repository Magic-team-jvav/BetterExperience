package com.github.edg_thexu.better_experience.menu;

import com.github.edg_thexu.better_experience.init.ModMenus;
import com.github.edg_thexu.better_experience.item.PotionBag;
import com.github.edg_thexu.better_experience.module.potionbag.PotionBagEffects;
import com.github.edg_thexu.better_experience.module.potionbag.PotionBagStorage;
import com.github.edg_thexu.better_experience.module.potionbag.PotionEffectRules;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;


/** Presentation slots page over a growable vector. Only the server transfers items. */
public class PotionBagMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 27;
    private final SimpleContainer display = new SimpleContainer(PAGE_SIZE);
    private final Player owner;
    private final ItemStack sourceBag;
    private final int sourceSlot;
    private final int[] quantities = new int[PAGE_SIZE];
    private final boolean[] effectEnabled = new boolean[PAGE_SIZE];
    private final DataSlot page = DataSlot.standalone();
    private final DataSlot pages = DataSlot.standalone();
    private final DataSlot collecting = DataSlot.standalone();

    public PotionBagMenu(int id, Inventory inventory, int sourceSlot) {
        super(ModMenus.POTION_BAG_MENU.get(), id);
        this.owner = inventory.player;
        this.sourceSlot = sourceSlot;
        this.sourceBag = inventory.getItem(sourceSlot);
        for (int i = 0; i < PAGE_SIZE; i++) {
            addSlot(new Slot(display, i, 8 + i % 9 * 18, 28 + i / 9 * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        for (int i = 0; i < 36; i++) {
            int inventorySlot = i < 27 ? i + 9 : i - 27;
            int y = i < 27 ? 118 + i / 9 * 18 : 176;
            addSlot(new Slot(inventory, inventorySlot, 8 + i % 9 * 18, y) {
                @Override public boolean mayPickup(Player player) { return getContainerSlot() != sourceSlot; }
                @Override public boolean mayPlace(ItemStack stack) { return getContainerSlot() != sourceSlot; }
            });
        }
        addDataSlot(page);
        addDataSlot(pages);
        addDataSlot(collecting);
        // Vanilla property packets transmit shorts; split each quantity to retain all 32 bits.
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override public int get() { return effectEnabled[index] ? 1 : 0; }
                @Override public void set(int value) { effectEnabled[index] = value != 0; }
            });
            for (int shift : new int[]{0, 16}) addDataSlot(new DataSlot() {
                @Override public int get() { return quantities[index] >>> shift & 0xFFFF; }
                @Override public void set(int value) {
                    quantities[index] = quantities[index] & ~(0xFFFF << shift) | (value & 0xFFFF) << shift;
                }
            });
        }
        refresh();
    }

    public int page() { return page.get(); }
    public int pages() { return Math.max(1, pages.get()); }
    public int quantity(int slot) { return quantities[slot]; }
    public boolean effectEnabled(int slot) { return effectEnabled[slot]; }
    public boolean autoCollect() { return collecting.get() != 0; }
    public static boolean canPlace(ItemStack stack) { return PotionEffectRules.isPotion(stack); }

    private void refresh() {
        if (owner.level().isClientSide) return;
        var contents = PotionBagStorage.read(sourceBag);
        pages.set(Math.max(1, (contents.entries().size() + PAGE_SIZE) / PAGE_SIZE));
        page.set(Math.min(page.get(), pages() - 1));
        collecting.set(contents.autoCollect() ? 1 : 0);
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page.get() * PAGE_SIZE + i;
            var entry = index < contents.entries().size() ? contents.entries().get(index) : null;
            display.setItem(i, entry == null ? ItemStack.EMPTY : entry.item());
            quantities[i] = entry == null ? 0 : entry.count();
            effectEnabled[i] = entry != null && entry.enabled();
        }
    }

    @Override public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    public void toggleAutoCollect() {
        if (owner.level().isClientSide || !stillValid(owner)) return;
        var contents = PotionBagStorage.read(sourceBag);
        PotionBagStorage.write(sourceBag, contents.withAutoCollect(!contents.autoCollect()));
        broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (button == 2) toggleAutoCollect();
        else if (button == 0 || button == 1) page.set(Math.clamp(page.get() + (button == 0 ? -1 : 1), 0, pages() - 1));
        else return false;
        broadcastChanges();
        return true;
    }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (player.level().isClientSide || !stillValid(player)) return;
        if (type == ClickType.SWAP && button == sourceSlot) return;
        if (slotId >= PAGE_SIZE && slotId < slots.size() && slots.get(slotId).getContainerSlot() == sourceSlot) return;
        if (slotId >= 0 && slotId < PAGE_SIZE) {
            int index = page.get() * PAGE_SIZE + slotId;
            var contents = PotionBagStorage.read(sourceBag);
            var entry = index < contents.entries().size() ? contents.entries().get(index) : null;
            if (button == 1 && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE)) {
                if (entry != null) PotionBagStorage.write(sourceBag, contents.withEffectEnabled(index, !entry.enabled()));
            } else if (type == ClickType.QUICK_MOVE) quickMoveStack(player, slotId);
            else if (type == ClickType.PICKUP && button == 0) {
                ItemStack carried = getCarried();
                if (!carried.isEmpty()) PotionBagStorage.insert(sourceBag, carried, carried.getCount());
                else if (entry != null) setCarried(take(index, Integer.MAX_VALUE));
            } else if (entry != null && type == ClickType.THROW && getCarried().isEmpty()) {
                player.drop(take(index, button == 0 ? 1 : Integer.MAX_VALUE), true);
            } else if (type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)) {
                ItemStack target = player.getInventory().getItem(button);
                if (!target.isEmpty()) PotionBagStorage.insert(sourceBag, target, target.getCount());
                else if (entry != null) player.getInventory().setItem(button, take(index, Integer.MAX_VALUE));
            } else if (entry != null && type == ClickType.CLONE && player.isCreative() && getCarried().isEmpty()) {
                ItemStack copy = entry.item();
                setCarried(copy.copyWithCount(copy.getMaxStackSize()));
            }
        } else super.clicked(slotId, button, type, player);
        broadcastChanges();
        PotionBagEffects.refresh(player);
    }

    private ItemStack take(int index, int requested) {
        var contents = PotionBagStorage.read(sourceBag);
        var entry = contents.entries().get(index);
        ItemStack item = entry.item();
        int amount = Math.min(entry.count(), Math.min(requested, item.getMaxStackSize()));
        PotionBagStorage.write(sourceBag, contents.remove(index, amount));
        return item.copyWithCount(amount);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        if (player.level().isClientSide || !stillValid(player) || slotId < 0 || slotId >= slots.size()) return ItemStack.EMPTY;
        if (slotId >= PAGE_SIZE) {
            Slot slot = slots.get(slotId);
            if (slot.getContainerSlot() == sourceSlot) return ItemStack.EMPTY;
            ItemStack item = slot.getItem();
            ItemStack original = item.copy();
            if (PotionBagStorage.insert(sourceBag, item, item.getCount()) == 0) return ItemStack.EMPTY;
            slot.setChanged();
            return original;
        }
        int index = page.get() * PAGE_SIZE + slotId;
        var contents = PotionBagStorage.read(sourceBag);
        if (index >= contents.entries().size()) return ItemStack.EMPTY;
        var entry = contents.entries().get(index);
        ItemStack item = entry.item();
        item.setCount(Math.min(entry.count(), item.getMaxStackSize()));
        ItemStack original = item.copy();
        if (!moveItemStackTo(item, PAGE_SIZE, slots.size(), true)) return ItemStack.EMPTY;
        PotionBagStorage.write(sourceBag, contents.remove(index, original.getCount() - item.getCount()));
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && sourceBag.getItem() instanceof PotionBag
                && player.getInventory().getItem(sourceSlot) == sourceBag;
    }
}
