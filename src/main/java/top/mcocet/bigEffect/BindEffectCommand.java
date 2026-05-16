package top.mcocet.bigEffect;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class BindEffectCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查权限
        if (!player.hasPermission("bigeffect.bind")) {
            player.sendMessage("§c你没有权限执行此命令!");
            return true;
        }
        
        // 检查参数
        if (args.length < 1) {
            player.sendMessage("§c用法: /bindEffect <药水效果名称> [等级]");
            player.sendMessage("§c示例: /bindEffect speed 2");
            player.sendMessage("§c可用效果: speed, strength, regeneration, jump_boost 等");
            return true;
        }
        
        // 获取玩家手中的物品
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§c请手持一个物品!");
            return true;
        }
        
        // 解析药水效果名称
        String effectName = args[0].toLowerCase();
        PotionEffectType effectType = PotionEffectType.getByKey(org.bukkit.NamespacedKey.fromString(effectName));
        
        if (effectType == null) {
            // 尝试添加 minecraft: 前缀
            effectType = PotionEffectType.getByKey(org.bukkit.NamespacedKey.fromString("minecraft:" + effectName));
        }
        
        if (effectType == null) {
            player.sendMessage("§c无效的药水效果名称: " + effectName);
            player.sendMessage("§c可用效果示例: speed, strength, regeneration, jump_boost, invisibility");
            return true;
        }
        
        // 解析等级(默认为1)
        int amplifier = 0; // 0表示等级I, 1表示等级II
        if (args.length >= 2) {
            try {
                amplifier = Integer.parseInt(args[1]) - 1; // 转换为内部等级(0-based)
                if (amplifier < 0 || amplifier > 255) {
                    player.sendMessage("§c等级必须在1-256之间!");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§c无效的等级: " + args[1]);
                return true;
            }
        }
        
        // 使用配置文件中的持续时间
        int duration = ConfigManager.getEffectDurationTicks();
        PotionEffect effect = new PotionEffect(effectType, duration, amplifier, false, false, true);
        
        // 绑定效果到物品（会追加到现有绑定效果中）
        ItemStack boundItem = EffectData.bindEffect(item, effect);
        
        if (boundItem != null) {
            player.getInventory().setItemInMainHand(boundItem);
            player.sendMessage("§a成功为物品绑定效果: §f" + getEffectDisplayName(effectType));
            player.sendMessage("§a等级: §f" + (amplifier + 1) + " §a持续时间: §f" + ConfigManager.getEffectDurationSeconds() + "秒");
            
            // 显示当前物品的所有绑定效果
            java.util.List<PotionEffect> allEffects = EffectData.getBoundEffects(boundItem);
            if (allEffects != null && allEffects.size() > 1) {
                player.sendMessage("§e当前物品共绑定 §f" + allEffects.size() + " §e个效果:");
                for (PotionEffect e : allEffects) {
                    player.sendMessage("  §7- " + getEffectDisplayName(e.getType()) + " " + getEffectRomanLevel(e.getAmplifier()));
                }
            }
        } else {
            player.sendMessage("§c绑定失败!");
        }
        
        return true;
    }
    
    private String getEffectDisplayName(PotionEffectType type) {
        if (type == null) return "未知效果";
        
        String name = type.getKey().getKey();
        switch (name) {
            case "speed": return "速度";
            case "slowness": return "缓慢";
            case "haste": return "急迫";
            case "mining_fatigue": return "挖掘疲劳";
            case "strength": return "力量";
            case "instant_health": return "瞬间治疗";
            case "instant_damage": return "瞬间伤害";
            case "jump_boost": return "跳跃提升";
            case "nausea": return "反胃";
            case "regeneration": return "生命恢复";
            case "resistance": return "抗性提升";
            case "fire_resistance": return "防火";
            case "water_breathing": return "水下呼吸";
            case "invisibility": return "隐身";
            case "blindness": return "失明";
            case "night_vision": return "夜视";
            case "hunger": return "饥饿";
            case "weakness": return "虚弱";
            case "poison": return "中毒";
            case "wither": return "凋零";
            case "health_boost": return "生命提升";
            case "absorption": return "伤害吸收";
            case "saturation": return "饱和";
            case "glowing": return "发光";
            case "levitation": return "漂浮";
            case "luck": return "幸运";
            case "unluck": return "霉运";
            case "slow_falling": return "缓降";
            case "conduit_power": return "潮涌能量";
            case "dolphins_grace": return "海豚的恩惠";
            case "bad_omen": return "不祥之兆";
            case "hero_of_the_village": return "村庄英雄";
            case "darkness": return "黑暗";
            case "trial_omen": return "试炼之兆";
            case "raid_omen": return "袭击之兆";
            case "wind_charged": return "蓄风";
            case "weaving": return "缠绕";
            case "oozing": return "渗浆";
            case "infested": return "寄生";
            default: return name;
        }
    }
    
    /**
     * 获取药水效果的等级(罗马数字)
     */
    private String getEffectRomanLevel(int amplifier) {
        switch (amplifier) {
            case 0: return "I";
            case 1: return "II";
            case 2: return "III";
            case 3: return "IV";
            case 4: return "V";
            default: return String.valueOf(amplifier + 1);
        }
    }
}
