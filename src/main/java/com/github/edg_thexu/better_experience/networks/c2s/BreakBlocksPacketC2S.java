package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.item.MagicBoomStaff;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.module.boomstaff.ExplodeManager;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffAimRange;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffBlockEligibility;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import org.confluence.mod.util.PlayerUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedList;
import java.util.Queue;

public record BreakBlocksPacketC2S(Vec3 center, int slot) implements CustomPacketPayload {

    private static final StreamCodec<ByteBuf, Vec3> CENTER_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    public static final StreamCodec<ByteBuf, BreakBlocksPacketC2S> STREAM_CODEC = StreamCodec.composite(
                    CENTER_CODEC, BreakBlocksPacketC2S::center,
                    ByteBufCodecs.VAR_INT, BreakBlocksPacketC2S::slot,
                    BreakBlocksPacketC2S::new
            );

    public static final Type<BreakBlocksPacketC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Better_experience.MODID, "break_blocks_packet_c2s"));

    public Type<? extends CustomPacketPayload> type() {
        return TYPE;

    }

    public static void handle(BreakBlocksPacketC2S packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if(CommonConfig.FORBIDDEN_MAGIC_BOOM_STAFF.get()){
                context.player().sendSystemMessage(Component.translatable("better_experience.info.forbidden_magic_boom_staff").withColor(0xbc6538));
                return;
            }
            ItemStack stack = context.player().getMainHandItem();
            if (!(context.player() instanceof ServerPlayer player)
                    || !(stack.getItem() instanceof MagicBoomStaff staff)
                    || player.getCooldowns().isOnCooldown(staff)) return;
            if (packet.slot() < 0 || packet.slot() > 8 || player.getInventory().selected != packet.slot()) return;
            var shape = stack.get(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
            if (shape == null || !shape.fits(staff.maxRange * 2 + 1)) return;
            Vec3 center = packet.center();
            if (!Double.isFinite(center.x) || !Double.isFinite(center.y) || !Double.isFinite(center.z)) return;
            // Movement packets and clicks can observe slightly different player positions.
            // Allow one block of synchronization drift while rejecting distant centers silently.
            var range = StaffAimRange.of(Double.NaN, staff.maxRange * 2);
            if (!range.withinMaximumReach(player.getEyePosition(), center, 1)) return;
            StaffSelectionBounds bounds = shape.centeredAt(center);
            if (!bounds.fits(staff.maxRange * 2 + 1)) return;
            int x1 = bounds.min().getX();
            int y1 = Math.max(bounds.min().getY(), player.level().getMinBuildHeight());
            int z1 = bounds.min().getZ();
            int x2 = bounds.max().getX();
            int y2 = Math.min(bounds.max().getY(), player.level().getMaxBuildHeight() - 1);
            int z2 = bounds.max().getZ();
            int centerX = (x2 + x1) / 2;
            int centerY = (y2 + y1) / 2;
            int centerZ = (z2 + z1) / 2;
            Level level = player.level();
            if (y1 > y2) return;

            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2))) {
                if (!level.hasChunkAt(pos)) return;
            }

            Queue<Tuple<BlockPos, Boolean>> blocks = new LinkedList<>();
            for(int y = y2; y >= y1; y--){
                for(int x = x1; x <= x2; x++){
                    for(int z = z1; z <= z2; z++){
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        var eligibility = StaffBlockEligibility.evaluate(level, pos, state, player.getOffhandItem());
                        if (eligibility == StaffBlockEligibility.Result.SKIP) continue;
                        blocks.add(new Tuple<>(pos, eligibility == StaffBlockEligibility.Result.DROP));
                    }
                }
            }
            if (blocks.isEmpty()) return;
            if (ConfluenceHelper.isLoaded()
                    && !PlayerUtils.extractMana(player, stack, () -> bounds.manaCost())) {
                player.displayClientMessage(Component.translatable("better_experience.staff.no_mana", bounds.manaCost()), true);
                return;
            }
            player.getCooldowns().addCooldown(staff, 20);
            ExplodeManager.BlockQueue queue = new ExplodeManager.BlockQueue(blocks, new Vec3(centerX, centerY, centerZ), player);
            ExplodeManager.getInstance().addBlockToQueue(queue);

        });
    }
}
