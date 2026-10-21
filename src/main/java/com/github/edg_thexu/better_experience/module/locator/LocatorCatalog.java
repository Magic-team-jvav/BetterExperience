package com.github.edg_thexu.better_experience.module.locator;

import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.networks.s2c.LocatorCatalogPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;

public final class LocatorCatalog {
    public record Entry(ResourceLocation id, boolean available, int marked) {}
    public static final int MAX_ENTRIES = 8192;
    private LocatorCatalog() {}

    public static void open(ServerPlayer player) {
        var registry = player.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var generation = player.serverLevel().getChunkSource().getGeneratorState();
        var marked = new HashMap<ResourceLocation, Integer>();
        var dimension = player.level().dimension().location();
        for (var marker : LocatorHistory.get(player).markers(player.getUUID())) {
            if (marker.dimension().equals(dimension)) marked.merge(marker.structure(), 1, Integer::sum);
        }
        var entries = registry.holders().sorted(Comparator.comparing(holder -> holder.key().location().toString()))
                .limit(MAX_ENTRIES).map(holder -> new Entry(holder.key().location(),
                        !generation.getPlacementsForStructure(holder).isEmpty(), marked.getOrDefault(holder.key().location(), 0))).toList();
        PacketDistributor.sendToPlayer(player, new LocatorCatalogPacket(player.getInventory().selected, entries,
                Optional.ofNullable(player.getMainHandItem().get(ModDataComponentTypes.LOCATOR_STRUCTURE.get()))));
    }
}
