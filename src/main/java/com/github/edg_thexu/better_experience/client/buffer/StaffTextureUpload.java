package com.github.edg_thexu.better_experience.client.buffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/** Float-box uploads must not inherit the atlas uploader's pixel unpack state. */
final class StaffTextureUpload {
    private StaffTextureUpload() {}

    static void upload(int width, int height, FloatBuffer data) {
        long required = (long) width * height * 4;
        if (width <= 0 || height <= 0 || required > data.remaining())
            throw new IllegalArgumentException("Incomplete staff bounds texture data");
        int alignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int swapBytes = GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES);
        int unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, GL11.GL_FALSE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_FLOAT, data);
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, swapBytes);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        }
    }
}
