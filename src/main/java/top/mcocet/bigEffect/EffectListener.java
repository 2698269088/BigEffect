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

            // 启动固定速率任务：延迟1tick开始，每100 ticks（5秒）执行一次
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
                100L  // 周期：每100 ticks（5秒）
            );

            playerSchedulers.put(playerId, scheduledTask);
            plugin.getLogger().info("[Folia] 已为 " + player.getName() + " 启动效果检查任务（GlobalRegionScheduler + EntityScheduler，固定速率 5秒）");
        } catch (Exception e) {
            plugin.getLogger().severe("[Folia] 启动任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查玩家物品栏，将需要应用的效果通过 EntityScheduler 提交到玩家所在区域线程执行
     */
    private void checkAndScheduleEffects(Player player) {
        if (!player.isOnline()) return;

        ItemStack[] contents = player.getInventory().getContents();

        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) continue;

            PotionEffect boundEffect = EffectData.getBoundEffect(item);
            if (boundEffect == null) continue;

            // 检查是否需要应用效果
            PotionEffect existingEffect = player.getPotionEffect(boundEffect.getType());
            boolean shouldApply = false;

            if (existingEffect == null) {
                shouldApply = true;
            } else if (existingEffect.getDuration() < 160) {
                shouldApply = true;
            }

            if (shouldApply) {
                // 通过 EntityScheduler 在玩家所在区域线程应用效果
                scheduleEffectOnPlayerThread(player, boundEffect);
            }
        }
    }

    /**
     * 使用玩家 EntityScheduler 在正确的区域线程上应用药水效果
     * 注意：run(Plugin, Consumer<ScheduledTask>, Runnable) 中
     *       Consumer 是任务执行体，Runnable 是取消回调（可为 null）
     */
    private void scheduleEffectOnPlayerThread(Player player, PotionEffect effect) {
        try {
            Object scheduler = player.getScheduler();
            if (scheduler == null) return;

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
                            effect.getType(), 200, effect.getAmplifier(), true, true, true
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
        
        task.runTaskTimer(plugin, 0L, 100L);
        playerSchedulers.put(playerId, task);
        plugin.getLogger().info("[Bukkit] 成功启动 " + player.getName() + " 的效果检查任务");
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
        plugin.getLogger().info("=== 启动效果检查任务 ===");
        plugin.getLogger().info("服务器类型: " + (isFolia ? "Folia" : "Paper/Spigot"));
        plugin.getLogger().info("当前在线玩家: " + onlinePlayers.size());
        
        for (Player player : onlinePlayers) {
            startPlayerTask(player);
        }
        
        plugin.getLogger().info("=== 所有任务启动完成 ===");
    }
    
    /**
     * 检查玩家物品栏并应用药水效果
     */
    private void checkAndApplyEffects(Player player) {
        plugin.getLogger().info("[===检查===] 开始检查玩家 " + player.getName() + " (" + player.getUniqueId() + ") 的物品栏");
        plugin.getLogger().info("[===检查===] 玩家在线状态: " + player.isOnline());
        
        ItemStack[] contents = player.getInventory().getContents();
        plugin.getLogger().info("[===检查===] 物品栏总槽位数: " + contents.length);
        
        boolean foundEffect = false;
        int slotIndex = 0;
        int totalItems = 0;
        
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) {
                slotIndex++;
                continue;
            }
            
            totalItems++;
            plugin.getLogger().info("[===物品===] 玩家 " + player.getName() + " 槽位 " + slotIndex + ": " + item.getType() + " x" + item.getAmount());
            
            // 检查是否有 ItemMeta
            if (item.hasItemMeta()) {
                plugin.getLogger().info("[===物品===] 槽位 " + slotIndex + " 有 ItemMeta");
            } else {
                plugin.getLogger().info("[===物品===] 槽位 " + slotIndex + " 无 ItemMeta，跳过");
                slotIndex++;
                continue;
            }
            
            // 检查物品是否绑定了药水效果
            plugin.getLogger().info("[===检查===] 尝试从槽位 " + slotIndex + " 读取绑定效果...");
            PotionEffect boundEffect = EffectData.getBoundEffect(item);
            
            if (boundEffect != null) {
                foundEffect = true;
                plugin.getLogger().info("[===成功===] 玩家 " + player.getName() + " 槽位 " + slotIndex + " 发现绑定效果: " + boundEffect.getType().getKey());
                plugin.getLogger().info("[===成功===] 效果等级: " + boundEffect.getAmplifier() + ", 持续时间: " + boundEffect.getDuration() + " ticks");
                
                // 获取当前已有的相同效果
                PotionEffect existingEffect = player.getPotionEffect(boundEffect.getType());
                
                // 如果没有该效果,或者效果的持续时间不足8秒(需要刷新)
                boolean shouldApply = false;
                
                if (existingEffect == null) {
                    shouldApply = true;
                    plugin.getLogger().info("[===效果===] 玩家 " + player.getName() + " 当前没有 " + boundEffect.getType().getKey() + " 效果，准备应用");
                } else {
                    plugin.getLogger().info("[===效果===] 玩家 " + player.getName() + " 已有 " + boundEffect.getType().getKey() + " 效果，剩余时间: " + existingEffect.getDuration() + " ticks");
                    // 如果剩余时间少于8秒(160 ticks),则刷新
                    if (existingEffect.getDuration() < 160) {
                        shouldApply = true;
                        plugin.getLogger().info("[===效果===] 效果即将过期，准备刷新");
                    } else {
                        plugin.getLogger().info("[===效果===] 效果仍在持续，无需刷新");
                    }
                }
                
                if (shouldApply) {
                    plugin.getLogger().info("[===应用===] 正在为玩家 " + player.getName() + " 添加效果: " + boundEffect.getType().getKey() + " Level " + (boundEffect.getAmplifier() + 1));
                    
                    try {
                        // 添加药水效果,持续10秒(200 ticks)
                        PotionEffect newEffect = new PotionEffect(
                            boundEffect.getType(),
                            200, // 10秒
                            boundEffect.getAmplifier(),
                            true, // 显示粒子效果
                            true, // 显示图标
                            true   // 覆盖现有效果
                        );
                        
                        plugin.getLogger().info("[===应用===] 创建的效果对象: " + newEffect.getType().getKey() + ", 持续时间: " + newEffect.getDuration() + ", 等级: " + newEffect.getAmplifier());
                        
                        player.addPotionEffect(newEffect);
                        
                        // 验证效果是否成功应用
                        PotionEffect verifyEffect = player.getPotionEffect(boundEffect.getType());
                        if (verifyEffect != null) {
                            plugin.getLogger().info("[===成功===] 效果已成功应用到玩家 " + player.getName() + "，剩余时间: " + verifyEffect.getDuration() + " ticks");
                        } else {
                            plugin.getLogger().warning("[===警告===] 效果应用后无法验证！玩家 " + player.getName() + " 没有 " + boundEffect.getType().getKey() + " 效果");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("[===错误===] 应用效果时发生异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                plugin.getLogger().info("[===检查===] 槽位 " + slotIndex + " 没有绑定效果");
            }
            
            slotIndex++;
        }
        
        if (!foundEffect) {
            plugin.getLogger().info("[===总结===] 玩家 " + player.getName() + " 的物品栏中共有 " + totalItems + " 个物品，但没有发现绑定效果的物品");
        } else {
            plugin.getLogger().info("[===总结===] 玩家 " + player.getName() + " 的物品栏检查完成，共检查 " + totalItems + " 个物品");
        }
    }
}
