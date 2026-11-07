package com.basefinder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Menu principal: cuadrado, con pestanas, interruptores y sliders animados. */
public class BaseFinderScreen extends Screen {
    private static final int PW = 440, PH = 268, HEADER = 30, SIDE = 100, FOOT = 18, TAB_H = 26, ROW_GAP = 4;
    private static final String[] TAB_NAMES = {"VISUAL", "BASES", "ESCANEO", "HUD"};

    private final List<List<Row>> tabs = new ArrayList<>();
    private final float[] tabHover = new float[TAB_NAMES.length];
    private int tab = 0;
    private float open = 0f;
    private float indicatorY = 0f;
    private float contentFade = 1f;
    private float masterHover = 0f;
    private long lastNs = System.nanoTime();
    private Row dragging;
    private boolean dirty;

    // layout del ultimo frame (para los clics)
    private int curPx, curPy;

    public BaseFinderScreen() {
        super(Component.literal("Base Finder"));

        List<Row> visual = new ArrayList<>();
        visual.add(new Toggle("Cofres y almacenes", () -> Settings.containers, v -> Settings.containers = v));
        visual.add(new Toggle("Spawners", () -> Settings.spawners, v -> Settings.spawners = v));
        visual.add(new Toggle("Transparentes (borde violeta)", () -> Settings.transparent, v -> Settings.transparent = v));
        visual.add(new Toggle("Decoracion de bases", () -> Settings.decor, v -> Settings.decor = v));
        visual.add(new Toggle("Cajas de base (rojo)", () -> Settings.baseBoxes, v -> Settings.baseBoxes = v));
        tabs.add(visual);

        List<Row> bases = new ArrayList<>();
        bases.add(new Slider("Tamano de area", 20, 100, 5, () -> Settings.areaSize, v -> Settings.areaSize = v, " bloques"));
        bases.add(new Slider("Puntuacion minima", 5, 60, 1, () -> Settings.baseScore, v -> Settings.baseScore = v, ""));
        bases.add(new Slider("Contenedores minimos", 1, 8, 1, () -> Settings.minContainers, v -> Settings.minContainers = v, ""));
        tabs.add(bases);

        List<Row> scan = new ArrayList<>();
        scan.add(new Slider("Radio de escaneo", 4, 16, 1, () -> Settings.scanRadius, v -> Settings.scanRadius = v, " chunks"));
        scan.add(new Slider("Altura maxima (Y)", -60, 120, 4, () -> Settings.maxY, v -> Settings.maxY = v, ""));
        scan.add(new Slider("Distancia de dibujo", 32, 256, 16, () -> Settings.renderRange, v -> Settings.renderRange = v, "m"));
        scan.add(new Slider("Chunks por tick", 1, 4, 1, () -> Settings.chunksPerTick, v -> Settings.chunksPerTick = v, ""));
        tabs.add(scan);

        List<Row> hud = new ArrayList<>();
        hud.add(new Toggle("Mostrar HUD", () -> Settings.hud, v -> Settings.hud = v));
        hud.add(new Toggle("Avisos de base", () -> Settings.toasts, v -> Settings.toasts = v));
        tabs.add(hud);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Settings.save();
        if (dirty) {
            Scanner.clear();
            BaseFinderClient.refreshNow();
        }
        super.onClose();
    }

    // ---------------------------------------------------------------- dibujo

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;

        open = Ui.approach(open, 1f, dt * 4.5f);
        contentFade = Ui.approach(contentFade, 1f, dt * 6f);
        float e = Ui.ease(open);

        int px = (this.width - PW) / 2;
        int py = (this.height - PH) / 2 + Math.round((1f - e) * 16f);
        curPx = px;
        curPy = py;

        // fondo oscurecido
        Ui.rect(g, 0, 0, this.width, this.height, Ui.a(0xFF000000, 0.5f * e));

        // panel
        Ui.rect(g, px - 1, py - 1, PW + 2, PH + 2, Ui.a(Ui.ACCENT, 0.30f * e));
        Ui.rect(g, px, py, PW, PH, Ui.a(Ui.BG, e));

        drawHeader(g, px, py, mouseX, mouseY, dt, e);
        drawSidebar(g, px, py, mouseX, mouseY, dt, e);
        drawContent(g, px, py, mouseX, mouseY, dt, e);

        // pie
        int fy = py + PH - FOOT;
        Ui.rect(g, px, fy, PW, FOOT, Ui.a(Ui.HEADER, e));
        Ui.rect(g, px, fy, PW, 1, Ui.a(Ui.BORDER, e));
        Ui.text(g, "ESC cerrar    B activar ESP    Arrastra los sliders", px + 10, fy + 5, Ui.a(Ui.DIM, e));

