package com.basefinder;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

/** HUD principal: módulos independientes, animados y reubicables. */
public final class HudOverlay {
    private HudOverlay() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui == null || mc.gui.screen() != null) return;
        if (!BaseFinderClient.hudEnabled) return;
        drawModules(g, mc, false);
    }

    /** Se usa dentro del editor para previsualizar exactamente los módulos del juego. */
    public static void renderEditor(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        drawModules(g, mc, true);
    }

    private static void drawModules(GuiGraphicsExtractor g, Minecraft mc, boolean editing) {
        int sw = g.guiWidth(), sh = g.guiHeight();
        int pulse = (int)(18 + 12 * (0.5 + 0.5 * Math.sin(Util.getMillis() / 420.0)));
        int accent = (pulse << 24) | 0x00B56BFF;

        drawStatus(g, mc, HudLayout.x(HudLayout.STATUS, sw), HudLayout.y(HudLayout.STATUS, sh), accent, editing);
        if (BaseFinderClient.radarEnabled || editing) {
            drawRadar(g, mc, HudLayout.x(HudLayout.RADAR, sw), HudLayout.y(HudLayout.RADAR, sh), accent, editing);
        }
        if (BaseFinderClient.chunkMapEnabled || editing) {
            drawChunks(g, mc, HudLayout.x(HudLayout.CHUNKS, sw), HudLayout.y(HudLayout.CHUNKS, sh), accent, editing);
        }
        if (BaseFinderClient.enabled || editing) {
            drawTargets(g, mc, HudLayout.x(HudLayout.TARGETS, sw), HudLayout.y(HudLayout.TARGETS, sh), accent, editing);
        }
        if (BaseFinderClient.hotkeyListEnabled || editing) {
            drawKeys(g, mc, HudLayout.x(HudLayout.HOTKEYS, sw), HudLayout.y(HudLayout.HOTKEYS, sh), accent, editing);
        }
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent) {
        g.fill(x, y, x + w, y + h, 0xE80A0811);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, accent);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, 0xAA593B79);
        g.outline(x, y, w, h, 0xCC9E68D0);
    }

    private static void tag(GuiGraphicsExtractor g, Minecraft mc, int x, int y, String label) {
        g.fill(x + 5, y + 5, x + 7 + mc.font.width(label) + 8, y + 18, 0xEE6C38A0);
        g.text(mc.font, label, x + 9, y + 8, 0xFFFFFFFF, true);
    }

    private static void drawStatus(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int accent, boolean editing) {
        panel(g, x, y, 166, 70, accent);
        g.text(mc.font, "BASEFINDER", x + 9, y + 9, 0xFFFFF6FF, true);
        g.text(mc.font, "26.3  •  CLIENT SCAN", x + 9, y + 22, 0xFFB8A4CB, false);
        g.fill(x + 9, y + 38, x + 157, y + 39, 0x553D3150);
        g.text(mc.font, BaseFinderClient.enabled ? "● ESCANEO ACTIVO" : "○ ESCANEO EN PAUSA",
                x + 9, y + 44, BaseFinderClient.enabled ? 0xFF7EFFB6 : 0xFFFFC079, false);
        g.text(mc.font, "X " + mc.player.blockPosition().getX() + "  Z " + mc.player.blockPosition().getZ(),
                x + 9, y + 57, 0xFFD8C9E9, false);
        if (editing) tag(g, mc, x, y, "ARRASTRAR");
    }

    private static void drawRadar(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int accent, boolean editing) {
        panel(g, x, y, 112, 112, accent);
        g.text(mc.font, "RADAR", x + 8, y + 8, 0xFFF7EFFF, true);
        int cx = x + 56, cy = y + 64;
        g.fill(cx - 35, cy - 35, cx + 36, cy + 36, 0xAA15101F);

        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI / 24.0;
            int px = cx + (int)(Math.cos(a) * 33);
            int py = cy + (int)(Math.sin(a) * 33);
            g.fill(px, py, px + 2, py + 2, 0xFF63477E);
        }
        g.fill(cx - 1, cy - 27, cx + 1, cy + 27, 0x665B4772);
        g.fill(cx - 27, cy - 1, cx + 27, cy + 1, 0x665B4772);
        g.text(mc.font, "N", cx - 3, cy - 45, 0xFFDCCDF0, false);

        double yaw = Math.toRadians(mc.player.getYRot());
        double px = mc.player.getX(), pz = mc.player.getZ();
        for (Marker marker : BaseFinderClient.markers) {
            if (!"BASE?".equals(marker.label()) && !"CHUNK".equals(marker.label())) continue;
            double dx = (marker.x0() + marker.x1()) * 0.5 - px;
            double dz = (marker.z0() + marker.z1()) * 0.5 - pz;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 160.0) continue;
            double angle = Math.atan2(dx, dz) + yaw;
            int radius = (int)Math.min(30, distance * 0.19);
            int rx = cx + (int)Math.round(Math.sin(angle) * radius);
            int rz = cy - (int)Math.round(Math.cos(angle) * radius);
            int color = "BASE?".equals(marker.label()) ? 0xFFFF526D : 0xFFFFC45B;
            g.fill(rx - 2, rz - 2, rx + 3, rz + 3, color);
        }
        g.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xFF65E6C0);
        if (editing) tag(g, mc, x, y, "RADAR");
    }

    private static void drawChunks(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int accent, boolean editing) {
        panel(g, x, y, 120, 104, accent);
        g.text(mc.font, "CHUNK MAP", x + 8, y + 8, 0xFFF7EFFF, true);
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int startX = x + 19, startY = y + 26;
        for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
                int bx = pcx + dx, bz = pcz + dz;
                int px = startX + (dx + 4) * 9;
                int py = startY + (dz + 4) * 8;
                boolean loaded = mc.level.getChunkSource().hasChunk(bx, bz);
                boolean suspicious = false;
                for (Marker marker : BaseFinderClient.markers) {
                    if (!"CHUNK".equals(marker.label())) continue;
                    int mx = (int)Math.floor((marker.x0() + marker.x1()) * 0.5) >> 4;
                    int mz = (int)Math.floor((marker.z0() + marker.z1()) * 0.5) >> 4;
                    if (mx == bx && mz == bz) { suspicious = true; break; }
                }
                int color = dx == 0 && dz == 0 ? 0xFF61E6BB
                        : suspicious ? 0xFFFFB44F : loaded ? 0xFF426D61 : 0xFF27212F;
                g.fill(px, py, px + 7, py + 6, color);
            }
        }
        if (editing) tag(g, mc, x, y, "CHUNKS");
    }

    private static void drawTargets(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int accent, boolean editing) {
        int bases = 0, chunks = 0, containers = 0;
        for (Marker marker : BaseFinderClient.markers) {
            if ("BASE?".equals(marker.label())) bases++;
            else if ("CHUNK".equals(marker.label())) chunks++;
            else containers++;
        }
        panel(g, x, y, 180, 64, accent);
        g.text(mc.font, "DETECCIONES", x + 9, y + 9, 0xFFFFF6FF, true);
        g.text(mc.font, "BASES " + bases, x + 9, y + 27, 0xFFFF6B82, false);
        g.text(mc.font, "CHUNKS " + chunks, x + 70, y + 27, 0xFFFFC45B, false);
        g.text(mc.font, "ESP " + containers, x + 9, y + 43, 0xFF72EAC0, false);
        if (editing) tag(g, mc, x, y, "TARGETS");
    }

    private static void drawKeys(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int accent, boolean editing) {
        panel(g, x, y, 154, 48, accent);
        g.text(mc.font, "B  SCAN  " + (BaseFinderClient.enabled ? "ON" : "OFF"),
                x + 9, y + 9, 0xFFF4EAFE, false);
        g.text(mc.font, "M  MENU", x + 9, y + 27, 0xFFC9B8DD, false);
        if (editing) tag(g, mc, x, y, "KEYS");
    }
}
