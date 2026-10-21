package com.github.edg_thexu.better_experience.module.locator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Start chunk identifies the whole structure, independently of the player's position or structure pieces. */
public record StructureMarker(ResourceLocation dimension, ResourceLocation structure, long startChunk, BlockPos position) {
    public static final Codec<StructureMarker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(StructureMarker::dimension),
            ResourceLocation.CODEC.fieldOf("structure").forGetter(StructureMarker::structure),
            Codec.LONG.fieldOf("start_chunk").forGetter(StructureMarker::startChunk),
            BlockPos.CODEC.fieldOf("position").forGetter(StructureMarker::position)
    ).apply(instance, StructureMarker::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureMarker> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public StructureMarker { position = position.immutable(); }

    public ResourceLocation waypointId() {
        return ResourceLocation.fromNamespaceAndPath("better_experience", "locator/" + dimension.toString().length() + "/"
                + structure.toString().length() + "/" + dimension.getNamespace() + "/"
                + dimension.getPath() + "/" + structure.getNamespace() + "/" + structure.getPath() + "/" + Long.toHexString(startChunk));
    }
}
