# 迁移

Eternal Parkour 取代 Infinite Parkour（IP）、Infinite Parkour Reborn、IPPlus 与 Infinite Elytra Parkour（IEP）。不要让这些旧 jar 与它同时运行。

## 升级前

1. 关闭服务器。
2. 备份 `plugins/IP/`、`plugins/IPPlus/` 和 `plugins/IEP/`（存在哪个就备份哪个）。
3. 从 `plugins/` 移除旧插件 jar。
4. 放入 Eternal Parkour jar 并启动服务器。

## 自动导入数据

启动时，插件会复制目标位置尚不存在的文件：

| 原位置 | 新位置 |
| --- | --- |
| `plugins/IP/` | `plugins/EternalParkour/` |
| `plugins/IPPlus/` | `plugins/EternalParkour/plus/` |
| `plugins/IEP/` | `plugins/EternalParkour/elytra/` |

导入不会修改或删除原目录，也不会覆盖 `plugins/EternalParkour/` 中已有的文件。每个原目录只导入一次，已导入的目录记录在 `plugins/EternalParkour/.legacy-imports` 中；删除其中一行即可重新导入对应目录。

## 保持不变的内容

- 权限节点（`ip.*`、`iep.*`）及其默认值。
- `/parkour`、`/witp`、`/ipp` 和 `/iep` 仍然可用；`/ip` 已由 `/ep` 取代。
- `%witp_...%` 和 `%iep_...%` 变量仍然可用；新配置请使用 `%eternalparkour_...%`。
- 跑酷世界名称（默认 `witp`）和 MySQL 表名。
- Infinite Parkour 保存的背包备份仍可通过 `/ep recoverinventory` 恢复。

## 检查清单

- 打开 `/ep`、`/ipp` 和 `/iep`。
- 检查玩家分数与排行榜。
- 测试自定义风格和建筑模板。
- 如果启用了 MySQL，检查数据库连接设置。
- 分别加入并离开三类跑酷，确认背包正确恢复。
- 检查控制台是否存在配置或材料名称警告。

全部数据和玩法确认无误后，再手动归档旧数据目录；在此之前请一直保留备份。
