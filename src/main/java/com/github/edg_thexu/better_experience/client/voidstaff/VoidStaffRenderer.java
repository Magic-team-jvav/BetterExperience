package com.github.edg_thexu.better_experience.client.voidstaff;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.mixin.accessor.PreviewRenderTypeAccess;
import com.github.edg_thexu.better_experience.mixin.accessor.PreviewRenderStateAccess;
import com.github.edg_thexu.better_experience.mixin.accessor.PreviewTextureAccess;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidCargo;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidPlacementRules;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

public final class VoidStaffRenderer {
    private static VoidCargo cached;
    private static BlockPos cachedPos;
    private static final List<BlockEntity> ENTITIES = new ArrayList<>();
    private static boolean failed;
    private VoidStaffRenderer() {}

    public static void clear() { cached = null; cachedPos = null; ENTITIES.clear(); failed = false; }

    public static void render(RenderLevelStageEvent event) {
        var minecraft = Minecraft.getInstance();
        var cargo = VoidStaffClient.cargo();
        var origin = VoidStaffClient.target();
        if (!VoidStaffClient.active() || cargo == null || origin == null) return;
        var level = minecraft.level;
        if (cargo != cached || !origin.equals(cachedPos)) {
            clear(); cached = cargo; cachedPos = origin;
            for (var part : cargo.parts()) {
                var pos = origin.offset(part.offset().rotate(VoidPlacementRules.rotation(cargo.parts().getFirst().state(), cargo.state())));
                var state = part.offset().equals(BlockPos.ZERO) ? cargo.state()
                        : VoidPlacementRules.partState(part, cargo.parts().getFirst().state(), cargo.state());
                BlockEntity entity = null;
                try {
                    if (state.getBlock() instanceof EntityBlock block) {
                        entity = block.newBlockEntity(pos, state);
                        if (entity != null) entity.loadWithComponents(part.display(), level.registryAccess());
                    }
                } catch (RuntimeException exception) {
                    Better_experience.LOGGER.warn("Cannot prepare void staff preview for {}", state, exception);
                }
                ENTITIES.add(entity);
            }
        }
        if (failed) return;
        float alpha = VoidStaffClient.validPlacement() ? 0.65F : (float) (0.2 + 0.55 * (0.5 + 0.5 * Math.sin(System.nanoTime() / 180_000_000.0)));
        var buffers = minecraft.renderBuffers().bufferSource();
        MultiBufferSource preview = type -> new AlphaVertex(buffers.getBuffer(translucent(type)), alpha);
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        try {
            for (int i = 0; i < cargo.parts().size(); i++) {
                var part = cargo.parts().get(i);
                var pos = origin.offset(part.offset().rotate(VoidPlacementRules.rotation(cargo.parts().getFirst().state(), cargo.state())));
                BlockState state = part.offset().equals(BlockPos.ZERO) ? cargo.state()
                        : VoidPlacementRules.partState(part, cargo.parts().getFirst().state(), cargo.state());
                var entity = ENTITIES.get(i);
                pose.pushPose();
                try {
                    pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                    if (entity != null && state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED) {
                        entity.setLevel(level);
                        minecraft.getBlockEntityRenderDispatcher().renderItem(entity, pose, preview, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    } else minecraft.getBlockRenderer().renderSingleBlock(state, pose, preview, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                } finally {
                    pose.popPose();
                    if (entity != null) entity.setLevel(null);
                }
            }
        } catch (RuntimeException exception) {
            failed = true;
            Better_experience.LOGGER.warn("Cannot render carried container", exception);
        } finally {
            buffers.endBatch();
        }
    }

    private static RenderType translucent(RenderType type) {
        if (type.format() == DefaultVertexFormat.NEW_ENTITY && type instanceof PreviewRenderTypeAccess access) {
            var state = (PreviewRenderStateAccess) (Object) access.betterExperience$state();
            var texture = (PreviewTextureAccess) (Object) state.betterExperience$textureState();
            var resource = texture.betterExperience$texture();
            if (resource.isPresent()) return RenderType.entityTranslucent(resource.get());
        }
        return type;
    }

    /** Keep each renderer's own texture atlas and shade only its vertex alpha. */
    private record AlphaVertex(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r, g, b, Math.round(a * alpha)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    }
}
