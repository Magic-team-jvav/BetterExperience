package com.github.edg_thexu.better_experience.module.ammochain;

import com.github.edg_thexu.better_experience.data.component.AmmoChainCursor;
import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.intergration.terra_gun.AmmoChainGunSupport;
import com.github.edg_thexu.better_experience.intergration.terra_gun.TGHelper;
import com.github.edg_thexu.better_experience.item.AmmoChain;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.List;
import java.util.function.Predicate;

/** Selection is read-only. Only a successful logical shot advances the persistent cursor. */
public final class AmmoChainManager {
    private static final List<TagKey<Item>> AMMO_TAGS = List.of(
            ammoTag("terra_guns:ammo"), ammoTag("c:ammo"), ammoTag("c:arrows"),
            ammoTag("c:bullets"), ammoTag("c:rockets"), ammoTag("c:darts"));
    private static final ResourceLocation FALLEN_STAR = ResourceLocation.parse("confluence:fallen_star");
    private record Active(ItemStack stack, AmmoChainSettings settings, AmmoChainCursor cursor) {}

    private AmmoChainManager() {}

    private static TagKey<Item> ammoTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    public static boolean isAmmo(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ArrowItem || stack.is(ItemTags.ARROWS)
                || stack.is(Items.FIREWORK_ROCKET) || AMMO_TAGS.stream().anyMatch(stack::is)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(FALLEN_STAR));
    }

    public static Predicate<ItemStack> projectilePredicate(Player player, ItemStack weapon) {
        if (weapon.getItem() instanceof ProjectileWeaponItem item) {
            return item.getAllSupportedProjectiles(weapon).or(item.getSupportedHeldProjectiles(weapon));
        }
        if (TGHelper.isLoaded() && AmmoChainGunSupport.isGun(weapon)) {
            return ammo -> AmmoChainGunSupport.compatible(player, ammo, weapon);
        }
        return ammo -> false;
    }

    public static boolean controls(Player player, ItemStack weapon) {
        return active(player, projectilePredicate(player, weapon)) != null;
    }

    public static ItemStack projectile(Player player, ItemStack weapon, ItemStack fallback) {
        return gunProjectile(player, weapon, projectilePredicate(player, weapon), fallback);
    }

    public static ItemStack gunProjectile(Player player, ItemStack weapon, Predicate<ItemStack> compatible, ItemStack fallback) {
        if (weapon.isEmpty()) return fallback;
        var active = active(player, compatible);
        if (active == null) return fallback;
        var cursor = AmmoChainQueue.choose(active.settings, active.cursor, index -> {
            var sample = active.settings.entries().get(index).ammo();
            return compatible.test(sample) && !findAmmo(player, sample).isEmpty();
        });
        return cursor.map(value -> findAmmo(player, active.settings.entries().get(value.index()).ammo())).orElse(ItemStack.EMPTY);
    }

    private static Active active(Player player, Predicate<ItemStack> compatible) {
        if (player.isSpectator()) return null;
        // The first enabled chain that applies to this weapon takes precedence, including when empty.
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            var active = active(player.getInventory().items.get(slot), compatible);
            if (active != null) return active;
        }
        return active(player.getOffhandItem(), compatible);
    }

    private static Active active(ItemStack stack, Predicate<ItemStack> compatible) {
        if (!(stack.getItem() instanceof AmmoChain)) return null;
        var settings = stack.getOrDefault(ModDataComponentTypes.AMMO_CHAIN_SETTINGS.get(), AmmoChainSettings.defaults());
        if (!settings.enabled() || settings.entries().stream().noneMatch(entry -> !entry.isEmpty() && compatible.test(entry.ammo()))) return null;
        return new Active(stack, settings, stack.getOrDefault(ModDataComponentTypes.AMMO_CHAIN_CURSOR.get(), AmmoChainCursor.DEFAULT));
    }

    private static ItemStack findAmmo(Player player, ItemStack sample) {
        for (var stack : player.getInventory().items) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, sample)) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static void shot(Player player, ItemStack weapon, ItemStack projectile) {
        if (!(player instanceof ServerPlayer) || projectile.isEmpty()) return;
        var sample = projectile.copyWithCount(1);
        sample.remove(DataComponents.INTANGIBLE_PROJECTILE);
        var compatible = projectilePredicate(player, weapon);
        var active = active(player, compatible);
        if (active == null) return;
        // Include the fired copy even if the last inventory item was consumed before this callback.
        var cursor = AmmoChainQueue.choose(active.settings, active.cursor, index -> {
            var configured = active.settings.entries().get(index).ammo();
            return compatible.test(configured) && (ItemStack.isSameItemSameComponents(configured, sample)
                    || !findAmmo(player, configured).isEmpty());
        });
        if (cursor.isEmpty() || !ItemStack.isSameItemSameComponents(active.settings.entries().get(cursor.get().index()).ammo(), sample)) return;
        active.stack.set(ModDataComponentTypes.AMMO_CHAIN_CURSOR.get(), AmmoChainQueue.advance(active.settings, cursor.get()));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }
}
