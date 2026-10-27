package com.github.edg_thexu.better_experience.module.potionbag;

import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.data.component.PotionBagContents;
import com.github.edg_thexu.better_experience.init.ModAttachments;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.item.PotionBag;
import com.github.edg_thexu.better_experience.menu.PotionBagMenu;
import com.github.edg_thexu.better_experience.networks.c2s.PotionApplyPacketC2S;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.confluence.mod.common.init.ModAttachmentTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Computes effects from actual server inventory; clients submit preferences, never grants. */
public final class PotionBagEffects {
    private PotionBagEffects() {}

    public static void tick(Player player) {
        if (!(player instanceof ServerPlayer) || player.tickCount % 20 != 0) return;
        collect(player);
        refresh(player);
    }

    private static void collect(Player player) {
        if (player.isSpectator() || !player.isAlive() || player.containerMenu instanceof PotionBagMenu) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack bag = player.getInventory().getItem(i);
            if (!(bag.getItem() instanceof PotionBag)) continue;
            if (!PotionBagStorage.read(bag).autoCollect()) continue;
            for (ItemStack item : player.getInventory().items) PotionBagStorage.insert(bag, item, item.getCount());
        }
    }

    public static void addBagEffects(ItemStack bag, int threshold, Map<Holder<MobEffect>, Integer> effects) {
        addBagEffects(PotionBagStorage.read(bag), threshold, effects);
    }

    public static void addBagEffects(PotionBagContents contents, int threshold, Map<Holder<MobEffect>, Integer> effects) {
        for (var entry : contents.entries()) {
            if (!entry.enabled() || entry.count() < threshold) continue;
            for (var effect : PotionEffectRules.potionEffects(entry.item())) effects.merge(effect.getA(), effect.getB(), Math::max);
        }
    }

    private static void addNormalItems(List<ItemStack> items, Map<Holder<MobEffect>, Integer> effects) {
        for (ItemStack item : items) {
            if (item.getItem() instanceof PotionBag) continue;
            for (var effect : PotionEffectRules.effects(item, false)) effects.merge(effect.getA(), effect.getB(), Math::max);
        }
    }

    public static void refresh(Player entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        var available = new HashMap<Holder<MobEffect>, Integer>();
        if (player.isAlive() && !player.isSpectator()) {
            int threshold = CommonConfig.POTION_BAG_EFFECT_COUNT.get();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item.getItem() instanceof PotionBag) {
                    addBagEffects(item, threshold, available);
                }
            }
            if (CommonConfig.AUTO_POTION_OPEN.get()) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    addNormalItems(List.of(player.getInventory().getItem(i)), available);
                }
                addNormalItems(player.getEnderChestInventory().getItems(), available);
                if (ConfluenceHelper.isLoaded()) {
                    addNormalItems(player.getData(ModAttachmentTypes.PIGGY_BANK).getItems(), available);
                    addNormalItems(player.getData(ModAttachmentTypes.SAFE).getItems(), available);
                }
            }
        }
        var data = player.getData(ModAttachments.AUTO_POTION);
        var desired = new HashMap<>(available);
        desired.keySet().removeIf(data::isForbidden);
        var previous = new HashMap<>(data.getCachedPotions());
        // The applied cache is transient; reconcile persisted grants after login.
        if (previous.isEmpty()) previous.putAll(data.getPotions());
        for (var effect : previous.entrySet()) {
            if (effect.getValue().equals(desired.get(effect.getKey()))) continue;
            MobEffectInstance active = player.getEffect(effect.getKey());
            if (active != null) {
                MobEffectInstance retained = withoutGrant(active, effect.getValue());
                if (retained == active) continue;
                player.removeEffect(effect.getKey());
                if (retained != null) player.addEffect(retained);
            }
        }
        for (var effect : desired.entrySet()) {
            MobEffectInstance active = player.getEffect(effect.getKey());
            if (active == null || active.getAmplifier() < effect.getValue()) {
                player.addEffect(new MobEffectInstance(effect.getKey(), -1, effect.getValue(), false, false));
            }
        }
        boolean changed = !data.getPotions().equals(available) || !data.getCachedPotions().equals(desired);
        data.getPotions().clear();
        data.getPotions().putAll(available);
        data.getCachedPotions().clear();
        data.getCachedPotions().putAll(desired);
        if (changed) PacketDistributor.sendToPlayer(player, new PotionApplyPacketC2S(data));
    }

    /** A stronger timed potion can hide our infinite effect. Remove that hidden grant too. */
    public static MobEffectInstance withoutGrant(MobEffectInstance active, int amplifier) {
        CompoundTag saved = (CompoundTag) active.save();
        CompoundTag cleaned = trimGrant(saved.copy(), amplifier);
        if (cleaned == null) return null;
        cleaned.put("id", saved.get("id").copy());
        return cleaned.equals(saved) ? active : MobEffectInstance.load(cleaned);
    }

    private static CompoundTag trimGrant(CompoundTag details, int amplifier) {
        if (details.contains("hidden_effect", Tag.TAG_COMPOUND)) {
            CompoundTag hidden = trimGrant(details.getCompound("hidden_effect"), amplifier);
            if (hidden == null) details.remove("hidden_effect");
            else details.put("hidden_effect", hidden);
        }
        if (details.getInt("duration") != -1 || details.getInt("amplifier") != amplifier) return details;
        return details.contains("hidden_effect", Tag.TAG_COMPOUND) ? details.getCompound("hidden_effect") : null;
    }
}
