package com.github.edg_thexu.better_experience.mixin.accessor;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public interface PreviewRenderTypeAccess {
    @Invoker("state") RenderType.CompositeState betterExperience$state();
}
