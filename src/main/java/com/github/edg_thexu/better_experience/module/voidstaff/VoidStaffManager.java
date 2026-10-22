package com.github.edg_thexu.better_experience.module.voidstaff;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.item.VoidStaff;
import com.github.edg_thexu.better_experience.networks.s2c.VoidCargoPacket;
import com.github.edg_thexu.better_experience.networks.s2c.VoidStaffRejectedPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.confluence.lib.util.LibUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** All ownership, protection checks and container transfers run on the server thread. */
public final class VoidStaffManager {
    private VoidStaffManager() {}

    private static boolean holding(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof VoidStaff && player.mayBuild() && !player.isSpectator();
    }

    public static void synchronize(ServerPlayer player) {
        var cargo = VoidCargoStorage.get(player).cargo(player.getUUID());
        var preview = cargo == null ? Optional.<VoidCargo>empty() : Optional.of(new VoidCargo(cargo.token(),
                cargo.parts().stream().map(part -> new VoidCargo.Part(part.offset(), part.state(), new CompoundTag(), part.display())).toList(), cargo.state()));
        PacketDistributor.sendToPlayer(player, new VoidCargoPacket(player.level().dimension().location(), preview));
    }

    public static void pickup(ServerPlayer player, BlockPos origin) {
        if (!holding(player) || !player.isShiftKeyDown() || !VoidPlacementRules.withinReach(player, origin)) return;
        var storage = VoidCargoStorage.get(player);
        if (storage.cargo(player.getUUID()) != null) { message(player, "carrying"); synchronize(player); return; }
        var level = player.serverLevel();
        if (!level.hasChunkAt(origin)) return;
        var first = level.getBlockState(origin);
        var positions = new ArrayList<BlockPos>();
        positions.add(origin.immutable());
        if (first.getBlock() instanceof ChestBlock && first.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos second = origin.relative(ChestBlock.getConnectedDirection(first));
            if (!level.hasChunkAt(second)) { message(player, "failed"); return; }
            var other = level.getBlockState(second);
            if (other.getBlock() != first.getBlock() || other.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                    || !second.relative(ChestBlock.getConnectedDirection(other)).equals(origin)) { message(player, "failed"); return; }
            positions.add(second);
        }
        try {
            var entities = new ArrayList<BlockEntity>();
            var states = new ArrayList<BlockState>();
            var handlers = new ArrayList<IItemHandler>();
            for (var pos : positions) {
                var state = level.getBlockState(pos);
                var entity = level.getBlockEntity(pos);
                boolean container = entity instanceof Container || entity instanceof MenuProvider || entity instanceof EnderChestBlockEntity || entity != null && (
                        level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null
                                || level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null);
                if (!container || !level.mayInteract(player, pos) || state.getDestroySpeed(level, pos) < 0
                        || entity instanceof BaseContainerBlockEntity base && !base.canOpen(player)
                        || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) {
                    message(player, "failed"); return;
                }
                entities.add(entity);
                states.add(state);
                var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                if (inventory != null) handlers.add(inventory);
                for (Direction side : LibUtils.DIRECTIONS) {
                    var sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
                    if (sided != null && !handlers.contains(sided)) handlers.add(sided);
                }
            }
            // Return carried cursor items before serialization, and invalidate live container views.
            for (var viewer : player.server.getPlayerList().getPlayers()) {
                if (viewer.containerMenu != viewer.inventoryMenu && viewer.containerMenu.slots.stream().anyMatch(slot ->
                        entities.stream().anyMatch(entity -> slot.container == entity
                                || entity instanceof Container container && slot.container instanceof CompoundContainer combined && combined.contains(container))
                                || slot instanceof SlotItemHandler itemSlot && handlers.contains(itemSlot.getItemHandler()))) {
                    viewer.closeContainer();
                }
            }
            var parts = new ArrayList<VoidCargo.Part>();
            for (int i = 0; i < positions.size(); i++) {
                var pos = positions.get(i);
                var entity = entities.get(i);
                if (level.getBlockEntity(pos) != entity || level.getBlockState(pos) != states.get(i)) { message(player, "failed"); return; }
                var data = entity.saveWithFullMetadata(level.registryAccess());
                // Verify that the full serialized type can be reconstructed before removing the original.
                var copy = BlockEntity.loadStatic(pos, states.get(i), data.copy(), level.registryAccess());
                if (copy == null || !copy.isValidBlockState(states.get(i))) { message(player, "failed"); return; }
                var display = entity.getUpdateTag(level.registryAccess());
                if (display.sizeInBytes() > 16_384) display = new CompoundTag();
                parts.add(new VoidCargo.Part(pos.subtract(origin), states.get(i), data, display));
            }
            var cargo = new VoidCargo(UUID.randomUUID(), parts, first);
            if (player.serverLevel() != level || !holding(player)) { message(player, "failed"); return; }
            var replacements = positions.stream().map(pos -> level.getFluidState(pos).createLegacyBlock()).toList();
            var snapshots = transfer(player, positions, replacements, List.of(), true);
            if (snapshots == null) { message(player, "failed"); return; }
            storage.put(player.getUUID(), cargo);
            finish(player, snapshots);
            synchronize(player);
            message(player, "picked_up");
        } catch (RuntimeException exception) {
            Better_experience.LOGGER.error("Cannot lift container at {}", origin, exception);
            message(player, "failed");
        }
    }

