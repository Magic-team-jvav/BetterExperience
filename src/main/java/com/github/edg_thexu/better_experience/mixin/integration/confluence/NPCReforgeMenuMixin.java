package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.config.CommonConfig;
import com.github.edg_thexu.better_experience.init.ModAttachments;
import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.module.reforge.BetterReforgeManager;
import com.github.edg_thexu.better_experience.module.reforge.ReforgeAccessories;
import com.github.edg_thexu.better_experience.module.reforge.ReforgeHistoryManager;
import com.github.edg_thexu.better_experience.networks.s2c.ClientBoundConfigPacket;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.confluence.mod.common.component.prefix.PrefixType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.confluence.mod.common.menu.NPCReforgeMenu")
//@Mixin(NPCReforgeMenu.class)
public abstract class NPCReforgeMenuMixin extends AbstractContainerMenu {
    @Inject(method = "clickMenuButton", at = @At(value = "INVOKE",
            target = "Lorg/confluence/mod/util/PrefixUtils;setAndUpdate(Lnet/minecraft/world/item/ItemStack;Lorg/confluence/mod/common/component/prefix/PrefixType;Lorg/confluence/mod/common/component/prefix/ModPrefix;)Lorg/confluence/mod/common/component/prefix/PrefixComponent;",
            shift = At.Shift.AFTER))
    private void recordHistory(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!player.level().isClientSide) {
            ReforgeHistoryManager.record(((org.confluence.mod.common.menu.NPCReforgeMenu) (Object) this).getReforgeItem());
        }
    }

    @Inject(method = "pollReforgeResult", at = @At("RETURN"))
    private void captureRolledPrefix(CallbackInfoReturnable<org.confluence.mod.common.menu.NPCReforgeMenu.ReforgeResult> cir) {
        org.confluence.mod.common.menu.NPCReforgeMenu.ReforgeResult result = cir.getReturnValue();
        if (result != null) ReforgeHistoryManager.pushRolled(result.item());
    }

    /** Curios 饰品不在 Confluence 的重铸标签里，这里把它们当作可重铸物品处理。 */
    @WrapOperation(method = {"slotsChanged", "clickMenuButton", "refreshCost", "quickMoveStack"},
            at = @At(value = "INVOKE", target = "Lorg/confluence/mod/util/PrefixUtils;couldReforge(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean betterExperience$allowAccessoryReforge(ItemStack stack, Operation<Boolean> original) {
        return original.call(stack) || ReforgeAccessories.isReforgeable(stack);
    }

    @WrapOperation(method = "slotsChanged",
            at = @At(value = "INVOKE", target = "Lorg/confluence/mod/util/PrefixUtils;getPrefixType(Lnet/minecraft/world/item/ItemStack;)Lorg/confluence/mod/common/component/prefix/PrefixType;"))
    private static PrefixType betterExperience$accessoryPrefixType(ItemStack stack, Operation<PrefixType> original) {
        PrefixType type = original.call(stack);
        return type == PrefixType.UNKNOWN && ReforgeAccessories.isAccessory(stack) ? PrefixType.ACCESSORY : type;
    }

    @Final
    @Shadow
    private int[] data;

    @Shadow @Final private Player player;

    protected NPCReforgeMenuMixin(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);

    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private static void init(int containerId, Inventory inventory, CallbackInfo ci) {
        Player player1 = inventory.player;
        if(player1 instanceof ServerPlayer serverPlayer){
            PacketDistributor.sendToPlayer(serverPlayer,
                    serverPlayer.getData(ModAttachments.TEMP_DATA).enableBetterReforge()?
                            new ClientBoundConfigPacket(2) :
                            new ClientBoundConfigPacket(3));
        }
    }


    @Inject(method = "slotsChanged", at = @At(value = "INVOKE", target = "Lorg/confluence/mod/util/PrefixUtils;getReforgeCost(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)I"))
    private void slotsChangedMixin(Container container, CallbackInfo ci, @Local PrefixType prefixType, @Local ItemStack itemStack) {

        if(CommonConfig.BETTER_REINFORCED_TOOL.get() && ConfluenceHelper.isLoaded()
                && this.player.getData(ModAttachments.TEMP_DATA).enableBetterReforge()) {
            try {
                data[1] = BetterReforgeManager.getBetterPrefix(prefixType, itemStack);
            } catch (RuntimeException ignore) {
            }
        }
    }



}
