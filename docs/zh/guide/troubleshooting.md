# 故障排查

## 插件无法加载

1. 确认服务端是 Paper 26.3。
2. 确认服务端使用 Java 25 或更高版本。
3. 移除旧 IPPlus 和 IEP jar，只保留一个 Eternal Parkour jar。
4. 查看控制台中的第一条异常，而不只是最后的“插件已禁用”。

## 修改配置后没有效果

- 检查 YAML 缩进与引号。
- 确认材料、粒子和音效存在于当前 Paper 版本。
- 修改后执行 `/ep reload`，它会重载核心、多人与鞘翅文件。
- 世界、模式注册或存储设置请重启服务器。
- 先备份，再与新生成的默认文件对比。

## 玩家无法打开菜单或加入

检查 `plugins/EternalParkour/config.yml` 中的 `permissions.enabled`、对应的 `default-values` 菜单开关以及玩家权限。鞘翅权限使用 `plugins/EternalParkour/elytra/config.yml` 中独立的 `permissions` 开关。

需要允许玩家加入方块跑酷时，还要确认核心配置为 `joining: true`。

## 背包没有恢复

玩家加入方块或鞘翅跑酷时，其背包、位置、游戏模式、饥饿值、飞行状态和药水效果会备份到 `plugins/EternalParkour/backups/<uuid>.json`，全部归还后备份即被删除。如果服务器未正常关闭，该备份会在玩家下次进入服务器时自动恢复，并提示玩家。

如需手动恢复，请让玩家保持在线且不在跑酷中，然后执行：

```text
/ep recoverinventory <玩家>
```

该命令会显示备份中物品的堆数。恢复会替换玩家当前的背包和游戏模式，因此需执行 `/ep recoverinventory <玩家> confirm` 才会真正恢复，恢复后备份会被删除。该命令需要 `ip.admin`，也能读取 Infinite Parkour 留下的背包备份（`inventories/<uuid>`）。

如果玩家再次加入跑酷时旧备份仍然存在（例如无法读取），它会被保留为 `backups/<uuid>.json.unrecovered-<时间>` 而不会被覆盖，控制台也会提示。如需恢复，请关闭服务器并将其改名回 `<uuid>.json`，玩家下次进入时即会恢复。

跑酷中发放的物品（菜单物品、鞘翅、决斗开始物品）都带有标记，不在跑酷中的玩家持有时会被移除。

## 迁移后的文件缺失

迁移只会在目标文件不存在时复制。请对比：

- `plugins/IP/` 与 `plugins/EternalParkour/`
- `plugins/IPPlus/` 与 `plugins/EternalParkour/plus/`
- `plugins/IEP/` 与 `plugins/EternalParkour/elytra/`

手动复制前必须关闭服务器，并保留两份备份。

## 报告问题

请提交 [GitHub Issue](https://github.com/jaymingxyz/EternalParkour/issues)，并附上：

- 准确的 Paper 与 Java 版本；
- 插件版本；
- 启动日志和完整异常；
- 已隐藏密码的相关配置；
- 可以稳定复现问题的步骤。

请勿在评价区报告问题，评价区不适合进行完整排查。
