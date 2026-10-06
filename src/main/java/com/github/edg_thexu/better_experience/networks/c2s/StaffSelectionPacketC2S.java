package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.SelectionStaff;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record StaffSelectionPacketC2S(int slot, Optional<StaffSelectionShape> shape) implements CustomPacketPayload {
    public static final Type<StaffSelectionPacketC2S> TYPE = new Type<>(Better_experience.space("staff_selection"));
    public static final StreamCodec<ByteBuf, StaffSelectionPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StaffSelectionPacketC2S::slot,
            ByteBufCodecs.optional(StaffSelectionShape.STREAM_CODEC), StaffSelectionPacketC2S::shape,
            StaffSelectionPacketC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(StaffSelectionPacketC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (packet.slot < 0 || packet.slot > 8 || player.getInventory().selected != packet.slot) return;
            var stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof SelectionStaff staff)) return;
            if (packet.shape.isPresent()) {
                StaffSelectionShape shape = packet.shape.get();
                if (!shape.fits(staff.maxRange * 2 + 1)) return;
                stack.set(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get(), shape);
            } else stack.remove(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
            player.containerMenu.broadcastChanges();
        });
    }
}
