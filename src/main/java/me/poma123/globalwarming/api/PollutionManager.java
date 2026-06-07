package me.poma123.globalwarming.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;

import me.poma123.globalwarming.GlobalWarmingPlugin;
import me.poma123.globalwarming.api.events.AsyncWorldPollutionChangeEvent;

/**
 * A very useful API that handles the pollution amount in {@link World} instances
 * and the pollution production of items and machines.
 *
 * @author poma123
 *
 */
public class PollutionManager {

    private static final String DATA_PATH = "data.pollution";

    /**
     * This returns the pollution amount at a {@link Location}
     *
     * @param loc
     *            The {@link Location} to get the pollution amount from
     *
     * @return the pollution amount at the given {@link Location}
     */
    public static double getPollutionAtLocation(@Nonnull Location loc) {
        Validate.notNull(loc, "The Location should not be null!");

        return getPollutionInWorld(loc.getWorld());
    }

    /**
     * This returns the pollution amount in a {@link World}
     *
     * @param world
     *            The {@link World} to get the pollution amount from
     *
     * @return the pollution amount in the given {@link World}
     */
    public static double getPollutionInWorld(@Nonnull World world) {
        Validate.notNull(world, "The World should not be null!");

        if (GlobalWarmingPlugin.getRegistry().isWorldEnabled(world.getName())) {
            Config config = GlobalWarmingPlugin.getRegistry().getWorldConfig(world);

            if (config != null) {
                return config.getDouble(DATA_PATH);
            }
        }
        return 0.0;
    }

    /**
     * This increases the pollution amount in a {@link World}
     *
     * @param world
     *            The {@link World} to set pollution amount
     * @param value
     *            The value to rise
     *
     * @return whether the change was successful
     */
    public static boolean risePollutionInWorld(@Nonnull World world, double value) {
        Validate.notNull(world, "The World should not be null!");
        Validate.isTrue(Double.isFinite(value) && value >= 0, "The pollution value must be a finite non-negative number!");

        if (GlobalWarmingPlugin.getRegistry().isWorldEnabled(world.getName())) {
            Config config = GlobalWarmingPlugin.getRegistry().getWorldConfig(world);

            if (config != null) {
                double oldValue = config.getDouble(DATA_PATH);
                value = oldValue + value;

                AsyncWorldPollutionChangeEvent event = new AsyncWorldPollutionChangeEvent(world, oldValue, value);
                Bukkit.getScheduler().runTaskAsynchronously(GlobalWarmingPlugin.getInstance(), () -> Bukkit.getPluginManager().callEvent(event));

                config.setValue(DATA_PATH, value);
                saveConfigAsync(config);
                return true;
            }
        }
        return false;
    }

    /**
     * This decreases the pollution amount in a {@link World}
     *
     * @param world
     *            The {@link World} to set pollution amount
     * @param value
     *            The value to descend
     *
     * @return whether the change was successful
     */
    public static boolean descendPollutionInWorld(@Nonnull World world, double value) {
        Validate.notNull(world, "The World should not be null!");
        Validate.isTrue(Double.isFinite(value) && value >= 0, "The pollution value must be a finite non-negative number!");

        if (GlobalWarmingPlugin.getRegistry().isWorldEnabled(world.getName())) {
            Config config = GlobalWarmingPlugin.getRegistry().getWorldConfig(world);

            if (config != null) {
                double oldValue = config.getDouble(DATA_PATH);
                value = Math.max(oldValue - value, 0.0);

                AsyncWorldPollutionChangeEvent event = new AsyncWorldPollutionChangeEvent(world, oldValue, value);
                Bukkit.getScheduler().runTaskAsynchronously(GlobalWarmingPlugin.getInstance(), () -> Bukkit.getPluginManager().callEvent(event));

                config.setValue(DATA_PATH, value);
                saveConfigAsync(config);
                return true;
            }
        }
        return false;
    }

