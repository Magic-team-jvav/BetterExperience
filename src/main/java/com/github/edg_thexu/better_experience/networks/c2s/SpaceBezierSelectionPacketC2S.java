package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.SpaceBezierShape;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.module.spacestaff.SpaceShape;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Lock the box and its exact point polygon in one server operation. */
public record SpaceBezierSelectionPacketC2S(int slot, SpaceBezierShape curve) implements CustomPacketPayload {
    public static final Type<SpaceBezierSelectionPacketC2S> TYPE = new Type<>(Better_experience.space("space_bezier_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceBezierSelectionPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpaceBezierSelectionPacketC2S::slot,
            SpaceBezierShape.STREAM_CODEC, SpaceBezierSelectionPacketC2S::curve, SpaceBezierSelectionPacketC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SpaceBezierSelectionPacketC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (packet.slot < 0 || packet.slot > 8 || player.getInventory().selected != packet.slot
                    || !(player.getMainHandItem().getItem() instanceof SpaceStaff)) return;
            var stack = player.getMainHandItem();
            var settings = stack.get(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get());
            if (settings == null || settings.shape() != SpaceShape.BEZIER) return;
            stack.set(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get(), new StaffSelectionShape(packet.curve.span()));
            stack.set(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get(), settings.withCurve(packet.curve));
            player.containerMenu.broadcastChanges();
        });
    }
}
