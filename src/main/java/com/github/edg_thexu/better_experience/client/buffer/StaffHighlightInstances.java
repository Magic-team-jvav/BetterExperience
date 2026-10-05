package com.github.edg_thexu.better_experience.client.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.List;

/** Uploads two RGBA texels per box; the GPU expands each instance into 12 triangles. */
final class StaffHighlightInstances implements AutoCloseable {
    private int texture;
    private int vertexArray;
    private int count;
    private BlockPos origin;

    void upload(List<AABB> boxes, BlockPos origin) {
        RenderSystem.assertOnRenderThread();
        if (texture == 0) texture = GlStateManager._genTexture();
        if (vertexArray == 0) vertexArray = GL30.glGenVertexArrays();
        this.origin = origin.immutable();
        count = boxes.size();
        int width = Math.min(1024, count * 2);
        int height = (count * 2 + width - 1) / width;
        FloatBuffer data = MemoryUtil.memCallocFloat(width * height * 4);
        int previousUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            for (AABB box : boxes) {
                data.put((float) (box.minX - origin.getX())).put((float) (box.minY - origin.getY()))
                        .put((float) (box.minZ - origin.getZ())).put(0);
                data.put((float) (box.maxX - origin.getX())).put((float) (box.maxY - origin.getY()))
                        .put((float) (box.maxZ - origin.getZ())).put(0);
            }
            data.rewind();
            RenderSystem.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            StaffTextureUpload.upload(width, height, data);
        } finally {
            RenderSystem.bindTexture(previousTexture);
            RenderSystem.activeTexture(previousUnit);
            MemoryUtil.memFree(data);
        }
    }

    void render(Matrix4f matrix, Vec3 camera, int r, int g, int b, float opacityScale) {
        var shader = StaffHighlightShader.get();
        if (shader == null || count == 0) return;
        Matrix4f modelView = modelViewFor(RenderSystem.getModelViewMatrix(), matrix, camera, origin);
        int previousArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        var previousShader = RenderSystem.getShader();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            RenderSystem.setShader(StaffHighlightShader::get);
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, modelView,
                    RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.setSampler("BoundsSampler", texture);
            shader.safeGetUniform("HighlightColor").set(r / 255f, g / 255f, b / 255f, opacityScale);
            shader.safeGetUniform("PulseTime").set((Util.getMillis() % 1200) / 1000f);
            shader.apply();
            GL30.glBindVertexArray(vertexArray);
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, 36, count);
        } finally {
            shader.clear();
            GL30.glBindVertexArray(previousArray);
            RenderSystem.bindTexture(previousTexture);
            RenderSystem.activeTexture(previousUnit);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    static Matrix4f modelViewFor(Matrix4f globalView, Matrix4f pose, Vec3 camera, BlockPos origin) {
        // Match BufferUploader: global view * baked pose * camera-relative box coordinates.
        return new Matrix4f(globalView).mul(pose).translate((float) (origin.getX() - camera.x),
                (float) (origin.getY() - camera.y), (float) (origin.getZ() - camera.z));
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (texture != 0) GlStateManager._deleteTexture(texture);
        if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
        texture = 0;
        vertexArray = 0;
        count = 0;
        origin = null;
    }
}
