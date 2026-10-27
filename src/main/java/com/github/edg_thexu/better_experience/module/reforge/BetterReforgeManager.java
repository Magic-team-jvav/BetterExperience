package com.github.edg_thexu.better_experience.module.reforge;

import com.github.edg_thexu.better_experience.client.gui.widget.FloatButton;
import com.github.edg_thexu.better_experience.init.ModAttachments;
import com.github.edg_thexu.better_experience.mixin.accessor.ScreenAccessor;
import com.github.edg_thexu.better_experience.networks.c2s.ServerBoundPacketC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.confluence.mod.client.gui.container.npc_screen.NPCReforgeScreen;
import org.confluence.mod.common.component.prefix.ModPrefix;
import org.confluence.mod.common.component.prefix.PrefixComponent;
import org.confluence.mod.common.component.prefix.PrefixType;
import org.confluence.mod.common.init.ModDataComponentTypes;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BetterReforgeManager {

    public static int getBetterPrefix(PrefixType prefixType, ItemStack itemStack){

        var available = prefixType.getAvailable();
        PrefixComponent cp = itemStack.get(ModDataComponentTypes.PREFIX);
        if (cp == null) {
            throw new IllegalStateException(itemStack + "不能获得前缀");
        }
        ModPrefix current = java.util.Arrays.stream(available)
                .filter(prefix -> prefix.name().equals(cp.name())).findFirst()
                .orElseGet(() -> ModPrefix.ID_MAP.values().stream()
                        .filter(prefix -> prefix.name().equals(cp.name())).findFirst().orElse(null));
        float originalValue = current == null ? 0.0F : current.value();
        List<Integer> availableList = new ArrayList<>();
        for (ModPrefix prefix : available) {
            Integer id = ModPrefix.ID_MAP.inverse().get(prefix);
            if (id == null || prefix.value() < originalValue) continue;
            availableList.add(id);
        }
        // 当前词条已经是最优时没有任何候选，退回当前词条，避免 getFirst() 抛异常让这次重铸直接失败。
        if (availableList.isEmpty()) {
            Integer id = current == null ? null : ModPrefix.ID_MAP.inverse().get(current);
            if (id != null) return id;
            for (ModPrefix prefix : available) {
                Integer fallback = ModPrefix.ID_MAP.inverse().get(prefix);
                if (fallback != null) return fallback;
            }
            throw new IllegalStateException(itemStack + "不能获得前缀");
        }
        Collections.shuffle(availableList);
        return availableList.getFirst();
    }

    @OnlyIn(Dist.CLIENT)
    public static void init(NPCReforgeScreen screen){
        FloatButton button = new FloatButton((FloatButton.Builder) FloatButton.builder(Component.literal("B"), (p)->{
            PacketDistributor.sendToServer(new ServerBoundPacketC2S(6));
        }).tooltip(Tooltip.create(Component.translatable("better_experience.tooltip.better_reforge.enable"))).bounds(screen.getGuiLeft() + 4, screen.getGuiTop() + 99, 28, 16));
        ((ScreenAccessor)screen).callAddRenderableWidget(button);
        button.setSelectedDirectly(Minecraft.getInstance().player.getData(ModAttachments.TEMP_DATA).enableBetterReforge());

    }
}
