package com.github.edg_thexu.better_experience.mixin.integration.confluence;

import com.github.edg_thexu.better_experience.module.reforge.BetterReforgeManager;
import com.github.edg_thexu.better_experience.module.reforge.ReforgeAccessories;
import com.github.edg_thexu.better_experience.client.gui.ReforgeHistoryPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.confluence.mod.common.init.ModSoundEvents;
import org.confluence.mod.common.menu.NPCReforgeMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.confluence.mod.client.gui.container.npc_screen.NPCReforgeScreen;

import java.util.Deque;

@Mixin(NPCReforgeScreen.class)
public abstract class NPCReforgeScreenMixin extends AbstractContainerScreen<NPCReforgeMenu> {
    @Unique private ReforgeHistoryPanel betterExperience$queue;

    /** Original popup stack: results are still collected there, but the queue replaces its rendering. */
    @Final @Shadow private Deque<?> prefixPopups;
    /** Hammer swing state, driven by the same values the original screen uses for a rolled prefix. */
    @Shadow private int hammerSwingAge;
    @Shadow private boolean hammerImpactPending;
    @Shadow private int bestPrefixPauseTicks;

    protected NPCReforgeScreenMixin(NPCReforgeMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        BetterReforgeManager.init((NPCReforgeScreen) (Object) this);
        if (betterExperience$queue == null) betterExperience$queue = new ReforgeHistoryPanel(menu);
    }

    @Inject(method = "containerTick", at = @At("RETURN"))
    private void tickQueue(CallbackInfo ci) {
        if (betterExperience$queue == null) return;
        betterExperience$queue.tick();
        if (prefixPopups != null) prefixPopups.clear();
    }

    @Inject(method = "renderBg", at = @At("RETURN"))
    private void renderQueue(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (betterExperience$queue != null) betterExperience$queue.render(graphics, leftPos, topPos, mouseX, mouseY, partialTick);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void queueClick(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        if (betterExperience$queue == null || !betterExperience$queue.click(x, y, button)) return;
        if (betterExperience$queue.consumeReforgeRequest()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSoundEvents.ITEM_REFORGE.get(), 1.0F));
            hammerSwingAge = 0;
            hammerImpactPending = true;
        }
        cir.setReturnValue(true);
    }

    /** 饰品不在 Confluence 的重铸标签里，原版的 canReforge 会因为 couldReforge 为 false 拦掉。 */
    @Inject(method = "canReforge", at = @At("RETURN"), cancellable = true)
    private void canReforgeAccessory(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!ReforgeAccessories.isReforgeable(menu.getReforgeItem())) return;
        if (!menu.getCarried().isEmpty() || menu.getCooldownTicks() != 0 || bestPrefixPauseTicks != 0) return;
        int cost = menu.getCost();
        cir.setReturnValue(cost < 0x3F3F3F3F && menu.getAvailableMoney() >= cost);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (betterExperience$queue != null && betterExperience$queue.scroll(vertical, x, y)) return true;
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
}

