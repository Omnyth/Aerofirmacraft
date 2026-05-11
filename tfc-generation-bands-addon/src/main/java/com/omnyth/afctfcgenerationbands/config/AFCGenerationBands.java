package com.omnyth.afctfcgenerationbands.config;

/**
 * Shared vertical generation ownership settings for TFC-based worlds.
 *
 * This class is intentionally standalone so other AFC addons/projects can depend on this jar
 * and read the same generation-band constants.
 *
 * Current role:
 * - TFC owns normal terrain generation from TFC_GENERATION_MIN_Y upward.
 * - AFC or other addons can own the lower bands without fighting TFC's noise filler.
 *
 * Later:
 * - Move these constants into a NeoForge config or datapack-backed band registry.
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