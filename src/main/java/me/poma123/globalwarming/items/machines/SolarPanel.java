package me.poma123.globalwarming.items.machines;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;

import me.poma123.globalwarming.GlobalWarmingPlugin;
import me.poma123.globalwarming.TemperatureManager;
import me.poma123.globalwarming.api.PollutionManager;

public class SolarPanel extends SlimefunItem {

    @ParametersAreNonnullByDefault
    public SolarPanel(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler(new SimpleBlockBreakHandler() {
            @Override
            public void onBlockBreak(Block b) {
                // No cleanup needed
            }
        });

        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return false;
            }

            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                World world = b.getWorld();
                if (!GlobalWarmingPlugin.getRegistry().isWorldEnabled(world.getName())) {
                    return;
                }
                if (world.getEnvironment() != World.Environment.NORMAL) {
                    return;
                }

                double absorbAmount;
                if (TemperatureManager.isDaytime(world)) {
                    if (world.hasStorm() || world.isThundering()) {
                        absorbAmount = 0.005; // Reduced during storm
                    } else {
                        absorbAmount = 0.03; // Full solar power
                    }
                } else {
                    absorbAmount = 0.002; // Minimal at night
                }

                if (absorbAmount > 0) {
                    PollutionManager.descendPollutionInWorld(world, absorbAmount);
                }
            }
        });
    }
}
