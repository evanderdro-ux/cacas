package com.basefinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Escanea chunks cargados de forma incremental y puntua zonas como "base". */
public final class Scanner {
    private Scanner() {}

    public record Hit(int x, int y, int z, BlockKind kind) {}

    public record BaseInfo(double x, double y, double z, int score, int containers) {}

    private record ChunkData(long tick, List<Hit> hits) {}

    private static final Map<Long, ChunkData> DATA = new HashMap<>();
    private static final Set<Long> KNOWN_BASES = new HashSet<>();
    private static ClientLevel lastLevel;
    private static long tickCounter;

    /** Bases detectadas en el ultimo calculo (para el HUD). */
    public static volatile List<BaseInfo> bases = List.of();

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    public static int chunksScanned() {
        return DATA.size();
    }

    public static void clear() {
        DATA.clear();
        KNOWN_BASES.clear();
        bases = List.of();
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        if (level != lastLevel) {
            clear();
            lastLevel = level;
        }
        tickCounter++;

        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int R = Settings.scanRadius;

        DATA.keySet().removeIf(k -> {
            int cx = (int) (k >> 32);
            int cz = (int) (long) k;
            return Math.abs(cx - pcx) > R + 2 || Math.abs(cz - pcz) > R + 2;
        });

        int budget = Settings.chunksPerTick;
        outer:
        for (int r = 0; r <= R; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    ChunkData d = DATA.get(key(cx, cz));
                    if (d != null && tickCounter - d.tick() < Config.RESCAN_TICKS) continue;
                    if (!level.getChunkSource().hasChunk(cx, cz)) continue;
                    DATA.put(key(cx, cz), scanChunk(level, cx, cz));
                    if (--budget <= 0) break outer;
                }
            }
        }
    }

    private static ChunkData scanChunk(ClientLevel level, int cx, int cz) {
        List<Hit> hits = new ArrayList<>();
        LevelChunk chunk = level.getChunk(cx, cz);
        LevelChunkSection[] sections = chunk.getSections();
        int minY = level.getMinY();
        int decor = 0;

        for (int i = 0; i < sections.length; i++) {
            int baseY = minY + i * 16;
            if (baseY > Settings.maxY) break;
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState st = section.getBlockState(x, y, z);
                        if (st.isAir()) continue;
                        BlockKind kind = BlockKind.of(st.getBlock());
                        if (kind == BlockKind.NONE) continue;
                        if (kind == BlockKind.DECOR && ++decor > Config.MAX_DECOR_PER_CHUNK) continue;
                        hits.add(new Hit((cx << 4) + x, baseY + y, (cz << 4) + z, kind));
                    }
                }
            }
        }
        return new ChunkData(tickCounter, hits);
    }

    private static final class Cell {
        int score, containers;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        void add(Hit h) {
            score += h.kind().weight;
            if (h.kind().isContainer()) containers++;
            if (h.kind().weight > 0) {
                minX = Math.min(minX, h.x()); maxX = Math.max(maxX, h.x());
                minY = Math.min(minY, h.y()); maxY = Math.max(maxY, h.y());
                minZ = Math.min(minZ, h.z()); maxZ = Math.max(maxZ, h.z());
            }
        }

        boolean isBase() {
            return minX != Integer.MAX_VALUE
                    && score >= Settings.baseScore
                    && containers >= Settings.minContainers;
        }
    }

    /** Construye la lista de cajas a dibujar (hilo principal, cada ~20 ticks). */
    public static List<Marker> buildMarkers(double px, double pz) {
        int A = Math.max(8, Settings.areaSize);
        Map<Long, Cell> cells = new HashMap<>();
        for (ChunkData d : DATA.values()) {
            for (Hit h : d.hits()) {
                long ck = ((long) Math.floorDiv(h.x(), A) << 32) ^ (Math.floorDiv(h.z(), A) & 0xffffffffL);
                cells.computeIfAbsent(ck, k -> new Cell()).add(h);
            }
        }

        double range = Settings.renderRange;
        double range2 = range * range;
        List<Marker> out = new ArrayList<>();
        List<Marker> boxes = new ArrayList<>();
        List<BaseInfo> found = new ArrayList<>();

        for (Map.Entry<Long, Cell> e : cells.entrySet()) {
            Cell c = e.getValue();
            if (!c.isBase()) continue;
            double cx = (c.minX + c.maxX) / 2.0 + 0.5;
            double cz = (c.minZ + c.maxZ) / 2.0 + 0.5;
            found.add(new BaseInfo(cx, (c.minY + c.maxY) / 2.0, cz, c.score, c.containers));

            if (KNOWN_BASES.add(e.getKey()) && Settings.toasts && BaseFinderClient.enabled) {
                int dist = (int) Math.round(Math.hypot(cx - px, cz - pz));
                Hud.toast("BASE DETECTADA   X " + Math.round(cx) + "  Z " + Math.round(cz) + "   " + dist + "m");
            }
            if (Settings.baseBoxes) {
                boxes.add(new Marker(c.minX - 0.5, c.minY - 0.5, c.minZ - 0.5,
                        c.maxX + 1.5, c.maxY + 1.5, c.maxZ + 1.5,
                        1f, 0.15f, 0.15f, 0.9f, true, 0.25f));
            }
        }
        bases = List.copyOf(found);

        for (ChunkData d : DATA.values()) {
            for (Hit h : d.hits()) {
                double dx = h.x() + 0.5 - px, dz = h.z() + 0.5 - pz;
                if (dx * dx + dz * dz > range2) continue;
                long ck = ((long) Math.floorDiv(h.x(), A) << 32) ^ (Math.floorDiv(h.z(), A) & 0xffffffffL);
                Cell c = cells.get(ck);
                boolean inBase = c != null && c.isBase();
                BlockKind k = h.kind();

                if (k.isContainer() && !Settings.containers) continue;
                if (k == BlockKind.SPAWNER && !Settings.spawners) continue;
                if (k == BlockKind.TRANSPARENT && !Settings.transparent) continue;
                if (k == BlockKind.DECOR && (!Settings.decor || !inBase)) continue;

                if (k == BlockKind.TRANSPARENT) {
                    float[] v = Config.VIOLET;
                    out.add(new Marker(h.x() - 0.01, h.y() - 0.01, h.z() - 0.01,
                            h.x() + 1.01, h.y() + 1.01, h.z() + 1.01,
                            v[0], v[1], v[2], v[3], true, 0.05f));
                } else if (k == BlockKind.DECOR) {
                    out.add(new Marker(h.x() + 0.2, h.y() + 0.2, h.z() + 0.2,
                            h.x() + 0.8, h.y() + 0.8, h.z() + 0.8,
                            k.r, k.g, k.b, 0.35f, false, 0f));
                } else {
                    out.add(new Marker(h.x() - 0.005, h.y() - 0.005, h.z() - 0.005,
                            h.x() + 1.005, h.y() + 1.005, h.z() + 1.005,
                            k.r, k.g, k.b, 0.45f, false, 0f));
                }
            }
        }

        out.sort(Comparator.comparingDouble(m -> {
            double dx = (m.x0() + m.x1()) / 2 - px, dz = (m.z0() + m.z1()) / 2 - pz;
            return dx * dx + dz * dz;
        }));
        if (out.size() > Config.MAX_MARKERS) out = new ArrayList<>(out.subList(0, Config.MAX_MARKERS));

        List<Marker> result = new ArrayList<>(boxes);
        result.addAll(out);
        return List.copyOf(result);
    }
}
