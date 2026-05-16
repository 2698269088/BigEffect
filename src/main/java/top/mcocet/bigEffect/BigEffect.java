package top.mcocet.bigEffect;

import org.bukkit.plugin.java.JavaPlugin;

public final class BigEffect extends JavaPlugin {

    @Override
    public void onEnable() {
        // 初始化配置管理器
        ConfigManager.init(this);
        
        // 初始化数据管理类
        EffectData.init(this);
        
        // 注册命令和 Tab 补全
        BigEffectTabCompleter tabCompleter = new BigEffectTabCompleter();
        
        BindEffectCommand bindCommand = new BindEffectCommand();
        getCommand("bindEffect").setExecutor(bindCommand);
        getCommand("bindEffect").setTabCompleter(tabCompleter);
        
        BigEffectCommand mainCommand = new BigEffectCommand();
        getCommand("bigEffect").setExecutor(mainCommand);
        getCommand("bigEffect").setTabCompleter(tabCompleter);
        
        // 注册事件监听器
        EffectListener effectListener = new EffectListener(this);
        getServer().getPluginManager().registerEvents(effectListener, this);
        
        // 启动效果检查任务，为当前在线玩家和后续加入的玩家启动
        effectListener.startTasksForExistingPlayers();
        
        // 发送启用消息
        getLogger().info("BigEffect 插件已启用!");
        getLogger().info("效果持续时间: " + ConfigManager.getEffectDurationSeconds() + "秒");
        getLogger().info("刷新检查间隔: " + ConfigManager.getRefreshIntervalSeconds() + "秒");
        getLogger().info("刷新阈值: " + ConfigManager.getRefreshThresholdSeconds() + "秒");
    }

    @Override
    public void onDisable() {
        // 发送禁用消息
        getLogger().info("BigEffect 插件已禁用!");
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        ConfigManager.reload();
        getLogger().info("配置已重新加载!");
        getLogger().info("效果持续时间: " + ConfigManager.getEffectDurationSeconds() + "秒");
        getLogger().info("刷新检查间隔: " + ConfigManager.getRefreshIntervalSeconds() + "秒");
        getLogger().info("刷新阈值: " + ConfigManager.getRefreshThresholdSeconds() + "秒");
    }
}