    /**
     * This sets the pollution amount in a {@link World}
     *
     * @param world
     *            The {@link World} to set pollution amount
     * @param newValue
     *            The value to set
     *
     * @return whether the change was successful
     */
    public static boolean setPollutionInWorld(@Nonnull World world, double newValue) {
        Validate.notNull(world, "The World should not be null!");
        Validate.isTrue(Double.isFinite(newValue) && newValue >= 0, "The pollution value must be a finite non-negative number!");

        if (GlobalWarmingPlugin.getRegistry().isWorldEnabled(world.getName())) {
            Config config = GlobalWarmingPlugin.getRegistry().getWorldConfig(world);

            if (config != null) {
                double oldValue = config.getDouble(DATA_PATH);

                AsyncWorldPollutionChangeEvent event = new AsyncWorldPollutionChangeEvent(world, oldValue, newValue);
                Bukkit.getScheduler().runTaskAsynchronously(GlobalWarmingPlugin.getInstance(), () -> Bukkit.getPluginManager().callEvent(event));

                config.setValue(DATA_PATH, newValue);
                saveConfigAsync(config);
                return true;
            }
        }
        return false;
    }

    private static final Set<Config> dirtyConfigs = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private static volatile boolean flushScheduled;

    private static void saveConfigAsync(Config config) {
        dirtyConfigs.add(config);
        scheduleFlush();
    }

    private static void scheduleFlush() {
        if (!flushScheduled) {
            flushScheduled = true;
            Bukkit.getScheduler().runTaskLater(GlobalWarmingPlugin.getInstance(), () -> {
                flushScheduled = false;
                List<Config> toSave = new ArrayList<>(dirtyConfigs);
                dirtyConfigs.clear();
                for (Config config : toSave) {
                    config.save();
                }
            }, 100); // Flush every 5 seconds (100 ticks)
        }
    }

    /**
     * This returns the pollution production of a {@link ItemStack}
     *
     * @param item
     *            The {@link ItemStack} to check
     *
     * @return the pollution production of the {@link ItemStack}
     */
    public static double isPollutedItem(@Nonnull ItemStack item) {
        Validate.notNull(item, "The ItemStack should not be null!");

        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        Map<String, Double> pollutedSlimefunItems = GlobalWarmingPlugin.getRegistry().getPollutedSlimefunItems();
        Map<Material, Double> pollutedVanillaItems = GlobalWarmingPlugin.getRegistry().getPollutedVanillaItems();

        if (sfItem != null && pollutedSlimefunItems.containsKey(sfItem.getId())) {
            return pollutedSlimefunItems.get(sfItem.getId());
        }

        if (pollutedVanillaItems.containsKey(item.getType())) {
            return pollutedVanillaItems.get(item.getType());
        }

        return 0.0;
    }

    /**
     * This returns the pollution production of a {@link SlimefunItem} machine
     *
     * @param id
     *            The ID of the {@link SlimefunItem} machine
     *
     * @return the pollution production of the {@link SlimefunItem} machine
     */
    public static double isPollutedMachine(@Nonnull String id) {
        Validate.notNull(id, "The Id should not be null!");

        SlimefunItem sfItem = SlimefunItem.getById(id);
        Map<String, Double> pollutedSlimefunMachines = GlobalWarmingPlugin.getRegistry().getPollutedSlimefunMachines();

        if (sfItem != null && pollutedSlimefunMachines.containsKey(sfItem.getId())) {
            return pollutedSlimefunMachines.get(sfItem.getId());
        }

        return 0.0;
    }

    /**
     * This returns the pollution absorption of a {@link SlimefunItem} machine
     *
     * @param id
     *            The ID of the {@link SlimefunItem} machine
     *
     * @return the pollution absorption of the {@link SlimefunItem} machine
     */
    public static double isAbsorbentMachine(@Nonnull String id) {
        Validate.notNull(id, "The Id should not be null!");

        SlimefunItem sfItem = SlimefunItem.getById(id);
        Map<String, Double> absorbentSlimefunMachines = GlobalWarmingPlugin.getRegistry().getAbsorbentSlimefunMachines();

        if (sfItem != null && absorbentSlimefunMachines.containsKey(sfItem.getId())) {
            return absorbentSlimefunMachines.get(sfItem.getId());
        }

        return 0.0;
    }
}
