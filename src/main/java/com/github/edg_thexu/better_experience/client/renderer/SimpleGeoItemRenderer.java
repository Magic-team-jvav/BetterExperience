package com.github.edg_thexu.better_experience.client.renderer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Minimal GeckoLib item renderer with explicit resources, without depending on Confluence's renderer. */
public class SimpleGeoItemRenderer<T extends Item & GeoAnimatable> implements GeoRenderProvider {

    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final @Nullable ResourceLocation animation;
    private GeoItemRenderer<T> renderer;

    public SimpleGeoItemRenderer(ResourceLocation model, ResourceLocation texture, @Nullable ResourceLocation animation) {
        this.model = model;
        this.texture = texture;
        this.animation = animation;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
        if (renderer == null) renderer = new GeoItemRenderer<>(createModel());
        return renderer;
    }

    private GeoModel<T> createModel() {
        return new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(T animatable) {
                return model;
            }

            @Override
            public ResourceLocation getTextureResource(T animatable) {
                return texture;
            }

            @Override
            public @Nullable ResourceLocation getAnimationResource(T animatable) {
                return animation;
            }
        };
    }
}
