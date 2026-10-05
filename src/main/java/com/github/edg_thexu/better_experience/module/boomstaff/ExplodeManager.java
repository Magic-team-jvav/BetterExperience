package com.github.edg_thexu.better_experience.module.boomstaff;

import com.github.edg_thexu.better_experience.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ExplodeManager {
    private static final int MAX_BLOCKS_PER_TICK = 500;
    private static final ExplodeManager INSTANCE = new ExplodeManager();
    private final List<BlockQueue> playerBlockQueues = new ArrayList<>();
    private final Queue<BlockQueue> pendingQueues = new ConcurrentLinkedQueue<>();

    private ExplodeManager() {}

    public static ExplodeManager getInstance() {
        return INSTANCE;
    }

    /** Defers new tasks until the next tick, including tasks queued by block callbacks. */
    public void addBlockToQueue(BlockQueue queue) {
        pendingQueues.add(queue);
    }

    public void clear() {
        pendingQueues.clear();
        playerBlockQueues.clear();
    }

    public void tickHandle() {
        BlockQueue pending;
        while ((pending = pendingQueues.poll()) != null) {
            playerBlockQueues.add(pending);
        }

        Iterator<BlockQueue> queues = playerBlockQueues.iterator();
        while (queues.hasNext()) {
            BlockQueue blockPosQueue = queues.next();
            Queue<Tuple<BlockPos, Boolean>> blockQueue = blockPosQueue.blockQueue;
            if (blockQueue.isEmpty() || blockPosQueue.player.hasDisconnected()
                    || blockPosQueue.player.isRemoved()
                    || blockPosQueue.player.serverLevel() != blockPosQueue.level) {
                queues.remove();
                continue;
            }
            ServerPlayer player = blockPosQueue.player;
            Vec3 center = blockPosQueue.center;
            ServerLevel level = blockPosQueue.level;

            List<ItemStack> allDrops = new ArrayList<>();
            double maxY = center.y;
            int top = blockQueue.peek().getA().getY();
            for (int i = 0; i < MAX_BLOCKS_PER_TICK; i++) {
                if (blockQueue.isEmpty()) {
                    break;
                }
                Tuple<BlockPos, Boolean> blockPos = blockQueue.peek();
                BlockPos pos = blockPos.getA();
                if (pos.getY() >= top) {
                    top = pos.getY();
                    blockQueue.poll();
                } else {
                    break;
                }
                if (!level.hasChunkAt(pos)) continue;
                maxY = pos.getY();
                boolean isDrop = blockPos.getB();
                BlockState state = level.getBlockState(pos);
                BlockEntity entity2 = level.getBlockEntity(pos);
                var eligibility = StaffBlockEligibility.evaluate(level, pos, state, blockPosQueue.tool);
                if (eligibility == StaffBlockEligibility.Result.SKIP
                        || !level.mayInteract(player, pos)
                        || CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled()) {
                    continue;
                }
                if (level.getBlockState(pos) != state) continue;
                // Remove without vanilla loot, then aggregate loot using the captured offhand tool.
                var drops = isDrop && eligibility == StaffBlockEligibility.Result.DROP
                        ? Block.getDrops(state, level, pos, entity2, player, blockPosQueue.tool)
                        : List.<ItemStack>of();
                if (!level.destroyBlock(pos, false, player)) continue;
                allDrops.addAll(drops);
                if (level.random.nextFloat() < 0.05f) {
                    level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.2f,0.6f);
                    level.sendParticles(ParticleTypes.EXPLOSION, pos.getX(), pos.getY(), pos.getZ(), 1, 0.5, 0.1,0,0);
                }
            }
            ModUtils.unionItemStacks(allDrops);

            for (var drop : allDrops) {
                if (drop.isEmpty()) continue;
                ItemEntity entity = new ItemEntity(level, center.x, maxY + 1, center.z, drop);
                entity.setDeltaMovement(0, 0.1, 0);
                level.addFreshEntity(entity);
            }
            if (blockQueue.isEmpty()) {
                queues.remove();
            }
        }
    }


    public record BlockQueue(Queue<Tuple<BlockPos, Boolean>> blockQueue, Vec3 center,
                             ServerPlayer player, ServerLevel level, ItemStack tool) {
        public BlockQueue(Queue<Tuple<BlockPos, Boolean>> blocks, Vec3 center, ServerPlayer player) {
            this(new java.util.ArrayDeque<>(blocks), center, player, player.serverLevel(), player.getOffhandItem().copy());
        }
    }
}
