package me.poma123.globalwarming.listeners;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

import me.poma123.globalwarming.GlobalWarmingPlugin;
import me.poma123.globalwarming.api.PollutionManager;
import me.poma123.globalwarming.api.Temperature;

public class CropGrowthListener implements Listener {

    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();

    @EventHandler
    public void onCropGrow(BlockGrowEvent e) {
        Block block = e.getBlock();
        if (!(block.getBlockData() instanceof Ageable)) {
            return;
        }

        World world = block.getWorld();
        if (!GlobalWarmingPlugin.getRegistry().isWorldEnabled(world.getName())) {
            return;
        }
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        double pollution = PollutionManager.getPollutionInWorld(world);
        Temperature temp = GlobalWarmingPlugin.getTemperatureManager().getTemperatureAtLocation(block.getLocation());
        double celsius = temp.getCelsiusValue();

        // Extreme conditions: high chance to stunt growth
        if (pollution > 80 || celsius < -10 || celsius > 45) {
            if (rnd.nextDouble() < 0.7) {
                e.setCancelled(true);
            }
            return;
        }

        // Poor conditions: moderate chance
        if (pollution > 40 || celsius < 0 || celsius > 36) {
            if (rnd.nextDouble() < 0.35) {
                e.setCancelled(true);
            }
            return;
        }

        // Clean, optimal conditions: crops always grow normally
        // No cancellation → natural growth rate
    }
}
