package top.mcocet.bigEffect;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BigEffectTabCompleter implements TabCompleter {
    
    // 所有可用的药水效果名称列表
    private static final List<String> ALL_EFFECTS = Arrays.stream(PotionEffectType.values())
            .map(type -> type.getKey().getKey())
            .collect(Collectors.toList());
    
    // 等级建议
    private static final List<String> LEVEL_SUGGESTIONS = Arrays.asList("1", "2", "3", "4", "5");
    
    // /bigEffect 子命令
    private static final List<String> SUBCOMMANDS = Arrays.asList("reload");
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("bindEffect")) {
            if (args.length == 1) {
                // 补全药水效果名称
                String input = args[0].toLowerCase();
                completions = ALL_EFFECTS.stream()
                        .filter(effect -> effect.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            } else if (args.length == 2) {
                // 补全等级
                completions = LEVEL_SUGGESTIONS;
            }
        } else if (command.getName().equalsIgnoreCase("bigEffect")) {
            if (args.length == 1) {
                // 补全子命令
                String input = args[0].toLowerCase();
                completions = SUBCOMMANDS.stream()
                        .filter(sub -> sub.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}
