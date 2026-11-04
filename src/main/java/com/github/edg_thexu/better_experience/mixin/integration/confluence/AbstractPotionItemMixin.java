package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.module.potionbag.PotionBagQuickUse;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.IdentityHashMap;

@Pseudo
@Mixin(targets = "org.confluence.mod.common.item.potion.AbstractPotionItem")
public abstract class AbstractPotionItemMixin {
    @WrapOperation(method = "use(Lnet/minecraft/world/entity/player/Player;FLjava/lang/Class;Ljava/util/function/ToIntFunction;)V",
            at = @At(value = "INVOKE", target = "Lorg/confluence/mod/common/item/storage/VoidBagItem;accessibleItems(Lnet/minecraft/world/entity/player/Player;)Ljava/lang/Iterable;"))
    private static Iterable<ItemStack> betterExperience$bagPotions(Player player, Operation<Iterable<ItemStack>> original,
            @Share("bagSources") LocalRef<IdentityHashMap<ItemStack, ItemStack>> sources) {
        var map = new IdentityHashMap<ItemStack, ItemStack>();
        sources.set(map);
        return PotionBagQuickUse.candidates(player, original.call(player), map);
    }

    @WrapOperation(method = "use(Lnet/minecraft/world/entity/player/Player;FLjava/lang/Class;Ljava/util/function/ToIntFunction;)V",
            at = @At(value = "INVOKE", target = "Lorg/confluence/mod/common/item/potion/AbstractPotionItem;finishQuickUse(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V"))
    private static void betterExperience$consumeBagPotion(ItemStack stack, Player player, Operation<Void> original,
            @Share("bagSources") LocalRef<IdentityHashMap<ItemStack, ItemStack>> sources) {
        ItemStack bag = sources.get() == null ? null : sources.get().get(stack);
        if (bag == null) original.call(stack, player);
        else PotionBagQuickUse.consume(player, bag, stack);
    }

    @Shadow
    public abstract ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity living);

    @WrapOperation(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;startUsingInstantly(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;"))
    private InteractionResultHolder<ItemStack> instantly(Level level, Player player, InteractionHand hand, Operation<InteractionResultHolder<ItemStack>> original) {
        if (CommonConfig.INSTANTLY_DRINK.get()) {
            return InteractionResultHolder.consume(finishUsingItem(player.getItemInHand(hand), level, player));
        }
        return original.call(level, player, hand);
    }
}
