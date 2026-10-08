package com.github.edg_thexu.better_experience.module.fluidstaff;

import com.github.edg_thexu.better_experience.data.component.FluidStaffSettings;
import com.github.edg_thexu.better_experience.data.component.FluidStaffStorage;
import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.FluidStaff;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Bounded server-thread jobs share the same geometry and rendered anchor as the construction staff. */
public final class FluidStaffManager {
    private static final FluidStaffManager INSTANCE = new FluidStaffManager();
    private static final int POSITIONS_PER_TICK = 128;
    private final Map<UUID, Job> jobs = new HashMap<>();

    private static final class Job {
        final ServerPlayer player;
        final ServerLevel level;
        final ItemStack staff;
        final int slot;
        final boolean creative;
        final FluidStaffSettings settings;
        final SpaceStaffSettings geometry;
        final StaffSelectionShape selection;
        final ArrayDeque<BlockPos> positions;
        final FluidStaffHandler reservoir;
        long transferred;

        Job(ServerPlayer player, List<BlockPos> positions) {
            this.player = player;
            level = player.serverLevel();
            staff = player.getMainHandItem();
            slot = player.getInventory().selected;
            creative = player.isCreative();
            settings = staff.getOrDefault(ModDataComponentTypes.FLUID_STAFF_SETTINGS.get(), FluidStaffSettings.defaults());
            geometry = staff.get(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get());
            selection = staff.get(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get());
            this.positions = new ArrayDeque<>(positions);
            reservoir = new FluidStaffHandler(staff);
        }
    }

    private FluidStaffManager() {}
    public static FluidStaffManager getInstance() { return INSTANCE; }
    public void clear() { jobs.clear(); }

