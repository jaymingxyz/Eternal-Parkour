# PlaceholderAPI

Install PlaceholderAPI and restart the server. The expansions are registered by Eternal Parkour; no separate eCloud download is required.

## Block parkour: `eternalparkour`

The old `witp` prefix still works, so `%witp_score%` and `%eternalparkour_score%` show the same value.

### General and player values

| Placeholder | Value |
| --- | --- |
| `%eternalparkour_version%` | Plugin version |
| `%eternalparkour_leader%` | Default-mode record holder |
| `%eternalparkour_leader_score%` | Default-mode record score |
| `%eternalparkour_rank%` | Player's default-mode rank |
| `%eternalparkour_highscore%` | Player's default-mode high score |
| `%eternalparkour_high_score_time%` | Time attached to that high score |
| `%eternalparkour_score%` | Current score |
| `%eternalparkour_time%` | Current formatted run time |
| `%eternalparkour_blocklead%` | Current block lead |
| `%eternalparkour_style%` | Current style key |
| `%eternalparkour_time_preference%` | Selected world time |
| `%eternalparkour_scoreboard%` | Whether the scoreboard is enabled |
| `%eternalparkour_difficulty%` | Numeric schematic difficulty |
| `%eternalparkour_difficulty_string%` | Easy, medium, hard, or very hard |
| `%eternalparkour_score_until_100%` | Points remaining until the next multiple of 100 |

Aliases include `%eternalparkour_ver%`, `%eternalparkour_record_player%`, `%eternalparkour_record_score%`, `%eternalparkour_record%`, `%eternalparkour_high_score%`, `%eternalparkour_current_score%`, `%eternalparkour_current_time%`, `%eternalparkour_lead%`, and `%eternalparkour_time_pref%`.

### Ranked values

Replace `<rank>` with a positive position:

```text
%eternalparkour_player_rank_<rank>%
%eternalparkour_score_rank_<rank>%
%eternalparkour_time_rank_<rank>%
%eternalparkour_difficulty_rank_<rank>%
%eternalparkour_difficulty_string_rank_<rank>%
```

Insert a mode key before the rank for a mode-specific leaderboard. For example:

```text
%eternalparkour_player_rank_speed_1%
%eternalparkour_score_rank_team_survival_3%
```

## Elytra parkour: `iep`

The player must be in an elytra run for these values:

| Placeholder | Value |
| --- | --- |
| `%iep_score%` | Current distance score |
| `%iep_time%` | Current elapsed time |
| `%iep_seed%` | Current seed |
| `%iep_speed%` | Current speed |

Leaderboard placeholders use `%iep_<mode>_<type>_<rank>%`, where type is `name`, `score`, `time`, or `seed`.

```text
%iep_default_name_1%
%iep_default_score_1%
```

Use the exact internal mode key. Modes containing spaces are not suitable for every placeholder-consuming plugin, so test them in the target display before deployment.
