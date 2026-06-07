package me.poma123.globalwarming.items;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.common.ChatColors;

import me.poma123.globalwarming.GlobalWarmingPlugin;
import me.poma123.globalwarming.api.TemperatureType;

public class EcoAnalyzer extends SlimefunItem {

    private static final NamespacedKey ECO_TYPE_KEY = new NamespacedKey(GlobalWarmingPlugin.getInstance(), "eco_type");

    @ParametersAreNonnullByDefault
    public EcoAnalyzer(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler((ItemUseHandler) e -> {
            Player p = e.getPlayer();
            ItemStack held = e.getItem();
            String typeStr = getStoredType(held);

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
                storeType(held, next.name());
                p.sendMessage(ChatColors.color("&7温度单位: &e" + next.getName()));
            } else {
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

    private String getStoredType(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(ECO_TYPE_KEY, PersistentDataType.STRING)) {
                return meta.getPersistentDataContainer().get(ECO_TYPE_KEY, PersistentDataType.STRING);
            }
        }
        return "CELSIUS";
    }

    private void storeType(ItemStack item, String type) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(ECO_TYPE_KEY, PersistentDataType.STRING, type);
            item.setItemMeta(meta);
        }
    }
}
