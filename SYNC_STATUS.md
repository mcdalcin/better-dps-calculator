# Sync Status

This file tracks the standalone Better DPS Calculator Plugin Hub payload against the OSRS Wiki DPS Calculator reference implementation.

For upstream parity work, start in `mcdalcin/better-dps-calculator-sync`. This repository should receive only review-ready plugin changes after the sync workspace has generated fixtures and passed replay/fuzz validation.

## Current Sync

| Field | Value |
|-------|-------|
| Reference repo | `https://github.com/weirdgloop/osrs-dps-calc` |
| Sync workspace repo | `https://github.com/mcdalcin/better-dps-calculator-sync` |
| Plugin Hub payload repo | `https://github.com/mcdalcin/better-dps-calculator` |
| Synced upstream commit | `b6bc098dc0d742b2b763375d2e78e1b611a22070` |
| Commit date | 2026-07-09 |
| Commit message | Fix leagues spellement bug (#915) |
| Last verified | 2026-07-10 |

## Verification Evidence

Parent sync workspace validation completed before this standalone handoff:

- The upstream reference was pinned exactly to `b6bc098dc0d742b2b763375d2e78e1b611a22070`.
- 146 deterministic TypeScript-reference fixtures replayed through the Java plugin copy.
- Focused tests verified equipment-domain artifacts and catalog loading, loadout stat aggregation, ammo applicability, attack-speed resolution, supported item variables, and Dizana's quiver behavior.
- Focused tests verified attack/hit-distribution construction and fixture compatibility for the included deterministic cases.
- The approved production, resource, and test payload trees were copied byte-for-byte from `/home/matth/git/better-dps-calculator-sync/runelite-plugin`.

Standalone Plugin Hub payload validation:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon --max-workers=1 clean build
```

The command above passed on 2026-07-10. This standalone build runs the included deterministic suite; it does not complete the opt-in high-volume seeded fuzz campaign.

## Required Flow For Upstream Changes

When `weirdgloop/osrs-dps-calc` changes, use this process.

1. Work in the sync workspace first.

   ```bash
   cd /home/matth/git/better-dps-calculator-sync
   ```

2. Follow the sync workspace update procedure.

   The authoritative docs are:

   - `/home/matth/git/better-dps-calculator-sync/README.md`
   - `/home/matth/git/better-dps-calculator-sync/runelite-plugin/README.md`
   - `/home/matth/git/better-dps-calculator-sync/runelite-plugin/SYNC_STATUS.md`
   - `/home/matth/git/better-dps-calculator-sync/sync-tests/README.md`

   The sync workspace must update the upstream reference, review upstream changes, update scenarios/generator as needed, generate fixtures, replay fixtures, fix Java parity drift, run full tests, run seeded fuzz replay, and update its own `SYNC_STATUS.md`.

3. Copy review-ready plugin changes into this standalone repo.

   Copy only publishable plugin payload files from `runelite-plugin/`. Do not copy the sync repo's `reference/`, `sync-tests/`, root README, or sync-only workflows.

   Decide and document the copy boundary before copying. Common payload paths are:

   - `src/main/java/com/dpscalc/`
   - `src/main/resources/com/dpscalc/`
   - `src/test/java/com/dpscalc/`
   - `src/test/resources/` for stable fixture resources
   - `build.gradle` only when the standalone build/test surface changes

4. Validate this standalone repo.

   ```bash
   JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon --max-workers=1 clean build
   ```

5. Update this file.

   Required fields to update:

   - synced upstream commit hash
   - commit date and message
   - last verified date
   - parent sync workspace validation evidence
   - standalone validation command actually run
   - known parity gaps that changed
   - any copy/handoff caveats from the sync workspace

6. Update `README.md` if repo roles, commands, copy boundaries, or Plugin Hub readiness notes changed.

7. Commit and push to this repo's `origin`.

   Do not push standalone Plugin Hub changes to `weirdgloop/osrs-dps-calc`. Do not treat the standalone repo as the place to regenerate upstream fixtures.

## Included Verification In This Payload

- Stable Java unit tests for core DPS calculations and plugin state conversion
- Standalone replay against 146 parent-generated reference fixtures pinned to `b6bc098d`
- Equipment-domain coverage for catalog artifacts, loadout stat totals, ammo applicability, attack speed, supported item variables, and Dizana's quiver
- Distribution coverage for attack/hit-distribution primitives and the included deterministic multi-hit/expected-damage cases
- Existing calculator coverage for enchanted bolts, vampyre damage transforms, raid scaling helpers, selected spell/element weakness cases, and common special weapon mechanics
- Java 11 CI via GitHub Actions

## Known Parity Gaps And Manual Boundaries

- Full parity is not established at this milestone; expanded seeded fuzz replay for the `b6bc098d` equipment/distribution payload remains unfinished, so additional scalar-formula, input-normalization, and distribution mismatches may still be found.
- Full special attack coverage is still incomplete; common specials are covered, but not every upstream special is guaranteed implemented.
- Live spell/autocast detection is not fully wired.
- Live equipment version/category detection is incomplete for some charged or variant-dependent items.
- Live defence reduction tracking is not automatic.
- Exact remote CoX party combat/mining/HP stats are not proven auto-readable from RuneLite APIs.
- Exact NPC HP is not universally exposed by RuneLite; live current HP is best-effort where health ratio/scale or HUD values are available.
- NPC-vs-player calculations are not implemented.
