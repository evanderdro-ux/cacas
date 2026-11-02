package com.basefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudScreen extends Screen {
    private static final String[] LABELS = {
        "ESP / ESCANEO", "RADAR CIRCULAR", "MAPA DE CHUNKS",
        "DETECTOR DE ZONAS", "BORDES VIOLETAS", "LISTA DE TECLAS", "HUD COMPLETO"
    };

    public HudScreen() { super(Component.literal("BaseFinder Control")); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int w = 330, h = 250, x = (width - w) / 2, y = (height - h) / 2;
        g.fill(0, 0, width, height, 0x99040009);
        g.fill(x, y, x + w, y + h, 0xF00C0915);
        g.fill(x, y, x + w, y + 3, 0xFFB34CFF);
        g.outline(x, y, w, h, 0xFF70428D);
        g.text(font, "BASEFINDER", x + 18, y + 15, 0xFFFFFFFF, true);
        g.text(font, "CONTROL CENTER  /  v2.0", x + 18, y + 31, 0xFFBBA5D4, false);
        boolean[] states = states();
        for (int i = 0; i < LABELS.length; i++) {
            int rowY = y + 54 + i * 24;
            g.fill(x + 14, rowY - 4, x + w - 14, rowY + 17, 0xFF17111F);
            g.text(font, LABELS[i], x + 23, rowY + 2, 0xFFECE2F7, false);
            int c = states[i] ? 0xFF55D99A : 0xFF6D6578;
            g.fill(x + w - 58, rowY + 2, x + w - 24, rowY + 13, c);
            g.text(font, states[i] ? "ON" : "OFF", x + w - 53, rowY + 3, 0xFF08070C, true);
        }
        g.text(font, "Click a row to toggle  •  ESC to close", x + 18, y + h - 19, 0xFFAA98BC, false);
    }

    private boolean[] states() {
        return new boolean[] {BaseFinderClient.enabled, BaseFinderClient.radarEnabled,
            BaseFinderClient.chunkMapEnabled, BaseFinderClient.detectorEnabled,
            BaseFinderClient.transparentEnabled, BaseFinderClient.hotkeyListEnabled,
            BaseFinderClient.hudEnabled};
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int top = (height - 250) / 2 + 50;
            int row = (int)((mouseY - top) / 24);
            if (mouseX >= (width - 330) / 2 && mouseX <= (width + 330) / 2 && row >= 0 && row < 7) {
                switch (row) {
                    case 0 -> BaseFinderClient.setScan(!BaseFinderClient.enabled);
                    case 1 -> BaseFinderClient.radarEnabled = !BaseFinderClient.radarEnabled;
                    case 2 -> BaseFinderClient.chunkMapEnabled = !BaseFinderClient.chunkMapEnabled;
                    case 3 -> BaseFinderClient.detectorEnabled = !BaseFinderClient.detectorEnabled;
                    case 4 -> BaseFinderClient.transparentEnabled = !BaseFinderClient.transparentEnabled;
                    case 5 -> BaseFinderClient.hotkeyListEnabled = !BaseFinderClient.hotkeyListEnabled;
                    case 6 -> BaseFinderClient.hudEnabled = !BaseFinderClient.hudEnabled;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean isPauseScreen() { return false; }
}
