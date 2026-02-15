# MythicRelics

Paper 1.21.11 plugin built with Java 21 + Maven.

## Build
```bash
mvn clean package
```

## Command
`/mythic` (permission: `mythic.admin`)

### Subcommands
- `/mythic give <player> <itemKey>`
- `/mythic cooldown reset <player> [ability|all]`
- `/mythic cooldown resetall`
- `/mythic cooldown clearall`
- `/mythic ritual start <player> <itemKey> [x y z]`
- `/mythic ritual status [player]`
- `/mythic ritual cancel <player>`
- `/mythic reload`

## Item keys
- `voidblade`
- `luckcoin`
- `windwaker`
- `heracles`
- `zeus`
- `goldenfleece`
- `cronos`
- `bagofwinds`

## Mythics
All mythics are unbreakable, blocked from enchanting/anvil enchanting, and tagged through PDC.

- **Void Blade**: boosted sword damage, Dragon Run flight mode, Dragon Breath shot.
- **Luck Coin**: 50/50 buff/debuff flip with timed mace reward on good outcome.
- **Wind Waker**: passive Speed II while holding + AoE gale ability.
- **Heracles Sword**: Titan’s Surge combat steroid and post-duration weakness.
- **Zeus’s Javelin**: armed empowered throw, lightning on hit, passive lightning melee proc.
- **Golden Fleece**: offhand resurrection control with 1-hour cooldown and inventory drop rules.
- **Cronos Scythe**: netherite sword variant with wither passive and Time Prison dome.
- **Bag of Winds** (non-mythic): mobility burst, cooldown, uses then break.

## Rituals
Rituals are command-started only. They persist to `rituals.yml` with owner, item, location, timer, and display UUID. Ritual HUD/bossbar, floating item display, particle/sound ambiance, and completion drops are included.

## Persistence
- `cooldowns.yml` keeps ability cooldown expiry timestamps.
- `rituals.yml` keeps active ritual state.
