package com.basefinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;

/** Escanea incrementalmente solo chunks que el cliente ya tiene cargados. */
public final class Scanner {
    private Scanner() {}

    public record Hit(int x, int y, int z, BlockKind kind) {}
    private record ChunkData(long tick, List<Hit> hits) {}

    private static final Map<Long, ChunkData> DATA = new HashMap<>();
    private static ClientLevel lastLevel;
    private static long tickCounter;

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    public static void clear() {
        DATA.clear();
        lastLevel = null;
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        if (level != lastLevel) {
            DATA.clear();
            lastLevel = level;
            tickCounter = 0;
        }
        tickCounter++;

        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int radius = Config.SCAN_RADIUS_CHUNKS;

        DATA.keySet().removeIf(k -> {
            int cx = (int) (k >> 32);
            int cz = (int) (long) k;
            return Math.abs(cx - pcx) > radius + 2 || Math.abs(cz - pcz) > radius + 2;
        });

        int budget = Config.CHUNKS_PER_TICK;
        outer:
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    ChunkData old = DATA.get(key(cx, cz));
                    if (old != null && tickCounter - old.tick() < Config.RESCAN_TICKS) continue;

                    // Importante: no se solicita ni se fuerza la carga de chunks.
                    if (!level.getChunkSource().hasChunk(cx, cz)) continue;
                    LevelChunk chunk = level.getChunk(cx, cz);
                    DATA.put(key(cx, cz), scanChunk(level, chunk, cx, cz));
                    if (--budget <= 0) break outer;
                }
            }
        }
    }

    private static ChunkData scanChunk(ClientLevel level, LevelChunk chunk, int cx, int cz) {
        List<Hit> hits = new ArrayList<>();
        LevelChunkSection[] sections = chunk.getSections();
        int minY = level.getMinY();

        for (int i = 0; i < sections.length; i++) {
            int baseY = minY + i * 16;
            if (baseY > Config.MAX_Y) break;
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;

            int yLimit = Math.min(16, Config.MAX_Y - baseY + 1);
            for (int y = 0; y < yLimit; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.isAir()) continue;
                        BlockKind kind = BlockKind.of(state.getBlock());
                        if (kind == BlockKind.NONE) continue;
                        hits.add(new Hit((cx << 4) + x, baseY + y, (cz << 4) + z, kind));
                    }
                }
            }
        }
        return new ChunkData(tickCounter, List.copyOf(hits));
    }

    private static final class Cell {
        int score;
        int containers;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        void add(Hit hit) {
            score += hit.kind().weight;
            if (hit.kind().isContainer()) containers++;
            if (hit.kind().weight > 0) {
                minX = Math.min(minX, hit.x());
                minY = Math.min(minY, hit.y());
                minZ = Math.min(minZ, hit.z());
                maxX = Math.max(maxX, hit.x());
                maxY = Math.max(maxY, hit.y());
                maxZ = Math.max(maxZ, hit.z());
            }
        }

        boolean isBase() {
            return score >= Config.BASE_SCORE && containers >= Config.BASE_MIN_CONTAINERS
                    && minX != Integer.MAX_VALUE;
        }
    }

    private static final class ChunkStats {
        int score, containers;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        void add(Hit hit) {
            score += hit.kind().weight;
            if (hit.kind().isContainer()) containers++;
            if (hit.kind().weight > 0) {
                minY = Math.min(minY, hit.y());
                maxY = Math.max(maxY, hit.y());
            }
        }
    }

    /** Prepara cajas de bases, chunks prometedores y objetivos individuales. */
    public static List<Marker> buildMarkers(double px, double pz) {
        int area = Config.AREA_SIZE;
        Map<Long, Cell> cells = new HashMap<>();
        Map<Long, ChunkStats> chunks = new HashMap<>();

        for (Map.Entry<Long, ChunkData> entry : DATA.entrySet()) {
            int cx = (int) (entry.getKey() >> 32);
            int cz = (int) (long) entry.getKey();
            ChunkStats stats = new ChunkStats();
            for (Hit hit : entry.getValue().hits()) {
                long cellKey = ((long) Math.floorDiv(hit.x(), area) << 32)
                        ^ (Math.floorDiv(hit.z(), area) & 0xffffffffL);
                cells.computeIfAbsent(cellKey, ignored -> new Cell()).add(hit);
                stats.add(hit);
            }
            chunks.put(key(cx, cz), stats);
        }

        double rangeSquared = (double) Config.RENDER_RANGE * Config.RENDER_RANGE;
        List<Marker> result = new ArrayList<>();

        if (BaseFinderClient.baseBoxesEnabled) {
            for (Cell cell : cells.values()) {
                if (!cell.isBase()) continue;
                double cx = (cell.minX + cell.maxX) * 0.5 - px;
                double cz = (cell.minZ + cell.maxZ) * 0.5 - pz;
                if (cx * cx + cz * cz > rangeSquared) continue;
                result.add(new Marker(cell.minX - 0.45, cell.minY - 0.45, cell.minZ - 0.45,
                        cell.maxX + 1.45, cell.maxY + 1.45, cell.maxZ + 1.45,
                        1.0f, 0.18f, 0.28f, 0.95f, true, 0.075f,
                        "BASE?", cell.score));
            }
        }

        if (BaseFinderClient.chunkBoxesEnabled) {
            for (Map.Entry<Long, ChunkStats> entry : chunks.entrySet()) {
                ChunkStats stats = entry.getValue();
                if (stats.score < Config.CHUNK_SCORE || stats.containers < 1
                        || stats.minY == Integer.MAX_VALUE) continue;
                int cx = (int) (entry.getKey() >> 32);
                int cz = (int) (long) entry.getKey();
                double wx = cx * 16.0 + 8.0 - px;
                double wz = cz * 16.0 + 8.0 - pz;
                if (wx * wx + wz * wz > rangeSquared) continue;

                float r = stats.containers >= 3 ? 1.0f : 0.95f;
                float green = stats.containers >= 3 ? 0.24f : 0.62f;
                result.add(new Marker(cx * 16.0 + 0.12, stats.minY - 1.0, cz * 16.0 + 0.12,
                        cx * 16.0 + 15.88, Math.max(stats.minY + 2.0, stats.maxY + 1.0),
                        cz * 16.0 + 15.88, r, green, 0.12f, 0.92f, true, 0.065f,
                        "CHUNK", stats.score));
            }
        }

        for (ChunkData data : DATA.values()) {
            for (Hit hit : data.hits()) {
                double dx = hit.x() + 0.5 - px;
                double dz = hit.z() + 0.5 - pz;
                if (dx * dx + dz * dz > rangeSquared) continue;

                BlockKind kind = hit.kind();
                if (kind.isContainer() && BaseFinderClient.containerEspEnabled) {
                    result.add(new Marker(hit.x() - 0.025, hit.y() - 0.025, hit.z() - 0.025,
                            hit.x() + 1.025, hit.y() + 1.025, hit.z() + 1.025,
                            kind.r, kind.g, kind.b, 1.0f, true, 0.055f,
                            kind.name(), 0));
                } else if (kind == BlockKind.SPAWNER && BaseFinderClient.containerEspEnabled) {
                    result.add(new Marker(hit.x() - 0.025, hit.y() - 0.025, hit.z() - 0.025,
                            hit.x() + 1.025, hit.y() + 1.025, hit.z() + 1.025,
                            kind.r, kind.g, kind.b, 1.0f, true, 0.055f, "SPAWNER", 0));
                } else if (kind == BlockKind.TRANSPARENT && BaseFinderClient.transparentEnabled) {
                    result.add(new Marker(hit.x() - 0.01, hit.y() - 0.01, hit.z() - 0.01,
                            hit.x() + 1.01, hit.y() + 1.01, hit.z() + 1.01,
                            Config.VIOLET[0], Config.VIOLET[1], Config.VIOLET[2], 1.0f,
                            true, 0.045f, "GLASS", 0));
                }
            }
        }

        result.sort(Comparator.comparingDouble(marker -> {
            double dx = (marker.x0() + marker.x1()) * 0.5 - px;
            double dz = (marker.z0() + marker.z1()) * 0.5 - pz;
            return dx * dx + dz * dz;
        }));
        if (result.size() > Config.MAX_MARKERS) {
            result = new ArrayList<>(result.subList(0, Config.MAX_MARKERS));
        }
        return List.copyOf(result);
    }
}
