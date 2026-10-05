package com.github.edg_thexu.better_experience.module.autopotion;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.networks.s2c.SyncDataS2C;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class ForbiddenConfig extends SimplePreparableReloadListener<ForbiddenConfig> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private record Snapshot(Set<Item> items, Map<MobEffect, Integer> amplifiers,
                            List<EffectAmp> effects, Set<String> modIds) {}
    private volatile Snapshot snapshot;

    record EffectAmp(MobEffect effect, int amp) {
        public static final Codec<EffectAmp> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(EffectAmp::effect),
                Codec.INT.optionalFieldOf("amp").forGetter(i->Optional.of(i.amp))
        ).apply(instance, (effect, amp)-> new EffectAmp(effect, amp.orElse(0))));
    }

    private static final class Holder {
        private static final ForbiddenConfig INSTANCE = new ForbiddenConfig(Set.of(), List.of(), Set.of());
    }

    public static ForbiddenConfig getInstance() {
        return Holder.INSTANCE;
    }

    public boolean isItemForbidden(Item item) {
        return snapshot.items.contains(item);
    }

    public boolean isEffectForbidden(MobEffect effect, int amp) {
        Integer threshold = snapshot.amplifiers.get(effect);
        return threshold != null && threshold <= amp;
    }

    public boolean isModForbidden(String modId) {
        return snapshot.modIds.contains(modId);
    }

    ForbiddenConfig(Set<Item> items, List<EffectAmp> effects, Set<String> modIds) {
        Map<MobEffect, Integer> amplifiers = new HashMap<>();
        for (EffectAmp effect : effects) {
            amplifiers.merge(effect.effect, effect.amp, Math::min);
        }
        snapshot = new Snapshot(Set.copyOf(items), Map.copyOf(amplifiers),
                List.copyOf(effects), Set.copyOf(modIds));
    }

    public static final MapCodec<ForbiddenConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("forbidden_items").forGetter(i->i.snapshot.items.stream().toList()),
            EffectAmp.CODEC.listOf().fieldOf("forbidden_effects").forGetter(i->i.snapshot.effects),
            Codec.STRING.listOf().optionalFieldOf("forbidden_modid_effect").forGetter(i->Optional.of(i.snapshot.modIds.stream().toList()))
    ).apply(instance, (item, effect, modId)-> modId
            .map(strings -> new ForbiddenConfig(new HashSet<>(item), effect, new HashSet<>(strings)))
            .orElseGet(() -> new ForbiddenConfig(new HashSet<>(item), effect, new HashSet<>()))));


    @Override
    protected ForbiddenConfig prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        ResourceLocation location = Better_experience.space("potion_config.json");
        Optional<Resource> file = resourceManager.getResource(location);
        if(file.isPresent()){
            try (Reader reader = file.get().openAsReader()) {
                JsonObject jsonobject = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                return CODEC.codec().decode(JsonOps.INSTANCE, jsonobject).getOrThrow().getFirst();
            } catch (RuntimeException | IOException ioexception) {
                Better_experience.LOGGER.error("Failed to load potion config {}", location, ioexception);
            }
        }
        return new ForbiddenConfig(Set.of(), List.of(), Set.of());
    }

    @Override
    protected void apply(ForbiddenConfig config, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        snapshot = config.snapshot;
        Better_experience.LOGGER.info("ForbiddenConfig reloaded");
        if(ServerLifecycleHooks.getCurrentServer() != null) {
            SyncDataS2C.syncForbiddenConfig();
        }
    }

    public static void handleServer(ForbiddenConfig config){
        getInstance().snapshot = config.snapshot;
    }

    public static void sync(ServerPlayer player){
        SyncDataS2C.syncForbiddenConfig(player);
    }

}
