package com.basefinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** Centro de control con secciones visuales y valores ajustables. */
public final class HudScreen extends Screen {
    private static final int PANEL_W = 556;
    private static final int PANEL_H = 410;

    public HudScreen() {
        super(Component.literal("BaseFinder Control Center"));
    }

    private int left() { return (width - PANEL_W) / 2; }
    private int top() { return (height - PANEL_H) / 2; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.fill(0, 0, width, height, 0xB9060410);

        int x = left(), y = top();
        int pulse = (int)(25 + 18 * (0.5 + 0.5 * Math.sin(Util.getMillis() / 500.0)));
        int accent = (pulse << 24) | 0x00B76BFF;

        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xF00C0915);
        g.fill(x, y, x + PANEL_W, y + 3, accent);
        g.fill(x, y + PANEL_H - 2, x + PANEL_W, y + PANEL_H, 0xFF593C79);
        g.outline(x, y, PANEL_W, PANEL_H, 0xFF8759B1);

        g.text(font, "BASEFINDER", x + 18, y + 13, 0xFFFFFFFF, true);
        g.text(font, "CONTROL CENTER  /  LIVE CONFIG", x + 18, y + 28, 0xFFC1A9D8, false);
        g.text(font, BaseFinderClient.enabled ? "● SCANNING" : "○ PAUSED",
                x + PANEL_W - 116, y + 17, BaseFinderClient.enabled ? 0xFF7DFFB7 : 0xFFFFBD76, true);

        int lx = x + 16, rx = x + 286;
        section(g, lx, y + 54, 254, "VISUALES / ESP");
        section(g, rx, y + 54, 254, "HUD / RADAR");

        String[] visualLabels = {
            "ESP de cofres y contenedores", "Cajas de zonas sospechosas",
            "Marcas de chunks sospechosos", "Lineas hacia zonas", 
            "Bordes de vidrio", "Nombres en panel de objetivos"
        };
        boolean[] visualStates = {
            BaseFinderClient.containerEspEnabled, BaseFinderClient.baseBoxesEnabled,
            BaseFinderClient.chunkBoxesEnabled, BaseFinderClient.tracersEnabled,
            BaseFinderClient.transparentEnabled, BaseFinderClient.labelsEnabled
        };
        for (int i = 0; i < visualLabels.length; i++) {
            drawToggle(g, mouseX, mouseY, lx, y + 78 + i * 32, 254, visualLabels[i], visualStates[i]);
        }

        String[] hudLabels = {"HUD completo", "Radar circular", "Mapa de chunks", "Lista de teclas"};
        boolean[] hudStates = {BaseFinderClient.hudEnabled, BaseFinderClient.radarEnabled,
                BaseFinderClient.chunkMapEnabled, BaseFinderClient.hotkeyListEnabled};
        for (int i = 0; i < hudLabels.length; i++) {
            drawToggle(g, mouseX, mouseY, rx, y + 78 + i * 29, 254, hudLabels[i], hudStates[i]);
        }

        section(g, rx, y + 201, 254, "AJUSTES DEL DETECTOR");
        setting(g, mouseX, mouseY, rx, y + 224, "Radio de escaneo", Config.SCAN_RADIUS_CHUNKS + "ch");
        setting(g, mouseX, mouseY, rx, y + 252, "Distancia ESP", Config.RENDER_RANGE + "b");
        setting(g, mouseX, mouseY, rx, y + 280, "Umbral de base", Integer.toString(Config.BASE_SCORE));
        setting(g, mouseX, mouseY, rx, y + 308, "Altura maxima", Integer.toString(Config.MAX_Y));
        setting(g, mouseX, mouseY, rx, y + 336, "Chunks por tick", Integer.toString(Config.CHUNKS_PER_TICK));

        g.fill(lx, y + 288, lx + 254, y + 352, 0x99171122);
        g.outline(lx, y + 288, 254, 64, 0x88543B6A);
        g.text(font, "TECLAS RAPIDAS", lx + 10, y + 298, 0xFFF2E7FA, true);
        g.text(font, "B  activar / pausar escaneo", lx + 10, y + 314, 0xFFBDAED0, false);
        g.text(font, "M  abrir este menu", lx + 10, y + 329, 0xFFBDAED0, false);
        g.text(font, "El escaneo solo usa chunks ya cargados.", lx + 10, y + 341, 0xFF8DDBB7, false);

        int ey = y + 365;
        g.fill(rx, ey, rx + 254, ey + 23, 0xFF65368C);
        g.outline(rx, ey, 254, 23, 0xFFBD84EF);
        g.text(font, "EDITAR / MOVER HUD", rx + 67, ey + 7, 0xFFFFFFFF, true);

