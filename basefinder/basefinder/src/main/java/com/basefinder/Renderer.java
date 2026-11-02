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

public final class Renderer {
    private Renderer() {}

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

        final Vec3 camera = context.levelState().cameraRenderState.pos;
        final double camX = camera.x, camY = camera.y, camZ = camera.z;

        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), ESP_TYPE, (pose, buf) -> {
            Matrix4fc matrix = pose.pose();
            int vertexCount = 0;
            int tracerCount = 0;
            for (Marker marker : list) {
                int cost = marker.outline() ? 288 : 24;
                if (vertexCount + cost > Config.MAX_VERTICES) break;

                float x0 = (float) (marker.x0() - camX);
                float y0 = (float) (marker.y0() - camY);
                float z0 = (float) (marker.z0() - camZ);
                float x1 = (float) (marker.x1() - camX);
                float y1 = (float) (marker.y1() - camY);
                float z1 = (float) (marker.z1() - camZ);

                if (marker.outline()) {
                    outline(matrix, buf, x0, y0, z0, x1, y1, z1, marker.thickness(),
                            marker.r(), marker.g(), marker.b(), marker.a());
                } else {
                    box(matrix, buf, x0, y0, z0, x1, y1, z1,
                            marker.r(), marker.g(), marker.b(), marker.a());
                }
                vertexCount += cost;

                if (BaseFinderClient.tracersEnabled && tracerCount < 14
                        && ("BASE?".equals(marker.label()) || "CHUNK".equals(marker.label()))) {
                    if (vertexCount + 24 > Config.MAX_VERTICES) break;
                    float tx = (x0 + x1) * 0.5f;
                    float ty = (y0 + y1) * 0.5f;
                    float tz = (z0 + z1) * 0.5f;
                    drawBeam(matrix, buf, 0f, 0f, 0f, tx, ty, tz,
                            0.025f, marker.r(), marker.g(), marker.b(), 0.82f);
                    vertexCount += 24;
                    tracerCount++;
                }
            }
        });
    }

    /** Bordes formados por prismas finos: se ven incluso tras terreno opaco. */
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

    /** Línea 3D de grosor pequeño, desde la cámara hasta un objetivo. */
    private static void drawBeam(Matrix4fc m, VertexConsumer b,
                                 float ax, float ay, float az, float bx, float by, float bz,
                                 float radius, float r, float g, float bl, float a) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.01f) return;
        dx /= length; dy /= length; dz /= length;

        float rx = Math.abs(dy) < 0.85f ? 0f : 1f;
        float ry = Math.abs(dy) < 0.85f ? 1f : 0f;
        float rz = 0f;
        float ux = dy * rz - dz * ry;
        float uy = dz * rx - dx * rz;
        float uz = dx * ry - dy * rx;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 0.0001f) return;
        ux = ux / ul * radius; uy = uy / ul * radius; uz = uz / ul * radius;
        float vx = dy * uz - dz * uy;
        float vy = dz * ux - dx * uz;
        float vz = dx * uy - dy * ux;

        float[][] start = new float[4][3];
        float[][] end = new float[4][3];
        int[][] signs = {{1,1},{-1,1},{-1,-1},{1,-1}};
        for (int i = 0; i < 4; i++) {
            float ox = ux * signs[i][0] + vx * signs[i][1];
            float oy = uy * signs[i][0] + vy * signs[i][1];
            float oz = uz * signs[i][0] + vz * signs[i][1];
            start[i][0] = ax + ox; start[i][1] = ay + oy; start[i][2] = az + oz;
            end[i][0] = bx + ox; end[i][1] = by + oy; end[i][2] = bz + oz;
        }
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) & 3;
            quad(m, b, start[i], start[next], end[next], end[i], r, g, bl, a);
        }
    }

    private static void quad(Matrix4fc m, VertexConsumer b, float[] p1, float[] p2,
                             float[] p3, float[] p4, float r, float g, float bl, float a) {
        b.addVertex(m, p1[0], p1[1], p1[2]).setColor(r, g, bl, a);
        b.addVertex(m, p2[0], p2[1], p2[2]).setColor(r, g, bl, a);
        b.addVertex(m, p3[0], p3[1], p3[2]).setColor(r, g, bl, a);
        b.addVertex(m, p4[0], p4[1], p4[2]).setColor(r, g, bl, a);
    }

    private static void box(Matrix4fc p, VertexConsumer b, float minX, float minY, float minZ,
                            float maxX, float maxY, float maxZ, float r, float g, float bl, float a) {
        b.addVertex(p, minX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, maxZ).setColor(r, g, bl, a);

        b.addVertex(p, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, minZ).setColor(r, g, bl, a);

        b.addVertex(p, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, minZ).setColor(r, g, bl, a);

        b.addVertex(p, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, maxZ).setColor(r, g, bl, a);

        b.addVertex(p, minX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, maxY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, maxY, minZ).setColor(r, g, bl, a);

        b.addVertex(p, minX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, minY, minZ).setColor(r, g, bl, a);
        b.addVertex(p, maxX, minY, maxZ).setColor(r, g, bl, a);
        b.addVertex(p, minX, minY, maxZ).setColor(r, g, bl, a);
    }
}
