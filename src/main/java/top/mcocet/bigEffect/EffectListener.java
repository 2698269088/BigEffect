package top.mcocet.bigEffect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectListener implements Listener {
    
    private final Plugin plugin;
    private final Map<UUID, Object> playerSchedulers = new HashMap<>();
    private final boolean isFolia;
    
    public EffectListener(Plugin plugin) {
        this.plugin = plugin;
        // 检测是否为 Folia 服务器
        this.isFolia = isFoliaServer();
    }
    
    /**
     * 检测是否为 Folia 服务器
     */
    private boolean isFoliaServer() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 玩家加入时开始检查物品栏
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("玩家 " + player.getName() + " 加入服务器");
        // 启动该玩家的效果检查任务
        startPlayerTask(player);
    }
    
    /**
     * 玩家离开时停止任务
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("玩家 " + player.getName() + " 离开服务器");
        stopPlayerTask(player);
    }
    
    /**
     * 启动玩家专属的效果检查任务
     */
    private void startPlayerTask(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 如果已有任务，先停止
        stopPlayerTask(player);
        
        plugin.getLogger().info("[任务] 为 " + player.getName() + " 启动效果检查任务");
        
        if (isFolia) {
            startFoliaPlayerTask(player, playerId);
        } else {
            startBukkitPlayerTask(player, playerId);
        }
    }
    
    /**
     * Folia 玩家任务 - 使用 GlobalRegionScheduler 定时，EntityScheduler 应用效果
     */
    private void startFoliaPlayerTask(Player player, UUID playerId) {
        try {
            // 获取 GlobalRegionScheduler: Bukkit.getServer().getGlobalRegionScheduler()
            Object globalScheduler = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(Bukkit.getServer());
            if (globalScheduler == null) {
                plugin.getLogger().warning("[Folia] 无法获取全局区域调度器");
                return;
            }

            // 获取 runAtFixedRate 方法
            java.lang.reflect.Method runAtFixedRateMethod = globalScheduler.getClass().getMethod(
                "runAtFixedRate",
                org.bukkit.plugin.Plugin.class,
                java.util.function.Consumer.class,
                long.class,
                long.class
            );

            // 启动固定速率任务：延迟1tick开始，使用配置的刷新间隔
            int intervalTicks = ConfigManager.getRefreshIntervalTicks();
            Object scheduledTask = runAtFixedRateMethod.invoke(
                globalScheduler,
                plugin,
                (java.util.function.Consumer<Object>) (task) -> {
                    // 遍历所有在线玩家，检查物品栏（在全局线程安全读取）
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        checkAndScheduleEffects(onlinePlayer);
                    }
                },
                1L,   // 初始延迟：1 tick后执行（必须 > 0）
                Math.max(1L, intervalTicks)  // 周期：使用配置文件中的刷新间隔
            );

            playerSchedulers.put(playerId, scheduledTask);
            // plugin.getLogger().info("[Folia] 已为 " + player.getName() + " 启动效果检查任务（GlobalRegionScheduler + EntityScheduler，固定速率 5秒）");
        } catch (Exception e) {
            plugin.getLogger().severe("[Folia] 启动任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查玩家物品栏，将需要应用的效果通过 EntityScheduler 提交到玩家所在区域线程执行
     * 支持一个物品绑定多个效果
     */
    private void checkAndScheduleEffects(Player player) {
        if (!player.isOnline()) return;

        ItemStack[] contents = player.getInventory().getContents();

        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;

            // 获取物品绑定的所有效果
            java.util.List<PotionEffect> boundEffects = EffectData.getBoundEffects(item);
            if (boundEffects == null || boundEffects.isEmpty()) continue;

            // 对每个绑定效果检查是否需要应用
            for (PotionEffect boundEffect : boundEffects) {
                PotionEffect existingEffect = player.getPotionEffect(boundEffect.getType());
                boolean shouldApply = false;

                if (existingEffect == null) {
                    shouldApply = true;
                } else if (existingEffect.getDuration() < ConfigManager.getRefreshThresholdTicks()) {
                    shouldApply = true;
                }

                if (shouldApply) {
                    scheduleEffectOnPlayerThread(player, boundEffect);
                }
            }
        }
    }

    /**
     * 使用玩家 EntityScheduler 在正确的区域线程上应用药水效果
     * 注意：run(Plugin, Consumer<ScheduledTask>, Runnable) 中
     *       Consumer 是任务执行体，Runnable 是取消回调（可为 null）
     * 持续时间使用配置文件中的值
     */
    private void scheduleEffectOnPlayerThread(Player player, PotionEffect effect) {
        try {
            Object scheduler = player.getScheduler();
            if (scheduler == null) return;

            int duration = ConfigManager.getEffectDurationTicks();

            java.lang.reflect.Method runMethod = scheduler.getClass().getMethod(
                "run",
                org.bukkit.plugin.Plugin.class,
                java.util.function.Consumer.class,
                java.lang.Runnable.class
            );

            runMethod.invoke(
                scheduler,
                plugin,
                (java.util.function.Consumer<Object>) (task) -> {
                    // Consumer 是任务执行体，在实体所属区域线程执行
                    if (player.isOnline()) {
                        player.addPotionEffect(new PotionEffect(
                            effect.getType(), duration, effect.getAmplifier(), true, true, true
                        ));
                    }
                },
                null  // Runnable 是取消回调，这里不需要
            );
        } catch (Exception e) {
            plugin.getLogger().warning("[Folia] 通过 EntityScheduler 应用效果失败: " + e.getMessage());
        }
    }
    
    /**
     * Bukkit 玩家任务
     */
    private void startBukkitPlayerTask(Player player, UUID playerId) {
        if (isFolia) {
            plugin.getLogger().warning("[Bukkit] Folia 环境不支持 BukkitScheduler，跳过任务创建");
            return;
        }
        org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                checkAndApplyEffects(player);
            }
        };
        
        task.runTaskTimer(plugin, 0L, ConfigManager.getRefreshIntervalTicks());
        playerSchedulers.put(playerId, task);
        // plugin.getLogger().info("[Bukkit] 成功启动 " + player.getName() + " 的效果检查任务");
    }
    
    /**
     * 停止玩家的效果检查任务
     */
    private void stopPlayerTask(Player player) {
        UUID playerId = player.getUniqueId();
        Object schedulerOrTask = playerSchedulers.remove(playerId);
        
        if (schedulerOrTask != null) {
            if (schedulerOrTask instanceof org.bukkit.scheduler.BukkitRunnable) {
                ((org.bukkit.scheduler.BukkitRunnable) schedulerOrTask).cancel();
                plugin.getLogger().info("[任务] 停止 " + player.getName() + " 的 Bukkit 任务");
            }
            // Folia 任务会自动清理
        }
    }
    
    /**
     * 为所有当前在线玩家启动任务（用于插件启用时）
     */
    public void startTasksForExistingPlayers() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player player : onlinePlayers) {
            startPlayerTask(player);
        }
    }
    
    /**
     * 检查玩家物品栏并应用药水效果（Bukkit 环境使用）
     * 支持一个物品绑定多个效果
     */
    private void checkAndApplyEffects(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        int slotIndex = 0;
        
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                slotIndex++;
                continue;
            }
            
            // 获取物品绑定的所有效果
            java.util.List<PotionEffect> boundEffects = EffectData.getBoundEffects(item);
            if (boundEffects == null || boundEffects.isEmpty()) {
                slotIndex++;
                continue;
            }
            
            // 对每个绑定效果检查是否需要应用
            for (PotionEffect boundEffect : boundEffects) {
                PotionEffect existingEffect = player.getPotionEffect(boundEffect.getType());
                boolean shouldApply = false;
                
                if (existingEffect == null) {
                    shouldApply = true;
                } else if (existingEffect.getDuration() < ConfigManager.getRefreshThresholdTicks()) {
                    shouldApply = true;
                }
                
                if (shouldApply) {
                    try {
                        PotionEffect newEffect = new PotionEffect(
                            boundEffect.getType(),
                            ConfigManager.getEffectDurationTicks(),
                            boundEffect.getAmplifier(),
                            true,
                            true,
                            true
                        );
                        player.addPotionEffect(newEffect);
                    } catch (Exception e) {
                        plugin.getLogger().severe("[错误] 应用效果时发生异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            slotIndex++;
        }
    }
}
