package com.github.edg_thexu.better_experience.client.buffer;

import com.github.edg_thexu.better_experience.data.component.SpaceStaffSettings;
import com.github.edg_thexu.better_experience.data.component.StaffSelectionShape;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import com.github.edg_thexu.better_experience.module.spacestaff.SpaceOutline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Shape guides remain on the GPU until their geometry or lock color changes. */
final class SpaceSelectionOutline {
    private VertexBuffer vertices;
    private StaffSelectionBounds cachedBounds;
    private StaffSelectionShape cachedSelection;
    private SpaceStaffSettings cachedSettings;
    private boolean cachedLocked;

    void clear() {
        if (vertices != null) vertices.close();
        vertices = null;
        cachedBounds = null;
        cachedSelection = null;
        cachedSettings = null;
    }

    void render(StaffSelectionBounds bounds, StaffSelectionShape selection, SpaceStaffSettings settings,
                boolean locked, Matrix4f pose, Vec3 camera) {
        if (!bounds.equals(cachedBounds) || !selection.equals(cachedSelection) || !settings.equals(cachedSettings)
                || locked != cachedLocked) {
            var lines = SpaceOutline.segments(bounds, selection, settings);
            if (vertices == null) vertices = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            Vec3 origin = Vec3.atLowerCornerOf(bounds.min());
            for (var line : lines) {
                int[] color = switch (line.guide()) {
                    case CENTER -> new int[]{255, 255, 255};
                    case HORIZONTAL_RADIUS -> new int[]{70, 255, 100};
                    case VERTICAL_RADIUS -> new int[]{100, 170, 255};
                    case CONTROL -> new int[]{255, 100, 220};
                    case SHAPE -> locked ? new int[]{60, 230, 255} : new int[]{255, 190, 35};
                };
                vertex(buffer, line.from().subtract(origin), color);
                vertex(buffer, line.to().subtract(origin), color);
            }
            vertices.bind();
            try { vertices.upload(buffer.buildOrThrow()); }
            finally { VertexBuffer.unbind(); }
            cachedBounds = bounds;
            cachedSelection = selection;
            cachedSettings = settings;
            cachedLocked = locked;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            vertices.bind();
            vertices.drawWithShader(StaffHighlightInstances.modelViewFor(RenderSystem.getModelViewMatrix(), pose, camera, bounds.min()),
                    RenderSystem.getProjectionMatrix(), GameRenderer.getPositionColorShader());
        } finally {
            VertexBuffer.unbind();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private static void vertex(BufferBuilder buffer, Vec3 point, int[] color) {
        buffer.addVertex((float) point.x, (float) point.y, (float) point.z).setColor(color[0], color[1], color[2], 240);
    }
}
