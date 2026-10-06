package com.github.edg_thexu.better_experience.client.gui;

import org.joml.Matrix4f;

public final class SpacePreviewTransform {
    private SpacePreviewTransform() {}

    public static Matrix4f model(int x, int y, int size) {
        float scale = size * 0.52F;
        // Match vanilla GUI item handedness: the GUI projection already reverses Y.
        return new Matrix4f().translate(x + size / 2F, y + size / 2F, 200)
                .scale(scale, -scale, scale)
                .rotateX((float) Math.toRadians(25)).rotateY((float) Math.toRadians(40))
                .translate(-0.5F, -0.5F, -0.5F);
    }
}
