package me.marquinho.protectedareaclient.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SkyboxRenderer {

    private static final Map<String, Identifier> textureCache = new HashMap<>();

    private static VertexBuffer sphereMesh = null;
    private static final int SPHERE_LAT = 32;
    private static final int SPHERE_LON = 64;
    private static final float SPHERE_RADIUS = 400f;

    public static void render(String skyboxName, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float alpha) {
        Identifier textureId = getOrLoadTexture(skyboxName);
        if (textureId == null) return;

        ensureSphereMesh();
        if (sphereMesh == null) return;

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        Matrix4f mv = new Matrix4f(modelViewMatrix);
        mv.m30(0f);
        mv.m31(0f);
        mv.m32(0f);

        sphereMesh.bind();
        sphereMesh.draw(mv, projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void ensureSphereMesh() {
        if (sphereMesh != null) return;

        BufferBuilder b = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);

        for (int lat = 0; lat < SPHERE_LAT; lat++) {
            float v0 = (float) lat / SPHERE_LAT;
            float v1 = (float) (lat + 1) / SPHERE_LAT;
            float th0 = (float) Math.PI * v0;
            float th1 = (float) Math.PI * v1;
            float cosT0 = (float) Math.cos(th0), sinT0 = (float) Math.sin(th0);
            float cosT1 = (float) Math.cos(th1), sinT1 = (float) Math.sin(th1);

            for (int lon = 0; lon < SPHERE_LON; lon++) {
                float u0 = (float) lon / SPHERE_LON;
                float u1 = (float) (lon + 1) / SPHERE_LON;
                float ph0 = (float) (2 * Math.PI * u0);
                float ph1 = (float) (2 * Math.PI * u1);
                float cosPh0 = (float) Math.cos(ph0), sinPh0 = (float) Math.sin(ph0);
                float cosPh1 = (float) Math.cos(ph1), sinPh1 = (float) Math.sin(ph1);

                float x00 = SPHERE_RADIUS * sinPh0 * sinT0;
                float y00 = SPHERE_RADIUS * cosT0;
                float z00 = SPHERE_RADIUS * cosPh0 * sinT0;

                float x10 = SPHERE_RADIUS * sinPh1 * sinT0;
                float y10 = SPHERE_RADIUS * cosT0;
                float z10 = SPHERE_RADIUS * cosPh1 * sinT0;

                float x01 = SPHERE_RADIUS * sinPh0 * sinT1;
                float y01 = SPHERE_RADIUS * cosT1;
                float z01 = SPHERE_RADIUS * cosPh0 * sinT1;

                float x11 = SPHERE_RADIUS * sinPh1 * sinT1;
                float y11 = SPHERE_RADIUS * cosT1;
                float z11 = SPHERE_RADIUS * cosPh1 * sinT1;

                float uv0 = 1f - v0;
                float uv1 = 1f - v1;

                b.vertex(x00, y00, z00).texture(u0, uv0);
                b.vertex(x10, y10, z10).texture(u1, uv0);
                b.vertex(x11, y11, z11).texture(u1, uv1);

                b.vertex(x00, y00, z00).texture(u0, uv0);
                b.vertex(x11, y11, z11).texture(u1, uv1);
                b.vertex(x01, y01, z01).texture(u0, uv1);
            }
        }

        BuiltBuffer built = b.endNullable();
        if (built == null) return;

        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(built);
        VertexBuffer.unbind();

        sphereMesh = vb;
    }

    private static Identifier getOrLoadTexture(String skyboxName) {
        if (textureCache.containsKey(skyboxName)) {
            return textureCache.get(skyboxName);
        }

        File file = new File(MinecraftClient.getInstance().runDirectory,
                "config/ProtectedArea/assets/skybox/" + skyboxName + ".png");

        if (!file.exists()) {
            System.err.println("[ProtectedArea] Skybox no encontrada: " + file.getAbsolutePath());
            textureCache.put(skyboxName, null);
            return null;
        }

        try (FileInputStream stream = new FileInputStream(file)) {
            NativeImage image = NativeImage.read(stream);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            Identifier id = MinecraftClient.getInstance().getTextureManager()
                    .registerDynamicTexture("protectedarea_skybox_" + skyboxName, texture);
            textureCache.put(skyboxName, id);
            System.out.println("[ProtectedArea] Skybox cargada: " + skyboxName);
            return id;
        } catch (IOException e) {
            System.err.println("[ProtectedArea] Error cargando skybox '" + skyboxName + "': " + e.getMessage());
            textureCache.put(skyboxName, null);
            return null;
        }
    }

    public static void invalidateCache() {
        textureCache.clear();
        if (sphereMesh != null) {
            sphereMesh.close();
            sphereMesh = null;
        }
    }
}
