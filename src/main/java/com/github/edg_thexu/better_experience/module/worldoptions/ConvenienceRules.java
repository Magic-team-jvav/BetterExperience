package com.github.edg_thexu.better_experience.module.worldoptions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public final class ConvenienceRules {
    public static final String LEATHER_GROUP = "better_experience:optional_leather";
    public static final String APPLE_GROUP = "better_experience:optional_enchanted_apple";

    private ConvenienceRules() {}

    public static boolean isPet(LivingEntity entity) {
        return entity instanceof Mob && entity instanceof OwnableEntity owned && owned.getOwnerUUID() != null;
    }

    public static boolean downedPet(LivingEntity entity) {
        return isPet(entity) && entity.getHealth() <= 1.0F
                && WorldOptions.enabled(entity.level(), WorldOptions.Feature.PROTECTED_PETS);
    }

    public static boolean neutral(LivingEntity entity) {
        return !isPet(entity) && (entity instanceof NeutralMob || entity instanceof Piglin || entity instanceof Dolphin
                || entity instanceof Llama || entity instanceof Panda || entity instanceof Goat);
    }

    public static boolean blocksAttack(LivingEntity attacker, LivingEntity target) {
        return downedPet(attacker) || downedPet(target) || target instanceof Player && neutral(attacker)
                && WorldOptions.enabled(attacker.level(), WorldOptions.Feature.FRIENDLY_NEUTRALS);
    }

    public static void forgetCombat(Mob mob) {
        boolean chasing = mob.getTarget() != null || mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        mob.setLastHurtByPlayer(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        if (chasing) {
            mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            mob.getNavigation().stop();
        }
        if (mob instanceof NeutralMob neutral) neutral.stopBeingAngry();
    }

    public static void stripCurses(ItemStack stack) {
        stripCurses(stack, DataComponents.ENCHANTMENTS);
        stripCurses(stack, DataComponents.STORED_ENCHANTMENTS);
    }

    private static void stripCurses(ItemStack stack, net.minecraft.core.component.DataComponentType<ItemEnchantments> type) {
        ItemEnchantments enchantments = stack.get(type);
        if (enchantments == null || enchantments.keySet().stream().noneMatch(holder -> holder.is(EnchantmentTags.CURSE))) return;
        var clean = new ItemEnchantments.Mutable(enchantments);
        clean.removeIf(holder -> holder.is(EnchantmentTags.CURSE));
        stack.set(type, clean.toImmutable());
    }

    public static boolean recipeEnabled(String group, Level level) {
        return switch (group) {
            case LEATHER_GROUP -> WorldOptions.enabled(level, WorldOptions.Feature.LEATHER_RECIPE);
            case APPLE_GROUP -> WorldOptions.enabled(level, WorldOptions.Feature.ENCHANTED_APPLE_RECIPE);
            default -> true;
        };
    }

    public static boolean fullSpeedBow(ItemStack stack) {
        return stack.getUseAnimation() == UseAnim.BOW && WorldOptions.clientEnabled(WorldOptions.Feature.FULL_SPEED_BOWS);
    }
}
