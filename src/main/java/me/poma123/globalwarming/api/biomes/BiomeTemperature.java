package me.poma123.globalwarming.api.biomes;

import org.apache.commons.lang3.Validate;

/**
 * This data type holds biome temperature data for our
 * {@link io.github.thebusybiscuit.slimefun4.utils.biomes.BiomeMap} file
 *
 * @author poma123
 *
 */
public class BiomeTemperature {

    private final double temperature;
    private final double maxTemperatureDropAtNight;

    public BiomeTemperature(double temperature, double maxTemperatureDropAtNight) {
        Validate.isTrue(Double.isFinite(temperature), "The temperature value must be finite!");
        Validate.isTrue(Double.isFinite(maxTemperatureDropAtNight), "The maxTemperatureDropAtNight value must be finite!");

        this.temperature = temperature;
        this.maxTemperatureDropAtNight = maxTemperatureDropAtNight;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getMaxTemperatureDropAtNight() {
        return maxTemperatureDropAtNight;
    }
}
