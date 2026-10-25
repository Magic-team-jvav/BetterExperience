package com.github.edg_thexu.better_experience.module.spawning;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.networks.c2s.SpawnControlPacket;
import com.github.edg_thexu.better_experience.networks.s2c.SpawnSnapshotPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Better_experience.MODID)
public final class SpawnController {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final double RANGE_SQUARED = 128.0 * 128.0;

    private SpawnController() {}

    private static final class Session {
        ResourceLocation dimension;
        ResourceLocation biome;
        long token;
        long lastQuery = Long.MIN_VALUE;
        long lastDetection = Long.MIN_VALUE;
        final Set<ResourceLocation> denied = new HashSet<>();
        Map<ResourceLocation, SpawnEnvironment.Entry> candidates = Map.of();

        void validate(ServerPlayer player) {
            ResourceLocation currentDimension = player.level().dimension().location();
            ResourceLocation currentBiome = biomeId(player.level().getBiome(player.blockPosition()));
            if (!currentDimension.equals(dimension) || !currentBiome.equals(biome)) {
                dimension = currentDimension;
                biome = currentBiome;
                token = UUID.randomUUID().getMostSignificantBits();
                denied.clear();
                candidates = Map.of();
                lastDetection = Long.MIN_VALUE;
            }
        }
    }

    private static ResourceLocation biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location()).orElse(SpawnControlPacket.NONE);
    }

    public static boolean canEdit(ServerPlayer player) {
        return player.hasPermissions(2) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    public static void handle(ServerPlayer player, SpawnControlPacket packet) {
        if (packet.action() < SpawnControlPacket.QUERY || packet.action() > SpawnControlPacket.RESET_FILTER) return;
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new Session());
        session.validate(player);
        long now = player.server.overworld().getGameTime();
        // Bound expensive environment queries without dropping edits or rapid checkbox clicks.
        if (packet.action() == SpawnControlPacket.QUERY && session.lastQuery != Long.MIN_VALUE && now - session.lastQuery < 10) return;
        session.lastQuery = now;
        var settings = SpawnSettings.get(player.server);
        if (canEdit(player)) {
            if (packet.action() == SpawnControlPacket.SETTINGS) settings.update(packet.rate(), packet.cap());
            if (packet.token() == session.token) {
                if (packet.action() == SpawnControlPacket.TOGGLE && session.candidates.containsKey(packet.entity())) {
                    if (packet.enabled()) session.denied.remove(packet.entity());
                    else session.denied.add(packet.entity());
                } else if (packet.action() == SpawnControlPacket.RESET_FILTER) {
                    session.denied.clear();
                }
            }
        }
        if (session.lastDetection == Long.MIN_VALUE || now - session.lastDetection >= 40) {
            session.candidates = SpawnEnvironment.detect(player);
            session.lastDetection = now;
        }
        List<SpawnSnapshotPacket.Entry> entries = session.candidates.values().stream()
                .limit(SpawnEnvironment.MAX_ENTRIES)
                .map(entry -> new SpawnSnapshotPacket.Entry(entry, !session.denied.contains(entry.id()))).toList();
        PacketDistributor.sendToPlayer(player, new SpawnSnapshotPacket(packet.request(), session.token, canEdit(player),
                settings.rate(), settings.cap(), session.biome, player.getBlockY(),
                player.serverLevel().getMaxLocalRawBrightness(player.blockPosition()), player.level().getDayTime(),
                session.candidates.size() > SpawnEnvironment.MAX_ENTRIES, entries));
    }

    /** A shared spawn belongs to the closest eligible player; overlapping players do not veto each other. */
    private static Session filterAt(ServerLevel level, BlockPos pos, Holder<Biome> biome) {
        ServerPlayer closest = null;
        double distance = RANGE_SQUARED;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) continue;
            double candidate = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (candidate < distance) {
                closest = player;
                distance = candidate;
            }
        }
        if (closest == null || !canEdit(closest)) return null;
        Session session = SESSIONS.get(closest.getUUID());
        if (session == null) return null;
        session.validate(closest);
        return session.biome.equals(biomeId(biome)) && !session.denied.isEmpty() ? session : null;
    }

    private static boolean denied(Session session, EntityType<?> type) {
        return session != null && session.denied.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void potentialSpawns(LevelEvent.PotentialSpawns event) {
        if (SESSIONS.isEmpty() || SpawnEnvironment.detecting() || !(event.getLevel() instanceof ServerLevel level)) return;
        Session session = filterAt(level, event.getPos(), level.getBiome(event.getPos()));
        if (session == null) return;
        for (var data : List.copyOf(event.getSpawnerDataList())) {
            if (denied(session, data.type)) event.removeSpawnerData(data);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (SESSIONS.isEmpty() || event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Mob mob) || mob.getSpawnType() != MobSpawnType.NATURAL) return;
        // Never load/promote chunks inside EntityJoinLevelEvent (can deadlock world loading).
        BlockPos pos = mob.blockPosition();
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;
        var biome = chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
        if (denied(filterAt(level, pos, biome), mob.getType())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Session session = SESSIONS.get(player.getUUID());
            if (session != null) session.validate(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SESSIONS.clear();
    }
}
