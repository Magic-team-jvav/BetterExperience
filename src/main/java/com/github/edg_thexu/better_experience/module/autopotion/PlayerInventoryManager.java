package com.github.edg_thexu.better_experience.module.autopotion;

import com.github.edg_thexu.better_experience.client.gui.container.PotionBagScreen;
import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.intergration.curios.CuriosHelper;
import com.github.edg_thexu.better_experience.module.potionbag.PotionEffectRules;
import com.github.edg_thexu.better_experience.utils.ModUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.confluence.lib.common.PlayerContainer;
import org.confluence.mod.client.gui.container.ExtraInventoryScreen;
import org.confluence.mod.common.init.ModAttachmentTypes;
import org.confluence.mod.common.init.ModTags;
import org.confluence.mod.common.init.block.FunctionalBlocks;
import org.confluence.mod.util.PlayerUtils;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;
import top.theillusivec4.curios.client.gui.CuriosScreen;

import java.util.*;

/**
 * 旧物品规则入口、物品高亮和自动存钱；药水效果由服务端 PotionBagEffects 计算。
 */
public class PlayerInventoryManager {

    private static final int DETECT_INTERVAL = 60;
    public boolean serverOpenAutoPotion = true;
    public static List<Pair<Holder<MobEffect>, Integer>> getApplyEffect(ItemStack stack, boolean ignoreCount) {
        return PotionEffectRules.effects(stack, ignoreCount);
    }

    public static List<Pair<Holder<MobEffect>, Integer>> getApplyEffect(ItemStack stack) {
        return getApplyEffect(stack, false);
    }

    public static boolean canApply(ItemStack stack){
        return !getApplyEffect(stack).isEmpty();
    }

    private static PlayerInventoryManager instance;

    public static PlayerInventoryManager getInstance(){
        if(instance == null)
            instance = new PlayerInventoryManager();
        return instance;
    }

    public void detect(Player player) {
        if (!player.level().isClientSide && player.tickCount % DETECT_INTERVAL == 0) detectServer(player);
    }

    // 自动存钱
    private void detectServer(Player player){
        NonNullList<ItemStack> items = player.getInventory().items;
        boolean autoSave = false;
        for (ItemStack stack : items) {
            if (ConfluenceHelper.isLoaded() && CommonConfig.AUTO_SAVE_MONEY.get() && stack.is(FunctionalBlocks.PIGGY_BANK.asItem())) {
                autoSave = true;
                break;
            }
        }
        if(autoSave){
            var piggyData = player.getData(ModAttachmentTypes.PIGGY_BANK.get());
            for(ItemStack stack : items){
                if(stack.is(ModTags.Items.COINS)){
                   ModUtils.tryPlaceBackItemStackToItemStacks(stack, piggyData.getItems());
                }
            }
            var extraInv = player.getData(ModAttachmentTypes.EXTRA_INVENTORY.get());
            for(int i = 0; i < 4; i++){
                ItemStack stack = extraInv.getCoins(i);
                if(!stack.isEmpty()){
                    ModUtils.tryPlaceBackItemStackToItemStacks(stack, piggyData.getItems());
                }
            }
            int slotIdx = 0;
            boolean dirty = false;
            for (var item : piggyData.getItems()) {
                if (item.is(ModTags.Items.COINS) && item.getCount() == 100) {
                    int index = PlayerUtils.COIN_2_INDEX.applyAsInt(item.getItem()) - 1;
                    if (index >= 0) {
                        piggyData.getItems().set(slotIdx, new ItemStack(PlayerUtils.INDEX_2_COIN.apply(3 - index)));
                        dirty = true;
                    }
                }
                slotIdx++;
            }
            if (dirty) {
                List<ItemStack> coins = new ArrayList<>();
                for (var item : piggyData.getItems()) {
                    if (item.is(ModTags.Items.COINS)) coins.add(item);
                }
                ModUtils.unionItemStacks(coins);
            }
        }
    }

    /**
     * 渲染物品应用的背景
     */
    @OnlyIn(Dist.CLIENT)
    public static void renderApply(AbstractContainerScreen screen, @Nullable Container container, ItemStack stack, GuiGraphics guiGraphics, int x, int y, float partialTick){

        String title = screen.getTitle().toString();
        if((
                ConfluenceHelper.isLoaded() && container instanceof PlayerContainer<?> ||  // 猪猪存钱罐和保险箱
                screen instanceof InventoryScreen || // 背包
                screen instanceof CreativeModeInventoryScreen || // 创造栏
                screen instanceof PotionBagScreen || // 药水袋
                screen instanceof ContainerScreen && (title.contains("enderchest") || title.contains("piggy_bank") || title.contains("safe")) ||
                        ConfluenceHelper.isLoaded() && screen instanceof ExtraInventoryScreen||  // 额外栏
                        CuriosHelper.isLoaded() && screen instanceof CuriosScreen  // 饰品栏
        )
                && canApply(stack)){

            guiGraphics.fillGradient(RenderType.guiOverlay(), x, y, x + 16, y + 16, 0x00200800, 0xf020FFF0, 0);

        }
    }

}
