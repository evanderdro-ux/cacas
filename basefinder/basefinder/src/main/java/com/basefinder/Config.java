package com.basefinder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Ajustes persistentes de BaseFinder. */
public final class Config {
    private Config() {}

    public static int SCAN_RADIUS_CHUNKS = 10;
    public static int CHUNKS_PER_TICK = 1;
    public static int RESCAN_TICKS = 400;
    public static int MAX_Y = 56;

    public static int AREA_SIZE = 32;
    public static int BASE_SCORE = 16;
    public static int BASE_MIN_CONTAINERS = 2;
    public static int CHUNK_SCORE = 7;

    public static int RENDER_RANGE = 160;
    public static int MAX_MARKERS = 1100;
    public static int MAX_VERTICES = 300_000;
    public static int MAX_DECOR_PER_CHUNK = 180;

    public static float[] VIOLET = {0.72f, 0.34f, 1.00f, 1.00f};

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("basefinder.properties");
    }

    public static void load() {
        Properties p = new Properties();
        Path f = file();
        if (Files.isRegularFile(f)) {
            try (Reader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                p.load(reader);
            } catch (IOException ignored) {}
        }
        SCAN_RADIUS_CHUNKS = intValue(p, "scanRadius", 10, 2, 20);
        CHUNKS_PER_TICK = intValue(p, "chunksPerTick", 1, 1, 4);
        RESCAN_TICKS = intValue(p, "rescanTicks", 400, 100, 2400);
        MAX_Y = intValue(p, "maxY", 56, -64, 320);
        AREA_SIZE = intValue(p, "areaSize", 32, 16, 64);
        BASE_SCORE = intValue(p, "baseScore", 16, 4, 80);
        BASE_MIN_CONTAINERS = intValue(p, "baseMinContainers", 2, 1, 12);
        CHUNK_SCORE = intValue(p, "chunkScore", 7, 2, 40);
        RENDER_RANGE = intValue(p, "renderRange", 160, 32, 256);
        MAX_MARKERS = intValue(p, "maxMarkers", 1100, 100, 2000);
        CHUNKS_PER_TICK = Math.max(1, Math.min(4, CHUNKS_PER_TICK));
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("scanRadius", Integer.toString(SCAN_RADIUS_CHUNKS));
        p.setProperty("chunksPerTick", Integer.toString(CHUNKS_PER_TICK));
        p.setProperty("rescanTicks", Integer.toString(RESCAN_TICKS));
        p.setProperty("maxY", Integer.toString(MAX_Y));
        p.setProperty("areaSize", Integer.toString(AREA_SIZE));
        p.setProperty("baseScore", Integer.toString(BASE_SCORE));
        p.setProperty("baseMinContainers", Integer.toString(BASE_MIN_CONTAINERS));
        p.setProperty("chunkScore", Integer.toString(CHUNK_SCORE));
        p.setProperty("renderRange", Integer.toString(RENDER_RANGE));
        p.setProperty("maxMarkers", Integer.toString(MAX_MARKERS));
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                p.store(writer, "BaseFinder settings");
            }
        } catch (IOException ignored) {}
    }

    private static int intValue(Properties p, String key, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(p.getProperty(key, Integer.toString(fallback)))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
