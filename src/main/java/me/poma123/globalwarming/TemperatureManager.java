package me.poma123.globalwarming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import io.github.thebusybiscuit.slimefun4.utils.biomes.BiomeMap;
import me.poma123.globalwarming.api.biomes.BiomeTemperature;
import me.poma123.globalwarming.api.PollutionManager;
import me.poma123.globalwarming.api.Temperature;
import me.poma123.globalwarming.api.TemperatureType;

/**
 * Handles the temperature calculations in different {@link Biome} instances
 * based on default biome temperature, pollution, weather and time.
 *
 * @author poma123
 *
 */
public class TemperatureManager {

    public static final String HOT = "☀";
    public static final String COLD = "❄";

    private static final BiomeTemperature DEFAULT_BIOME_TEMP = new BiomeTemperature(15, 0);

    private final Map<String, Map<Biome, Double>> worldTemperatureChangeFactorMap = new ConcurrentHashMap<>();

    protected void runCalculationTask(long delay, long interval) {
        // Cache biome list once — immutable, no need to re-fetch each cycle
        final List<Biome> allBiomes = new ArrayList<>();
        RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).forEach(allBiomes::add);

        Bukkit.getScheduler().runTaskTimerAsynchronously(GlobalWarmingPlugin.getInstance(), () -> {

            for (String w : GlobalWarmingPlugin.getRegistry().getEnabledWorlds()) {
                World world = Bukkit.getWorld(w);

                if (world != null) {
                    // Apply natural pollution decay (even without players)
                    double decayRate = GlobalWarmingPlugin.getRegistry().getPollutionNaturalDecay();
                    if (decayRate > 0) {
                        PollutionManager.descendPollutionInWorld(world, decayRate);
                    }

                    if (!world.getPlayers().isEmpty()) {
                        Map<Biome, Double> map = new HashMap<>(allBiomes.size());
                        boolean isNormalEnvironment = world.getEnvironment() == World.Environment.NORMAL;

                        // Pre-compute world-level values — avoid ~60 redundant lookups per world
                        BiomeMap<BiomeTemperature> biomeMap = GlobalWarmingPlugin.getRegistry().getBiomeMap();
                        double pollutionEffect = isNormalEnvironment
                            ? PollutionManager.getPollutionInWorld(world) * GlobalWarmingPlugin.getRegistry().getPollutionMultiply()
                            : 0;
                        double stormDrop = isNormalEnvironment && world.hasStorm()
                            ? GlobalWarmingPlugin.getRegistry().getStormTemperatureDrop()
                            : 0;
                        boolean daytime = isDaytime(world);
                        double nightFraction = 0;
                        if (isNormalEnvironment && !daytime) {
                            double nightTime = world.getTime() - 12300F;
                            if (nightTime > 5775) {
                                nightTime = 5775 - (nightTime - 5775);
                            }
                            nightFraction = nightTime / 5775;
                        }

                        for (Biome biome : allBiomes) {
                            BiomeTemperature biomeTemperature = biomeMap.getOrDefault(biome, DEFAULT_BIOME_TEMP);
                            double celsius = biomeTemperature.getTemperature();

                            if (isNormalEnvironment) {
                                if (nightFraction > 0) {
                                    double nightDrop = biomeTemperature.getMaxTemperatureDropAtNight();
                                    celsius -= nightDrop * nightFraction;
                                } else if (stormDrop > 0) {
                                    celsius -= stormDrop;
                                }
                                celsius += pollutionEffect;
                            }

                            map.put(biome, celsius);
                        }
                        worldTemperatureChangeFactorMap.put(w, map);
                    }
                }
            }
        }, delay, interval);
    }

    public Temperature getTemperatureAtLocation(@Nonnull Location loc) {
        World world = loc.getWorld();
        Biome biome = world.getComputedBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        Map<Biome, Double> map = worldTemperatureChangeFactorMap.get(world.getName());

        if (map == null) {
            return new Temperature(0);
        }

        return new Temperature(map.getOrDefault(biome, 15.0));
    }

    public String getTemperatureString(@Nonnull Location loc, @Nonnull TemperatureType tempType) {
        if (!GlobalWarmingPlugin.getRegistry().isWorldEnabled(loc.getWorld().getName())) {
            return "&c该世界不可用";
        }

        Temperature temp = getTemperatureAtLocation(loc);

        if (temp == null) {
            return "&7正在测量中...";
        }

        double celsiusValue = temp.getCelsiusValue();
        String prefix;

        if (celsiusValue <= 18) {
            prefix = "&b" + COLD;
        } else if (celsiusValue <= 24) {
            prefix = "&a" + HOT;
        } else if (celsiusValue <= 28) {
            prefix = "&e" + HOT;
        } else if (celsiusValue <= 36) {
            prefix = "&6" + HOT;
        } else if (celsiusValue <= 45) {
            prefix = "&c" + HOT;
        } else {
            prefix = "&4" + HOT;
        }
        temp.setTemperatureType(tempType);

        return prefix + " " + fixDouble(temp.getConvertedValue()) + " &7" + tempType.getSuffix();
    }

    public String getAirQualityString(@Nonnull World world, @Nonnull TemperatureType tempType) {
        if (!GlobalWarmingPlugin.getRegistry().isWorldEnabled(world.getName()) || world.getEnvironment() != World.Environment.NORMAL) {
            return "&c该世界不可用";
        }

        double pollutionEffect = PollutionManager.getPollutionInWorld(world) * GlobalWarmingPlugin.getRegistry().getPollutionMultiply();
        String prefix;

        if (pollutionEffect <= -1.5 || pollutionEffect >= 1.5) {
            prefix = "&c";
        } else if (pollutionEffect <= -0.5 || pollutionEffect >= 0.5) {
            prefix = "&e";
        } else if (pollutionEffect < 0 || pollutionEffect > 0) {
            prefix = "&a";
        } else {
            prefix = "&f";
        }

        double difference = pollutionEffect;

        if (tempType != TemperatureType.CELSIUS) {
            difference = getDifference(15.0 + pollutionEffect, 15.0, tempType);
        }

        prefix = prefix + (difference > 0 ? "+" : "");

        return prefix + fixDouble(difference) + " &7" + tempType.getSuffix();
    }

    public Temperature addTemperatureChangeFactors(@Nonnull World world, @Nonnull Biome biome, @Nonnull Temperature temperature) {
        BiomeMap<BiomeTemperature> biomeMap = GlobalWarmingPlugin.getRegistry().getBiomeMap();
        double celsiusValue = temperature.getCelsiusValue();
        double nightDrop = biomeMap.getOrDefault(biome, DEFAULT_BIOME_TEMP).getMaxTemperatureDropAtNight();

        if (world.getEnvironment() == World.Environment.NORMAL) {
            if (!isDaytime(world)) {
                double nightTime = world.getTime() - 12300F;

                if (nightTime > 5775) {
                    nightTime = 5775 - (nightTime - 5775);
                }

                double dropPercent = nightTime / 5775;

                celsiusValue = celsiusValue - (nightDrop * dropPercent);
            } else if (world.hasStorm()) {
                celsiusValue = celsiusValue - GlobalWarmingPlugin.getRegistry().getStormTemperatureDrop();
            }
        }

        celsiusValue = celsiusValue + (PollutionManager.getPollutionInWorld(world) * GlobalWarmingPlugin.getRegistry().getPollutionMultiply());

        return new Temperature(celsiusValue);
    }

    public static double getDifference(double currentValue, double defaultValue, @Nonnull TemperatureType type) {
        double convertedCurrent = new Temperature(currentValue, type).getConvertedValue();
        double convertedDefault = new Temperature(defaultValue, type).getConvertedValue();

        double difference = Math.abs(convertedCurrent - convertedDefault);

        if (convertedCurrent < convertedDefault) {
            difference = difference*-1;
        }

        return difference;
    }

    public static boolean isDaytime(@Nonnull World world) {
        long time = world.getTime();
        return (time < 12300 || time > 23850);
    }

    public static double fixDouble(double amount, int digits) {
        if (digits == 0) {
            return (int) amount;
        }
        double factor = Math.pow(10, digits);
        return Math.round(amount * factor) / factor;
    }

    public static double fixDouble(double amount) {
        return fixDouble(amount, 2);
    }
}
