package me.poma123.globalwarming.tasks;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.poma123.globalwarming.GlobalWarmingPlugin;
import me.poma123.globalwarming.api.PollutionManager;
import me.poma123.globalwarming.api.Temperature;

public class ComfortTask extends MechanicTask {

    private final ThreadLocalRandom rnd;
    private final double chance;

    @ParametersAreNonnullByDefault
    public ComfortTask(double chance) {
        rnd = ThreadLocalRandom.current();
        this.chance = chance;
    }

    @Override
    public void run() {
        Set<String> enabledWorlds = GlobalWarmingPlugin.getRegistry().getEnabledWorlds();

        for (String worldName : enabledWorlds) {
            World w = Bukkit.getWorld(worldName);

            if (w != null && GlobalWarmingPlugin.getRegistry().isWorldEnabled(w.getName()) && w.getEnvironment() == World.Environment.NORMAL && !w.getPlayers().isEmpty()) {
                double pollution = PollutionManager.getPollutionInWorld(w);

                for (Player p : w.getPlayers()) {
                    double random = rnd.nextDouble();

                    if (random < chance) {
                        Temperature temp = GlobalWarmingPlugin.getTemperatureManager().getTemperatureAtLocation(p.getLocation());
                        double celsiusValue = temp.getCelsiusValue();

                        // Comfort zone: 10-28°C with moderate pollution — all players benefit
                        if (celsiusValue >= 10 && celsiusValue <= 28 && pollution < 20) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false, false));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, false));
                        }
                    }
                }
            }
        }
    }
}
