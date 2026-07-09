# Better DPS Calculator

A RuneLite plugin that calculates theoretical DPS for Old School RuneScape from your current equipment, stats, prayers, and target monster.

This standalone repository is the Plugin Hub review payload for the RuneLite plugin. The parent `osrs-dps-calc` workspace remains the sync workspace for comparing against the OSRS Wiki DPS Calculator.

## Features

- Real-time DPS calculation for melee, ranged, and magic combat styles
- Overlay and side-panel views for current target DPS
- Gear snapshot comparison tools
- Prayer, boost, slayer, salve, void, and common special weapon mechanics
- Bundled monster data with runtime refresh from the OSRS DPS calculator data source

## Plugin Hub Readiness

- Java 11 target
- Root `runelite-plugin.properties` with `build=standard`
- Root `LICENSE`
- Root `icon.png` for Plugin Hub listing
- Runtime resources loaded from the plugin jar via resource streams
- No runtime reflection lookup for combat styles
- No custom third-party runtime dependency in `build.gradle`

## Building

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon clean build
```

Run tests:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon test
```

Run the development client:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon runClient
```

## Sync Status

Parity fixtures are generated and replayed in the parent OSRS DPS calculator workspace before review-ready plugin updates are copied here.

- Reference repository: `https://github.com/weirdgloop/osrs-dps-calc`
- Synced reference commit: `1bebf1330bc3a81394819e8ae6ba8a4d4ae80328`
- Last verified: `2026-07-07`

See [SYNC_STATUS.md](SYNC_STATUS.md) for current parity notes and the parent sync workflow.

## Project Structure

```text
src/main/java/com/dpscalc/       RuneLite plugin, UI, state, data, and DPS calculation code
src/main/resources/com/dpscalc/  Bundled icon and monster data
src/test/java/com/dpscalc/       Stable unit and plugin state tests
```

## License

GPL-3.0, matching the source calculator project this plugin is synced against.