        Ui.border(g, px, py, PW, PH, Ui.a(Ui.BORDER, e));
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void drawHeader(GuiGraphicsExtractor g, int px, int py, int mx, int my, float dt, float e) {
        Ui.rect(g, px, py, PW, HEADER, Ui.a(Ui.HEADER, e));
        Ui.rect(g, px, py + HEADER, PW, 1, Ui.a(Ui.ACCENT, e));
        Ui.rect(g, px + 10, py + 11, 8, 8, Ui.a(Ui.ACCENT, e));
        Ui.text(g, "BASE FINDER", px + 24, py + 11, Ui.a(Ui.TEXT, e));
        Ui.text(g, "26.3", px + 24 + Ui.width("BASE FINDER") + 8, py + 11, Ui.a(Ui.DIM, e));

        // boton maestro ESP
        int bw = 92, bh = 18;
        int bx = px + PW - bw - 10, by = py + (HEADER - bh) / 2;
        boolean hov = inside(mx, my, bx, by, bw, bh);
        masterHover = Ui.approach(masterHover, hov ? 1f : 0f, dt * 10f);
        boolean on = BaseFinderClient.enabled;
        int base = on ? Ui.mix(0xFF1C3A2A, 0xFF24503A, masterHover) : Ui.mix(0xFF3A1C1C, 0xFF502424, masterHover);
        int edge = on ? Ui.GOOD : Ui.BAD;
        Ui.rect(g, bx, by, bw, bh, Ui.a(base, e));
        Ui.border(g, bx, by, bw, bh, Ui.a(edge, e));
        String label = on ? "ESP  ACTIVO" : "ESP  APAGADO";
        Ui.text(g, label, bx + (bw - Ui.width(label)) / 2, by + 5, Ui.a(Ui.TEXT, e));
    }

    private void drawSidebar(GuiGraphicsExtractor g, int px, int py, int mx, int my, float dt, float e) {
        int sy = py + HEADER + 1;
        Ui.rect(g, px, sy, SIDE, PH - HEADER - 1 - FOOT, Ui.a(Ui.SIDEBAR, e));
        Ui.rect(g, px + SIDE, sy, 1, PH - HEADER - 1 - FOOT, Ui.a(Ui.BORDER, e));

        int top = sy + 10;
        float targetY = top + tab * TAB_H;
        if (indicatorY == 0f) indicatorY = targetY;
        indicatorY = Ui.approach(indicatorY, targetY, Math.max(1f, Math.abs(targetY - indicatorY)) * dt * 14f);

        for (int i = 0; i < TAB_NAMES.length; i++) {
            int ty = top + i * TAB_H;
            boolean hov = inside(mx, my, px, ty, SIDE, TAB_H);
            tabHover[i] = Ui.approach(tabHover[i], hov ? 1f : 0f, dt * 10f);
            if (tabHover[i] > 0.01f) {
                Ui.rect(g, px, ty, SIDE, TAB_H, Ui.a(Ui.ROW_HOVER, e * tabHover[i]));
            }
            int c = i == tab ? Ui.TEXT : Ui.mix(Ui.DIM, Ui.TEXT, tabHover[i]);
            Ui.text(g, TAB_NAMES[i], px + 16 + Math.round(tabHover[i] * 3f), ty + (TAB_H - 8) / 2, Ui.a(c, e));
        }
        // indicador deslizante
        Ui.rect(g, px, Math.round(indicatorY), 3, TAB_H, Ui.a(Ui.ACCENT, e));
    }

    private void drawContent(GuiGraphicsExtractor g, int px, int py, int mx, int my, float dt, float e) {
        int x = px + SIDE + 14 + Math.round((1f - Ui.ease(contentFade)) * 10f);
        int w = PW - SIDE - 28;
        int y = py + HEADER + 14;
        float alpha = e * Ui.ease(contentFade);

        Ui.text(g, TAB_NAMES[tab], x, y, Ui.a(Ui.ACCENT, alpha));
        Ui.rect(g, x, y + 12, w, 1, Ui.a(Ui.BORDER, alpha));
        y += 20;

        for (Row r : tabs.get(tab)) {
            r.draw(g, x, y, w, mx, my, dt, alpha);
            y += r.h + ROW_GAP;
        }
    }

    // ----------------------------------------------------------------- ratón

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        // boton maestro
        int bw = 92, bh = 18;
        int bx = curPx + PW - bw - 10, by = curPy + (HEADER - bh) / 2;
        if (inside(mx, my, bx, by, bw, bh)) {
            BaseFinderClient.setEnabled(!BaseFinderClient.enabled);
            return true;
        }

