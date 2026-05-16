package top.mcocet.bigEffect;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BigEffectCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c用法: /bigEffect reload");
            sender.sendMessage("§c可用子命令: reload - 重新加载配置文件");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bigeffect.admin")) {
                sender.sendMessage("§c你没有权限执行此命令!");
                return true;
            }
            
            BigEffect plugin = BigEffect.getPlugin(BigEffect.class);
            plugin.reloadConfig();
            sender.sendMessage("§a配置已重新加载!");
            return true;
        }
        
        sender.sendMessage("§c未知子命令: " + args[0]);
        sender.sendMessage("§c可用子命令: reload - 重新加载配置文件");
        return true;
    }
}
