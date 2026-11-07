package com.basefinder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

/** HUD cuadrado: panel de estado + avisos (toasts) animados. */
public final class Hud {
    private Hud() {}

    private record Toast(String text, long born) {}

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final long TOAST_LIFE_MS = 4200;
    private static final String[] COMPASS = {"E", "SE", "S", "SO", "O", "NO", "N", "NE"};

    private static float panelAnim = 0f;
    private static float pulse = 0f;
    private static long lastNs = System.nanoTime();

    public static void init() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(BaseFinderClient.MOD_ID, "hud"), Hud::render);
    }

    public static void toast(String text) {
        TOASTS.add(new Toast(text, System.currentTimeMillis()));
        while (TOASTS.size() > 4) TOASTS.remove(0);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;

        boolean show = BaseFinderClient.enabled && Settings.hud;
        panelAnim = Ui.approach(panelAnim, show ? 1f : 0f, dt * 5f);
        pulse = (pulse + dt * 2.2f) % 2f;

        if (panelAnim > 0.01f) drawPanel(g, mc, Ui.ease(panelAnim));
        drawToasts(g, mc);
    }

    private static void drawPanel(GuiGraphicsExtractor g, Minecraft mc, float e) {
        int w = 150, h = 78;
        int x = 6 - Math.round((1f - e) * 40f);
        int y = 6;

        Ui.rect(g, x, y, w, h, Ui.a(Ui.BG, e));
        Ui.border(g, x, y, w, h, Ui.a(Ui.BORDER, e));
        Ui.rect(g, x, y, 2, h, Ui.a(Ui.ACCENT, e));

        // cabecera con indicador pulsante
        float p = pulse < 1f ? pulse : 2f - pulse;
        Ui.rect(g, x + 10, y + 7, 6, 6, Ui.a(Ui.mix(Ui.ACCENT_DARK, Ui.ACCENT, p), e));
        Ui.text(g, "BASE FINDER", x + 22, y + 6, Ui.a(Ui.TEXT, e));
        Ui.rect(g, x + 8, y + 19, w - 16, 1, Ui.a(Ui.BORDER, e));

        int markers = BaseFinderClient.markers.size();
        List<Scanner.BaseInfo> bases = Scanner.bases;

        line(g, x, y + 24, "CHUNKS", String.valueOf(Scanner.chunksScanned()), Ui.TEXT, e);
        line(g, x, y + 35, "OBJETOS", String.valueOf(markers), Ui.TEXT, e);
        line(g, x, y + 46, "BASES", String.valueOf(bases.size()), bases.isEmpty() ? Ui.DIM : Ui.GOOD, e);

        Scanner.BaseInfo best = null;
        double bestD = Double.MAX_VALUE;
        double px = mc.player.getX(), pz = mc.player.getZ();
        for (Scanner.BaseInfo b : bases) {
            double d = Math.hypot(b.x() - px, b.z() - pz);
            if (d < bestD) { bestD = d; best = b; }
        }
        if (best != null) {
            int idx = (int) Math.round(Math.atan2(best.z() - pz, best.x() - px) / (Math.PI / 4.0));
            String dir = COMPASS[((idx % 8) + 8) % 8];
            line(g, x, y + 57, "CERCANA", Math.round(bestD) + "m " + dir, Ui.ACCENT, e);
            Ui.text(g, "X " + Math.round(best.x()) + "  Z " + Math.round(best.z()), x + 10, y + 67, Ui.a(Ui.DIM, e));
        } else {
            line(g, x, y + 57, "CERCANA", "--", Ui.DIM, e);
        }
    }

    private static void line(GuiGraphicsExtractor g, int x, int y, String label, String value, int color, float e) {
        Ui.text(g, label, x + 10, y, Ui.a(Ui.DIM, e));
        Ui.text(g, value, x + 150 - 10 - Ui.width(value), y, Ui.a(color, e));
    }

    private static void drawToasts(GuiGraphicsExtractor g, Minecraft mc) {
        long now = System.currentTimeMillis();
        TOASTS.removeIf(t -> now - t.born() > TOAST_LIFE_MS);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int y = 6;
        for (Toast t : TOASTS) {
            long age = now - t.born();
            float in = Ui.ease(age / 260f);
            float out = age > TOAST_LIFE_MS - 300 ? Math.max(0f, (TOAST_LIFE_MS - age) / 300f) : 1f;
            float e = in * out;

            int w = Ui.width(t.text()) + 24;
            int h = 20;
            int x = screenW - w - 6 + Math.round((1f - in) * (w + 10));

            Ui.rect(g, x, y, w, h, Ui.a(Ui.BG, out));
            Ui.border(g, x, y, w, h, Ui.a(Ui.BORDER, out));
            Ui.rect(g, x, y, 3, h, Ui.a(Ui.ACCENT, out));
            Ui.text(g, t.text(), x + 12, y + 6, Ui.a(Ui.TEXT, e));
            // barra de tiempo
            float left = 1f - (age / (float) TOAST_LIFE_MS);
            Ui.rect(g, x + 3, y + h - 2, Math.round((w - 3) * Math.max(0f, left)), 2, Ui.a(Ui.ACCENT, out));
            y += h + 4;
        }
    }
}
