package top.mcocet.bigEffect;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    
    private static Plugin plugin;
    private static FileConfiguration config;
    private static File configFile;
    
    // 默认配置值
    private static int effectDurationTicks = 200; // 效果持续时间（ticks）
    private static int refreshIntervalTicks = 100; // 刷新检查间隔（ticks）
    private static int refreshThresholdTicks = 160; // 刷新阈值（剩余时间小于此值时刷新）
    
    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        // 如果配置文件不存在，创建默认配置
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 读取配置值
        effectDurationTicks = config.getInt("effect_duration_ticks", 200);
        refreshIntervalTicks = config.getInt("refresh_interval_ticks", 100);
        refreshThresholdTicks = config.getInt("refresh_threshold_ticks", 160);
    }
    
    /**
     * 重新加载配置文件
     */
    public static void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        effectDurationTicks = config.getInt("effect_duration_ticks", 200);
        refreshIntervalTicks = config.getInt("refresh_interval_ticks", 100);
        refreshThresholdTicks = config.getInt("refresh_threshold_ticks", 160);
    }
    
    /**
     * 获取效果持续时间（ticks）
     */
    public static int getEffectDurationTicks() {
        return effectDurationTicks;
    }
    
    /**
     * 获取刷新检查间隔（ticks）
     */
    public static int getRefreshIntervalTicks() {
        return refreshIntervalTicks;
    }
    
    /**
     * 获取刷新阈值（ticks）
     * 当效果剩余时间小于此值时进行刷新
     */
    public static int getRefreshThresholdTicks() {
        return refreshThresholdTicks;
    }
    
    /**
     * 获取效果持续时间（秒）
     */
    public static double getEffectDurationSeconds() {
        return effectDurationTicks / 20.0;
    }
    
    /**
     * 获取刷新检查间隔（秒）
     */
    public static double getRefreshIntervalSeconds() {
        return refreshIntervalTicks / 20.0;
    }
    
    /**
     * 获取刷新阈值（秒）
     */
    public static double getRefreshThresholdSeconds() {
        return refreshThresholdTicks / 20.0;
    }
    
    /**
     * 保存配置更改
     */
    public static void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存配置文件失败: " + e.getMessage());
        }
    }
}
