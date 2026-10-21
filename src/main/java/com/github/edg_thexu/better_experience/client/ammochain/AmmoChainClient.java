package com.github.edg_thexu.better_experience.client.ammochain;

import com.github.edg_thexu.better_experience.client.gui.AmmoChainScreen;
import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;
import com.github.edg_thexu.better_experience.item.AmmoChain;
import net.minecraft.client.Minecraft;

public final class AmmoChainClient {
    private AmmoChainClient() {}

    public static void open(int slot, AmmoChainSettings settings) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || slot < 0
                || slot >= minecraft.player.getInventory().getContainerSize()
                || !(minecraft.player.getInventory().getItem(slot).getItem() instanceof AmmoChain)) return;
        minecraft.setScreen(new AmmoChainScreen(slot, settings));
    }
}
