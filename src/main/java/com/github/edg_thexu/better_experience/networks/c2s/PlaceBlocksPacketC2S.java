package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffAimRange;
import com.github.edg_thexu.better_experience.module.spacestaff.SpaceGeometry;
import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementManager;
import com.github.edg_thexu.better_experience.module.spacestaff.SpacePlacementRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client sends only the rendered center. Settings and shape come from the server-held staff. */
public record PlaceBlocksPacketC2S(Vec3 center, int slot) implements CustomPacketPayload {
    public static final Type<PlaceBlocksPacketC2S> TYPE = new Type<>(Better_experience.space("space_staff_place"));
    public static final StreamCodec<FriendlyByteBuf, PlaceBlocksPacketC2S> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeVec3(packet.center); buffer.writeVarInt(packet.slot); },
            buffer -> new PlaceBlocksPacketC2S(buffer.readVec3(), buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PlaceBlocksPacketC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.mayBuild() || player.isSpectator()
                    || packet.slot < 0 || packet.slot > 8 || player.getInventory().selected != packet.slot
                    || !(player.getMainHandItem().getItem() instanceof SpaceStaff staff)
                    || player.getCooldowns().isOnCooldown(staff)) return;
            Vec3 center = packet.center;
            if (!Double.isFinite(center.x) || !Double.isFinite(center.y) || !Double.isFinite(center.z)
                    || !StaffAimRange.of(Double.NaN, staff.maxRange * 2).withinMaximumReach(player.getEyePosition(), center, 1)) return;
            var stack = player.getMainHandItem();
            var selection = stack.get(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
            var settings = stack.get(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get());
            if (selection == null || settings == null || !selection.fits(21)
                    || !SpacePlacementRules.supported(settings.state())) return;
            var positions = SpaceGeometry.positions(selection.centeredAt(center), selection, settings);
            if (positions.stream().anyMatch(pos -> !player.serverLevel().hasChunkAt(pos))) return;
            if (SpacePlacementManager.getInstance().enqueue(player, positions, settings)) {
                player.getCooldowns().addCooldown(staff, 20);
            }
        });
    }
}
