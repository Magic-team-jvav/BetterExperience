package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.StructureLocatorOrb;
import com.github.edg_thexu.better_experience.module.locator.StructureLocatorManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LocatorTargetPacketC2S(int slot, ResourceLocation target, boolean search) implements CustomPacketPayload {
    public static final Type<LocatorTargetPacketC2S> TYPE = new Type<>(Better_experience.space("locator_target"));
    public static final StreamCodec<FriendlyByteBuf, LocatorTargetPacketC2S> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.slot); buffer.writeResourceLocation(packet.target); buffer.writeBoolean(packet.search);
    }, buffer -> new LocatorTargetPacketC2S(buffer.readVarInt(), buffer.readResourceLocation(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(LocatorTargetPacketC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()
                    || packet.slot < 0 || packet.slot > 8 || player.getInventory().selected != packet.slot
                    || !(player.getMainHandItem().getItem() instanceof StructureLocatorOrb)
                    || !player.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(packet.target)) return;
            StructureLocatorManager.getInstance().cancel(player.getUUID());
            var stack = player.getMainHandItem();
            if (!packet.target.equals(stack.get(ModDataComponentTypes.LOCATOR_STRUCTURE.get()))) {
                stack.remove(ModDataComponentTypes.LOCATOR_MARKER.get());
            }
            stack.set(ModDataComponentTypes.LOCATOR_STRUCTURE.get(), packet.target);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            if (packet.search) StructureLocatorManager.getInstance().start(player, packet.target);
        });
    }
}
