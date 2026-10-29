package com.github.edg_thexu.better_experience.block;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.renderer.SimpleGeoItemRenderer;
import com.github.edg_thexu.better_experience.init.ModBlocks;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.confluence_lib.ConfluenceLibHelper;
import com.github.edg_thexu.better_experience.menu.AutoFishMenu;
import com.github.edg_thexu.better_experience.mixed.IFishingHook;
import com.github.edg_thexu.better_experience.mixed.IPlayer;
import com.github.edg_thexu.better_experience.module.autofish.AutoFishManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static net.minecraft.world.level.block.ChestBlock.FACING;


public class AutoFishBlock extends BaseEntityBlock {

    public AutoFishBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TURN, false));

    }
    public static final BooleanProperty TURN = BooleanProperty.create("turn_on");


    public static final MapCodec<AutoFishBlock> CODEC = simpleCodec(AutoFishBlock::new);

    @Override
    protected @NotNull MapCodec<AutoFishBlock> codec() {return CODEC;}

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, isMoving);
    }
    public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE, TURN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placeContext) {
        return defaultBlockState()
                .setValue(FACING, placeContext.getHorizontalDirection().getOpposite())
                .setValue(TYPE, ChestType.SINGLE)
                .setValue(TURN, false);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new AutoFishMachineEntity(blockPos, blockState);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if(state.hasBlockEntity()){
            BlockEntity entity = level.getBlockEntity(pos);
            ((IPlayer)player).betterExperience$setInteractBlockEntity(entity);
            if(!level.isClientSide && entity instanceof AutoFishMachineEntity entity1) {
                player.openMenu(new SimpleMenuProvider(entity1, Component.translatable("block.better_experience.autofish_machine")));
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        useItemOn(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlocks.AUTO_FISH_BLOCK_ENTITY.get(), (level, pos, state, entity) -> {
            if (level instanceof ServerLevel serverLevel) entity.serverTick(serverLevel);
            else entity.cacheTime = Math.max(0, entity.cacheTime - 1);
        });
    }

    public static class AutoFishMachineEntity extends ChestBlockEntity  implements GeoBlockEntity,  WorldlyContainer{


        int ticks = 0;
        // 上一次上钩时间
        int lastTick = 0;
        // 下次上钩时间
        public int fishingTime = 0;
        public int cacheTime;// 客户端记录时间
        // 冷却缩减
        float cdReduce = 1;

        Player owner;
        UUID ownerUUID;
        // 水中鱼钩位置
        Vec3 target;
        /**
         * 简单状态：
         * <p>0：未开始
         * <p>1：尝试启动
         * <p>2：运行中
         */
        public int isStarted = 0;
        private final ContainerData dataAccess;

        int [] face;

        public int getState(){
            return isStarted;
        }

        public AutoFishMachineEntity(BlockPos pos, BlockState blockState) {
            super(ModBlocks.AUTO_FISH_BLOCK_ENTITY.get(), pos, blockState);

            this.dataAccess = new ContainerData() {
                public int get(int id) {
                    return switch (id) {
                        case 0 -> isStarted;
                        case 1->  Math.max(0, lastTick + fishingTime - ticks + 19) / 20;
                        default -> 0;
                    };
                }
                public void set(int id, int value) {
                    switch (id) {
                        case 0 -> isStarted = value;
                        case 1 -> cdReduce = value * 0.01f;
                    }

                }
                public int getCount() {
                    return 2;
                }
            };

            if(this.getItems().size() < getContainerSize()){
                this.setItems(NonNullList.withSize(getContainerSize(), ItemStack.EMPTY));
            }
            face = IntStream.range(0, 27).toArray();
            this.openersCounter = new ContainerOpenersCounter() {
                protected void onOpen(Level level, BlockPos blockPos, BlockState state) {

                }

                protected void onClose(Level level, BlockPos blockPos, BlockState state) {

                }

                protected void openerCountChanged(Level level, BlockPos blockPos, BlockState state, int id, int param) {
                    signalOpenCount(level, blockPos, state, id, param);
                }

                protected boolean isOwnContainer(Player player) {
                    if (!(player.containerMenu instanceof ChestMenu)) {
                        return false;
                    } else {
                        Container container = ((ChestMenu)player.containerMenu).getContainer();
                        return container == AutoFishMachineEntity.this || container instanceof CompoundContainer && ((CompoundContainer)container).contains(AutoFishMachineEntity.this);
                    }
                }
            };
        }

        public void serverTick(ServerLevel serverLevel) {
            if (isStarted == 0) return;
            if (ownerUUID == null && owner != null) ownerUUID = owner.getUUID();
            owner = ownerUUID == null ? null : serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
            if (owner == null || !owner.isAlive() || owner.level() != serverLevel) {
                if (isStarted != 1) { isStarted = 1; updateState(); }
                return;
            }
            if (isStarted == 1 && !tryStart(owner)) return;
            if (!(getItem(27).getItem() instanceof FishingRodItem) || !findTarget()) { stop(); return; }
            if (++ticks - lastTick < fishingTime) return;
            if (!hasConduit()) { stop(); return; }
            ItemStack bait = getItem(28);
            try {
                for (ItemStack caught : AutoFishManager.catchItems((net.minecraft.server.level.ServerPlayer) owner,
                        serverLevel, worldPosition, target, getItem(27), bait, getItem(29))) {
                    ItemStack item = caught.copy();
                    insertCatch(item);
                    if (!item.isEmpty()) Containers.dropItemStack(serverLevel, worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1, worldPosition.getZ() + 0.5, item);
                }
                if (!bait.isEmpty() && serverLevel.random.nextFloat() < net.minecraft.util.Mth.clamp(0.33F - owner.getLuck() * 0.2F, 0, 1)) bait.shrink(1);
            } catch (RuntimeException failure) {
                Better_experience.LOGGER.error("Automatic fishing failed at {}", worldPosition, failure);
                stop();
                return;
            }
            lastTick = ticks;
            fishingTime = 20;
            updateState();
        }

        public void insertCatch(ItemStack item) {
            for (int slot = 0; slot < 27 && !item.isEmpty(); slot++) {
                ItemStack current = getItem(slot);
                if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, item)) continue;
                int count = Math.min(item.getCount(), Math.max(0, Math.min(getMaxStackSize(), current.getMaxStackSize()) - current.getCount()));
                current.grow(count);
                item.shrink(count);
            }
            for (int slot = 0; slot < 27 && !item.isEmpty(); slot++) {
                if (getItem(slot).isEmpty()) setItem(slot, item.split(Math.min(item.getMaxStackSize(), getMaxStackSize())));
            }
            setChanged();
        }

        public boolean tryStart(Player player){
            if (player == null || level == null || level.isClientSide || player.level() != level) return false;
            if (!(getItem(27).getItem() instanceof FishingRodItem)) {
                player.sendSystemMessage(Component.translatable("better_experience.autofish.missing_rod"));
                stop();
                return false;
            }
            boolean hasConduit = hasConduit();
            if(!hasConduit){
                player.sendSystemMessage(Component.translatable("better_experience.autofish.info.lack").append(Component.translatable(Blocks.CONDUIT.getDescriptionId())));
                this.stop();
                return false;
            }

            if(this.findTarget()){
                this.owner = player;
                this.ownerUUID = player.getUUID();
                this.lastTick = this.ticks;
                this.fishingTime = 20;
//                player.sendSystemMessage(Component.literal("start"));
                this.isStarted = 2;
                updateState();
                return true;

            }else{
                player.sendSystemMessage(Component.translatable("better_experience.autofish.missing_water"));
                this.stop();
                return false;
            }

        }

        private boolean hasConduit() {
            if (level == null) return false;
            for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP}) {
                if (level.getBlockState(worldPosition.relative(direction)).is(Blocks.CONDUIT)) return true;
            }
            return false;
        }

        public void stop(){
            this.isStarted = 0;
            this.dataAccess.set(0,0);
            target = null;
            updateState();
        }

        public void updateState(){
            setChanged();
            if(level instanceof ServerLevel sl){
                BlockState before = getBlockState();
                BlockState after = before.setValue(TURN, isStarted == 2);
                if (before != after) sl.setBlock(worldPosition, after, 3);
                sl.sendBlockUpdated(worldPosition, before, after, 2);
            }
        }

        private boolean findTarget(){
            if (level == null) return false;
            target = null;
            BlockPos pos = this.getBlockPos();
            // 水下正下方
            for(int i = 1; i < 4; i++){
                BlockState state = this.level.getBlockState(pos.below(i));
                boolean flag = state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA);
                if (flag || (ConfluenceHelper.isLoaded() && (state.is(org.confluence.mod.common.init.block.ModBlocks.SHIMMER) || state.is(org.confluence.mod.common.init.block.ModBlocks.HONEY)))) {
                    target = new Vec3(pos.getX() + 0.5, pos.getY() - i, pos.getZ() + 0.5);
                    return true;
                }
            }

            return false;
        }

        @Override
        public int getContainerSize() {
            return 30;
        }

        @Override
        protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory) {
            return new AutoFishMenu(id, inventory, this, this.dataAccess);
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt, HolderLookup.@NotNull Provider lookupProvider) {
            super.onDataPacket(net, pkt, lookupProvider);
            CompoundTag tag = pkt.getTag();
            isStarted = tag.getInt("started");
            fishingTime = tag.getInt("fishTime");
            cacheTime = fishingTime;
            if(tag.contains("owner")){
                this.ownerUUID = tag.getUUID("owner");
            }
        }

        @Override
        protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
            this.setItems(NonNullList.withSize(getContainerSize(), ItemStack.EMPTY));
            super.loadAdditional(tag, registries);
            isStarted = tag.getInt("started") == 0 ? 0 : 1;
            owner = null;
            ownerUUID = null;
            ticks = 0;
            lastTick = 0;
            fishingTime = 20;
            if(tag.contains("owner")){
                this.ownerUUID = tag.getUUID("owner");
            }
        }

        @Override
        public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
            CompoundTag tag = super.getUpdateTag(registries);
            tag.putInt("started", isStarted);
            tag.putInt("fishTime", fishingTime);
            if (ownerUUID != null) tag.putUUID("owner", ownerUUID);
            return tag;
        }

        @Override
        protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("started", isStarted);
            if (ownerUUID != null) tag.putUUID("owner", ownerUUID);
        }
        private final AnimatableInstanceCache CACHE = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return CACHE;
        }

        @Override
        public int @NotNull [] getSlotsForFace(@NotNull Direction direction) {
            if(direction == Direction.UP){
                return new int[]{28};
            }
            return face;
        }

        @Override
        public boolean canPlaceItemThroughFace(int i, @NotNull ItemStack itemStack, @Nullable Direction direction) {
            if(direction == Direction.UP){
                return ConfluenceHelper.isLoaded() && itemStack.getItem() instanceof org.confluence.mod.common.item.fishing.IBait;
            }
            return true;
        }

        @Override
        public boolean canTakeItemThroughFace(int i, @NotNull ItemStack itemStack, @NotNull Direction direction) {
            return i < 27;
        }
    }

    public static class Item extends BlockItem implements GeoItem {
        private final AnimatableInstanceCache CACHE = GeckoLibUtil.createInstanceCache(this);

        public Item(AutoFishBlock pBlock) {
            super(pBlock, new Properties()
                    // todo
//                    .component(TCDataComponentTypes.MOD_RARITY, ModRarity.BLUE)
            );
        }

        @Override
        public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
            consumer.accept(new SimpleGeoItemRenderer<>(
                    Better_experience.space("geo/block/auto_fish_block.geo.json"),
                    Better_experience.space("textures/block/auto_fish_block.png"),
                    null
                    ));
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return CACHE;
        }
    }

}