    public static void changeState(ServerPlayer player, int slot, UUID token, BlockState state) {
        var cargo = owned(player, slot, token);
        if (cargo == null || !VoidPlacementRules.validState(cargo.parts().getFirst().state(), state)) return;
        VoidCargoStorage.get(player).put(player.getUUID(), cargo.withState(state));
        synchronize(player);
    }

    private static VoidCargo owned(ServerPlayer player, int slot, UUID token) {
        if (!holding(player) || player.getInventory().selected != slot) return null;
        var cargo = VoidCargoStorage.get(player).cargo(player.getUUID());
        return cargo != null && cargo.token().equals(token) ? cargo : null;
    }

    public static void place(ServerPlayer player, int slot, UUID token, BlockPos origin, BlockState state) {
        var cargo = owned(player, slot, token);
        if (cargo == null || !player.isShiftKeyDown() || !VoidPlacementRules.withinReach(player, origin)
                || !VoidPlacementRules.validState(cargo.parts().getFirst().state(), state)) return;
        var level = player.serverLevel();
        try {
            var positions = new ArrayList<BlockPos>();
            var states = new ArrayList<BlockState>();
            var entities = new ArrayList<BlockEntity>();
            var primary = cargo.parts().getFirst().state();
            var rotation = VoidPlacementRules.rotation(primary, state);
            for (var part : cargo.parts()) {
                var pos = origin.offset(part.offset().rotate(rotation));
                var desired = part.offset().equals(BlockPos.ZERO) ? state : VoidPlacementRules.partState(part, primary, state);
                if (!VoidPlacementRules.canPlace(level, pos, desired) || !level.mayInteract(player, pos)) { reject(player, cargo, origin); return; }
                var data = part.data();
                data.putInt("x", pos.getX()); data.putInt("y", pos.getY()); data.putInt("z", pos.getZ());
                var entity = BlockEntity.loadStatic(pos, desired, data, level.registryAccess());
                if (entity == null || !entity.isValidBlockState(desired)) { message(player, "failed"); return; }
                positions.add(pos); states.add(desired); entities.add(entity);
            }
            if (player.serverLevel() != level) return;
            var snapshots = transfer(player, positions, states, entities, false);
            if (snapshots == null) { reject(player, cargo, origin); return; }
            VoidCargoStorage.get(player).remove(player.getUUID());
            finish(player, snapshots);
            synchronize(player);
            message(player, "placed");
        } catch (RuntimeException exception) {
            Better_experience.LOGGER.error("Cannot place carried container at {}", origin, exception);
            message(player, "failed");
        }
    }

