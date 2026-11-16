package com.github.edg_thexu.better_experience.intergration.confluence;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.mod.common.entity.fishing.FishingSession;
import org.confluence.mod.common.item.fishing.IBait;
import org.confluence.mod.mixed.IFishingHook;
import org.confluence.mod.util.PlayerUtils;
import org.confluence.terra_curio.common.init.TCItems;
import org.confluence.terra_curio.common.item.curio.BaseCurioItem;
import org.confluence.terra_curio.util.TCUtils;

/** Binds machine equipment to synchronous loot generation without editing the owner's inventory or attributes. */
public final class ConfluenceFishingSupport {
    private static final ThreadLocal<MachineCatch> CURRENT = new ThreadLocal<>();

    private ConfluenceFishingSupport() {}

    public static MachineCatch current(Player player) {
        MachineCatch current = CURRENT.get();
        return current != null && current.player == player && player.fishing == current.hook ? current : null;
    }

    public static Scope bind(ServerPlayer player, FishingHook hook, ItemStack rod, ItemStack bait, ItemStack accessory) {
        MachineCatch previous = CURRENT.get();
        // An independent rod stack also makes the native offhand/alternate-rod attribute calculation apply.
        CURRENT.set(new MachineCatch(player, hook, rod.copy(), bait, accessory));
        return new Scope(previous);
    }

    public static void startSession(FishingHook hook, Player player, ItemStack rod) {
        IFishingHook.of(hook).confluence$setFishingSession(new FishingSession(player, rod, InteractionHand.MAIN_HAND));
    }

    public static float power(Player player, ItemStack pole, Item bait, ItemStack accessory) {
        if (player instanceof ServerPlayer serverPlayer) return PlayerUtils.getFishingPower(serverPlayer);
        AttributeInstance evaluated = new AttributeInstance(ConfluenceMagicLib.FISHING_POWER, ignored -> {});
        if (player != null) {
            AttributeInstance attribute = player.getAttribute(ConfluenceMagicLib.FISHING_POWER);
            if (attribute != null) evaluated.replaceFrom(attribute);
            player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (type, modifier) -> {
                if (type.is(ConfluenceMagicLib.FISHING_POWER.getKey())) evaluated.removeModifier(modifier.id());
            });
        }
        if (pole != null) addRodModifiers(evaluated, pole);
        if (accessory != null) addAccessoryModifiers(evaluated, accessory);
        float baitPower = bait instanceof IBait value ? value.getBaitBonus() * 100 : 0;
        return (float) evaluated.getValue() + baitPower;
    }

    public static AttributeInstance withMachineAccessory(Player player, Holder<Attribute> type, AttributeInstance original) {
        MachineCatch current = current(player);
        if (current == null || original == null || !type.is(ConfluenceMagicLib.FISHING_POWER.getKey())) return original;
        AttributeInstance copy = new AttributeInstance(type, ignored -> {});
        copy.replaceFrom(original);
        addAccessoryModifiers(copy, current.accessory);
        return copy;
    }

    private static void addRodModifiers(AttributeInstance evaluated, ItemStack item) {
        item.forEachModifier(EquipmentSlot.MAINHAND, (type, modifier) -> {
            if (type.is(ConfluenceMagicLib.FISHING_POWER.getKey())) replaceModifier(evaluated, modifier);
        });
    }

    private static void addAccessoryModifiers(AttributeInstance evaluated, ItemStack item) {
        addRodModifiers(evaluated, item);
        if (item.getItem() instanceof BaseCurioItem curio) {
            curio.getAttributeModifiers(item).forEach((type, modifier) -> {
                if (type.is(ConfluenceMagicLib.FISHING_POWER.getKey())) replaceModifier(evaluated, modifier);
            });
            return;
        }
        var component = TCUtils.getAccessoriesComponent(item);
        var attributes = component == null ? null : component.get(TCItems.ATTRIBUTES);
        if (attributes != null) attributes.get().forEach((type, modifier) -> {
            if (type.is(ConfluenceMagicLib.FISHING_POWER.getKey())) replaceModifier(evaluated, modifier);
        });
    }

    private static void replaceModifier(AttributeInstance evaluated, AttributeModifier modifier) {
        evaluated.removeModifier(modifier.id());
        evaluated.addTransientModifier(modifier);
    }

    public record MachineCatch(ServerPlayer player, FishingHook hook, ItemStack rod, ItemStack bait, ItemStack accessory) {}

    public static final class Scope implements AutoCloseable {
        private final MachineCatch previous;

        private Scope(MachineCatch previous) { this.previous = previous; }

        @Override
        public void close() {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