        // pestanas
        int top = curPy + HEADER + 1 + 10;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            if (inside(mx, my, curPx, top + i * TAB_H, SIDE, TAB_H)) {
                if (i != tab) {
                    tab = i;
                    contentFade = 0f;
                }
                return true;
            }
        }

        // filas
        for (Row r : tabs.get(tab)) {
            if (r.hit(mx, my)) {
                if (r.click(mx)) {
                    dirty = true;
                    if (r instanceof Slider) dragging = r;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null) {
            dragging.drag((int) event.x());
            dirty = true;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    // ------------------------------------------------------------------ filas

    private abstract static class Row {
        int h;
        int lx, ly, lw;
        float hover;

        abstract void draw(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, float dt, float alpha);

        boolean hit(int mx, int my) {
            return mx >= lx && mx < lx + lw && my >= ly && my < ly + h;
        }

        abstract boolean click(int mx);

        void drag(int mx) {}
    }

    private static final class Toggle extends Row {
        private final String label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float anim = -1f;

        Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            this.label = label;
            this.get = get;
            this.set = set;
            this.h = 24;
        }

        @Override
        void draw(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, float dt, float alpha) {
            lx = x; ly = y; lw = w;
            boolean on = get.getAsBoolean();
            if (anim < 0f) anim = on ? 1f : 0f;
            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            hover = Ui.approach(hover, hov ? 1f : 0f, dt * 10f);
            anim = Ui.approach(anim, on ? 1f : 0f, dt * 9f);

            Ui.rect(g, x, y, w, h, Ui.a(Ui.mix(Ui.ROW, Ui.ROW_HOVER, hover), alpha));
            Ui.rect(g, x, y, 2, h, Ui.a(Ui.ACCENT, alpha * anim));
            Ui.text(g, label, x + 10, y + (h - 8) / 2, Ui.a(Ui.mix(Ui.DIM, Ui.TEXT, Math.max(hover, anim)), alpha));

            int sw = 30, sh = 12;
            int sx = x + w - sw - 10, sy = y + (h - sh) / 2;
            Ui.rect(g, sx, sy, sw, sh, Ui.a(Ui.mix(Ui.TRACK, Ui.ACCENT_DARK, anim), alpha));
            Ui.border(g, sx, sy, sw, sh, Ui.a(Ui.mix(Ui.BORDER, Ui.ACCENT, anim), alpha));
            int kx = sx + 2 + Math.round((sw - 4 - 8) * anim);
            Ui.rect(g, kx, sy + 2, 8, 8, Ui.a(Ui.mix(Ui.DIM, 0xFFFFFFFF, anim), alpha));
        }

        @Override
        boolean click(int mx) {
            set.accept(!get.getAsBoolean());
            return true;
        }
    }

    private static final class Slider extends Row {
        private final String label, suffix;
        private final int min, max, step;
        private final IntSupplier get;
        private final IntConsumer set;
        private float disp = -1f;
        private int bx, bw;

        Slider(String label, int min, int max, int step, IntSupplier get, IntConsumer set, String suffix) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.get = get;
            this.set = set;
            this.suffix = suffix;
            this.h = 34;
        }

        @Override
        void draw(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, float dt, float alpha) {
            lx = x; ly = y; lw = w;
            int v = get.getAsInt();
            float target = (v - min) / (float) (max - min);
            if (disp < 0f) disp = target;
            disp = Ui.approach(disp, target, Math.max(0.002f, Math.abs(target - disp)) * dt * 14f);

            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            hover = Ui.approach(hover, hov ? 1f : 0f, dt * 10f);
            Ui.rect(g, x, y, w, h, Ui.a(Ui.mix(Ui.ROW, Ui.ROW_HOVER, hover), alpha));

            Ui.text(g, label, x + 10, y + 6, Ui.a(Ui.mix(Ui.DIM, Ui.TEXT, hover), alpha));
            String val = v + suffix;
            Ui.text(g, val, x + w - 10 - Ui.width(val), y + 6, Ui.a(Ui.ACCENT, alpha));

            bx = x + 10;
            bw = w - 20;
            int by = y + 21, bh = 6;
            Ui.rect(g, bx, by, bw, bh, Ui.a(Ui.TRACK, alpha));
            int fill = Math.round(bw * disp);
            Ui.rect(g, bx, by, fill, bh, Ui.a(Ui.ACCENT, alpha));
            Ui.border(g, bx, by, bw, bh, Ui.a(Ui.BORDER, alpha));
            Ui.rect(g, bx + fill - 2, by - 3, 4, bh + 6, Ui.a(0xFFFFFFFF, alpha));
        }

        @Override
        boolean click(int mx) {
            drag(mx);
            return true;
        }

        @Override
        void drag(int mx) {
            if (bw <= 0) return;
            float f = Math.max(0f, Math.min(1f, (mx - bx) / (float) bw));
            int v = min + Math.round(f * (max - min) / (float) step) * step;
            set.accept(Math.max(min, Math.min(max, v)));
        }
    }
}