    /** Notifications and drops are suppressed until the complete transfer is accepted. */
    private static List<BlockSnapshot> transfer(ServerPlayer player, List<BlockPos> positions, List<BlockState> states,
                                               List<BlockEntity> entities, boolean lifting) {
        var level = player.serverLevel();
        if (level.captureBlockSnapshots || level.restoringBlockSnapshots) return null;
        var originals = positions.stream().map(pos -> BlockSnapshot.create(level.dimension(), level, pos, Block.UPDATE_ALL)).toList();
        var captured = new ArrayList<BlockSnapshot>();
        int firstSnapshot = level.capturedBlockSnapshots.size();
        boolean accepted = false;
        try {
            level.captureBlockSnapshots = true;
            level.restoringBlockSnapshots = true;
            if (lifting) positions.forEach(level::removeBlockEntity);
            for (int i = 0; i < positions.size(); i++) {
                if (!level.setBlock(positions.get(i), states.get(i), Block.UPDATE_ALL)) return null;
            }
            level.restoringBlockSnapshots = false;
            if (!lifting) {
                boolean canceled = originals.size() > 1 ? EventHooks.onMultiBlockPlace(player, originals, Direction.UP)
                        : EventHooks.onBlockPlace(player, originals.getFirst(), Direction.UP);
                if (canceled) return null;
            }
            if (!holding(player) || player.serverLevel() != level
                    || !lifting && !VoidPlacementRules.withinReach(player, positions.getFirst())) return null;
            for (int i = 0; i < positions.size(); i++) {
                if (level.getBlockState(positions.get(i)) != states.get(i) || !level.mayInteract(player, positions.get(i))) return null;
            }
            if (level.capturedBlockSnapshots.subList(firstSnapshot, level.capturedBlockSnapshots.size()).stream()
                    .anyMatch(snapshot -> !positions.contains(snapshot.getPos()))) return null;
            if (!lifting) for (int i = 0; i < entities.size(); i++) {
                level.removeBlockEntity(positions.get(i));
                level.setBlockEntity(entities.get(i));
                if (level.getBlockEntity(positions.get(i)) != entities.get(i)) return null;
                entities.get(i).setChanged();
            }
            accepted = true;
            return originals;
        } finally {
            level.captureBlockSnapshots = false;
            var pending = level.capturedBlockSnapshots.subList(firstSnapshot, level.capturedBlockSnapshots.size());
            captured.addAll(pending); pending.clear();
            level.restoringBlockSnapshots = true;
            try {
                if (!accepted) {
                    for (var snapshot : captured.reversed()) snapshot.restore(Block.UPDATE_CLIENTS);
                    for (var snapshot : originals.reversed()) snapshot.restore(Block.UPDATE_CLIENTS);
                }
            } finally {
                level.restoringBlockSnapshots = false;
            }
        }
    }

    private static void finish(ServerPlayer player, List<BlockSnapshot> snapshots) {
        var level = player.serverLevel();
        // The cache has already changed ownership; a notification error must never roll the transfer back.
        for (var snapshot : snapshots) try {
            var pos = snapshot.getPos();
            var state = level.getBlockState(pos);
            state.onPlace(level, pos, snapshot.getState(), false);
            level.markAndNotifyBlock(pos, level.getChunkAt(pos), snapshot.getState(), state, Block.UPDATE_ALL, 512);
        } catch (RuntimeException exception) {
            Better_experience.LOGGER.error("Container transferred, but neighbor notification failed at {}", snapshot.getPos(), exception);
        }
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("better_experience.void_staff." + key), true);
    }
    private static void reject(ServerPlayer player, VoidCargo cargo, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new VoidStaffRejectedPacket(cargo.token(), pos));
        message(player, "blocked");
    }
}
