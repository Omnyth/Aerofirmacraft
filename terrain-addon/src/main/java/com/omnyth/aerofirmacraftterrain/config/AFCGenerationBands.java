package com.omnyth.aerofirmacraftterrain.config;

/**
 * Central vertical ownership settings for AFC terrain generation.
 *
 * Current goal:
 * - Let the world/dimension extend below old TFC minY.
 * - Prevent TFC's own terrain noise filler from owning the lower AFC bands.
 * - Leave lower bands available for AFC-owned biome/generator layers.
 *
 * Later this can become a proper NeoForge config or datapack-backed band registry.
 */
public final class AFCGenerationBands {
    public static final int WORLD_MIN_Y = -256;
    public static final int WORLD_MAX_Y = 319;

    public static final int TFC_GENERATION_MIN_Y = -64;
    public static final int TFC_GENERATION_MAX_Y = 319;

    public static final int AFC_LOWER_WORLD_MIN_Y = -256;
    public static final int AFC_LOWER_WORLD_MAX_Y = -65;

    public static final int LOWER_OCEAN_MIN_Y = -256;
    public static final int LOWER_OCEAN_WATER_TOP_Y = -193;

    public static final int SKY_GAP_MIN_Y = -192;
    public static final int SKY_GAP_MAX_Y = -65;

    private AFCGenerationBands() {}
}