    public boolean enqueue(ServerPlayer player, List<BlockPos> positions) {
        if (jobs.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.translatable("better_experience.fluid.busy"), true);
            return false;
        }
        jobs.put(player.getUUID(), new Job(player, positions));
        return true;
    }

    public void tick() {
        for (Job job : List.copyOf(jobs.values())) {
            if (!active(job)) {
                jobs.remove(job.player.getUUID(), job);
                continue;
            }
            for (int i = 0; i < POSITIONS_PER_TICK && !job.positions.isEmpty() && active(job); i++) {
                if (exhausted(job)) break;
                BlockPos pos = job.positions.removeFirst();
                if (!job.level.hasChunkAt(pos) || job.level.isOutsideBuildHeight(pos)
                        || !job.level.getWorldBorder().isWithinBounds(pos) || !job.level.mayInteract(job.player, pos)) continue;
                var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                var interaction = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(job.player,
                        InteractionHand.MAIN_HAND, pos, hit));
                if (interaction.isCanceled() || interaction.getUseItem() == TriState.FALSE || !active(job)) continue;
                if (!transferMachine(job, pos)) {
                    if (job.settings.placing()) placeWorld(job, pos);
                    else absorbWorld(job, pos);
                }
            }
            job.player.getInventory().setChanged();
            job.player.containerMenu.broadcastChanges();
            if (job.positions.isEmpty() || exhausted(job)) {
                job.player.displayClientMessage(Component.translatable(job.settings.placing()
                        ? "better_experience.fluid.placed" : "better_experience.fluid.absorbed", job.transferred), true);
                jobs.remove(job.player.getUUID(), job);
            }
        }
    }

    private static boolean active(Job job) {
        return INSTANCE.jobs.get(job.player.getUUID()) == job && !job.player.hasDisconnected()
                && !job.player.isRemoved() && job.player.serverLevel() == job.level
                && job.player.mayBuild() && !job.player.isSpectator() && job.player.isCreative() == job.creative
                && job.player.getInventory().selected == job.slot && job.player.getMainHandItem() == job.staff
                && job.staff.getItem() instanceof FluidStaff
                && job.settings.equals(job.staff.get(ModDataComponentTypes.FLUID_STAFF_SETTINGS.get()))
                && java.util.Objects.equals(job.geometry, job.staff.get(ModDataComponentTypes.SPACE_STAFF_SETTINGS.get()))
                && java.util.Objects.equals(job.selection, job.staff.get(ModDataComponentTypes.STAFF_SELECTION_SHAPE.get()));
    }

    private static boolean exhausted(Job job) {
        if (job.creative) return false;
        var storage = job.staff.getOrDefault(ModDataComponentTypes.FLUID_STAFF_STORAGE.get(), FluidStaffStorage.EMPTY);
        return job.settings.placing() ? storage.amount(job.settings.fluid()) == 0 : storage.total() >= FluidStaffStorage.CAPACITY;
    }

    /** Never replace a machine block: interact through its unsided and sided fluid capabilities. */
    private static boolean transferMachine(Job job, BlockPos pos) {
        Set<IFluidHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var unsided = FluidUtil.getFluidHandler(job.level, pos, null);
        unsided.ifPresent(handler -> transferHandler(job, handler, seen));
        for (Direction direction : Direction.values()) {
            if (!active(job) || exhausted(job)) break;
            FluidUtil.getFluidHandler(job.level, pos, direction).ifPresent(handler -> transferHandler(job, handler, seen));
        }
        return !seen.isEmpty();
    }

    private static void transferHandler(Job job, IFluidHandler handler, Set<IFluidHandler> seen) {
        if (!seen.add(handler) || !active(job) || exhausted(job)) return;
        if (job.settings.placing()) {
            var fluid = job.settings.fluid().copyWithAmount(job.creative ? Integer.MAX_VALUE : FluidStaffStorage.CAPACITY);
            IFluidHandler source = job.reservoir;
            if (job.creative) {
                var infinite = new FluidTank(Integer.MAX_VALUE);
                infinite.setFluid(fluid);
                source = infinite;
            }
            job.transferred += FluidUtil.tryFluidTransfer(handler, source, fluid, true).getAmount();
        } else {
            int tanks = Math.min(FluidStaffStorage.MAX_TYPES, Math.max(0, handler.getTanks()));
            for (int tank = 0; tank < tanks && active(job) && !exhausted(job); tank++) {
                var fluid = handler.getFluidInTank(tank);
                if (fluid.isEmpty()) continue;
                if (job.creative) job.transferred += handler.drain(fluid, IFluidHandler.FluidAction.EXECUTE).getAmount();
                else job.transferred += FluidUtil.tryFluidTransfer(job.reservoir, handler, fluid, true).getAmount();
            }
        }
    }

    private static void absorbWorld(Job job, BlockPos pos) {
        if (!FluidStaffRules.canAbsorb(job.level, pos)) return;
        var state = job.level.getBlockState(pos);
        var resource = new FluidStack(state.getFluidState().getType(), 1000);
        if (!job.creative && job.reservoir.fill(resource, IFluidHandler.FluidAction.SIMULATE) < 1000) return;
        if (CommonHooks.fireBlockBreak(job.level, job.player.gameMode.getGameModeForPlayer(), job.player, pos, state).isCanceled()
                || !active(job) || job.level.getBlockState(pos) != state) return;
        FluidStack[] drained = {FluidStack.EMPTY};
        boolean accepted = FluidWorldTransaction.apply(job.player, pos, false, () -> {
            if (state.getBlock() instanceof BucketPickup pickup) {
                var container = pickup.pickupBlock(job.player, job.level, pos, state);
                var remaining = job.level.getFluidState(pos);
                if (remaining.isSource() && remaining.getType().isSame(resource.getFluid())) return false;
                var contained = FluidUtil.getFluidContained(container).orElse(FluidStack.EMPTY);
                drained[0] = !contained.isEmpty() && contained.getFluid().isSame(resource.getFluid())
                        ? contained.copyWithAmount(1000) : resource;
                return true;
            }
            boolean changed = job.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (changed) drained[0] = resource;
            return changed;
        }, () -> active(job) && !drained[0].isEmpty() && (job.creative
                || job.reservoir.fill(drained[0], IFluidHandler.FluidAction.SIMULATE) == drained[0].getAmount()),
                () -> { if (!job.creative) job.reservoir.fill(drained[0], IFluidHandler.FluidAction.EXECUTE); });
        if (accepted) job.transferred += drained[0].getAmount();
    }

    private static void placeWorld(Job job, BlockPos pos) {
        var fluid = job.settings.fluid().copyWithAmount(1000);
        if (!FluidStaffRules.canPlace(job.level, pos, fluid, job.player)
                || (!job.creative && job.reservoir.drain(fluid, IFluidHandler.FluidAction.SIMULATE).getAmount() < 1000)) return;
        var type = fluid.getFluidType();
        if (type.isVaporizedOnPlacement(job.level, pos, fluid)) {
            if (!job.creative) job.reservoir.drain(fluid, IFluidHandler.FluidAction.EXECUTE);
            type.onVaporize(job.player, job.level, pos, fluid);
            job.transferred += 1000;
            return;
        }
        var before = job.level.getBlockState(pos);
        var placed = type.getStateForPlacement(job.level, pos, fluid);
        boolean accepted = FluidWorldTransaction.apply(job.player, pos, true, () -> {
            if (before.getBlock() instanceof LiquidBlockContainer container) {
                return container.placeLiquid(job.level, pos, before, placed);
            }
            return job.level.setBlock(pos, type.getBlockForFluidState(job.level, pos, placed), Block.UPDATE_ALL);
        }, () -> active(job) && job.level.getFluidState(pos).getType().isSame(placed.getType())
                && (job.creative || job.reservoir.drain(fluid, IFluidHandler.FluidAction.SIMULATE).getAmount() == 1000),
                () -> { if (!job.creative) job.reservoir.drain(fluid, IFluidHandler.FluidAction.EXECUTE); });
        if (accepted) job.transferred += 1000;
    }
}
