# 命令

`<尖括号>` 表示必填参数，`[方括号]` 表示可选参数。

## 方块跑酷

`/ep`、`/eternalparkour`、`/ep` 和 `/witp` 是同一个命令。

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/ep` | 打开主菜单；非玩家执行时显示帮助 | `ip.main` |
| `/ep help` | 显示可用命令 | — |
| `/ep join [模式/玩家]` | 加入默认模式、指定模式或其他玩家的会话 | `ip.join` |
| `/ep leave` | 离开当前会话 | `ip.quit` |
| `/ep menu` | 打开主菜单 | `ip.main` |
| `/ep play` | 打开模式选择 | `ip.play` |
| `/ep leaderboard [模式]` | 打开排行榜 | `ip.community.leaderboards` |
| `/ep reload` | 重载全部配置和语言文件：核心、多人与鞘翅 | `ip.admin` |
| `/ep reset <everyone/玩家/UUID>` | 永久清除方块跑酷成绩 | `ip.admin` |
| `/ep forcejoin <everyone/nearest/玩家>` | 强制玩家加入默认模式 | `ip.admin` |
| `/ep forceleave <everyone/玩家>` | 强制玩家离开跑酷 | `ip.admin` |
| `/ep recoverinventory <玩家>` | 恢复在线玩家保存的背包 | `ip.admin` |

### 方块跑酷建筑模板

以下命令需要 `ip.admin`，并且必须由玩家执行。

| 命令 | 说明 |
| --- | --- |
| `/ep schematic wand` | 获取选区工具 |
| `/ep schematic pos1` | 在当前位置设置点 1 |
| `/ep schematic pos2` | 在当前位置设置点 2 |
| `/ep schematic save` | 用随机代码保存选区 |
| `/ep schematic paste <文件>` | 在当前位置粘贴已加载的模板 |

## 多人模式

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/ipp create` | 打开多人模式选择 | `ip.multiplayer` |
| `/ipp lobbies` | 查看正在运行的多人房间 | `ip.active` |
| `/ipp invite` | 打开邀请菜单 | `ip.invite` |
| `/ipp lobbygm pos1` | 设置大厅模式选区点 1 | `ip.admin` |
| `/ipp lobbygm pos2` | 设置大厅模式选区点 2 | `ip.admin` |
| `/ipp lobbygm save` | 保存大厅模式选区 | `ip.admin` |

`/ipp multiplayer` 等同于 `/ipp create`；`/ipp lobby` 等同于 `/ipp lobbies`；旧的 `/ipp reload` 会执行 `/ep reload`。

## 鞘翅跑酷

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/iep play` | 打开游玩菜单 | `iep.play` |
| `/iep leaderboards` | 打开鞘翅排行榜 | `iep.leaderboard` |
| `/iep settings` | 打开鞘翅设置 | `iep.setting` |
| `/iep leave` | 离开鞘翅跑酷 | `iep.leave` |
| `/iep seed <种子>` | 设置当前跑酷的非负种子 | `iep.setting.seed` |
| `/iep schematic <x,y,z> <x,y,z>` | 将长方体选区保存为鞘翅模板 | OP |
| `/iep reset <玩家/UUID> [模式]` | 清除全部或指定模式成绩 | OP |
