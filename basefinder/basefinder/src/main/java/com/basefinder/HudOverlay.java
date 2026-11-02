package com.basefinder;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

public final class HudOverlay {
    private HudOverlay() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui == null || mc.gui.screen() != null) return;
        if (!BaseFinderClient.hudEnabled) return;

        int pulse = (int)(18 + 12 * (0.5 + 0.5 * Math.sin(Util.getMillis() / 420.0)));
        int accent = (pulse << 24) | 0x00A855FF;
        int x = 12, y = 12;
        panel(g, x, y, 154, 112);
        g.fill(x + 7, y + 7, x + 147, y + 9, accent);
        g.text(mc.font, "BASEFINDER", x + 10, y + 14, 0xFFF4F0FF, true);
        g.text(mc.font, "26.3  /  LIVE SCAN", x + 10, y + 27, 0xFF9F91B7, false);

        if (BaseFinderClient.radarEnabled) drawRadar(g, mc, x + 48, y + 76);
        if (BaseFinderClient.chunkMapEnabled) drawChunkMap(g, mc, x + 91, y + 48);

        g.text(mc.font, "X " + mc.player.blockPosition().getX() + "  Z " + mc.player.blockPosition().getZ(),
                x + 9, y + 98, 0xFFD7C9E8, false);

        int right = g.guiWidth() - 164;
        panel(g, right, 12, 152, 58);
        g.fill(right + 7, 19, right + 9, 61, accent);
        g.text(mc.font, "SCANNER", right + 15, 19, 0xFFF4F0FF, true);
        g.text(mc.font, BaseFinderClient.enabled ? "● ACTIVE" : "○ PAUSED",
                right + 15, 33, BaseFinderClient.enabled ? 0xFF7CFFB2 : 0xFFFFB36B, false);
        g.text(mc.font, "Targets: " + BaseFinderClient.markers.size(),
                right + 15, 47, 0xFFBEB2CF, false);

        if (BaseFinderClient.hotkeyListEnabled) {
            int hy = g.guiHeight() - 54;
            panel(g, 12, hy, 150, 42);
            g.text(mc.font, "B  SCAN   " + (BaseFinderClient.enabled ? "ON" : "OFF"), 21, hy + 8, 0xFFE8DFFF, false);
            g.text(mc.font, "M  MENU", 21, hy + 23, 0xFFB9A8D0, false);
        }
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xD90B0912);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF49305D);
        g.outline(x, y, w, h, 0xAA9A63C7);
    }

    private static void drawRadar(GuiGraphicsExtractor g, Minecraft mc, int cx, int cy) {
        g.fill(cx - 30, cy - 30, cx + 31, cy + 31, 0xAA171222);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12.0;
            int px = cx + (int)(Math.cos(a) * 29);
            int py = cy + (int)(Math.sin(a) * 29);
            g.fill(px, py, px + 2, py + 2, 0xFF78549A);
        }
        g.fill(cx - 1, cy - 20, cx + 1, cy + 20, 0x554D3A65);
        g.fill(cx - 20, cy - 1, cx + 20, cy + 1, 0x554D3A65);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFF5A8A);
        g.text(mc.font, "N", cx - 3, cy - 42, 0xFFE6D9F5, false);
    }

    private static void drawChunkMap(GuiGraphicsExtractor g, Minecraft mc, int x, int y) {
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        for (int dz = -3; dz <= 3; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                int px = x + (dx + 3) * 8;
                int py = y + (dz + 3) * 8;
                boolean loaded = mc.level.getChunkSource().hasChunk(pcx + dx, pcz + dz);
                int color = dx == 0 && dz == 0 ? 0xFFFF5A8A : loaded ? 0xFF6BBD91 : 0xFF292331;
                g.fill(px, py, px + 6, py + 6, color);
            }
        }
    }
}
