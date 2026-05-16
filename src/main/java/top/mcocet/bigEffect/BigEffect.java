package top.mcocet.bigEffect;

import org.bukkit.plugin.java.JavaPlugin;

public final class BigEffect extends JavaPlugin {

    @Override
    public void onEnable() {
        // 初始化数据管理类
        EffectData.init(this);
        
        // 注册命令
        BindEffectCommand bindCommand = new BindEffectCommand();
        getCommand("bindEffect").setExecutor(bindCommand);
        
        // 注册事件监听器
        EffectListener effectListener = new EffectListener(this);
        getServer().getPluginManager().registerEvents(effectListener, this);
        
        // 启动效果检查任务，为当前在线玩家和后续加入的玩家启动
        effectListener.startTasksForExistingPlayers();
        
        // 发送启用消息
        getLogger().info("BigEffect 插件已启用!");
    }

    @Override
    public void onDisable() {
        // 发送禁用消息
        getLogger().info("BigEffect 插件已禁用!");
    }
}
