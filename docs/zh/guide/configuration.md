# 配置

大幅修改前请关闭服务器并备份对应文件。`/ep reload` 可重载全部模块（核心、多人与鞘翅）的配置和语言文件；涉及世界、模式注册或存储时，完整重启最稳妥。

## 文件索引

| 文件 | 用途 |
| --- | --- |
| `plugins/EternalParkour/config.yml` | 核心行为、世界、存储、权限、风格、粒子和默认设置 |
| `plugins/EternalParkour/generation.yml` | 跳跃距离、概率和方块生成 |
| `plugins/EternalParkour/rewards-v2.yml` | 方块跑酷及多人奖励 |
| `plugins/EternalParkour/schematics/schematics.yml` | 建筑模板难度注册 |
| `plugins/EternalParkour/locales/<语言>.yml` | 核心菜单和消息 |
| `plugins/EternalParkour/plus/config.yml` | 多人模式和渐变风格 |
| `plugins/EternalParkour/plus/locales/<语言>.yml` | 多人菜单和消息 |
| `plugins/EternalParkour/elytra/config.yml` | 鞘翅模式、设置、存储和风格 |
| `plugins/EternalParkour/elytra/rewards.yml` | 鞘翅奖励 |
| `plugins/EternalParkour/elytra/locales/<语言>.yml` | 鞘翅菜单和消息 |

## 核心配置

`plugins/EternalParkour/config.yml` 的主要部分：

- `joining`：是否允许玩家加入方块跑酷。
- `modules.elytra`：是否启用鞘翅跑酷（默认 `true`），它使用名为 `iep` 的第二个虚空世界，修改后需重启。
- `bungeecord`：自动加入、返回服务器和非代理返回坐标。
- `sql`：MySQL 地址、端口、账号、数据库和表前缀。
- `world`：跑酷世界名、重载时删除和后备世界。
- `options`：方块预生成数量、时间格式和背包处理。
- `permissions`：权限检查与按风格授权。
- `focus-mode`：跑酷（方块或鞘翅）中只允许本插件的命令和白名单中的命令名。服务器有交易或拍卖插件时建议开启，详见[生存服务器](./survival-servers)。
- `styles.list`：随机方块风格。
- `scoring`：计分和周期奖励行为。
- `particles`：生成粒子的形状、类型以及音效。音效使用 `block.note_block.guitar` 这样的键名，旧名称如 `BLOCK_NOTE_BLOCK_GUITAR` 也可以使用。
- `default-values`：菜单开关和玩家默认设置。

::: warning 背包安全
请保持 `options.inventory-saving` 开启。它会把跑酷改变的所有玩家状态备份到 `backups/`，服务器崩溃也不会丢失物品：备份会在玩家下次进入时自动恢复。详见[故障排查](./troubleshooting#背包没有恢复)。
:::

## 多人配置

`plugins/EternalParkour/plus/config.yml` 包含：

- `send_back_after_multiplayer`：比赛结束后恢复原位置/数据，或进入普通单人跑酷。
- `gamemodes.<名称>.enabled`：显示或隐藏模式。
- 各模式的 `time`、`goal`、`max` 和 `island_distance` 等参数。
- `styles.incremental`：按顺序循环使用的材料列表。

模式键为 `hourglass`、`lobby`、`practice`、`speed`、`super_jump`、`time_trial`、`wave_trial`、`duels` 和 `team_survival`。

## 鞘翅配置

`plugins/EternalParkour/elytra/config.yml` 包含：

- `join-on-join`、`permissions`、`time-format`。
- `proxy` 返回服务器设置。
- `mysql` 数据库连接。
- `mode-settings` 中的 Close、Min Speed、Obstacle、Speed Demon 和 Time Trial。
- `settings` 中的风格、半径、时间、种子、语言、坠落、信息和公制单位。
- `styles.random` 与 `styles.incremental` 材料列表。

半径必须为 3–6，世界时间为 0–24000，固定种子为 0–1,000,000；默认值 `-1` 表示随机种子。

`/ep reload` 会应用此文件、`rewards.yml` 和鞘翅语言文件的修改，进行中的航线也会生效；开关 `mode-settings` 中的模式或修改 `mysql` 设置仍需重启。

## 自定义语言

复制现有语言文件，以目标语言键重命名，只翻译值而不要改动 YAML 路径。保留 MiniMessage 标签和替换变量。自定义鞘翅风格的显示名应写在对应语言文件的 `styles.names.<键>` 下。
