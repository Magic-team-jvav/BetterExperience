package com.github.edg_thexu.better_experience.networks;

import com.github.edg_thexu.better_experience.intergration.jei.JeiHelper;
import com.github.edg_thexu.better_experience.networks.c2s.BreakBlocksPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.AmmoChainSettingsPacketC2S;
import com.github.edg_thexu.better_experience.networks.s2c.AmmoChainOpenPacket;
import com.github.edg_thexu.better_experience.networks.c2s.FluidStaffSettingsPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.FluidStaffActionPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.PlaceBlocksPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.PotionApplyPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.SearchJeiIngredientsPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.ServerBoundPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.SpaceBezierSelectionPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.SpaceStaffSettingsPacketC2S;
import com.github.edg_thexu.better_experience.networks.c2s.StaffSelectionPacketC2S;
import com.github.edg_thexu.better_experience.networks.s2c.ClientBoundConfigPacket;
import com.github.edg_thexu.better_experience.networks.s2c.EnderChestItemsS2C;
import com.github.edg_thexu.better_experience.networks.s2c.SyncDataS2C;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.github.edg_thexu.better_experience.networks.c2s.LocatorTargetPacketC2S;
import com.github.edg_thexu.better_experience.networks.s2c.LocatorCatalogPacket;
import com.github.edg_thexu.better_experience.networks.s2c.LocatorMarkersPacket;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


public final class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("9");
        registrar.playToServer(AmmoChainSettingsPacketC2S.TYPE, AmmoChainSettingsPacketC2S.STREAM_CODEC, AmmoChainSettingsPacketC2S::handle);
        registrar.playToClient(AmmoChainOpenPacket.TYPE, AmmoChainOpenPacket.STREAM_CODEC, AmmoChainOpenPacket::handle);
        registrar.playBidirectional(PotionApplyPacketC2S.TYPE, PotionApplyPacketC2S.STREAM_CODEC, PotionApplyPacketC2S::handle);
        registrar.playToServer(LocatorTargetPacketC2S.TYPE, LocatorTargetPacketC2S.STREAM_CODEC, LocatorTargetPacketC2S::handle);
        registrar.playToClient(LocatorCatalogPacket.TYPE, LocatorCatalogPacket.STREAM_CODEC, LocatorCatalogPacket::handle);
        registrar.playToClient(LocatorMarkersPacket.TYPE, LocatorMarkersPacket.STREAM_CODEC, LocatorMarkersPacket::handle);
        registrar.playToServer(FluidStaffSettingsPacketC2S.TYPE, FluidStaffSettingsPacketC2S.STREAM_CODEC, FluidStaffSettingsPacketC2S::handle);
        registrar.playToServer(FluidStaffActionPacketC2S.TYPE, FluidStaffActionPacketC2S.STREAM_CODEC, FluidStaffActionPacketC2S::handle);
        registrar.playToServer(PlaceBlocksPacketC2S.TYPE, PlaceBlocksPacketC2S.STREAM_CODEC, PlaceBlocksPacketC2S::handle);
        registrar.playToServer(SpaceStaffSettingsPacketC2S.TYPE, SpaceStaffSettingsPacketC2S.STREAM_CODEC, SpaceStaffSettingsPacketC2S::handle);
        registrar.playToServer(SpaceBezierSelectionPacketC2S.TYPE, SpaceBezierSelectionPacketC2S.STREAM_CODEC, SpaceBezierSelectionPacketC2S::handle);
        registrar.playToServer(BreakBlocksPacketC2S.TYPE, BreakBlocksPacketC2S.STREAM_CODEC, BreakBlocksPacketC2S::handle);
        registrar.playToServer(StaffSelectionPacketC2S.TYPE, StaffSelectionPacketC2S.STREAM_CODEC, StaffSelectionPacketC2S::handle);
        registrar.playToServer(ServerBoundPacketC2S.TYPE, ServerBoundPacketC2S.STREAM_CODEC, ServerBoundPacketC2S::handle);
        if(JeiHelper.isLoaded()) {
            registrar.playToServer(SearchJeiIngredientsPacketC2S.TYPE, SearchJeiIngredientsPacketC2S.STREAM_CODEC, SearchJeiIngredientsPacketC2S::handle);
        }

        registrar.playToClient(EnderChestItemsS2C.TYPE, EnderChestItemsS2C.STREAM_CODEC, EnderChestItemsS2C::handle);
        registrar.playToClient(ClientBoundConfigPacket.TYPE, ClientBoundConfigPacket.STREAM_CODEC, ClientBoundConfigPacket::handle);
        registrar.playToClient(SyncDataS2C.TYPE, SyncDataS2C.STREAM_CODEC, SyncDataS2C::handle);
    }
}
