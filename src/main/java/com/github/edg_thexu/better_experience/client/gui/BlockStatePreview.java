package com.github.edg_thexu.better_experience.client.gui;

import com.github.edg_thexu.better_experience.Better_experience;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Renders the selected state itself, including orientation and slab/stair geometry. */
public final class BlockStatePreview {
    private BlockState cachedState;
    private BlockEntity entity;
    private boolean failed;

    public void render(GuiGraphics graphics, BlockState state, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean changed = cachedState != state;
        if (changed) {
            cachedState = state;
            entity = null;
            failed = false;
        }
        graphics.fill(x, y, x + size, y + size, 0xDC18212B);
        graphics.renderOutline(x, y, size, size, 0xFF78CCE6);
        if (failed) {
            graphics.drawCenteredString(minecraft.font, "?", x + size / 2, y + size / 2 - 4, 0xFFFFFF);
            return;
        }
        graphics.flush();
        graphics.enableScissor(x + 1, y + 1, x + size - 1, y + size - 1);
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.mulPose(SpacePreviewTransform.model(x, y, size));
            Lighting.setupFor3DItems();
            RenderSystem.enableDepthTest();
            if (changed && state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED
                    && state.getBlock() instanceof EntityBlock block) {
                entity = block.newBlockEntity(BlockPos.ZERO, state);
            }
            if (entity != null) {
                // No world insertion or ticking; the level allows renderers to read this state's facing.
                entity.setLevel(minecraft.level);
                minecraft.getBlockEntityRenderDispatcher().renderItem(entity, pose, graphics.bufferSource(),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } else {
                minecraft.getBlockRenderer().renderSingleBlock(state, pose, graphics.bufferSource(),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
        } catch (RuntimeException exception) {
            // One broken third-party model must not make the state editor impossible to use.
            failed = true;
            Better_experience.LOGGER.warn("Cannot preview block state {}", state, exception);
        } finally {
            try {
                // GuiGraphics.flush disables depth testing, which is unsuitable for a 3D model.
                graphics.bufferSource().endBatch();
            } finally {
                if (entity != null) entity.setLevel(null);
                pose.popPose();
                graphics.disableScissor();
                RenderSystem.setShaderColor(1, 1, 1, 1);
                Lighting.setupFor3DItems();
                RenderSystem.enableDepthTest();
            }
        }
    }

    public void clear() {
        cachedState = null;
        entity = null;
        failed = false;
    }
}
