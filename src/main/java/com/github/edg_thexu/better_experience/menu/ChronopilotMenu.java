package com.github.edg_thexu.better_experience.menu;

import com.github.edg_thexu.better_experience.block.ChronopilotBlock;
import com.github.edg_thexu.better_experience.init.ModBlocks;
import com.github.edg_thexu.better_experience.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ChronopilotMenu extends AbstractContainerMenu {
    private final Level level;
    private final BlockPos pos;
    private final DataSlot time;
    private long lastChange = Long.MIN_VALUE;

    public ChronopilotMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.CHRONOPILOT_MENU.get(), id);
        this.level = inventory.player.level();
        this.pos = pos.immutable();
        this.time = addDataSlot(new DataSlot() {
            private int syncedTime = (int) Math.floorMod(level.getDayTime(), 24000L);

            @Override
            public int get() {
                return level.isClientSide ? syncedTime : (int) Math.floorMod(level.getDayTime(), 24000L);
            }

            @Override
            public void set(int value) {
                syncedTime = value;
            }
        });
    }

    public int time() {
        return time.get();
    }

    public static String formatTime(long ticks) {
        int minutes = (int) ((Math.floorMod(ticks, 24000L) + 6000L) % 24000L * 1440L / 24000L);
        return "%02d:%02d".formatted(minutes / 60, minutes % 60);
    }

    public static int ticksForTime(int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) throw new IllegalArgumentException("Invalid clock time");
        // Round up so converting back never displays the preceding minute.
        return Math.floorMod(((hour * 60 + minute) * 50 + 2) / 3 - 6000, 24000);
    }

    public void setTime(ServerPlayer player, int ticks) {
        if (ticks < 0 || ticks >= 24000 || !stillValid(player) || !player.mayBuild()
                || !level.mayInteract(player, pos)) return;
        long now = level.getGameTime();
        if (lastChange != Long.MIN_VALUE && now - lastChange < 5) return;
        lastChange = now;
        for (ServerLevel serverLevel : player.server.getAllLevels()) {
            serverLevel.setDayTime(Math.floorDiv(serverLevel.getDayTime(), 24000L) * 24000L + ticks);
        }
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level() != level || player.isSpectator() || !level.hasChunkAt(pos)
                || !player.canInteractWithBlock(pos, 1.0)) return false;
        BlockState base = level.getBlockState(pos);
        BlockState upper = level.getBlockState(pos.above());
        return base.is(ModBlocks.CHRONOPILOT.get()) && base.getValue(ChronopilotBlock.PART).isBase()
                && upper.is(base.getBlock()) && upper.getValue(ChronopilotBlock.PART).isUpper()
                && upper.getValue(ChronopilotBlock.FACING) == base.getValue(ChronopilotBlock.FACING);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
