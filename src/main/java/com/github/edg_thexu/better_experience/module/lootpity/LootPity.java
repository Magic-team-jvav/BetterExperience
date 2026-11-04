package com.github.edg_thexu.better_experience.module.lootpity;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.init.ModAttachments;
import com.github.edg_thexu.better_experience.mixin.accessor.PityLootItemAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LootPity {
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();
    private static final TagKey<Item> INCLUDED = TagKey.create(net.minecraft.core.registries.Registries.ITEM, Better_experience.space("pity_drops"));
    private static final TagKey<Item> EXCLUDED = TagKey.create(net.minecraft.core.registries.Registries.ITEM, Better_experience.space("pity_excluded"));

    private LootPity() {}

    public static Attempt begin(LootTable table, LootContext context) {
        if (CURRENT.get() != null || !CommonConfig.RARE_DROP_PITY.get()) return null;
        ResourceLocation tableId = table.getLootTableId();
        if (tableId == null) return null;
        Entity subject = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        ServerPlayer player;
        String source = tableId.toString();
        if (table.getParamSet() == LootContextParamSets.GIFT && subject instanceof ServerPlayer opener) {
            player = opener;
        } else if (table.getParamSet() == LootContextParamSets.ENTITY && subject instanceof Mob mob
                && (mob instanceof Enemy || mob.getType().getCategory() == MobCategory.MONSTER)) {
            Entity killer = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
            if (killer == null) killer = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
            if (killer instanceof OwnableEntity owned) killer = owned.getOwner();
            if (!(killer instanceof ServerPlayer owner)) return null;
            player = owner;
            source += "|" + BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        } else {
            return null;
        }
        Attempt attempt = new Attempt(player.getData(ModAttachments.LOOT_PITY), source, context);
        CURRENT.set(attempt);
        return attempt;
    }

    public static Attempt current(LootContext context) {
        Attempt attempt = CURRENT.get();
        return attempt != null && attempt.context.getLevel() == context.getLevel()
                && attempt.context.getParamOrNull(LootContextParams.THIS_ENTITY) == context.getParamOrNull(LootContextParams.THIS_ENTITY)
                ? attempt : null;
    }

    public static void end(Attempt attempt, List<ItemStack> result) {
        try {
            if (result != null) {
                Set<String> dropped = new HashSet<>();
                for (ItemStack stack : result) {
                    if (!stack.isEmpty()) dropped.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                }
                attempt.state.finish(attempt.source, attempt.scores, dropped);
            }
        } finally {
            CURRENT.remove();
        }
    }

    public static Item target(LootPoolEntryContainer entry) {
        if (!(entry instanceof LootItem)) return null;
        Item item = ((PityLootItemAccess) entry).betterExperience$item().value();
        return rare(item) ? item : null;
    }

    public static boolean rare(Item item) {
        ItemStack stack = item.getDefaultInstance();
        if (stack.is(EXCLUDED)) return false;
        if (stack.is(INCLUDED)) return true;
        return stack.getMaxStackSize() == 1 && !(item instanceof BlockItem || item instanceof BucketItem
                || item instanceof PotionItem || item instanceof SpawnEggItem);
    }

    public static final class Attempt {
        private final LootPityState state;
        private final String source;
        private final LootContext context;
        private final Map<String, Double> scores = new HashMap<>();

        private Attempt(LootPityState state, String source, LootContext context) {
            this.state = state;
            this.source = source;
            this.context = context;
        }

        public double score(Item item, double base) {
            if (base <= 0) return 0;
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            // Multiple pools, nested tables and bonus rolls count once for this kill/bag.
            return scores.computeIfAbsent(id, key -> state.chance(source, key, base));
        }
    }
}
