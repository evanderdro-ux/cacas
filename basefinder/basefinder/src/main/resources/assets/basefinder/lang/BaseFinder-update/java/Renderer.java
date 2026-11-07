package com.basefinder;

import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.joml.Matrix4fc;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/**
 * Dibuja cajas a traves de paredes en Minecraft 26.3.
 * Usa COLLECT_SUBMITS + submitCustomGeometry con un RenderType sin test de profundidad.
 */
public final class Renderer {
    private Renderer() {}

    // Pipeline "relleno de cajas" pero con el test de profundidad siempre aprobado = se ve a traves de bloques.
    private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(BaseFinderClient.MOD_ID, "pipeline/esp_through_walls"))
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .build());

    private static final RenderType ESP_TYPE =
            RenderType.create(BaseFinderClient.MOD_ID + ":esp", RenderSetup.builder(THROUGH_WALLS).createRenderSetup());

    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(Renderer::collect);
    }

    private static void collect(LevelRenderContext context) {
        if (!BaseFinderClient.enabled) return;
        final List<Marker> list = BaseFinderClient.markers;
        if (list.isEmpty()) return;

        final Vec3 cam = context.levelState().cameraRenderState.pos;
        final double camX = cam.x, camY = cam.y, camZ = cam.z;

        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), ESP_TYPE, (pose, buf) -> {
            Matrix4fc m = pose.pose();
            int vertexCount = 0;
            for (Marker mk : list) {
                int cost = mk.outline() ? 288 : 24;
                if (vertexCount + cost > Config.MAX_VERTICES) break;
                vertexCount += cost;

                // Coordenadas relativas a la camara (evita jitter con coordenadas enormes de 2b2t)
                float x0 = (float) (mk.x0() - camX), y0 = (float) (mk.y0() - camY), z0 = (float) (mk.z0() - camZ);
                float x1 = (float) (mk.x1() - camX), y1 = (float) (mk.y1() - camY), z1 = (float) (mk.z1() - camZ);

                if (mk.outline()) {
                    outline(m, buf, x0, y0, z0, x1, y1, z1, mk.thickness(), mk.r(), mk.g(), mk.b(), mk.a());
                } else {
                    box(m, buf, x0, y0, z0, x1, y1, z1, mk.r(), mk.g(), mk.b(), mk.a());
                }
            }
        });
    }

    /** 12 aristas como cajas finas = borde. */
    private static void outline(Matrix4fc m, VertexConsumer b, float x0, float y0, float z0,
                                float x1, float y1, float z1, float t,
                                float r, float g, float bl, float a) {
        float[] ys = {y0, y1 - t};
        float[] zs = {z0, z1 - t};
        float[] xs = {x0, x1 - t};
        for (float y : ys) for (float z : zs) box(m, b, x0, y, z, x1, y + t, z + t, r, g, bl, a);
        for (float x : xs) for (float z : zs) box(m, b, x, y0, z, x + t, y1, z + t, r, g, bl, a);
        for (float x : xs) for (float y : ys) box(m, b, x, y, z0, x + t, y + t, z1, r, g, bl, a);
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
}
