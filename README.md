# BigEffect

给物品绑定药水效果，手持即可激活。

## 功能介绍

BigEffect 是一款 Minecraft 服务器插件，允许管理员将药水效果绑定到物品上。当玩家手持该物品时，会自动获得对应的药水效果。

### 特性

- 将任意药水效果绑定到物品
- 一个物品可绑定多个药水效果
- 手持绑定物品自动激活所有效果
- 效果持续时间和刷新间隔可通过配置文件自定义
- 支持 Folia 和 Paper/Spigot 服务端
- 物品 Lore 显示所有绑定效果信息

## 命令

### `/bindEffect <药水效果名称> [等级]`

为手持物品绑定药水效果（多次执行可叠加多个效果）。

**权限**: `bigeffect.bind`（默认仅 OP）

**用法示例**:
```
/bindEffect speed          # 绑定速度 I
/bindEffect speed 2        # 绑定速度 II
/bindEffect strength 3     # 绑定力量 III
/bindEffect regeneration   # 绑定生命恢复 I
/bindEffect speed 2        # 再次执行可替换已有的速度效果
```

### `/bigEffect reload`

重新加载配置文件。

**权限**: `bigeffect.admin`（默认仅 OP）

## 药水效果列表

| 英文名称 | 中文名称 |
|---------|---------|
| speed | 速度 |
| slowness | 缓慢 |
| haste | 急迫 |
| mining_fatigue | 挖掘疲劳 |
| strength | 力量 |
| instant_health | 瞬间治疗 |
| instant_damage | 瞬间伤害 |
| jump_boost | 跳跃提升 |
| nausea | 反胃 |
| regeneration | 生命恢复 |
| resistance | 抗性提升 |
| fire_resistance | 防火 |
| water_breathing | 水下呼吸 |
| invisibility | 隐身 |
| blindness | 失明 |
| night_vision | 夜视 |
| hunger | 饥饿 |
| weakness | 虚弱 |
| poison | 中毒 |
| wither | 凋零 |
| health_boost | 生命提升 |
| absorption | 伤害吸收 |
| saturation | 饱和 |
| glowing | 发光 |
| levitation | 漂浮 |
| luck | 幸运 |
| unluck | 霉运 |
| slow_falling | 缓降 |
| conduit_power | 潮涌能量 |
| dolphins_grace | 海豚的恩惠 |
| bad_omen | 不祥之兆 |
| hero_of_the_village | 村庄英雄 |
| darkness | 黑暗 |
| trial_omen | 试炼之兆 |
| raid_omen | 袭击之兆 |
| wind_charged | 蓄风 |
| weaving | 缠绕 |
| oozing | 渗浆 |
| infested | 寄生 |

## 配置文件

插件首次加载时会在 `plugins/BigEffect/config.yml` 生成默认配置：

```yaml
# 药水效果持续时间（ticks）
# 1 tick = 0.05 秒，200 ticks = 10 秒
effect_duration_ticks: 200

# 刷新检查间隔（ticks）
# 每多少 ticks 检查一次玩家物品栏
# 100 ticks = 5 秒
refresh_interval_ticks: 100

# 刷新阈值（ticks）
# 当效果剩余时间小于此值时进行刷新
# 160 ticks = 8 秒
refresh_threshold_ticks: 160
```

修改配置后执行 `/bigEffect reload` 即可生效。

## 使用说明

1. 手持想要绑定效果的物品
2. 执行 `/bindEffect <效果名称> [等级]` 绑定效果（可多次执行绑定多个效果）
3. 物品 Lore 会显示所有绑定效果信息
4. 手持该物品即可自动获得所有药水效果
5. 修改配置文件后使用 `/bigEffect reload` 重新加载

## 技术信息

- **适用版本**: Minecraft 1.21+
- **支持服务端**: Folia、Paper、Spigot
- **Java 版本**: Java 21+
