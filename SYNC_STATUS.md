# Sync Status

This file tracks synchronization between Better DPS Calculator and the OSRS Wiki DPS Calculator reference implementation.

## Current Sync

| Field | Value |
|-------|-------|
| Reference repo | `https://github.com/weirdgloop/osrs-dps-calc` |
| Synced commit | `1bebf1330bc3a81394819e8ae6ba8a4d4ae80328` |
| Commit date | 2026-07-07 |
| Commit message | fix uv in dockerfile |
| Last verified | 2026-07-07 |

## Included Verification

- Stable Java unit tests for core DPS calculations and plugin state conversion
- Parent-workspace conformance replay before review-ready copies are published here
- Java 11 CI via GitHub Actions

## Sync Boundary

The parent `osrs-dps-calc` workspace remains the source of truth for reference fixture generation and high-volume parity work. This standalone repository intentionally contains only the review-ready RuneLite plugin payload and passing local tests.

When the reference calculator changes, update and validate the parent sync workspace first, copy review-ready plugin source changes into this repository, then run:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon clean build
```

## Known Parity Gaps

- Full special attack calculations
- Bolt enchantment procs
- Tome of fire/water bonuses
- Some raid scaling and invocation-specific mechanics
- Defence reduction tracking
- NPC-vs-player calculations
