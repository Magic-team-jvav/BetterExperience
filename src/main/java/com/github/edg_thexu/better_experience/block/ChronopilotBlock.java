package com.github.edg_thexu.better_experience.block;

import com.github.edg_thexu.better_experience.init.ModBlocks;
import com.github.edg_thexu.better_experience.menu.ChronopilotMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Vertical two-part block with horizontal facing, self-contained so Confluence Lib is not required. */
public class ChronopilotBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ChronopilotBlock> CODEC = simpleCodec(ChronopilotBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("vertical_two_part", Part.class);
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

    public ChronopilotBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.BASE));
    }

    @Override
    protected MapCodec<? extends ChronopilotBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedPos().getY() >= context.getLevel().getMaxBuildHeight() - 1) return null;
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return context.getLevel().getBlockState(context.getClickedPos().above()).canBeReplaced(context) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) level.setBlockAndUpdate(pos.above(), state.setValue(PART, Part.UP));
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
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
        // A property-only update must not remove the partner block.
        if (state.is(replacement.getBlock())) return;
        if (state.hasBlockEntity()) level.removeBlockEntity(pos);
        BlockPos otherPos = pos.relative(Part.getConnectedDirection(state));
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

    /** Serialized as {@code vertical_two_part=base|up}, matching the shipped blockstate and saved worlds. */
    public enum Part implements StringRepresentable {
        BASE("base"),
        UP("up");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        public static Direction getConnectedDirection(BlockState state) {
            return state.getValue(PART).isBase() ? Direction.UP : Direction.DOWN;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        public boolean isBase() {
            return this == BASE;
        }

        public boolean isUpper() {
            return this == UP;
        }
    }
}
