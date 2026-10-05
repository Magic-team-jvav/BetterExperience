package com.github.edg_thexu.better_experience.module.faststorage;

import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.sophisticated.SophisticatedHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import org.confluence.mod.common.attachment.PlayerPiggyBankContainer;
import org.confluence.mod.common.attachment.PlayerSafeContainer;
import org.confluence.mod.common.init.ModAttachmentTypes;
import org.confluence.mod.common.init.block.FunctionalBlocks;

public class StorageManager {

    private static final int STORAGE_RANGE = 5;

    /**
     * 物品存放到周围的箱子
     * @param player 玩家
     */
    public static void saveAll(Player player){
        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos center = player.blockPosition();
        for(int i = -STORAGE_RANGE; i <= STORAGE_RANGE; i++){
            for(int j = -STORAGE_RANGE; j <= STORAGE_RANGE; j++){
                for(int k = -STORAGE_RANGE; k <= STORAGE_RANGE; k++){
                    BlockPos pos = center.offset(i, j, k);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if(blockEntity instanceof ChestBlockEntity entity){
                        for(ItemStack stack : player.getInventory().items){
                            for(int slot = 0; slot < entity.getContainerSize(); slot++){
                                ItemStack containerStack = entity.getItem(slot);
                                if (com.github.edg_thexu.better_experience.utils.ModUtils.mergeItemStacks(stack, containerStack) > 0) {
                                    entity.setChanged();
                                    player.getInventory().setChanged();
                                }
                            }
                        }
                    }else if(SophisticatedHelper.isStorageLoaded() && blockEntity instanceof StorageBlockEntity entity) {
                        // 兼容精妙存储
                        for(ItemStack stack : player.getInventory().items){
                            for(int slot = 0; slot < entity.getStorageWrapper().getNumberOfInventorySlots(); slot++){
                                var handler = entity.getStorageWrapper().getInventoryHandler();
                                ItemStack containerStack = handler.getStackInSlot(slot);
                                if (!stack.isEmpty() && !containerStack.isEmpty()
                                        && ItemStack.isSameItemSameComponents(stack, containerStack)) {
                                    ItemStack remaining = handler.insertItem(slot, stack, false);
                                    stack.setCount(remaining.getCount());
                                    player.getInventory().setChanged();
                                }
                            }
                        }


                    }
                }
            }
        }
    }

    /**
     * 无需防止就可以打开存钱罐
     */
    public static void openPiggy(Level level, Player player, InteractionHand usedHand){
        ItemStack stack = player.getItemInHand(usedHand);
        if(usedHand == InteractionHand.MAIN_HAND ){
            if(ConfluenceHelper.isLoaded()) {
                if(stack.is(FunctionalBlocks.PIGGY_BANK.asItem())) {
                    PlayerPiggyBankContainer container = player.getData(ModAttachmentTypes.PIGGY_BANK);
                    player.openMenu(new SimpleMenuProvider((id, inventory, player1) -> {
                        return new ChestMenu(MenuType.GENERIC_9x6, id, inventory, container, 6);
                    }, Component.translatable("container.confluence.piggy_bank")));
                }
                else if(stack.is(FunctionalBlocks.SAFE.asItem())){
                    PlayerSafeContainer container = player.getData(ModAttachmentTypes.SAFE);
                    player.openMenu(new SimpleMenuProvider((id, inventory, player1) -> {
                        return new ChestMenu(MenuType.GENERIC_9x6, id, inventory, container, 6);
                    }, Component.translatable("container.confluence.safe")));
                }
            }
            if(stack.is(Blocks.ENDER_CHEST.asItem())){
                PlayerEnderChestContainer playerenderchestcontainer = player.getEnderChestInventory();

                player.openMenu(new SimpleMenuProvider((id, inventory, player1) -> {
                    return ChestMenu.threeRows(id, inventory, playerenderchestcontainer);
                },  Component.translatable("container.enderchest")));
            }
        }
    }
}
