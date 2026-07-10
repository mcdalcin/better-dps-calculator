# Better DPS Calculator

A RuneLite plugin that calculates theoretical DPS for Old School RuneScape from your current equipment, stats, prayers, and target monster.

This repository is the standalone RuneLite Plugin Hub payload. It should contain only review-ready plugin source, resources, tests, metadata, and CI needed to build the plugin.

Parity work against the OSRS Wiki DPS Calculator happens first in the sync workspace: `mcdalcin/better-dps-calculator-sync`.

## Repository Roles

- `weirdgloop/osrs-dps-calc`: upstream OSRS Wiki DPS Calculator reference. This is the TypeScript calculator we match.
- `mcdalcin/better-dps-calculator-sync`: sync/parity workspace. Use it to update the upstream reference, generate fixtures, run fixture replay, run seeded fuzz replay, and prove Java parity.
- `mcdalcin/better-dps-calculator`: this repository. Use it as the Plugin Hub payload after the sync workspace is green.

Do not implement new upstream parity behavior directly here first. Start in the sync repo, prove the behavior with fixtures/tests/fuzzing, then copy the review-ready plugin changes into this repo.

## Features

- Real-time DPS calculation for melee, ranged, and magic combat styles
- Overlay and side-panel views for current target DPS
- Gear snapshot comparison tools
- Prayer, boost, slayer, salve, void, and common special weapon mechanics
- Reference-backed equipment resolution for loadouts, stat totals, ammo applicability, attack speed, and supported item variables
- Hit-distribution primitives used by verified multi-hit and expected-damage calculations
- Fixture-backed parity with the OSRS Wiki DPS Calculator for the currently synced reference commit
- Bundled monster data with runtime refresh from the OSRS DPS calculator data source

## Plugin Hub Readiness

- Java 11 target
- Root `runelite-plugin.properties` with `build=standard`
- Root `LICENSE`
- Root `icon.png` for Plugin Hub listing
- Runtime resources loaded from the plugin jar via resource streams
- No runtime reflection lookup for combat styles
- Gson is the only custom third-party runtime dependency; `implementation` scope is required because the production equipment-domain catalog parses bundled JSON at runtime

## Build, Test, And Run

Use Java 11.

Build and test exactly as CI does:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon --max-workers=1 clean build
```

Run tests without cleaning:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon test
```

Run the development client:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon runClient
```

## Upstream Calculator Update Flow

When new code lands in the Wiki calculator, do not start in this repo. Use this flow:

1. Go to the sync workspace.

   ```bash
   cd /home/matth/git/better-dps-calculator-sync
   ```

2. Update the upstream reference and generate fixtures there.

   The sync workspace docs describe the authoritative workflow:

   - `/home/matth/git/better-dps-calculator-sync/README.md`
   - `/home/matth/git/better-dps-calculator-sync/runelite-plugin/README.md`
   - `/home/matth/git/better-dps-calculator-sync/runelite-plugin/SYNC_STATUS.md`
   - `/home/matth/git/better-dps-calculator-sync/sync-tests/README.md`

3. In the sync workspace, update scenarios/generator/Java parity code until all required checks pass.

   Required sync-side evidence normally includes:

   - deterministic fixture generation from `reference/osrs-dps-calc`
   - fixture replay through `FixtureReplayTest`
   - full Java 11 Gradle build in `runelite-plugin/`
   - seeded fuzz replay for broad calculator coverage
   - updated sync workspace `SYNC_STATUS.md`

4. Copy only review-ready plugin payload changes into this repo.

   Copy from `/home/matth/git/better-dps-calculator-sync/runelite-plugin` into this repository. Do not copy the sync repo's `reference/`, `sync-tests/`, root README, or sync-only GitHub workflow files.

   Before copying, identify the intended file set. Typical copied areas are:

   - `src/main/java/com/dpscalc/`
   - `src/main/resources/com/dpscalc/`
   - `src/test/java/com/dpscalc/`
   - `src/test/resources/` when stable replay fixtures should be included here
   - `build.gradle` only when standalone build/test needs the same dependency or test task change

5. Validate this standalone repo.

   ```bash
   JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon --max-workers=1 clean build
   ```

6. Update this repo's docs.

   Update `SYNC_STATUS.md` with the synced upstream commit, sync workspace evidence, standalone validation command, last verified date, and known gaps. Update this README if the repo roles, build commands, or handoff rules change.

7. Commit and push this repo to `origin`.

   Keep standalone commits focused on Plugin Hub payload changes. The sync repo should already contain the fixture-generation evidence and broader parity history.

## Current Sync Status

- Reference repository: `https://github.com/weirdgloop/osrs-dps-calc`
- Sync workspace: `https://github.com/mcdalcin/better-dps-calculator-sync`
- Synced reference commit: `b6bc098dc0d742b2b763375d2e78e1b611a22070`
- Deterministic reference fixtures: 146
- Last verified: 2026-07-10

This milestone verifies the included deterministic equipment and distribution coverage; it does not claim complete calculator parity. Expanded seeded fuzz replay and the remaining live/unimplemented areas listed in [SYNC_STATUS.md](SYNC_STATUS.md) are still open.

See [SYNC_STATUS.md](SYNC_STATUS.md) for current parity notes, validation evidence, update requirements, and known gaps.

## Project Structure

```text
src/main/java/com/dpscalc/       RuneLite plugin, UI, state, data, and DPS calculation code
src/main/resources/com/dpscalc/  Bundled icon and monster data
src/test/java/com/dpscalc/       Stable unit, fixture replay, and plugin state tests
src/test/resources/              Stable fixture resources when included in the standalone payload
```

## CI

GitHub Actions runs the Java 11 clean build on pushes and pull requests to `main`:

```bash
./gradlew --no-daemon clean build
```

## License

GPL-3.0, matching the source calculator project this plugin is synced against.
