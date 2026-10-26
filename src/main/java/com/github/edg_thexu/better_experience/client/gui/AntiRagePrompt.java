package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.networks.c2s.AntiRageSettingsPacket;
import com.github.edg_thexu.better_experience.networks.s2c.AntiRageSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.confluence.mod.client.gui.AskForSoftcoreScreen;

@EventBusSubscriber(modid = Better_experience.MODID, value = Dist.CLIENT)
public final class AntiRagePrompt {
    private AntiRagePrompt() {}

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AskForSoftcoreScreen screen)) return;
        int top = (screen.height - 110) / 2 - 33;
        event.addListener(new Toggle((screen.width - 190) / 2, Math.min(screen.height - 24, top + 176)));
        if (Minecraft.getInstance().player != null) PacketDistributor.sendToServer(new AntiRageSettingsPacket(false, false));
    }

    public static void receive(AntiRageSnapshotPacket packet) {
        if (!(Minecraft.getInstance().screen instanceof AskForSoftcoreScreen screen)) return;
        for (var child : screen.children()) {
            if (child instanceof Toggle toggle) toggle.accept(packet);
        }
    }

    private static final class Toggle extends Button {
        private boolean enabled;

        Toggle(int x, int y) {
            super(x, y, 190, 20, Component.translatable("better_experience.spawning.loading"), ignored -> {}, DEFAULT_NARRATION);
            active = false;
        }

        @Override
        public void onPress() {
            active = false;
            PacketDistributor.sendToServer(new AntiRageSettingsPacket(true, !enabled));
        }

        void accept(AntiRageSnapshotPacket packet) {
            enabled = packet.enabled();
            active = packet.editable();
            setMessage(Component.translatable("better_experience.anti_rage.prompt.toggle", Component.translatable(enabled ? "options.on" : "options.off")));
            setTooltip(Tooltip.create(Component.translatable(packet.editable() ? "better_experience.anti_rage.prompt.help" : "better_experience.anti_rage.prompt.read_only")));
        }
    }
}
