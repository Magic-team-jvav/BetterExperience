package com.github.edg_thexu.better_experience.intergration.jei;

import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.registries.recipehandler.IRecipeHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.Map;

public class JeiHelper {
    public static boolean isLoaded() {
        return ModList.get().isLoaded("jei");
    }


    // 服务器接受客户端的handler处理配方信息
    public static void findIngredients(IRecipeHandler handler, Player player){
        if(!CommonConfig.QUICK_JEI_FETCH.get() || !isLoaded()){
            return;
        }
        Level level = player.level();
        SearchCache searchCache = new SearchCache();

        int r = 5;
        for(int i = -r; i <= r; i++){
            for(int j = -r; j <= r; j++){
                for(int k = -r; k <= r; k++) {
                    BlockPos pos = player.blockPosition().offset(i, j, k);
                    searchCache.checkBlock(level, pos);
                }
            }
        }

        List<?> ingredients = handler.getIngredient(level);

        boolean flag = true;
        for(var ing : ingredients){
            if(!handler.getVisitor().visit(level, searchCache, ing, handler)){
                flag = false;
                // 注释掉可以查询所有缺少的材料
//                break;
            }
        }

        if(!flag){
            Map<Ingredient, Integer> requiredCount = searchCache.requiredCount;
            MutableComponent message = Component.literal("");
            for(var entry : requiredCount.entrySet()){
                message.append(entry.getKey().getItems()[0].getDisplayName())
                        .append(" x").append(String.valueOf(entry.getKey().getItems()[0].getCount() * entry.getValue())).append(", ");
            }
            player.sendSystemMessage(Component.translatable("better_experience.info.jei.not_enough_ingredients"));
            player.sendSystemMessage(message);

        }else{
//            player.sendSystemMessage(Component.literal("Found all ingredients"));
            searchCache.markedMap.map.forEach((pos, marked) -> {
                BlockEntity entity = level.getBlockEntity(pos);
                if(entity instanceof Container container){
                    marked.forEach(pair -> {
                        int slot = pair.getA();
                        MutableInt count = pair.getB();
                        ItemStack stack = container.getItem(slot);
                        ItemStack copy = stack.copy();
                        copy.setCount(count.intValue());
                        player.getInventory().placeItemBackInInventory(copy);
                        stack.shrink(count.intValue());
                    });
                }
            });
        }

    }

//    // 客户端点击配方，生成handler并发送给服务器
//    @OnlyIn(Dist.CLIENT)
//    public static void notifyFindIntegrations(List<RecipeLayoutWithButtons<?>> recipeLayoutsWithButtons, double mouseX, double mouseY, int button, int count) {
//
//        if(count <= 0){
//            return;
//        }
//        for( var lay : recipeLayoutsWithButtons){
//            Button button1 = ((IRecipeLayoutWithButtons) (Object) lay).betterExperience$getButton();
//            if(button1 != null && button1.isMouseOver(mouseX, mouseY) && button == 0){
//                button1.onClick(mouseX, mouseY);
//                button1.playDownSound(Minecraft.getInstance().getSoundManager());
//                var recipe = lay.recipeLayout().getRecipe();
//
//                JeiRegistries.RecipeHandlerFactoryProviders.REGISTRY.entrySet().stream()
//                        .filter(h -> h.getValue().match(recipe))
//                        .min(Comparator.comparing(a -> a.getValue().priority()))
//                        .map(Map.Entry::getValue)
//                        .ifPresent(handler-> {
//                            IRecipeHandler<?> handler1 = handler.create((RecipeLayout<?>) lay.recipeLayout(), recipe, count);
//                            if(handler1!= null) {
//                                PacketDistributor.sendToServer(new SearchJeiIngredientsPacketC2S(handler1));
//                            }
//                        });
//
//            }
//        }
//    }
//
//    @OnlyIn(Dist.CLIENT)
//    public static void updatePos(RecipeLayoutWithButtons<?> recipeLayoutWithButtons){
//        Button button = ((IRecipeLayoutWithButtons) (Object) recipeLayoutWithButtons).betterExperience$getButton();
//        if(button != null) {
//            Rect2i rect = recipeLayoutWithButtons.recipeLayout().getRect();
//            button.setPosition(
//                    recipeLayoutWithButtons.recipeLayout().getRect().getX() + rect.getWidth() + 6,
//                    rect.getY());
//        }
//
//    }
}
