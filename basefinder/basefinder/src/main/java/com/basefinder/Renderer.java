package com.basefinder;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/**
 * Dibuja cajas a traves de paredes. Basado en el ejemplo oficial de Fabric
 * "Rendering in the World" (26.2). Si en 26.3 algo cambio, SOLO este archivo
 * necesita ajustes.
 */
public final class Renderer {
    private Renderer() {}

    private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(BaseFinderClient.MOD_ID, "pipeline/esp_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED_BUFFER =
            new StagedVertexBuffer(() -> "BaseFinder ESP Buffer", 8 * 1024 * 1024);

    private static int vertexCount;

    public static void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(Renderer::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> STAGED_BUFFER.close());
    }

    private static void render(LevelRenderContext context) {
        if (!BaseFinderClient.enabled) return;
        List<Marker> list = BaseFinderClient.markers;
        if (list.isEmpty()) return;

        RenderPipeline pipeline = THROUGH_WALLS;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        assert formatBinding != null;

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(formatBinding, primitive,
                primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);

        Vec3 cam = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();
        VertexConsumer buf = STAGED_BUFFER.getVertexBuilder(draw);
        Matrix4fc m = matrices.last().pose();

        vertexCount = 0;
        for (Marker mk : list) {
            int cost = mk.outline() ? 288 : 24;
            if (vertexCount + cost > Config.MAX_VERTICES) break;
            vertexCount += cost;

            // Coordenadas relativas a la camara (evita jitter en coordenadas enormes de 2b2t)
            float x0 = (float) (mk.x0() - cam.x), y0 = (float) (mk.y0() - cam.y), z0 = (float) (mk.z0() - cam.z);
            float x1 = (float) (mk.x1() - cam.x), y1 = (float) (mk.y1() - cam.y), z1 = (float) (mk.z1() - cam.z);

            if (mk.outline()) {
                outline(m, buf, x0, y0, z0, x1, y1, z1, mk.thickness(), mk.r(), mk.g(), mk.b(), mk.a());
            } else {
                box(m, buf, x0, y0, z0, x1, y1, z1, mk.r(), mk.g(), mk.b(), mk.a());
            }
        }

        STAGED_BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null) {
            drawPass(Minecraft.getInstance(), info, pipeline);
        }
        STAGED_BUFFER.endFrame();
    }

    /** 12 aristas como cajas finas = borde. */
    private static void outline(Matrix4fc m, VertexConsumer b, float x0, float y0, float z0,
                                float x1, float y1, float z1, float t,
                                float r, float g, float bl, float a) {
        float[] ys = {y0, y1 - t};
        float[] zs = {z0, z1 - t};
        float[] xs = {x0, x1 - t};
        for (float y : ys) for (float z : zs) box(m, b, x0, y, z, x1, y + t, z + t, r, g, bl, a);   // aristas en X
        for (float x : xs) for (float z : zs) box(m, b, x, y0, z, x + t, y1, z + t, r, g, bl, a);   // aristas en Y
        for (float x : xs) for (float y : ys) box(m, b, x, y, z0, x + t, y + t, z1, r, g, bl, a);   // aristas en Z
    }

    private static void box(Matrix4fc p, VertexConsumer b, float minX, float minY, float minZ,
                            float maxX, float maxY, float maxZ, float r, float g, float bl, float a) {
        // Front
        b.addVertex(p, minX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, maxZ).setColor(r, g, bl, a);
        // Back
        b.addVertex(p, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, minZ).setColor(r, g, bl, a);
        // Left
        b.addVertex(p, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, minZ).setColor(r, g, bl, a);
        // Right
        b.addVertex(p, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, maxZ).setColor(r, g, bl, a);
        // Top
        b.addVertex(p, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, minZ).setColor(r, g, bl, a);
        // Bottom
        b.addVertex(p, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, minY, maxZ).setColor(r, g, bl, a);
    }

    private static void drawPass(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        assert colorTexture != null;

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "basefinder esp", colorTexture, Optional.empty(),
                        mainTarget.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }
}
