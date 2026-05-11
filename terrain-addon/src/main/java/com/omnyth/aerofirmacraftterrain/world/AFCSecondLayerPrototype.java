package com.omnyth.aerofirmacraftterrain.world;

/**
 * Prototype settings for duplicating a TFC-generated vertical band into the lower world.
 *
 * This is not final AFC lower-ocean generation. It is a controlled bridge test:
 * - Vanilla World Parameters makes the world valid down to -256.
 * - TFC Generation Bands keeps TFC's own normal generation at -64..319.
 * - This terrain addon copies TFC-generated blocks from the upper band into -250..-65.
 */
public final class AFCSecondLayerPrototype {
    public static final int SOURCE_MIN_Y = -64;
    public static final int TARGET_MAX_Y = -65;
    public static final int BOTTOM_BEDROCK_RESERVE = 6;

    private AFCSecondLayerPrototype() {}
}