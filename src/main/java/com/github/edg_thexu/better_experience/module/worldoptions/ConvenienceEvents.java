package com.github.edg_thexu.better_experience.module.worldoptions;

import com.github.edg_thexu.better_experience.Better_experience;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

@EventBusSubscriber(modid = Better_experience.MODID)
public final class ConvenienceEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void target(LivingChangeTargetEvent event) {
        var target = event.getNewAboutToBeSetTarget();
        if (target != null && !event.getEntity().level().isClientSide && ConvenienceRules.blocksAttack(event.getEntity(), target)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && ConvenienceRules.blocksAttack(attacker, event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void protectPet(LivingDamageEvent.Pre event) {
        var pet = event.getEntity();
        if (pet.level().isClientSide || !ConvenienceRules.isPet(pet)
                || !WorldOptions.enabled(pet.level(), WorldOptions.Feature.PROTECTED_PETS)) return;
        float limit = Math.max(0.0F, pet.getHealth() - 1.0F);
        if (event.getNewDamage() >= limit) {
            event.setNewDamage(limit);
            ConvenienceRules.forgetCombat((Mob) pet);
            if (event.getSource().getEntity() instanceof Mob attacker) ConvenienceRules.forgetCombat(attacker);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void protectLastHealth(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity living && !living.level().isClientSide
                && ConvenienceRules.downedPet(living)) event.setInvulnerable(true);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        if (ConvenienceRules.downedPet(mob)) {
            ConvenienceRules.forgetCombat(mob);
            return;
        }
        var target = mob.getTarget();
        if (target != null && ConvenienceRules.blocksAttack(mob, target)) ConvenienceRules.forgetCombat(mob);
        var brain = mob.getBrain();
        var attackMemory = net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET;
        if (brain.hasMemoryValue(attackMemory) && ConvenienceRules.blocksAttack(mob, brain.getMemory(attackMemory).orElseThrow())) {
            ConvenienceRules.forgetCombat(mob);
        }
        var angerMemory = net.minecraft.world.entity.ai.memory.MemoryModuleType.ANGRY_AT;
        if (ConvenienceRules.neutral(mob) && WorldOptions.enabled(level, WorldOptions.Feature.FRIENDLY_NEUTRALS)
                && brain.hasMemoryValue(angerMemory) && level.getPlayerByUUID(brain.getMemory(angerMemory).orElseThrow()) != null) {
            ConvenienceRules.forgetCombat(mob);
        }
        if (mob instanceof NeutralMob neutral && ConvenienceRules.neutral(mob)
                && WorldOptions.enabled(level, WorldOptions.Feature.FRIENDLY_NEUTRALS)
                && (neutral.isAngryAtAllPlayers(level) || neutral.getPersistentAngerTarget() != null
                && level.getPlayerByUUID(neutral.getPersistentAngerTarget()) != null)) ConvenienceRules.forgetCombat(mob);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void generatedEquipment(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof Mob mob) || event.getLevel().isClientSide()
                || !WorldOptions.enabled(mob.level(), WorldOptions.Feature.NO_CURSED_LOOT)) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) ConvenienceRules.stripCurses(mob.getItemBySlot(slot));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void iron(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof IronGolem) || event.getEntity().level().isClientSide
                || !WorldOptions.enabled(event.getEntity().level(), WorldOptions.Feature.EXTRA_GOLEM_IRON)) return;
        for (ItemEntity drop : List.copyOf(event.getDrops())) {
            if (!drop.getItem().is(Items.IRON_INGOT)) continue;
            var extra = new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
            extra.setDeltaMovement(drop.getDeltaMovement());
            event.getDrops().add(extra);
        }
    }
}
