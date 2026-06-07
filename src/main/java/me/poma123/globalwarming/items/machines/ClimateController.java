package me.poma123.globalwarming.items.machines;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;

import me.poma123.globalwarming.GlobalWarmingPlugin;
import me.poma123.globalwarming.api.Temperature;

public class ClimateController extends SlimefunItem {

    private static final int PROTECTION_RADIUS = 7;
    private static final int PROTECTION_RADIUS_SQ = PROTECTION_RADIUS * PROTECTION_RADIUS;

    @ParametersAreNonnullByDefault
    public ClimateController(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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
                return true; // Must run on main thread for player operations
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

                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distanceSquared(b.getLocation()) > PROTECTION_RADIUS_SQ) {
                        continue;
                    }

                    Temperature temp = GlobalWarmingPlugin.getTemperatureManager().getTemperatureAtLocation(p.getLocation());
                    double celsius = temp.getCelsiusValue();

                    // Protect from extreme heat
                    if (celsius >= 36 && p.getFireTicks() > 0) {
                        p.setFireTicks(0);
                    }

                    // Remove temperature-induced slowness
                    if (celsius <= -10 || celsius >= 36) {
                        p.removePotionEffect(PotionEffectType.SLOWNESS);
                    }
                }
            }
        });
    }
}
