package com.github.edg_thexu.better_experience.client.buffer;

import com.github.edg_thexu.better_experience.client.StaffSelectionHandler;
import com.github.edg_thexu.better_experience.config.ClientConfig;
import com.github.edg_thexu.better_experience.item.SpaceStaff;
import com.github.edg_thexu.better_experience.module.boomstaff.StaffSelectionBounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Translucent volume and solid edge bars, visible through blocks. */
@OnlyIn(Dist.CLIENT)
public final class AABBBuffer {
    private static final AABBBuffer INSTANCE = new AABBBuffer();
    private final SpaceSelectionOutline spaceOutline = new SpaceSelectionOutline();
    private final StaffBlockHighlight blockHighlight = new StaffBlockHighlight();
    private StaffSelectionBounds lastLockedBounds;
    private long highlightResumeMillis;
    private static final float EDGE_HALF_WIDTH = .025f;
    private static final int[][] FACES = {{0,1,2,3}, {4,7,6,5}, {0,3,7,4}, {1,5,6,2}, {0,4,5,1}, {3,2,6,7}};

    public static AABBBuffer getInstance() { return INSTANCE; }

    public void render(RenderLevelStageEvent event) {
        if (!ClientConfig.SHOW_OUTLINES.get()) {
            clearBlockHighlight();
            spaceOutline.clear();
            return;
        }
        var bounds = StaffSelectionHandler.preview();
        if (bounds == null) {
            clearBlockHighlight();
            spaceOutline.clear();
            return;
        }
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        // Subtract camera coordinates before converting to float.
        float x1 = (float) (bounds.min().getX() - camera.x - .015);
        float y1 = (float) (bounds.min().getY() - camera.y - .015);
        float z1 = (float) (bounds.min().getZ() - camera.z - .015);
        float x2 = (float) (bounds.max().getX() + 1 - camera.x + .015);
        float y2 = (float) (bounds.max().getY() + 1 - camera.y + .015);
        float z2 = (float) (bounds.max().getZ() + 1 - camera.z + .015);
        boolean locked = StaffSelectionHandler.isLocked();
        int r = locked ? 60 : 255;
        int g = locked ? 230 : 190;
        int b = locked ? 255 : 35;
        Matrix4f matrix = event.getPoseStack().last().pose();
        if (locked) {
            long now = Util.getMillis();
            if (!bounds.equals(lastLockedBounds)) {
                lastLockedBounds = bounds;
                highlightResumeMillis = now + StaffSelectionHandler.HIGHLIGHT_SETTLE_MILLIS;
            }
            // Keep only the outer selection frame while scrolling or repositioning.
            // Do not scan blocks, rebuild meshes, or draw an old mesh during movement.
            if (Minecraft.getInstance().screen == null && now >= highlightResumeMillis
                    && !StaffSelectionHandler.isLockedSelectionMoving())
                blockHighlight.render(bounds, matrix, camera, r, g, b);
        } else clearBlockHighlight();
        var held = Minecraft.getInstance().player.getMainHandItem();
        if (held.getItem() instanceof SpaceStaff) {
            var settings = StaffSelectionHandler.previewSettings();
            spaceOutline.render(bounds, StaffSelectionHandler.previewShape(bounds), settings, locked, matrix, camera);
            return;
        }
        spaceOutline.clear();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        box(buffer, matrix, x1, y1, z1, x2, y2, z2, r, g, b, 22);
        float w = EDGE_HALF_WIDTH;
        for (float y : new float[]{y1, y2}) {
            for (float z : new float[]{z1, z2})
                box(buffer, matrix, x1 - w, y - w, z - w, x2 + w, y + w, z + w, r, g, b, 240);
        }
        for (float x : new float[]{x1, x2}) {
            for (float z : new float[]{z1, z2})
                box(buffer, matrix, x - w, y1 - w, z - w, x + w, y2 + w, z + w, r, g, b, 240);
            for (float y : new float[]{y1, y2})
                box(buffer, matrix, x - w, y - w, z1 - w, x + w, y + w, z2 + w, r, g, b, 240);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private void clearBlockHighlight() {
        blockHighlight.clear();
        lastLockedBounds = null;
        highlightResumeMillis = 0;
    }

    private static void box(BufferBuilder buffer, Matrix4f matrix,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            int r, int g, int b, int a) {
        float[][] corners = {{x1,y1,z1}, {x2,y1,z1}, {x2,y2,z1}, {x1,y2,z1},
                {x1,y1,z2}, {x2,y1,z2}, {x2,y2,z2}, {x1,y2,z2}};
        for (int[] face : FACES) {
            for (int index : face) {
                float[] pos = corners[index];
                buffer.addVertex(matrix, pos[0], pos[1], pos[2]).setColor(r, g, b, a);
            }
        }
    }
}
