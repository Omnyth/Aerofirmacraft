package com.omnyth.tfcgenerationbands.config;

import com.omnyth.tfcgenerationbands.TFCGenerationBands;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TFCGenerationBandsConfig {
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("tfc_generation_bands.properties");

    private static volatile boolean enabled = true;

    // Default is intentionally non-invasive for the current -256..319 test world.
    // Change this to -64 once another addon owns/generates the lower range.
    private static volatile int tfcGenerationMinY = -256;
    private static volatile int tfcGenerationMaxY = 319;

    private TFCGenerationBandsConfig() {}

    public static void loadOrCreate() {
        if (!LOADED.compareAndSet(false, true)) {
            return;
        }

        try {
            if (Files.notExists(CONFIG_PATH)) {
                writeDefaultConfig();
            }

            final Properties properties = new Properties();

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                properties.load(reader);
            }

            enabled = Boolean.parseBoolean(properties.getProperty("enabled", Boolean.toString(enabled)));
            tfcGenerationMinY = parseInt(properties, "tfc_generation_min_y", tfcGenerationMinY);
            tfcGenerationMaxY = parseInt(properties, "tfc_generation_max_y", tfcGenerationMaxY);

            if (tfcGenerationMaxY < tfcGenerationMinY) {
                TFCGenerationBands.LOGGER.warn(
                        "TFC Generation Bands config has maxY < minY ({} < {}). Disabling clamp for safety.",
                        tfcGenerationMaxY,
                        tfcGenerationMinY
                );
                enabled = false;
            }

            TFCGenerationBands.LOGGER.info(
                    "Loaded TFC Generation Bands config: enabled={} tfc_generation_min_y={} tfc_generation_max_y={}",
                    enabled,
                    tfcGenerationMinY,
                    tfcGenerationMaxY
            );
        } catch (IOException exception) {
            TFCGenerationBands.LOGGER.error(
                    "Failed to load TFC Generation Bands config at {}. Disabling clamp for safety.",
                    CONFIG_PATH,
                    exception
            );
            enabled = false;
        }
    }

    public static boolean enabled() {
        loadOrCreate();
        return enabled;
    }

    public static int tfcGenerationMinY() {
        loadOrCreate();
        return tfcGenerationMinY;
    }

    public static int tfcGenerationMaxY() {
        loadOrCreate();
        return tfcGenerationMaxY;
    }

    public static Path configPath() {
        return CONFIG_PATH;
    }

    private static int parseInt(final Properties properties, final String key, final int fallback) {
        final String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            TFCGenerationBands.LOGGER.warn(
                    "Invalid integer for {}='{}'. Using fallback {}.",
                    key,
                    value,
                    fallback
            );
            return fallback;
        }
    }

    private static void writeDefaultConfig() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());

        final Properties defaults = new Properties();
        defaults.setProperty("enabled", "true");
        defaults.setProperty("tfc_generation_min_y", "-256");
        defaults.setProperty("tfc_generation_max_y", "319");

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            writer.write("# TFC Generation Bands\n");
            writer.write("# Controls the vertical range where TerraFirmaCraft's own terrain/noise generator is allowed to run.\n");
            writer.write("# This does not change dimension height. Dimension height still comes from the world/dimension settings.\n");
            writer.write("# Default is non-invasive for a -256..319 world. Change tfc_generation_min_y to -64 once another addon owns/generates below -64.\n");
            writer.write("# Restart Minecraft after changing this file.\n");
            writer.write("\n");
            defaults.store(writer, null);
        }
    }
}