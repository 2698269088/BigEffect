package top.mcocet.bigEffect;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class EffectData {
    
    private static final String EFFECT_KEY = "potion_effect";  // 不包含冒号，plugin的命名空间会自动添加
    private static Plugin plugin;
    
    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    /**
     * 为物品绑定药水效果
     */
    public static ItemStack bindEffect(ItemStack item, PotionEffect effect) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        NamespacedKey key = new NamespacedKey(plugin, EFFECT_KEY);
        
        // 存储药水效果类型和持续时间、等级（使用 | 作为分隔符，避免与 NamespacedKey 的冒号冲突）
        String effectDataString = effect.getType().getKey().toString() + "|" + 
                                 effect.getDuration() + "|" + 
                                 effect.getAmplifier();
        
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, effectDataString);
        
        // 添加Lore标识
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(" "); // 空行分隔
        lore.add("§7§o绑定效果: " + getEffectDisplayName(effect.getType()) + " " + getEffectRomanLevel(effect.getAmplifier()));
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 从物品中获取绑定的药水效果
     */
    public static PotionEffect getBoundEffect(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, EFFECT_KEY);

        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return null;
        }

        String effectDataString = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (effectDataString == null) {
            return null;
        }

        try {
            String[] parts = effectDataString.split("\\|");
            if (parts.length != 3) {
                return null;
            }

            PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.fromString(parts[0]));
            int duration = Integer.parseInt(parts[1]);
            int amplifier = Integer.parseInt(parts[2]);

            if (type != null) {
                return new PotionEffect(type, duration, amplifier, false, false, true);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[EffectData] 解析绑定效果数据失败: " + e.getMessage());
        }

        return null;
    }
    
    /**
     * 检查物品是否绑定了药水效果
     */
    public static boolean hasBoundEffect(ItemStack item) {
        return getBoundEffect(item) != null;
    }
    
    /**
     * 获取药水效果的等级(罗马数字)
     */
    private static String getEffectRomanLevel(int amplifier) {
        switch (amplifier) {
            case 0: return "I";
            case 1: return "II";
            case 2: return "III";
            case 3: return "IV";
            case 4: return "V";
            default: return String.valueOf(amplifier + 1);
        }
    }
    
    /**
     * 获取药水效果的显示名称
     */
    private static String getEffectDisplayName(PotionEffectType type) {
        if (type == null) return "未知效果";
        
        String name = type.getKey().getKey();
        // 简化显示名称
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
}
