package com.basefinder;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/** Clasifica bloques por el nombre del registro (robusto a renombres de clases). */
public enum BlockKind {
    NONE(0, 0f, 0f, 0f),
    CHEST(3, 1.00f, 0.60f, 0.10f),
    ENDER_CHEST(3, 0.10f, 0.90f, 0.90f),
    SHULKER(4, 1.00f, 0.20f, 0.80f),
    STORAGE(2, 1.00f, 0.90f, 0.20f),
    SPAWNER(0, 0.20f, 1.00f, 0.30f),
    TRANSPARENT(1, 0.62f, 0.20f, 1.00f),
    DECOR(1, 0.90f, 0.90f, 0.90f);

    public final int weight;
    public final float r, g, b;

    BlockKind(int weight, float r, float g, float b) {
        this.weight = weight;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public boolean isContainer() {
        return this == CHEST || this == ENDER_CHEST || this == SHULKER || this == STORAGE;
    }

    private static final Map<Block, BlockKind> CACHE = new HashMap<>();

    public static BlockKind of(Block block) {
        BlockKind k = CACHE.get(block);
        if (k == null) {
            k = compute(BuiltInRegistries.BLOCK.getKey(block).getPath());
            CACHE.put(block, k);
        }
        return k;
    }

    private static BlockKind compute(String p) {
        if (p.contains("shulker_box")) return SHULKER;
        if (p.equals("ender_chest")) return ENDER_CHEST;
        if (p.equals("chest") || p.equals("trapped_chest")) return CHEST;
        if (p.equals("barrel") || p.equals("hopper") || p.equals("dispenser") || p.equals("dropper")
                || p.equals("furnace") || p.equals("blast_furnace") || p.equals("smoker")
                || p.equals("brewing_stand") || p.equals("crafter") || p.equals("chiseled_bookshelf")) return STORAGE;
        if (p.equals("spawner") || p.equals("trial_spawner")) return SPAWNER;
        if (p.contains("glass")) return TRANSPARENT;

        if (p.endsWith("_bed") || p.contains("banner") || p.contains("carpet") || p.equals("bookshelf")
                || p.contains("lantern") || p.contains("_sign") || p.contains("concrete")
                || p.endsWith("_wool") || p.equals("crafting_table") || p.equals("enchanting_table")
                || p.contains("anvil") || p.equals("beacon") || p.equals("bell") || p.equals("lectern")
                || p.equals("cauldron") || p.contains("campfire") || p.equals("end_rod")
                || p.equals("sea_lantern") || p.equals("glowstone") || p.contains("quartz")
                || p.equals("jukebox") || p.equals("note_block") || p.contains("flower_pot")
                || p.equals("obsidian") || p.equals("crying_obsidian") || p.contains("shelf")) return DECOR;
        return NONE;
    }
}
