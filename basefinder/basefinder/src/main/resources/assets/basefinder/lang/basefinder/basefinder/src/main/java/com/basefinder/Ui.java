package com.basefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Utilidades de dibujo: todo cuadrado, sin esquinas redondeadas. */
public final class Ui {
    private Ui() {}

    // Paleta
    public static final int ACCENT = 0xFF9D33FF;       // violeta
    public static final int ACCENT_DARK = 0xFF3A1766;
    public static final int BG = 0xF00C0C10;
    public static final int HEADER = 0xFF121218;
    public static final int SIDEBAR = 0xFF0F0F14;
    public static final int ROW = 0xFF14141B;
    public static final int ROW_HOVER = 0xFF1C1C26;
    public static final int TRACK = 0xFF1E1E28;
    public static final int BORDER = 0xFF2A2A36;
    public static final int TEXT = 0xFFF0F0F6;
    public static final int DIM = 0xFF8A8A9A;
    public static final int GOOD = 0xFF3DDC84;
    public static final int BAD = 0xFFFF4D4D;

    /** Multiplica el alfa de un color ARGB por 'alpha' (0..1). */
    public static int a(int argb, float alpha) {
        int al = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 0xFF) + ((((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t));
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int g = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Acerca cur a target a velocidad fija (independiente de los FPS si step = dt*vel). */
    public static float approach(float cur, float target, float step) {
        if (cur < target) return Math.min(target, cur + step);
        return Math.max(target, cur - step);
    }

    /** Ease-out cubico. */
    public static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (((color >>> 24) & 0xFF) == 0 || w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, color);
    }

    public static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        rect(g, x, y, w, 1, color);
        rect(g, x, y + h - 1, w, 1, color);
        rect(g, x, y + 1, 1, h - 2, color);
        rect(g, x + w - 1, y + 1, 1, h - 2, color);
    }

    public static void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        if (((color >>> 24) & 0xFF) < 12) return;
        g.text(Minecraft.getInstance().font, s, x, y, color, false);
    }

    public static int width(String s) {
        return Minecraft.getInstance().font.width(s);
    }
}
