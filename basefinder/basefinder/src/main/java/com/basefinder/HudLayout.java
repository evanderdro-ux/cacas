package com.basefinder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Posiciones independientes y persistentes de cada módulo del HUD. */
public final class HudLayout {
    public static final int STATUS = 0;
    public static final int RADAR = 1;
    public static final int CHUNKS = 2;
    public static final int TARGETS = 3;
    public static final int HOTKEYS = 4;
    public static final int COUNT = 5;

    private static final int[] WIDTH = {166, 112, 120, 180, 154};
    private static final int[] HEIGHT = {70, 112, 104, 64, 48};
    private static final int[] X = {-1, -1, -1, -1, -1};
    private static final int[] Y = {-1, -1, -1, -1, -1};

    private HudLayout() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("basefinder-hud.properties");
    }

    public static void load() {
        Properties p = new Properties();
        try {
            Path f = file();
            if (Files.isRegularFile(f)) {
                try (var in = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    p.load(in);
                }
            }
        } catch (IOException ignored) {}
        for (int i = 0; i < COUNT; i++) {
            X[i] = read(p, "x" + i);
            Y[i] = read(p, "y" + i);
        }
    }

    private static int read(Properties p, String key) {
        try {
            return Integer.parseInt(p.getProperty(key, "-1"));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static int width(int id) { return WIDTH[id]; }
    public static int height(int id) { return HEIGHT[id]; }

    public static int x(int id, int screenWidth) {
        int value = X[id] < 0 ? defaultX(id, screenWidth) : X[id];
        return clamp(value, 0, Math.max(0, screenWidth - WIDTH[id]));
    }

    public static int y(int id, int screenHeight) {
        int value = Y[id] < 0 ? defaultY(id, screenHeight) : Y[id];
        return clamp(value, 0, Math.max(0, screenHeight - HEIGHT[id]));
    }

    private static int defaultX(int id, int w) {
        return switch (id) {
            case RADAR, CHUNKS -> w - WIDTH[id] - 12;
            case TARGETS -> (w - WIDTH[id]) / 2;
            default -> 12;
        };
    }

    private static int defaultY(int id, int h) {
        return switch (id) {
            case CHUNKS -> h - HEIGHT[id] - 12;
            case TARGETS -> h - HEIGHT[id] - 12;
            case HOTKEYS -> h - HEIGHT[id] - 12;
            default -> 12;
        };
    }

    public static void move(int id, int x, int y, int screenWidth, int screenHeight) {
        X[id] = clamp(x, 0, Math.max(0, screenWidth - WIDTH[id]));
        Y[id] = clamp(y, 0, Math.max(0, screenHeight - HEIGHT[id]));
    }

    public static void reset() {
        for (int i = 0; i < COUNT; i++) {
            X[i] = -1;
            Y[i] = -1;
        }
        save();
    }

    public static void save() {
        Properties p = new Properties();
        for (int i = 0; i < COUNT; i++) {
            p.setProperty("x" + i, Integer.toString(X[i]));
            p.setProperty("y" + i, Integer.toString(Y[i]));
        }
        try {
            Files.createDirectories(file().getParent());
            try (var out = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                p.store(out, "BaseFinder HUD layout");
            }
        } catch (IOException ignored) {}
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
