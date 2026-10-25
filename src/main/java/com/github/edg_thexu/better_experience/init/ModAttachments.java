package com.github.edg_thexu.better_experience.init;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.attachment.AutoPotionAttachment;
import com.github.edg_thexu.better_experience.attachment.EnderChestAttachment;
import com.github.edg_thexu.better_experience.attachment.TempAttachment;
import com.github.edg_thexu.better_experience.module.antirage.AntiRageState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Better_experience.MODID);

    public static final Supplier<AttachmentType<EnderChestAttachment>> ENDER_CHEST = TYPES.register("ender_chest", () -> AttachmentType.serializable(EnderChestAttachment::new).copyOnDeath().build());
    public static final Supplier<AttachmentType<AutoPotionAttachment>> AUTO_POTION = TYPES.register("auto_potion", () -> AttachmentType.serializable(AutoPotionAttachment::new).copyOnDeath().build());
    public static final Supplier<AttachmentType<EnderChestAttachment>> PIG_CHEST = TYPES.register("pig_chest", () -> AttachmentType.serializable(EnderChestAttachment::new).copyOnDeath().build());
    public static final Supplier<AttachmentType<EnderChestAttachment>> SAFE_CHEST = TYPES.register("safe_chest", () -> AttachmentType.serializable(EnderChestAttachment::new).copyOnDeath().build());
    public static final Supplier<AttachmentType<TempAttachment>> TEMP_DATA = TYPES.register("temp_data", () -> AttachmentType.serializable(TempAttachment::new).copyOnDeath().build());
    public static final Supplier<AttachmentType<AntiRageState>> ANTI_RAGE = TYPES.register("anti_rage", () -> AttachmentType.serializable(AntiRageState::new).copyOnDeath().build());


}
