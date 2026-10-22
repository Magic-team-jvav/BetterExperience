package com.github.edg_thexu.better_experience.block;

import com.github.edg_thexu.better_experience.init.ModBlocks;
import com.github.edg_thexu.better_experience.menu.ChronopilotMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.confluence.lib.common.block.HorizontalDirectionalWithVerticalTwoPartBlock;
import org.confluence.lib.common.block.StateProperties.VerticalTwoPart;
import org.jetbrains.annotations.Nullable;

/** Uses the same two-part foundation as TerraFurniture's grandfather clocks. */
public class ChronopilotBlock extends HorizontalDirectionalWithVerticalTwoPartBlock implements EntityBlock {
    public static final MapCodec<ChronopilotBlock> CODEC = simpleCodec(ChronopilotBlock::new);
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

    public ChronopilotBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ChronopilotBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedPos().getY() >= context.getLevel().getMaxBuildHeight() - 1) return null;
        return super.getStateForPlacement(context);
    }

    public static BlockPos basePos(BlockState state, BlockPos pos) {
        return state.getValue(PART).isBase() ? pos : pos.below();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
            BlockPos base = basePos(state, pos);
            if (level.mayInteract(player, base)) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, opener) -> new ChronopilotMenu(id, inventory, base),
                        Component.translatable("block.better_experience.chronopilot")), buffer -> buffer.writeBlockPos(base));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        useWithoutItem(state, level, pos, player, hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART).isBase() ? new Entity(pos, state) : null;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART).isUpper()) {
            BlockPos base = pos.below();
            if (level.getBlockState(base).is(this)) level.destroyBlock(base, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        // The shared base removes its partner even for property-only updates.
        if (state.is(replacement.getBlock())) return;
        if (state.hasBlockEntity()) level.removeBlockEntity(pos);
        BlockPos otherPos = pos.relative(VerticalTwoPart.getConnectedDirection(state));
        BlockState other = level.getBlockState(otherPos);
        if (other.is(this) && other.getValue(PART) != state.getValue(PART)
                && other.getValue(FACING) == state.getValue(FACING)) {
            level.destroyBlock(otherPos, !level.isClientSide && state.getValue(PART).isUpper());
        }
    }

    public static class Entity extends BlockEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(ModBlocks.CHRONOPILOT_ENTITY.get(), pos, state);
        }
    }
}
