package me.poma123.globalwarming.api;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.Validate;

/**
 * A very simple API that handles the conversion between
 * {@link TemperatureType} scales.
 *
 * @author poma123
 *
 */
public class Temperature {
    private double celsiusValue;
    private TemperatureType tempType = TemperatureType.CELSIUS;

    public Temperature(double value) {
        Validate.isTrue(Double.isFinite(value), "The Temperature value must be finite!");

        this.celsiusValue = value;
    }

    public Temperature(double value, @Nonnull TemperatureType type) {
        Validate.isTrue(Double.isFinite(value), "The Temperature value must be finite!");
        Validate.notNull(type, "The TemperatureType should not be null!");

        celsiusValue = value;
        tempType = type;
    }

    public double getCelsiusValue() {
        return celsiusValue;
    }

    public double getFahrenheitValue() {
        return celsiusValue * 1.8 + 32;
    }

    public double getKelvinValue() {
        return celsiusValue + 273.15;
    }

    public double getConvertedValue() {
        switch (tempType) {
            case FAHRENHEIT:
                return getFahrenheitValue();
            case KELVIN:
                return getKelvinValue();
            default:
                return celsiusValue;
        }
    }

    @Nonnull
    public TemperatureType getTemperatureType() {
        return tempType;
    }

    public void setTemperatureType(@Nonnull TemperatureType type) {
        Validate.notNull(type, "The TemperatureType should not be null!");

        tempType = type;
    }
}
