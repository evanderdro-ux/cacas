package com.basefinder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Ajustes en vivo (los cambia el menu) y guardados en config/basefinder.properties. */
public final class Settings {
    private Settings() {}

    public static boolean containers = true;
    public static boolean spawners = true;
    public static boolean transparent = true;
    public static boolean decor = true;
    public static boolean baseBoxes = true;
    public static boolean hud = true;
    public static boolean toasts = true;

    public static int scanRadius = 10;
    public static int chunksPerTick = 1;
    public static int maxY = 56;
    public static int renderRange = 160;
    public static int baseScore = 20;
    public static int minContainers = 2;
    public static int areaSize = 50;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("basefinder.properties");
    }

    private static boolean b(Properties p, String k, boolean d) {
        String v = p.getProperty(k);
        return v == null ? d : Boolean.parseBoolean(v);
    }

    private static int i(Properties p, String k, int d) {
        try {
            String v = p.getProperty(k);
            return v == null ? d : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return d;
        }
    }

    public static void load() {
        Properties p = new Properties();
        try (var in = Files.newInputStream(file())) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        containers = b(p, "containers", containers);
        spawners = b(p, "spawners", spawners);
        transparent = b(p, "transparent", transparent);
        decor = b(p, "decor", decor);
        baseBoxes = b(p, "baseBoxes", baseBoxes);
        hud = b(p, "hud", hud);
        toasts = b(p, "toasts", toasts);
        scanRadius = i(p, "scanRadius", scanRadius);
        chunksPerTick = i(p, "chunksPerTick", chunksPerTick);
        maxY = i(p, "maxY", maxY);
        renderRange = i(p, "renderRange", renderRange);
        baseScore = i(p, "baseScore", baseScore);
        minContainers = i(p, "minContainers", minContainers);
        areaSize = i(p, "areaSize", areaSize);
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("containers", String.valueOf(containers));
        p.setProperty("spawners", String.valueOf(spawners));
        p.setProperty("transparent", String.valueOf(transparent));
        p.setProperty("decor", String.valueOf(decor));
        p.setProperty("baseBoxes", String.valueOf(baseBoxes));
        p.setProperty("hud", String.valueOf(hud));
        p.setProperty("toasts", String.valueOf(toasts));
        p.setProperty("scanRadius", String.valueOf(scanRadius));
        p.setProperty("chunksPerTick", String.valueOf(chunksPerTick));
        p.setProperty("maxY", String.valueOf(maxY));
        p.setProperty("renderRange", String.valueOf(renderRange));
        p.setProperty("baseScore", String.valueOf(baseScore));
        p.setProperty("minContainers", String.valueOf(minContainers));
        p.setProperty("areaSize", String.valueOf(areaSize));
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Base Finder");
        } catch (IOException ignored) {
        }
    }
}
