package com.github.edg_thexu.better_experience.client.buffer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Real GPU readback of the production shaders, using an invisible desktop OpenGL window. */
public final class StaffGpuRenderTest {
    public static void main(String[] args) throws IOException {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(64, 64, "Staff GPU verification", 0, 0);
        if (window == 0) throw new AssertionError("OpenGL 3.2 context creation failed");
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            verifyShader();
            System.out.println("Staff GPU checks passed (unpack-state isolation, instance expansion, camera transforms, texture row crossing, alpha pulse and empty gaps).");
        } finally {
            GL.setCapabilities(null);
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }

    private static void verifyShader() throws IOException {
        int vertex = compile(GL20.GL_VERTEX_SHADER, "vsh");
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, "fsh");
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0)
            throw new AssertionError(GL20.glGetProgramInfoLog(program));
        int framebuffer = GL30.glGenFramebuffers();
        int output = GL11.glGenTextures();
        int bounds = GL11.glGenTextures();
        int vao = GL30.glGenVertexArrays();
        int unpackBuffer = GL15.glGenBuffers();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, output);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 64, 64, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, output, 0);
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new AssertionError("Incomplete GPU test framebuffer");
            // Instance 2 crosses a texture row. Instance 1 is outside the viewport.
            var data = stack.callocFloat(32);
            data.put(new float[]{-.8f,-.4f,0,0, -.3f,.4f,.1f,0,
                    10,10,0,0, 11,11,.1f,0, .3f,-.4f,0,0, .8f,.4f,.1f,0}).rewind();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bounds);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            // Reproduce atlas-upload state without letting the driver read beyond our buffer.
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
            GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER, 4096L, GL15.GL_STREAM_DRAW);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 8);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 11);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 2);
            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, GL11.GL_TRUE);
            StaffTextureUpload.upload(4, 2, data);
            if (GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT) != 8
                    || GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH) != 11
                    || GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS) != 1
                    || GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS) != 2
                    || GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES) != GL11.GL_TRUE
                    || GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING) != unpackBuffer)
                throw new AssertionError("Staff upload did not restore Minecraft pixel unpack state");
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, GL11.GL_FALSE);
            GL20.glUseProgram(program);
            var identity = stack.floats(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1);
            GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program, "ProjMat"), false, identity);
            GL20.glUniform1i(GL20.glGetUniformLocation(program, "BoundsSampler"), 0);
            GL20.glUniform4f(GL20.glGetUniformLocation(program, "HighlightColor"), 1, 0, 0, 1);
            GL30.glBindVertexArray(vao);
            GL11.glViewport(0, 0, 64, 64);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glClearColor(0, 0, 0, 0);
            // These camera and pose transforms cancel only when the production renderer
            // includes Minecraft's global model-view matrix, as the outline renderer does.
            var globalView = new Matrix4f().rotationZ((float) (Math.PI / 2));
            var pose = new Matrix4f().rotationZ((float) (-Math.PI / 2)).translate(.25f, .5f, -.75f);
            var cameraModelView = StaffHighlightInstances.modelViewFor(globalView, pose,
                    new Vec3(100.25, 200.5, -300.75), new BlockPos(100, 200, -300));
            var cameraMatrix = stack.mallocFloat(16);
            cameraModelView.get(cameraMatrix);
            for (var modelView : new java.nio.FloatBuffer[]{identity, cameraMatrix}) {
                GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program, "ModelViewMat"), false, modelView);
                for (float time : new float[]{.3f, .9f}) {
                    GL20.glUniform1f(GL20.glGetUniformLocation(program, "PulseTime"), time);
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                    GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, 36, 3);
                    int expectedAlpha = time < .5f ? 24 : 2;
                    for (int x : new int[]{14, 49}) {
                        var pixel = stack.malloc(4);
                        GL11.glReadPixels(x, 32, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
                        if ((pixel.get(0) & 255) != 255 || Math.abs((pixel.get(3) & 255) - expectedAlpha) > 1)
                            throw new AssertionError("Instance position or pulsing alpha differs from expected GPU output");
                    }
                    var gap = stack.malloc(4);
                    GL11.glReadPixels(32, 32, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, gap);
                    if ((gap.get(3) & 255) != 0) throw new AssertionError("GPU highlight fills a gap between selected boxes");
                }
            }
            if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("OpenGL render error");
        } finally {
            GL20.glUseProgram(0);
            GL30.glBindVertexArray(0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL30.glDeleteVertexArrays(vao);
            GL15.glDeleteBuffers(unpackBuffer);
            GL30.glDeleteFramebuffers(framebuffer);
            GL11.glDeleteTextures(output);
            GL11.glDeleteTextures(bounds);
            GL20.glDeleteProgram(program);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
        }
    }

    private static int compile(int type, String suffix) throws IOException {
        String path = "/assets/better_experience/shaders/core/staff_highlight." + suffix;
        try (var resource = StaffGpuRenderTest.class.getResourceAsStream(path)) {
            if (resource == null) throw new AssertionError("Missing production shader " + path);
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, new String(resource.readAllBytes(), StandardCharsets.UTF_8));
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0)
                throw new AssertionError(GL20.glGetShaderInfoLog(shader));
            return shader;
        }
    }
}
