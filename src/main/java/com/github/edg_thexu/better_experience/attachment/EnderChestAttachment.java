package com.github.edg_thexu.better_experience.attachment;

import com.github.edg_thexu.better_experience.intergration.confluence.ConfluenceHelper;
import com.github.edg_thexu.better_experience.module.autopotion.PlayerInventoryManager;
import com.github.edg_thexu.better_experience.networks.s2c.EnderChestItemsS2C;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.confluence.mod.common.init.ModAttachmentTypes;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

public class EnderChestAttachment implements INBTSerializable<CompoundTag> {

    private List<Item> items = new ArrayList<>();

    public static final Codec<EnderChestAttachment> CODEC = CompoundTag.CODEC.xmap(i->{
        EnderChestAttachment attachment = new EnderChestAttachment();
        attachment.deserializeNBT(null, i);
        return attachment;
    }, i->i.serializeNBT(null));

    public static final StreamCodec<ByteBuf, EnderChestAttachment> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public void refresh(EnderChestAttachment attachment){
        items = new ArrayList<>(attachment.items);
    }
    public List<Item> getItems() {
        return List.copyOf(items);
    }


    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < items.size(); i++) {
            tag.putString("item" + i, BuiltInRegistries.ITEM.getKey(items.get(i)).toString());
        }
        return tag;
    }


    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        items.clear();
        for (int i = 0; i < tag.size(); i++) {
            String key = tag.getString("item" + i);
            if (key.isEmpty()) continue;
            ResourceLocation location = ResourceLocation.tryParse(key);
            if (location != null && BuiltInRegistries.ITEM.containsKey(location)) {
                items.add(BuiltInRegistries.ITEM.get(location));
            }
        }
    }

    private static void sync(ServerPlayer player, List<ItemStack> items, EnderChestItemsS2C.TypeIndex typeIndex){
        EnderChestAttachment att = new EnderChestAttachment();
//        var items = player.getEnderChestInventory().getItems();
        List<Item> itemList = new ArrayList<>();
        for (ItemStack stack : items) {
            if(PlayerInventoryManager.canApply(stack)){
                itemList.add(stack.getItem());
            }
        }
        att.items = itemList;
        PacketDistributor.sendToPlayer(player, new EnderChestItemsS2C(att, typeIndex));
    }

    public static void syncEnderChest(ServerPlayer player){
        sync(player, player.getEnderChestInventory().getItems(), EnderChestItemsS2C.TypeIndex.ENDER);
    }

    public static void syncPig(ServerPlayer player){
        sync(player, player.getData(ModAttachmentTypes.PIGGY_BANK).getItems(),EnderChestItemsS2C.TypeIndex.PIG);
    }

    public static void syncSafe(ServerPlayer player){
        sync(player, player.getData(ModAttachmentTypes.SAFE).getItems(), EnderChestItemsS2C.TypeIndex.SAFE);
    }

    public static void syncAll(ServerPlayer player){
        syncEnderChest(player);
        if(ConfluenceHelper.isLoaded()) {
            syncPig(player);
            syncSafe(player);
        }

    }
}
