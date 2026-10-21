package com.github.edg_thexu.better_experience.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** The current slot and the number of successful shots already fired from it. */
public record AmmoChainCursor(int index, int fired) {
    public static final AmmoChainCursor DEFAULT = new AmmoChainCursor(0, 0);
    public static final Codec<AmmoChainCursor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, AmmoChainSettings.MAX_SLOTS - 1).fieldOf("index").forGetter(AmmoChainCursor::index),
            Codec.intRange(0, AmmoChainSettings.MAX_SHOTS - 1).fieldOf("fired").forGetter(AmmoChainCursor::fired)
    ).apply(instance, AmmoChainCursor::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AmmoChainCursor> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AmmoChainCursor decode(RegistryFriendlyByteBuf buffer) {
            int index = buffer.readVarInt();
            int fired = buffer.readVarInt();
            if (!valid(index, fired)) throw new DecoderException("Invalid ammunition chain cursor");
            return new AmmoChainCursor(index, fired);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AmmoChainCursor value) {
            buffer.writeVarInt(value.index);
            buffer.writeVarInt(value.fired);
        }
    };

    public AmmoChainCursor {
        if (!valid(index, fired)) throw new IllegalArgumentException("Invalid ammunition chain cursor");
    }

    private static boolean valid(int index, int fired) {
        return index >= 0 && index < AmmoChainSettings.MAX_SLOTS && fired >= 0 && fired < AmmoChainSettings.MAX_SHOTS;
    }
}
