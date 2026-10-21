package com.github.edg_thexu.better_experience.networks.c2s;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.data.component.AmmoChainCursor;
import com.github.edg_thexu.better_experience.data.component.AmmoChainSettings;
import com.github.edg_thexu.better_experience.init.ModDataComponentTypes;
import com.github.edg_thexu.better_experience.item.AmmoChain;
import com.github.edg_thexu.better_experience.module.ammochain.AmmoChainManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AmmoChainSettingsPacketC2S(int slot, AmmoChainSettings settings) implements CustomPacketPayload {
    public static final Type<AmmoChainSettingsPacketC2S> TYPE = new Type<>(Better_experience.space("ammo_chain_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AmmoChainSettingsPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AmmoChainSettingsPacketC2S::slot, AmmoChainSettings.STREAM_CODEC,
            AmmoChainSettingsPacketC2S::settings, AmmoChainSettingsPacketC2S::new);
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(AmmoChainSettingsPacketC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.isSpectator() || packet.slot < 0 || packet.slot >= player.getInventory().items.size() && packet.slot != 40) return;
            var stack = player.getInventory().getItem(packet.slot);
            if (!(stack.getItem() instanceof AmmoChain)) return;
            var previous = stack.getOrDefault(ModDataComponentTypes.AMMO_CHAIN_SETTINGS.get(), AmmoChainSettings.defaults());
            for (var entry : packet.settings.entries()) {
                if (entry.isEmpty()) continue;
                var ammo = entry.ammo();
                if (!AmmoChainManager.isAmmo(ammo)) return;
                boolean owned = player.getInventory().items.stream().anyMatch(item -> !item.isEmpty() && ItemStack.isSameItemSameComponents(item, ammo));
                boolean retained = previous.entries().stream().anyMatch(item -> ItemStack.isSameItemSameComponents(item.ammo(), ammo));
                if (!owned && !retained) return;
            }
            stack.set(ModDataComponentTypes.AMMO_CHAIN_SETTINGS.get(), packet.settings);
            stack.set(ModDataComponentTypes.AMMO_CHAIN_CURSOR.get(), AmmoChainCursor.DEFAULT);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
    }
}
