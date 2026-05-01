# Pale Moon Scripture Series — Design Spec
**Date:** 2026-05-01

## Overview

Two Soul path cultivation techniques forming a series: **Pale Moon Scripture** (T1) and **Gibbous Moon Scripture** (T2). Both draw power from lunar energy, rewarding players who cultivate under moonlight while looking directly at the moon. Both punish cultivation under direct sunlight with fire damage.

---

## T1 — Pale Moon Scripture (rename + update)

### Rename
| What | Old | New |
|---|---|---|
| Registry key | `pale_moon_sutra` | `pale_moon_scripture` |
| Display name | `"Pale Moon Technique"` | `"Pale Moon Scripture"` |
| Translation key (technique) | `ascension.technique.pale_moon_technique` | `ascension.technique.pale_moon_scripture` |
| Translation key (skill title) | `ascension.skill.pale_moon_cultivation_skill` | updated to match |

Class name `PaleMoonTechnique` and skill class `PaleMoonCultivationSkill` stay unchanged internally.

### Mechanics
- **Base cultivation rate:** `1.0` per tick
- **Moon bonus:** Night + can see sky + looking at moon → `1.5×` multiplier (rate becomes `1.5`)
- **No bonus:** Night + can see sky + not looking at moon → `1.0×` (base, no bonus)
- **Sun damage:** Day + can see sky → `5% of max health per second` as fire damage (`1% per tick`)

---

## T2 — Gibbous Moon Scripture (new)

### Registration
- Technique registry key: `gibbous_moon_scripture`
- Skill registry key: `gibbous_moon_cultivation_skill`
- New classes: `GibbousMoonTechnique`, `GibbousMoonCultivationSkill`

### Mechanics
- **Base cultivation rate:** `1.5` per tick (50% more than T1)
- **Moon bonus:** Night + can see sky + looking at moon → `2.0×` multiplier (rate becomes `3.0`)
- **No bonus:** Night + can see sky + not looking at moon → `1.5×` (base rate, no extra)
- **Sun damage:** Same as T1 — `5% max health/s` fire damage

### Compatibility
- Incompatible with: `PaleMoonTechnique`, `ScholarlySoulTechnique`, itself
- Compatible with all other techniques

---

## Moon-Looking Detection (shared helper)

A static helper method defined in `PaleMoonCultivationSkill`, reused by `GibbousMoonCultivationSkill`.

**Conditions (all must be true):**
1. `level.isNight()`
2. `level.canSeeSky(entity.blockPosition())`
3. Entity look direction dot moon direction > `0.98` (~11° cone)

**Moon direction calculation:**
```
sunAngle = level.getSunAngle(1.0f)
moonAngle = sunAngle + PI        // moon is opposite the sun
moonDir = Vec3(0, sin(moonAngle), -cos(moonAngle))
lookDir = entity.getLookAngle().normalize()
isLooking = moonDir.dot(lookDir) > 0.98
```

The celestial arc rotates around the X axis in Minecraft, so the moon direction is a vector in the Y-Z plane.

---

## Sun Damage Logic

**Conditions (all must be true):**
1. `!level.isNight()`
2. `level.canSeeSky(entity.blockPosition())`

**Damage:** Applied every tick inside `continueCasting` on the server side.
- Amount: `entity.getMaxHealth() * 0.01f` (1% per tick = 5% per second at 20 ticks/s)
- Type: `DamageSource` fire (`level.damageSources().inFire()` or equivalent)

---

## Files Changed

| File | Change |
|---|---|
| `PaleMoonTechnique.java` | Update display name, `isCompatibleWith` to also block `GibbousMoonTechnique` |
| `PaleMoonCultivationSkill.java` | Replace night-sky check with moon-looking + add sun damage + expose static helper |
| `ModTechniques.java` | Rename registry key, add `GIBBOUS_MOON_SCRIPTURE` entry |
| `ModSkills.java` | Add `GIBBOUS_MOON_CULTIVATION_SKILL` entry |
| `GibbousMoonTechnique.java` | New file |
| `GibbousMoonCultivationSkill.java` | New file |
| `lang.java` | Update T1 keys, add T2 keys |

---

## Effective Rate Summary

| Condition | T1 rate | T2 rate |
|---|---|---|
| Day, open sky | `1.0` + fire damage | `1.5` + fire damage |
| Night, open sky, not looking at moon | `1.0` | `1.5` |
| Night, open sky, looking at moon | `1.5` | `3.0` |
| Underground / indoors | `1.0` | `1.5` |
