# Sync Status

This file tracks the standalone Better DPS Calculator Plugin Hub payload against the OSRS Wiki DPS Calculator reference implementation.

For upstream parity work, start in `mcdalcin/better-dps-calculator-sync`. This repository should receive only review-ready plugin changes after the sync workspace has generated fixtures and passed replay/fuzz validation.

## Current Sync

| Field | Value |
|-------|-------|
| Reference repo | `https://github.com/weirdgloop/osrs-dps-calc` |
| Sync workspace repo | `https://github.com/mcdalcin/better-dps-calculator-sync` |
| Plugin Hub payload repo | `https://github.com/mcdalcin/better-dps-calculator` |
| Synced upstream commit | `1bebf1330bc3a81394819e8ae6ba8a4d4ae80328` |
| Commit date | 2026-07-07 |
| Commit message | fix uv in dockerfile |
| Last verified | 2026-07-09 |

## Verification Evidence

Parent sync workspace validation completed before these standalone changes were published:

- 145 deterministic TypeScript-reference fixtures replayed through the Java plugin copy.
- 10,000 seeded fuzz fixtures replayed in ten 1000-case batches for seed `1592598566`.
- Full Java 11 Gradle build passed in `/home/matth/git/osrs-dps-calc/runelite-plugin`.
- Live ToA/CoX context, raid scaling, and monster stat copy support were validated with focused tests in the sync workspace.

Standalone Plugin Hub payload validation:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon clean build
```

## Required Flow For Upstream Changes

When `weirdgloop/osrs-dps-calc` changes, use this process.

1. Work in the sync workspace first.

   ```bash
   cd /home/matth/git/osrs-dps-calc
   ```

2. Follow the sync workspace update procedure.

   The authoritative docs are:

   - `/home/matth/git/osrs-dps-calc/README.md`
   - `/home/matth/git/osrs-dps-calc/runelite-plugin/README.md`
   - `/home/matth/git/osrs-dps-calc/runelite-plugin/SYNC_STATUS.md`
   - `/home/matth/git/osrs-dps-calc/sync-tests/README.md`

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
   JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon clean build
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
- Standalone replay against 145 parent-generated reference fixtures
- Calculator coverage for enchanted bolts, vampyre damage transforms, raid scaling helpers, selected spell/element weakness cases, and common special weapon mechanics
- Java 11 CI via GitHub Actions

## Known Parity Gaps And Manual Boundaries

- Full special attack coverage is still incomplete; common specials are covered, but not every upstream special is guaranteed implemented.
- Live spell/autocast detection is not fully wired.
- Live equipment version/category detection is incomplete for some charged or variant-dependent items.
- Live defence reduction tracking is not automatic.
- Exact remote CoX party combat/mining/HP stats are not proven auto-readable from RuneLite APIs.
- Exact NPC HP is not universally exposed by RuneLite; live current HP is best-effort where health ratio/scale or HUD values are available.
- NPC-vs-player calculations are not implemented.
