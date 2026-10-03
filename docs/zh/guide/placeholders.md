# PlaceholderAPI

安装 PlaceholderAPI 并重启服务器。变量扩展由 Eternal Parkour 自行注册，无需从 eCloud 另行下载。

## 方块跑酷：`eternalparkour`

旧的 `witp` 前缀仍然可用，例如 `%witp_score%` 与 `%eternalparkour_score%` 显示相同的值。

| 变量 | 内容 |
| --- | --- |
| `%eternalparkour_version%` | 插件版本 |
| `%eternalparkour_leader%` | 默认模式纪录保持者 |
| `%eternalparkour_leader_score%` | 默认模式纪录分数 |
| `%eternalparkour_rank%` | 玩家默认模式排名 |
| `%eternalparkour_highscore%` | 玩家默认模式最高分 |
| `%eternalparkour_high_score_time%` | 最高分对应时间 |
| `%eternalparkour_score%` | 当前分数 |
| `%eternalparkour_time%` | 当前格式化用时 |
| `%eternalparkour_blocklead%` | 当前方块预生成数量 |
| `%eternalparkour_style%` | 当前风格键 |
| `%eternalparkour_time_preference%` | 玩家选择的世界时间 |
| `%eternalparkour_scoreboard%` | 是否显示计分板 |
| `%eternalparkour_difficulty%` | 模板难度数值 |
| `%eternalparkour_difficulty_string%` | 模板难度文字 |
| `%eternalparkour_score_until_100%` | 距离下一个 100 倍数还差多少分 |

别名包括 `%eternalparkour_ver%`、`%eternalparkour_record_player%`、`%eternalparkour_record_score%`、`%eternalparkour_record%`、`%eternalparkour_high_score%`、`%eternalparkour_current_score%`、`%eternalparkour_current_time%`、`%eternalparkour_lead%` 和 `%eternalparkour_time_pref%`。

### 排名变量

将 `<排名>` 替换为正整数：

```text
%eternalparkour_player_rank_<排名>%
%eternalparkour_score_rank_<排名>%
%eternalparkour_time_rank_<排名>%
%eternalparkour_difficulty_rank_<排名>%
%eternalparkour_difficulty_string_rank_<排名>%
```

在排名前加入模式键可查询指定模式：

```text
%eternalparkour_player_rank_speed_1%
%eternalparkour_score_rank_team_survival_3%
```

## 鞘翅跑酷：`iep`

以下变量要求玩家正在进行鞘翅跑酷：

| 变量 | 内容 |
| --- | --- |
| `%iep_score%` | 当前距离分数 |
| `%iep_time%` | 当前用时 |
| `%iep_seed%` | 当前种子 |
| `%iep_speed%` | 当前速度 |

排行榜格式为 `%iep_<模式>_<类型>_<排名>%`，类型可为 `name`、`score`、`time` 或 `seed`。

```text
%iep_default_name_1%
%iep_default_score_1%
```

请使用准确的内部模式键。名称含空格的模式并不适用于所有变量显示插件，上线前应在目标位置实际测试。