        g.text(font, "ESC  CERRAR", x + PANEL_W - 100, y + PANEL_H - 16, 0xFF9D8BAC, false);
    }

    private void section(GuiGraphicsExtractor g, int x, int y, int w, String title) {
        g.fill(x, y, x + w, y + 18, 0xFF21162C);
        g.fill(x, y, x + 2, y + 18, 0xFFB65BFF);
        g.text(font, title, x + 9, y + 5, 0xFFDCC5F5, true);
    }

    private void drawToggle(GuiGraphicsExtractor g, int mx, int my, int x, int y,
                            int w, String title, boolean active) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + 27;
        g.fill(x, y, x + w, y + 27, hover ? 0xFF2B2037 : 0xFF18131F);
        g.text(font, title, x + 9, y + 9, active ? 0xFFF3EAFE : 0xFF9B90A8, false);
        int pillX = x + w - 48;
        g.fill(pillX, y + 6, pillX + 39, y + 21, active ? 0xFF327C65 : 0xFF51495B);
        g.text(font, active ? "ON" : "OFF", pillX + 9, y + 10, 0xFFFFFFFF, true);
    }

    private void setting(GuiGraphicsExtractor g, int mx, int my, int x, int y, String name, String value) {
        g.fill(x, y, x + 254, y + 25, 0xFF18131F);
        g.text(font, name, x + 8, y + 8, 0xFFD8CBE6, false);
        g.fill(x + 158, y + 3, x + 178, y + 22, hover(mx, my, x + 158, y + 3, 20, 19) ? 0xFF75489A : 0xFF382A49);
        g.text(font, "-", x + 165, y + 8, 0xFFFFFFFF, true);
        g.fill(x + 180, y + 3, x + 226, y + 22, 0xFF2A2035);
        g.text(font, value, x + 184, y + 8, 0xFFF0E3FA, false);
        g.fill(x + 230, y + 3, x + 250, y + 22, hover(mx, my, x + 230, y + 3, 20, 19) ? 0xFF75489A : 0xFF382A49);
        g.text(font, "+", x + 236, y + 8, 0xFFFFFFFF, true);
    }

    private boolean hover(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button() != 1) return super.mouseClicked(event, doubleClick);

        double mx = event.x(), my = event.y();
        int x = left(), y = top(), lx = x + 16, rx = x + 286;

        if (hover(mx, my, lx, y + 78, 254, 27)) BaseFinderClient.containerEspEnabled = !BaseFinderClient.containerEspEnabled;
        else if (hover(mx, my, lx, y + 110, 254, 27)) BaseFinderClient.baseBoxesEnabled = !BaseFinderClient.baseBoxesEnabled;
        else if (hover(mx, my, lx, y + 142, 254, 27)) BaseFinderClient.chunkBoxesEnabled = !BaseFinderClient.chunkBoxesEnabled;
        else if (hover(mx, my, lx, y + 174, 254, 27)) BaseFinderClient.tracersEnabled = !BaseFinderClient.tracersEnabled;
        else if (hover(mx, my, lx, y + 206, 254, 27)) BaseFinderClient.transparentEnabled = !BaseFinderClient.transparentEnabled;
        else if (hover(mx, my, lx, y + 238, 254, 27)) BaseFinderClient.labelsEnabled = !BaseFinderClient.labelsEnabled;
        else if (hover(mx, my, rx, y + 78, 254, 27)) BaseFinderClient.hudEnabled = !BaseFinderClient.hudEnabled;
        else if (hover(mx, my, rx, y + 107, 254, 27)) BaseFinderClient.radarEnabled = !BaseFinderClient.radarEnabled;
        else if (hover(mx, my, rx, y + 136, 254, 27)) BaseFinderClient.chunkMapEnabled = !BaseFinderClient.chunkMapEnabled;
        else if (hover(mx, my, rx, y + 165, 254, 27)) BaseFinderClient.hotkeyListEnabled = !BaseFinderClient.hotkeyListEnabled;
        else if (hover(mx, my, rx, y + 224, 254, 25)) adjust(0, mx, rx, y + 224);
        else if (hover(mx, my, rx, y + 252, 254, 25)) adjust(1, mx, rx, y + 252);
        else if (hover(mx, my, rx, y + 280, 254, 25)) adjust(2, mx, rx, y + 280);
        else if (hover(mx, my, rx, y + 308, 254, 25)) adjust(3, mx, rx, y + 308);
        else if (hover(mx, my, rx, y + 336, 254, 25)) adjust(4, mx, rx, y + 336);
        else if (hover(mx, my, rx, y + 365, 254, 23)) {
            minecraft.gui.setScreen(new HudEditorScreen());
            return true;
        } else return super.mouseClicked(event, doubleClick);

        Scanner.clear();
        BaseFinderClient.markers = java.util.List.of();
        return true;
    }

    private void adjust(int option, double mouseX, int x, int y) {
        if (mouseX >= x + 230) { /* increase */ }
        else if (mouseX >= x + 158) { /* decrease */ }
        else return;
        int direction = mouseX >= x + 230 ? 1 : -1;
        switch (option) {
            case 0 -> Config.SCAN_RADIUS_CHUNKS = clamp(Config.SCAN_RADIUS_CHUNKS + direction, 2, 20);
            case 1 -> Config.RENDER_RANGE = clamp(Config.RENDER_RANGE + direction * 16, 32, 256);
            case 2 -> Config.BASE_SCORE = clamp(Config.BASE_SCORE + direction * 2, 4, 80);
            case 3 -> Config.MAX_Y = clamp(Config.MAX_Y + direction * 8, -64, 320);
            case 4 -> Config.CHUNKS_PER_TICK = clamp(Config.CHUNKS_PER_TICK + direction, 1, 4);
        }
        Config.save();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
