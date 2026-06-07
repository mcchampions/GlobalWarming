package me.poma123.globalwarming;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

import me.poma123.globalwarming.api.TemperatureType;
import me.poma123.globalwarming.commands.GlobalWarmingCommand;
import me.poma123.globalwarming.items.CinnabariteResource;
import me.poma123.globalwarming.items.EcoAnalyzer;
import me.poma123.globalwarming.items.machines.AirCompressor;
import me.poma123.globalwarming.items.machines.AirPurifier;
import me.poma123.globalwarming.items.machines.TemperatureMeter;
import me.poma123.globalwarming.listeners.PollutionListener;
import me.poma123.globalwarming.listeners.WorldListener;
import me.poma123.globalwarming.tasks.BurnTask;
import me.poma123.globalwarming.tasks.ComfortTask;
import me.poma123.globalwarming.tasks.FireTask;
import me.poma123.globalwarming.tasks.MeltTask;
import me.poma123.globalwarming.tasks.SlownessTask;

public class GlobalWarmingPlugin extends JavaPlugin implements SlimefunAddon {

    private static GlobalWarmingPlugin instance;
    private static Registry registry = new Registry();
    private final TemperatureManager temperatureManager = new TemperatureManager();
    private final GlobalWarmingCommand command = new GlobalWarmingCommand(this);
    private final Config cfg = new Config(this);
    private Config messages;

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public void onEnable() {
        instance = this;

        new Metrics(this, 9132);

        final File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            try (java.io.InputStream in = this.getClass().getResourceAsStream("/messages.yml")) {
                Files.copy(in, messagesFile.toPath());
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "无法创建默认配置 messages.yml", e);
            }
        }
        messages = new Config(this, "messages.yml");

        File biomeMapDirectory = new File(getDataFolder(), "biome-maps");
        if (!biomeMapDirectory.exists()) {
            biomeMapDirectory.mkdirs();
        }

        // Create biome map files
        final File pre118BiomeMap = new File(biomeMapDirectory, "pre-1.18.json");
        if (!pre118BiomeMap.exists()) {
            try (java.io.InputStream in = this.getClass().getResourceAsStream("/biome-maps/pre-1.18.json")) {
                Files.copy(in, pre118BiomeMap.toPath());
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "无法创建默认配置 biome-maps/pre-1.18.json", e);
            }
        }

        final File post118BiomeMap = new File(biomeMapDirectory, "post-1.18.json");
        if (!post118BiomeMap.exists()) {
            try (java.io.InputStream in = this.getClass().getResourceAsStream("/biome-maps/post-1.18.json")) {
                Files.copy(in, post118BiomeMap.toPath());
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "无法创建默认配置 biome-maps/post-1.18.json", e);
            }
        }

        registerItems();
        registerResearches();
        registry.load(cfg, messages);
        scheduleTasks();

        command.register();
        Bukkit.getPluginManager().registerEvents(new PollutionListener(), this);
        Bukkit.getPluginManager().registerEvents(new WorldListener(), this);
    }

    private void registerItems() {
        ItemGroup itemGroup = new ItemGroup(new NamespacedKey(this, "global_warming"), new CustomItemStack(Items.THERMOMETER, "&2全球变暖"));

        new TemperatureMeter(itemGroup, Items.THERMOMETER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.NICKEL_INGOT, new ItemStack(Material.GLASS), SlimefunItems.NICKEL_INGOT,
                SlimefunItems.NICKEL_INGOT, Items.MERCURY, SlimefunItems.NICKEL_INGOT,
                SlimefunItems.NICKEL_INGOT, new ItemStack(Material.GLASS), SlimefunItems.NICKEL_INGOT
        }) {
            @Override
            public void tick(Block b) {
                Location loc = b.getLocation();
                updateHologram(b, GlobalWarmingPlugin.getTemperatureManager().getTemperatureString(loc, TemperatureType.valueOf(BlockStorage.getLocationInfo(loc, "type"))));
            }
        }.register(this);

        new TemperatureMeter(itemGroup, Items.AIR_QUALITY_METER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.BILLON_INGOT, SlimefunItems.BILLON_INGOT, SlimefunItems.BILLON_INGOT,
                SlimefunItems.SOLDER_INGOT, Items.THERMOMETER, SlimefunItems.SOLDER_INGOT,
                SlimefunItems.SOLDER_INGOT, SlimefunItems.MAGNET, SlimefunItems.SOLDER_INGOT
        }) {
            @Override
            public void tick(Block b) {
                Location loc = b.getLocation();
                updateHologram(b, "&7环境变化: " + GlobalWarmingPlugin.getTemperatureManager().getAirQualityString(loc.getWorld(), TemperatureType.valueOf(BlockStorage.getLocationInfo(loc, "type"))));
            }
        }.register(this);

        new AirCompressor(itemGroup, Items.AIR_COMPRESSOR, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                SlimefunItems.SOLDER_INGOT, Items.FILTER, SlimefunItems.SOLDER_INGOT,
                SlimefunItems.ALUMINUM_BRASS_INGOT, SlimefunItems.ELECTRIC_MOTOR, SlimefunItems.ALUMINUM_BRASS_INGOT,
                SlimefunItems.SOLDER_INGOT, SlimefunItems.BATTERY, SlimefunItems.SOLDER_INGOT
        }) {
            @Override
            public int getEnergyConsumption() {
                return 16;
            }

            @Override
            public int getCapacity() {
                return 512;
            }

            @Override
            public int getSpeed() {
                return 1;
            }
        }.register(this);

        new SlimefunItem(itemGroup, Items.EMPTY_CANISTER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                null, SlimefunItems.SOLDER_INGOT, null,
                SlimefunItems.SOLDER_INGOT, new ItemStack(Material.GLASS_BOTTLE), SlimefunItems.SOLDER_INGOT,
                SlimefunItems.SOLDER_INGOT, SlimefunItems.SOLDER_INGOT, SlimefunItems.SOLDER_INGOT
        }).register(this);

        new SimpleSlimefunItem<ItemConsumptionHandler>(itemGroup, Items.CO2_CANISTER, AirCompressor.RECIPE_TYPE, new ItemStack[] {
                null, null, null,
                null, Items.EMPTY_CANISTER, null,
                null, null, null
        }) {
            @Override
            public ItemConsumptionHandler getItemHandler() {
                return (e, p, item) -> e.setCancelled(true);
            }
        }.register(this);

        new SlimefunItem(itemGroup, Items.CINNABARITE, RecipeType.GEO_MINER, new ItemStack[]{}).register(this);
        new CinnabariteResource().register();

        new SlimefunItem(itemGroup, Items.MERCURY, RecipeType.SMELTERY, new ItemStack[]{
                Items.CINNABARITE, null, null,
                null, null, null,
                null, null, null
        }).register(this);

        new SlimefunItem(itemGroup, Items.FILTER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[]{
                null, new ItemStack(Material.GLASS), null,
                new ItemStack(Material.GLASS), SlimefunItems.GOLD_PAN, new ItemStack(Material.GLASS),
                null, new ItemStack(Material.GLASS), null
        }).register(this);

        new AirPurifier(itemGroup, Items.AIR_PURIFIER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[]{
                new ItemStack(Material.GLASS), Items.FILTER, new ItemStack(Material.GLASS),
                new ItemStack(Material.GLASS), new ItemStack(Material.POTTED_LILY_OF_THE_VALLEY), new ItemStack(Material.GLASS),
                null, new ItemStack(Material.IRON_BARS), null
        }).register(this);

        new EcoAnalyzer(itemGroup, Items.ECO_ANALYZER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[]{
                null, Items.THERMOMETER, null,
                null, new ItemStack(Material.STICK), null,
                null, new ItemStack(Material.GLASS_PANE), null
        }).register(this);
    }

    private void registerResearches() {
        registerResearch("thermometer", 69696969, "温度计", 10, Items.THERMOMETER);
        registerResearch("air_quality_meter", 69696970, "空气质量监测仪", 30, Items.AIR_QUALITY_METER);
        registerResearch("air_compressor", 69696971, "空气压缩机", 40, Items.AIR_COMPRESSOR);
        registerResearch("canisters", 69696972, "污染存储", 6, Items.EMPTY_CANISTER, Items.CO2_CANISTER);
        registerResearch("filter", 69696973, "过滤", 8, Items.FILTER);
        registerResearch("mercury", 69696974, "水银", 12, Items.CINNABARITE, Items.MERCURY);
        registerResearch("air_purifier", 69696975, "空气净化器", 16, Items.AIR_PURIFIER);
        registerResearch("eco_analyzer", 69696976, "环境分析仪", 2, Items.ECO_ANALYZER);
    }

    private void scheduleTasks() {
        if (cfg.getBoolean("mechanics.FOREST_FIRES.enabled")) {
            new FireTask(cfg.getOrSetDefault("mechanics.FOREST_FIRES.min-temperature-in-celsius", 40.0),
                    cfg.getOrSetDefault("mechanics.FOREST_FIRES.chance", 0.3),
                    cfg.getOrSetDefault("mechanics.FOREST_FIRES.fire-per-second", 10)
            ).scheduleRepeating(0, 20);
        }

        if (cfg.getBoolean("mechanics.ICE_MELTING.enabled")) {
            new MeltTask(cfg.getOrSetDefault("mechanics.ICE_MELTING.min-temperature-in-celsius", 2.0),
                    cfg.getOrSetDefault("mechanics.ICE_MELTING.chance", 0.5),
                    cfg.getOrSetDefault("mechanics.ICE_MELTING.melt-per-second", 10)
            ).scheduleRepeating(0, 20);
        }

        if (cfg.getBoolean("mechanics.SLOWNESS.enabled")) {
            new SlownessTask(cfg.getOrSetDefault("mechanics.SLOWNESS.chance", 0.8)).scheduleRepeating(0, 200);
        }

        if (cfg.getBoolean("mechanics.BURN.enabled")) {
            new BurnTask(cfg.getOrSetDefault("mechanics.BURN.chance", 0.8)).scheduleRepeating(0, 200);
        }

        // Comfort bonus in clean, optimal-temperature environments
        new ComfortTask(0.3).scheduleRepeating(0, 200);

        temperatureManager.runCalculationTask(0, 100);

        if (cfg.getOrSetDefault("player-experience.action-bar-hud", true)) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                for (String w : registry.getEnabledWorlds()) {
                    World world = Bukkit.getWorld(w);
                    if (world == null || !registry.isWorldEnabled(w) || world.getPlayers().isEmpty()) {
                        continue;
                    }
                    for (Player p : world.getPlayers()) {
                        String tempStr = temperatureManager.getTemperatureString(p.getLocation(), TemperatureType.CELSIUS);
                        String airStr = temperatureManager.getAirQualityString(world, TemperatureType.CELSIUS);
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(ChatColors.color(
                                        tempStr + " &8| &7环境: " + airStr)));
                    }
                }
            }, 20, 40);
        }
    }

    private void registerResearch(String key, int id, String name, int defaultCost, ItemStack... items) {
        Research research = new Research(new NamespacedKey(this, key), id, name, defaultCost);

        for (ItemStack item : items) {
            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            if (sfItem != null) {
                research.addItems(sfItem);
            }
        }

        research.register();
    }

    public static Registry getRegistry() {
        return registry;
    }

    public static TemperatureManager getTemperatureManager() {
        return instance.temperatureManager;
    }

    public static GlobalWarmingPlugin getInstance() {
        return instance;
    }

    public static GlobalWarmingCommand getCommand() {
        return instance.command;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/GuizhanCraft/GlobalWarming-CN/issues";
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    public static Config getCfg() {
        return instance.cfg;
    }

    public static Config getMessagesConfig() {
        return instance.messages;
    }

}
