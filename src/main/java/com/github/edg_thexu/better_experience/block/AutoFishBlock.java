package com.github.edg_thexu.better_experience.block;

import com.github.edg_thexu.better_experience.Better_experience;
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
import org.confluence.lib.util.LibMathUtils;
import org.confluence.lib.util.LibUtils;
import org.confluence.mod.client.renderer.item.SimpleGeoItemRenderer;
import org.confluence.mod.common.init.ModTags;
import org.confluence.mod.common.init.item.AccessoryItems;
import org.confluence.mod.common.init.item.FishingPoleItems;
import org.confluence.mod.common.item.fishing.AbstractFishingPole;
import org.confluence.mod.common.item.fishing.BaitItem;
import org.confluence.terra_curio.util.TCUtils;
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? createTickerHelper(pBlockEntityType, ModBlocks.AUTO_FISH_BLOCK_ENTITY.get(),  (level, pos, state, entity)-> {
            entity.cacheTime = Math.max(0, entity.cacheTime - 1);
//            System.out.println(entity.cacheTime);

        }) : createTickerHelper(pBlockEntityType, ModBlocks.AUTO_FISH_BLOCK_ENTITY.get(),  (level, pos, state, entity)-> {

            // 未开始
            if(entity.dataAccess.get(0) == 0)
                return;

            // 启动游戏重新加载
            Player player = entity.owner;
            if(player == null && entity.ownerUUID != null && level instanceof ServerLevel serverLevel){
                entity.owner = (Player) serverLevel.getEntity(entity.ownerUUID);
                entity.tryStart(entity.owner);
            }
            // 尝试启动
            if(entity.dataAccess.get(0) == 1){
                entity.tryStart(player);
            }


            if(level instanceof ServerLevel serverLevel && player!= null && entity.target != null && entity.lastTick + entity.fishingTime < ++entity.ticks){
//                for(int ii=0;ii<100;ii++) {

                    ItemStack poleStack = entity.getItem(27);
                    ItemStack bait = entity.getItem(28);
                    ItemStack curios = entity.getItem(29);

                    if (poleStack.getItem() instanceof FishingRodItem pole) {
                        FishingHook hook;
                        var oldHook = player.fishing;

                        try {
                            float power = AutoFishManager.computeFishingPower(null, poleStack, bait.getItem(), curios);
                            if (ConfluenceLibHelper.isLoaded() && pole instanceof AbstractFishingPole pole1) {
                                hook = pole1.getHook(poleStack, player, level, (int) power, 5);
                            } else {
                                hook = new FishingHook(player, level, (int) power, 5);
                            }
                        } catch (Exception e) {
                            Better_experience.LOGGER.error("Failed to create fishing hook", e);
                            return;
                        }
                        player.fishing = oldHook;
                        ((IFishingHook) hook).betterExperience$setPos(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                        ((IFishingHook) hook).betterExperience$setSimulation(true);

                        if (pole == FishingPoleItems.HOTLINE_FISHING_HOOK.get() ||
                                (!bait.isEmpty() && bait.is(ModTags.Items.LAVA_PROOF_BAIT)) ||
                                TCUtils.hasType(player, AccessoryItems.LAVAPROOF$FISHING$HOOK)
                        ) org.confluence.mod.mixed.IFishingHook.of(hook).confluence$setIsLavaHook();

                        hook.setPos(entity.target);
                        hook.nibble = 10;

                        // 模拟收杆
                        hook.retrieve(poleStack);
                        List<ItemStack> items = ((IFishingHook) hook).betterExperience$getItems();

                        for (ItemStack item : items) {

//                            if (item.getItem() != Items.ENCHANTED_BOOK) {
//                                item.setCount(0);
//                                continue;
//                            }
//                            boolean keep = false;
//                            var keys = item.getComponents().get(DataComponents.STORED_ENCHANTMENTS).keySet();
//                            for (var key : keys) {
//                                if(key.getKey().location().getNamespace().equals(TerraEntity.MODID)){
//                                    keep = true;
//                                }
//                            }
//                            if(!keep){
//                                item.setCount(0);
//                                continue;
//                            }

                            // 尝试堆叠
                            for (int i = 0; i < 27; i++) {
                                ItemStack current = entity.getItem(i);
                                if (current.isEmpty()) {
                                    entity.setItem(i, item.copy());
                                    item.setCount(0);
                                    break;
                                }
                                if (current.is(item.getItem()) && current.getCount() < current.getMaxStackSize()) {
                                    // 计算可以堆叠的数量
                                    int transfer = Math.min(item.getCount(), current.getMaxStackSize() - current.getCount());
                                    current.grow(transfer); // 增加当前物品的数量
                                    item.shrink(transfer);  // 减少新物品的数量
                                    if (item.isEmpty()) {
                                        break; // 如果新物品被完全堆叠，返回true
                                    }
                                }
                            }
                            // 溢出
                            if (!item.isEmpty()) {
//                            var f = state.getValue(FACING);
//                            float dir = f.getRotation().angle();
//                            float r = 0.1f;
//                            ItemEntity itemEntity = new ItemEntity(level,
//                                    pos.getX() + 0.5 + r * Math.sin(dir),
//                                    pos.getY() + 1,
//                                    pos.getZ() + 0.5 + r * Math.cos(dir), item);
                                LibUtils.createItemEntity(item, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, level, 0);
                            }
                        }

                        // 消耗鱼饵, 根据运气降低消耗概率
                        if (!bait.isEmpty() && LibMathUtils.checkChance(0.33f - entity.owner.getLuck() * 0.2f, level.random)) {
                            bait.shrink(1);
                        }
                        if (!entity.tryStart(entity.owner)) {
                            return;
                        }

                        entity.lastTick = entity.ticks;

                        if(ConfluenceLibHelper.isLoaded() && bait.getItem() instanceof BaitItem bait1){
                            entity.cdReduce = (1 - bait1.getBaitBonus() * 0.5f);
                        }
//                        entity.fishingTime = (int) (20 * 10 * entity.cdReduce);
                        entity.fishingTime = (int) (20);
                        entity.updateState();

                    } else {
                        // 没有钓竿，终止
                        entity.dataAccess.set(0, 0);
                        entity.updateState();

                    }
                }
//            }

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
                        case 1->  (lastTick + fishingTime - ticks) / 20;
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

        public boolean tryStart(Player player){
            // 周围要有潮涌核心
            BlockPos[] around = new BlockPos[]{
                    this.getBlockPos().offset(0,0,1),
                    this.getBlockPos().offset(0,0,-1),
                    this.getBlockPos().offset(1,0,0),
                    this.getBlockPos().offset(-1,0,0),
                    this.getBlockPos().offset(0,1,0)
//                    this.getBlockPos().offset(0,-1,0)
            };
            boolean hasConduit = false;
            for(int i=0;i<5;i++){
                if (level != null && level.getBlockState(around[i]).is(Blocks.CONDUIT)) {
                    hasConduit = true;
                    break;
                }
            }
            if(!hasConduit){
                player.sendSystemMessage(Component.translatable("better_experience.autofish.info.lack").append(Component.translatable(Blocks.CONDUIT.getDescriptionId())));
                this.stop();
                return false;
            }

            if(this.findTarget()){
                this.owner = player;
//                player.sendSystemMessage(Component.literal("start"));
                this.isStarted = 2;
                getBlockState().setValue(TURN, true);
                updateState();
                return true;

            }else{
//                player.sendSystemMessage(Component.literal("no water"));
                this.stop();
                return false;
            }

        }

        public void stop(){
            this.isStarted = 0;
            this.dataAccess.set(0,0);
            getBlockState().setValue(TURN, false);
            updateState();
        }

        public void updateState(){
            if(level instanceof ServerLevel sl){
                sl.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
            }
        }

        private boolean findTarget(){
            BlockPos pos = this.getBlockPos();
            // 水下正下方
            for(int i = 1; i < 4; i++){
                BlockState state = this.level.getBlockState(pos.below(i));
                boolean flag = this.level != null && state.is(Blocks.WATER) || state.is(Blocks.LAVA);
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
            isStarted = tag.getInt("started");

            if(tag.contains("owner")){
                this.ownerUUID = tag.getUUID("owner");
            }
        }

        @Override
        public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
            CompoundTag tag = super.getUpdateTag(registries);
            tag.putInt("started", isStarted);
            tag.putInt("fishTime", fishingTime);
            if(owner != null)
                tag.putUUID("owner", owner.getUUID());
            return tag;
        }

        @Override
        protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("started", isStarted);
            if(owner != null)
                tag.putUUID("owner", owner.getUUID());
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
                return itemStack.getItem() instanceof BaitItem;
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
