package com.github.edg_thexu.better_experience.attachment;

import com.github.edg_thexu.better_experience.networks.c2s.PotionApplyPacketC2S;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;

public class AutoPotionAttachment implements INBTSerializable<CompoundTag> {

    Map<Holder<MobEffect>, Integer> potions = new HashMap<>();
    Map<Holder<MobEffect>, Integer> _potions = new HashMap<>();
    // client-side only
    HashSet<Holder<MobEffect>> forbiddens = new HashSet<>();
    boolean dirty = false;

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        List<String> list = potions.keySet().stream().map(integer -> BuiltInRegistries.MOB_EFFECT.getKey(integer.value()).toString()).toList();
        List<Integer> amps = potions.values().stream().toList();
        int size = list.size();
        tag.putInt("size", size);
        for (int i = 0; i < size; i++) {
            tag.putString("potion" + i, list.get(i));
            tag.putInt("amp" + i, amps.get(i));
        }
        tag.putInt("forbiddenSize", forbiddens.size());
        List<Holder<MobEffect>> forbiddenList = forbiddens.stream().toList();
        for (int i = 0; i < forbiddens.size(); i++) {
            ResourceLocation location = BuiltInRegistries.MOB_EFFECT.getKey(forbiddenList.get(i).value());
            if (location == null) {
                continue;
            }
            tag.putString("forbidden" + i, location.toString());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        int size = Math.clamp(tag.getInt("size"), 0, BuiltInRegistries.MOB_EFFECT.size());
        potions = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String potion = tag.getString("potion" + i);
            int amp = tag.getInt("amp" + i);
            ResourceLocation location = ResourceLocation.tryParse(potion);
            if (location == null) {
                continue;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(location);
            if (effect == null) {
                continue;
            }
            potions.put(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), amp);
        }
        forbiddens.clear();
        int forbiddenSize = Math.clamp(tag.getInt("forbiddenSize"), 0, BuiltInRegistries.MOB_EFFECT.size());
        for (int i = 0; i < forbiddenSize; i++) {
            String potion = tag.getString("forbidden" + i);
            ResourceLocation location = ResourceLocation.tryParse(potion);
            if (location == null) {
                continue;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(location);
            if (effect == null) {
                continue;
            }
            forbiddens.add(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
        }
    }

    public void removePotion(Holder<MobEffect> potion) {
        if(potions.remove(potion)!= null) {
            dirty = true;
        }
    }

    public void addForbidden(Holder<MobEffect> potion) {
        if(forbiddens.add(potion)){
            dirty = true;
        }
    }

    public void removeForbidden(Holder<MobEffect> potion) {
        if(forbiddens.remove(potion)){
            dirty = true;
        }
    }

    public boolean isForbidden(Holder<MobEffect> potion) {
        return forbiddens.contains(potion);
    }

    public void addPotion(Holder<MobEffect> potion, int amp) {
        if(potions.containsKey(potion)) {
            if(potions.get(potion) < amp){
                potions.put(potion, amp);
                dirty = true;
            }
        }else{
            potions.put(potion, amp);
            dirty = true;
        }
    }

    public Map<Holder<MobEffect>, Integer> getPotions() {
        return potions;
    }

    public Map<Holder<MobEffect>, Integer> getCachedPotions() {
        return _potions;
    }

    public void sync(){
        sync(false);
    }

    public void sync(boolean force) {
        if (!force && !dirty) return;
        AutoPotionAttachment preferences = new AutoPotionAttachment();
        preferences.forbiddens = new HashSet<>(forbiddens);
        PacketDistributor.sendToServer(new PotionApplyPacketC2S(preferences));
        dirty = false;
    }

    public void copyPreferences(AutoPotionAttachment attachment) {
        this.forbiddens = new HashSet<>(attachment.forbiddens);
    }

    public void copyFromServer(AutoPotionAttachment attachment) {
        copyPreferences(attachment);
        this.potions = new HashMap<>(attachment.potions);
        this._potions = new HashMap<>(attachment.potions);
    }

}
