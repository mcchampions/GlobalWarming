package me.poma123.globalwarming.commands.subcommands;

import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.thebusybiscuit.slimefun4.libraries.dough.common.ChatColors;

import me.poma123.globalwarming.GlobalWarmingPlugin;
import me.poma123.globalwarming.TemperatureManager;
import me.poma123.globalwarming.api.PollutionManager;
import me.poma123.globalwarming.commands.GlobalWarmingCommand;
import me.poma123.globalwarming.commands.SubCommand;

class PollutionCommand extends SubCommand {

    PollutionCommand(GlobalWarmingPlugin plugin, GlobalWarmingCommand cmd) {
        super(plugin, cmd, "pollution", "允许你手动修改污染值", false);
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (sender.hasPermission("globalwarming.command.pollution") || !(sender instanceof Player)) {
            if (args.length > 2) {
                World world = Bukkit.getWorld(args[2]);

                if (world != null && GlobalWarmingPlugin.getRegistry().isWorldEnabled(world.getName())) {
                    if ("get".equalsIgnoreCase(args[1])) {
                        double pollution = TemperatureManager.fixDouble(PollutionManager.getPollutionInWorld(world), 2);

                        sender.sendMessage(ChatColors.color("&b世界 &a" + world.getName() + " &b的污染值为: &a" + pollution));
                    } else if ("set".equalsIgnoreCase(args[1])) {
                        if (args.length > 3) {
                            setPollution(sender, world, args);
                        } else {
                            sender.sendMessage(ChatColors.color("&4用法: &c/globalwarming pollution <set> <world> <amount>"));
                        }
                    }
                } else {
                    sender.sendMessage(ChatColors.color("&4该世界无法使用该指令"));
                }
            } else {
                sender.sendMessage(ChatColors.color("&用法: &c/globalwarming pollution <set|get> <world>"));
            }
        } else {
            sender.sendMessage(ChatColors.color("&4你没有足够的权限执行此命令"));
        }
    }

    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");

    private void setPollution(CommandSender sender, World world, String[] args) {
        double amount = parseAmount(args);

        if (!Double.isNaN(amount)) {
            if (PollutionManager.setPollutionInWorld(world, amount)) {
                String displayValue = TemperatureManager.fixDouble(amount, 2) + "";
                sender.sendMessage(ChatColors.color("&b已设置世界 '&a%world%&b' 的污染值为 '&a%newValue%&b'").replace("%newValue%", displayValue).replace("%world%", world.getName()));
            } else {
                sender.sendMessage(ChatColors.color("&4该世界无法使用该指令"));
            }
        } else {
            sender.sendMessage(ChatColors.color("&4%amount% &c不是一个有效的值").replace("%amount%", args.length >= 4 ? args[3] : ""));
        }
    }

    private double parseAmount(String[] args) {
        if (args.length >= 4 && DECIMAL_PATTERN.matcher(args[3]).matches()) {
            try {
                return Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }
}
