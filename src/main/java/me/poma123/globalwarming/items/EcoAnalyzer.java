package me.poma123.globalwarming.items;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.common.ChatColors;

import me.poma123.globalwarming.GlobalWarmingPlugin;
import me.poma123.globalwarming.api.TemperatureType;

public class EcoAnalyzer extends SlimefunItem {

    @ParametersAreNonnullByDefault
    public EcoAnalyzer(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler((ItemUseHandler) e -> {
            Player p = e.getPlayer();
            String typeStr = getStoredType(p);

            TemperatureType type;
            try {
                type = TemperatureType.valueOf(typeStr);
            } catch (IllegalArgumentException ex) {
                type = TemperatureType.CELSIUS;
            }

            TemperatureType next = switch (type) {
                case CELSIUS -> TemperatureType.FAHRENHEIT;
                case FAHRENHEIT -> TemperatureType.KELVIN;
                case KELVIN -> TemperatureType.CELSIUS;
            };

            if (p.isSneaking()) {
                // Shift+right-click: cycle temperature unit
                storeType(p, next.name());
                p.sendMessage(ChatColors.color("&7温度单位: &e" + next.getName()));
            } else {
                // Right-click: show environment info
                String tempStr = GlobalWarmingPlugin.getTemperatureManager().getTemperatureString(p.getLocation(), type);
                String airStr = GlobalWarmingPlugin.getTemperatureManager().getAirQualityString(p.getWorld(), type);
                p.sendMessage(ChatColors.color("&a=== 环境分析 ==="));
                p.sendMessage(ChatColors.color("&7温度: " + tempStr));
                p.sendMessage(ChatColors.color("&7空气质量: " + airStr));
                p.sendMessage(ChatColors.color("&8蹲下+右键切换温度单位"));
            }

            e.cancel();
        });
    }

    private String getStoredType(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(GlobalWarmingPlugin.getInstance(), "eco_type"), org.bukkit.persistence.PersistentDataType.STRING)) {
                return meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(GlobalWarmingPlugin.getInstance(), "eco_type"), org.bukkit.persistence.PersistentDataType.STRING);
            }
        }
        return "CELSIUS";
    }

    private void storeType(Player p, String type) {
        ItemStack item = p.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(GlobalWarmingPlugin.getInstance(), "eco_type"), org.bukkit.persistence.PersistentDataType.STRING, type);
            item.setItemMeta(meta);
        }
    }
}
