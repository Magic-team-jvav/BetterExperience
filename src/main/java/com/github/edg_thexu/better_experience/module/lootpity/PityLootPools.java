package com.github.edg_thexu.better_experience.module.lootpity;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.mixin.accessor.PityLootEntryAccess;
import com.github.edg_thexu.better_experience.mixin.accessor.PityLootPoolAccess;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Supports ordinary rare-item/empty weighted pools while retaining their functions and roll counts. */
public final class PityLootPools {
    private static final Map<LootPool, Optional<Plan>> PLANS = Collections.synchronizedMap(new WeakHashMap<>());

    private PityLootPools() {}

    public static boolean generate(LootPool pool, Consumer<ItemStack> output, LootContext context) {
        LootPity.Attempt attempt = LootPity.current(context);
        if (attempt == null) return false;
        Optional<Plan> plan = PLANS.computeIfAbsent(pool, key -> compile(key, context));
        if (plan.isEmpty()) return false;
        plan.get().generate(pool, output, context, attempt);
        return true;
    }

    private static Optional<Plan> compile(LootPool pool, LootContext context) {
        List<LootPoolEntryContainer> rawEntries = ((PityLootPoolAccess) pool).betterExperience$entries();
        if (rawEntries.stream().noneMatch(entry -> LootPity.target(entry) != null)) return Optional.empty();
        for (LootPoolEntryContainer entry : rawEntries) {
            if (!(entry instanceof LootItem || entry instanceof EmptyLootItem)) return Optional.empty();
            // Per-entry chance gates in a multi-entry pool change which weights participate.
            // Leave such custom combinations to vanilla rather than inventing a marginal chance.
            if (rawEntries.size() > 1 && conditions(entry).stream().anyMatch(PityChance::supported)) return Optional.empty();
        }
        List<LootItemCondition> poolConditions = ((PityLootPoolAccess) pool).betterExperience$conditions();
        if (poolConditions.stream().anyMatch(PityChance::supported)
                && (!(pool.getRolls() instanceof net.minecraft.world.level.storage.loot.providers.number.ConstantValue rolls) || rolls.value() != 1
                || !(pool.getBonusRolls() instanceof net.minecraft.world.level.storage.loot.providers.number.ConstantValue bonus) || bonus.value() != 0)) {
            return Optional.empty();
        }
        try {
            List<Candidate> candidates = new ArrayList<>();
            var ops = RegistryOps.create(JsonOps.INSTANCE, context.getLevel().registryAccess());
            for (LootPoolEntryContainer entry : rawEntries) {
                List<LootItemCondition> chances = conditions(entry).stream().filter(PityChance::supported).toList();
                LootPoolEntryContainer filtered = entry;
                if (!chances.isEmpty()) {
                    JsonObject json = LootPoolEntries.CODEC.encodeStart(ops, entry).getOrThrow().getAsJsonObject();
                    JsonArray remaining = new JsonArray();
                    for (LootItemCondition condition : conditions(entry)) {
                        if (!PityChance.supported(condition)) remaining.add(LootItemCondition.DIRECT_CODEC.encodeStart(ops, condition).getOrThrow());
                    }
                    json.add("conditions", remaining);
                    filtered = LootPoolEntries.CODEC.parse(ops, json).getOrThrow();
                }
                candidates.add(new Candidate(filtered, LootPity.target(entry), chances));
            }
            PityLootPoolAccess access = (PityLootPoolAccess) pool;
            return Optional.of(new Plan(candidates, access.betterExperience$conditions(), access.betterExperience$functions()));
        } catch (RuntimeException error) {
            Better_experience.LOGGER.warn("Leaving unsupported rare loot pool {} unchanged", pool.getName(), error);
            return Optional.empty();
        }
    }

    private static List<LootItemCondition> conditions(LootPoolEntryContainer entry) {
        return ((PityLootEntryAccess) entry).betterExperience$conditions();
    }

    private record Candidate(LootPoolEntryContainer entry, Item item, List<LootItemCondition> chances) {}
    private record Expanded(LootPoolEntry entry, Item item, double chance, int weight, boolean empty) {}

    private record Plan(List<Candidate> candidates, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        private void generate(LootPool pool, Consumer<ItemStack> output, LootContext context, LootPity.Attempt attempt) {
            for (LootItemCondition condition : conditions) {
                if (!PityChance.supported(condition) && !condition.test(context)) return;
            }
            double gate = 1;
            for (LootItemCondition condition : conditions) {
                if (PityChance.supported(condition)) gate *= PityChance.probability(condition, context);
            }
            if (gate <= 0) return;
            Consumer<ItemStack> decorated = LootItemFunction.decorate(LootItemFunctions.compose(functions), output, context);
            int rolls = pool.getRolls().getInt(context)
                    + net.minecraft.util.Mth.floor(pool.getBonusRolls().getFloat(context) * context.getLuck());
            for (int roll = 0; roll < rolls; roll++) {
                List<Expanded> entries = new ArrayList<>();
                for (Candidate candidate : candidates) {
                    double chance = 1;
                    for (LootItemCondition condition : candidate.chances) chance *= PityChance.probability(condition, context);
                    double entryChance = chance;
                    candidate.entry.expand(context, entry -> {
                        int weight = entry.getWeight(context.getLuck());
                        if (weight > 0) entries.add(new Expanded(entry, candidate.item, entryChance, weight, candidate.entry instanceof EmptyLootItem));
                    });
                }
                double total = entries.stream().mapToDouble(Expanded::weight).sum();
                if (total <= 0) continue;
                double[] scores = new double[entries.size()];
                for (int i = 0; i < scores.length; i++) {
                    Expanded entry = entries.get(i);
                    double base = gate * entry.chance * entry.weight / total;
                    if (!entry.empty) scores[i] = entry.item == null ? base : attempt.score(entry.item, base);
                }
                double[] probabilities = PityRolls.probabilities(scores, true);
                double random = context.getRandom().nextDouble();
                for (int i = 0; i < probabilities.length; i++) {
                    random -= probabilities[i];
                    if (random < 0) {
                        entries.get(i).entry.createItemStack(decorated, context);
                        break;
                    }
                }
            }
        }
    }
}
