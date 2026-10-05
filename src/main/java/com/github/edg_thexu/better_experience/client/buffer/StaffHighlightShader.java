package com.github.edg_thexu.better_experience.client.buffer;

import com.github.edg_thexu.better_experience.Better_experience;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/** Reloaded and closed by Minecraft together with the other core shaders. */
public final class StaffHighlightShader {
    private static ShaderInstance shader;

    private StaffHighlightShader() {}

    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Better_experience.MODID, "staff_highlight"),
                DefaultVertexFormat.POSITION), loaded -> shader = loaded);
    }

    static ShaderInstance get() { return shader; }
}
