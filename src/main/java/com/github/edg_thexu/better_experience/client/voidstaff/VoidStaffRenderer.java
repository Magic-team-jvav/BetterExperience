package com.github.edg_thexu.better_experience.client.voidstaff;

import com.github.edg_thexu.better_experience.Better_experience;
import com.github.edg_thexu.better_experience.client.gui.BlockStatePreview;
import com.github.edg_thexu.better_experience.mixin.accessor.PreviewRenderTypeAccess;
import com.github.edg_thexu.better_experience.mixin.accessor.PreviewRenderStateAccess;
import com.github.edg_thexu.better_experience.mixin.accessor.PreviewTextureAccess;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidCargo;
import com.github.edg_thexu.better_experience.module.voidstaff.VoidPlacementRules;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VoidStaffRenderer {
    /** The preview must not borrow the level's buffer source: flushing it would draw the level's pending batches early. */
    private static final ByteBufferBuilder PREVIEW_BUFFER = new ByteBufferBuilder(1536);
    private static final MultiBufferSource.BufferSource PREVIEW = MultiBufferSource.immediate(PREVIEW_BUFFER);
    private static final List<BlockEntity> ENTITIES = new ArrayList<>();
    /** States whose block model threw; they are skipped until another block is carried. */
    private static final Set<BlockState> MODEL_FAILED = new HashSet<>();
    /** States whose block entity renderer threw. Models still render for them. */
    private static final Set<BlockState> RENDERER_FAILED = new HashSet<>();
    private static VoidCargo cached;
    private static VoidCargo reportedCargo;
    private static BlockPos cachedPos;
    private static BlockState nameState;
    private static Component nameCache;
    private static boolean flushFailed;
    private static boolean previewMessageLogged;
    private VoidStaffRenderer() {}

    public static void clear() {
        cached = null;
        cachedPos = null;
        ENTITIES.clear();
        nameState = null;
        nameCache = null;
    }

    /**
     * Display name of the carried block. Blocks such as Sophisticated Storage barrels declare
     * "%s%sBarrel" and only resolve it through their item or block entity, so the raw block name leaks placeholders.
     */
    public static Component displayName(BlockState state) {
        if (nameCache == null || nameState != state) {
            nameState = state;
            nameCache = resolveName(state);
        }
        return nameCache;
    }

    private static Component resolveName(BlockState state) {
        for (var entity : ENTITIES) {
            if (entity instanceof Nameable nameable && entity.getBlockState() == state) {
                try {
                    return nameable.getDisplayName();
                } catch (RuntimeException exception) {
                    Better_experience.LOGGER.warn("Cannot name void staff preview for {}", state, exception);
                    break;
                }
            }
        }
        return BlockStatePreview.displayName(state);
    }

    public static void render(RenderLevelStageEvent event) {
        try {
            renderPreview(event);
        } catch (RuntimeException exception) {
            // A broken third-party block entity must never escape into the level render pass.
            previewFailed(exception);
        }
    }

    private static void renderPreview(RenderLevelStageEvent event) {
        var minecraft = Minecraft.getInstance();
        var cargo = VoidStaffClient.cargo();
        var origin = VoidStaffClient.target();
        var level = minecraft.level;
        if (!VoidStaffClient.active() || cargo == null || origin == null || level == null) {
            // Drop the cached block entities so they don't keep a reference to an unloaded level.
            clear();
            return;
        }
        if (cargo != reportedCargo) {
            reportedCargo = cargo;
            MODEL_FAILED.clear();
            RENDERER_FAILED.clear();
            previewMessageLogged = false;
        }
        if (cargo != cached || !origin.equals(cachedPos)) {
            cached = cargo; cachedPos = origin;
            ENTITIES.clear();
            nameState = null;
            nameCache = null;
            for (var part : cargo.parts()) {
                var pos = origin.offset(part.offset().rotate(VoidPlacementRules.rotation(cargo.parts().getFirst().state(), cargo.state())));
                var state = part.offset().equals(BlockPos.ZERO) ? cargo.state()
                        : VoidPlacementRules.partState(part, cargo.parts().getFirst().state(), cargo.state());
                BlockEntity entity = null;
                try {
                    if (state.getBlock() instanceof EntityBlock block) {
                        entity = block.newBlockEntity(pos, state);
                        if (entity != null) {
                            entity.loadWithComponents(part.display(), level.registryAccess());
                            entity.setLevel(level);
                        }
                    }
                } catch (RuntimeException exception) {
                    Better_experience.LOGGER.warn("Cannot prepare void staff preview for {}", state, exception);
                }
                ENTITIES.add(entity);
            }
        }
        if (!VoidStaffClient.validPlacement()) return;
        float alpha = 0.65F;
        MultiBufferSource preview = type -> new AlphaVertex(PREVIEW.getBuffer(translucent(type)), alpha);
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        try {
            for (int i = 0; i < cargo.parts().size(); i++) {
                var part = cargo.parts().get(i);
                var pos = origin.offset(part.offset().rotate(VoidPlacementRules.rotation(cargo.parts().getFirst().state(), cargo.state())));
                var state = part.offset().equals(BlockPos.ZERO) ? cargo.state()
                        : VoidPlacementRules.partState(part, cargo.parts().getFirst().state(), cargo.state());
                var entity = i < ENTITIES.size() ? ENTITIES.get(i) : null;
                // The world draws the block model and its block entity renderer; reproducing both keeps
                // dynamic overlays such as Sophisticated Storage dye, contents and upgrades visible.
                boolean entityShape = state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED;
                boolean drawModel = entity == null || !entityShape;
                pose.pushPose();
                try {
                    pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                    if (drawModel && !MODEL_FAILED.contains(state)) {
                        try {
                            ModelData modelData = entity == null ? ModelData.EMPTY : entity.getModelData();
                            renderModel(minecraft, level, state, pos, entity, pose, preview, modelData, alpha);
                        } catch (RuntimeException exception) {
                            if (MODEL_FAILED.add(state)) Better_experience.LOGGER.warn("Cannot render void staff preview model for {}", state, exception);
                        }
                    }
                    if (entity != null && !RENDERER_FAILED.contains(state)) {
                        try {
                            // Some block entities (e.g. Sophisticated Storage barrels) assume a non-null level in setLevel.
                            if (entity.getLevel() != level) entity.setLevel(level);
                            minecraft.getBlockEntityRenderDispatcher().renderItem(entity, pose, preview, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                        } catch (RuntimeException exception) {
                            if (RENDERER_FAILED.add(state)) Better_experience.LOGGER.warn("Cannot render void staff preview renderer for {}", state, exception);
                        }
                    }
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            flushPreview();
        }
    }

    private static void renderModel(Minecraft minecraft, BlockAndTintGetter level, BlockState state, BlockPos pos,
                                   BlockEntity entity, PoseStack pose, MultiBufferSource preview, ModelData modelData,
                                   float alpha) {
        var renderer = minecraft.getBlockRenderer();
        var model = renderer.getBlockModel(state);
        var renderTypes = model.getRenderTypes(state, RandomSource.create(42), modelData);
        var previewLevel = new PreviewBlockView(level, pos, state, entity);
        for (var renderType : renderTypes) {
            renderer.renderBatched(state, pos, previewLevel, pose,
                    new AlphaVertex(preview.getBuffer(translucent(renderType)), alpha), false,
                    RandomSource.create(state.getSeed(pos)), modelData, renderType);
        }
    }

    private static void previewFailed(RuntimeException exception) {
        if (!previewMessageLogged) {
            previewMessageLogged = true;
            Better_experience.LOGGER.warn("Cannot render void staff preview", exception);
        }
        clear();
    }

    private static void flushPreview() {
        try {
            PREVIEW.endBatch();
            flushFailed = false;
        } catch (RuntimeException exception) {
            if (!flushFailed) Better_experience.LOGGER.warn("Cannot draw void staff preview", exception);
            flushFailed = true;
        }
    }

    private static RenderType translucent(RenderType type) {
        if (type.format() == DefaultVertexFormat.BLOCK) return RenderType.translucent();
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

    /** Supplies block colors and model data with the carried entity at its preview position. */
    private record PreviewBlockView(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                    BlockEntity entity) implements BlockAndTintGetter {
        @Override public BlockEntity getBlockEntity(BlockPos query) {
            return pos.equals(query) ? entity : level.getBlockEntity(query);
        }
        @Override public BlockState getBlockState(BlockPos query) {
            return pos.equals(query) ? state : level.getBlockState(query);
        }
        @Override public FluidState getFluidState(BlockPos query) {
            return pos.equals(query) ? state.getFluidState() : level.getFluidState(query);
        }
        @Override public int getHeight() { return level.getHeight(); }
        @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
        @Override public float getShade(Direction direction, boolean shade) {
            return level.getShade(direction, shade);
        }
        @Override public LevelLightEngine getLightEngine() { return level.getLightEngine(); }
        @Override public int getBlockTint(BlockPos query, ColorResolver resolver) {
            return level.getBlockTint(query, resolver);
        }
    }
}
