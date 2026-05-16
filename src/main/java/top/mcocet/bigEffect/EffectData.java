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
    
    private static final String EFFECT_KEY = "potion_effects";  // 不包含冒号，plugin的命名空间会自动添加
    private static Plugin plugin;
    
    // 分隔符：效果之间用 ; 分隔，效果属性用 | 分隔
    private static final String EFFECT_SEPARATOR = ";";
    private static final String PROPERTY_SEPARATOR = "|";
    
    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    /**
     * 为物品绑定单个药水效果（如果已有其他效果则追加）
     */
    public static ItemStack bindEffect(ItemStack item, PotionEffect effect) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        // 获取已有效果列表
        List<PotionEffect> existingEffects = getBoundEffects(item);
        if (existingEffects == null) {
            existingEffects = new ArrayList<>();
        }
        
        // 检查是否已存在同类型效果，存在则替换
        existingEffects.removeIf(e -> e.getType().equals(effect.getType()));
        existingEffects.add(effect);
        
        return saveEffectsToItem(item, existingEffects);
    }
    
    /**
     * 为物品批量绑定多个药水效果
     */
    public static ItemStack bindEffects(ItemStack item, List<PotionEffect> effects) {
        if (item == null || item.getType() == Material.AIR || effects == null || effects.isEmpty()) {
            return null;
        }
        
        // 获取已有效果列表
        List<PotionEffect> existingEffects = getBoundEffects(item);
        if (existingEffects == null) {
            existingEffects = new ArrayList<>();
        }
        
        // 添加新效果（同类型替换）
        for (PotionEffect newEffect : effects) {
            existingEffects.removeIf(e -> e.getType().equals(newEffect.getType()));
            existingEffects.add(newEffect);
        }
        
        return saveEffectsToItem(item, existingEffects);
    }
    
    /**
     * 将效果列表保存到物品
     */
    private static ItemStack saveEffectsToItem(ItemStack item, List<PotionEffect> effects) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        NamespacedKey key = new NamespacedKey(plugin, EFFECT_KEY);
        
        // 构建效果数据字符串：type|duration|amplifier;type|duration|amplifier;...
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < effects.size(); i++) {
            PotionEffect effect = effects.get(i);
            sb.append(effect.getType().getKey().toString())
              .append(PROPERTY_SEPARATOR)
              .append(effect.getDuration())
              .append(PROPERTY_SEPARATOR)
              .append(effect.getAmplifier());
            
            if (i < effects.size() - 1) {
                sb.append(EFFECT_SEPARATOR);
            }
        }
        
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sb.toString());
        
        // 更新Lore标识
        updateEffectLore(meta, effects);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 更新物品的效果Lore
     */
    private static void updateEffectLore(ItemMeta meta, List<PotionEffect> effects) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // 移除旧的效果Lore（以"绑定效果:"开头的行）
        lore.removeIf(line -> line.contains("绑定效果:"));
        lore.removeIf(String::isEmpty);
        lore.removeIf(line -> line.trim().isEmpty());
        
        // 添加新的效果Lore
        if (!effects.isEmpty()) {
            lore.add(""); // 空行分隔
            for (PotionEffect effect : effects) {
                lore.add("§7§o绑定效果: " + getEffectDisplayName(effect.getType()) + " " + getEffectRomanLevel(effect.getAmplifier()));
            }
        }
        
        meta.setLore(lore);
    }
    
    /**
     * 从物品中获取单个绑定的药水效果（兼容旧版本，返回第一个效果）
     */
    public static PotionEffect getBoundEffect(ItemStack item) {
        List<PotionEffect> effects = getBoundEffects(item);
        return (effects != null && !effects.isEmpty()) ? effects.get(0) : null;
    }
    
    /**
     * 从物品中获取所有绑定的药水效果列表
     */
    public static List<PotionEffect> getBoundEffects(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, EFFECT_KEY);

        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return null;
        }

        String effectDataString = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (effectDataString == null || effectDataString.isEmpty()) {
            return null;
        }

        List<PotionEffect> effects = new ArrayList<>();
        try {
            // 按 ; 分割多个效果
            String[] effectStrings = effectDataString.split("\\" + EFFECT_SEPARATOR);
            
            for (String effectStr : effectStrings) {
                String[] parts = effectStr.split("\\" + PROPERTY_SEPARATOR);
                if (parts.length != 3) {
                    continue;
                }

                PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.fromString(parts[0]));
                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);

                if (type != null) {
                    effects.add(new PotionEffect(type, duration, amplifier, false, false, true));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[EffectData] 解析绑定效果数据失败: " + e.getMessage());
        }

        return effects.isEmpty() ? null : effects;
    }
    
    /**
     * 检查物品是否绑定了药水效果
     */
    public static boolean hasBoundEffect(ItemStack item) {
        List<PotionEffect> effects = getBoundEffects(item);
        return effects != null && !effects.isEmpty();
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
