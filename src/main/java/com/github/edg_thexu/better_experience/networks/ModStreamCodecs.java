package com.github.edg_thexu.better_experience.networks;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Block state codec kept in-house so Confluence Lib is not needed to register the payloads. */
public final class ModStreamCodecs {

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE = new StreamCodec<>() {
        private final StreamCodec<RegistryFriendlyByteBuf, Block> blockCodec = ByteBufCodecs.registry(Registries.BLOCK);

        @Override
        public BlockState decode(RegistryFriendlyByteBuf buffer) {
            Block block = blockCodec.decode(buffer);
            return block.getStateDefinition().getPossibleStates().get(buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BlockState value) {
            Block block = value.getBlock();
            blockCodec.encode(buffer, block);
            buffer.writeVarInt(block.getStateDefinition().getPossibleStates().indexOf(value));
        }
    };

    private ModStreamCodecs() {}
}
