# Sync Status

This file tracks synchronization between Better DPS Calculator and the OSRS Wiki DPS Calculator reference implementation.

## Current Sync

| Field | Value |
|-------|-------|
| Reference repo | `https://github.com/weirdgloop/osrs-dps-calc` |
| Synced commit | `1bebf1330bc3a81394819e8ae6ba8a4d4ae80328` |
| Commit date | 2026-07-07 |
| Commit message | fix uv in dockerfile |
| Last verified | 2026-07-09 |

## Included Verification

- Stable Java unit tests for core DPS calculations and plugin state conversion
- Standalone replay against 145 parent-generated reference fixtures, including enchanted bolts and vampyre damage transforms
- Parent-workspace conformance replay, full Java tests, and seeded fuzz sweep before review-ready copies are published here
- Java 11 CI via GitHub Actions

## Latest Verification

Validated with Java 11 on 2026-07-09:

```bash
# Parent sync workspace
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew test
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew test --tests com.dpscalc.FixtureReplayTest
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=0
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=1000
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=2000
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=3000
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=4000
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=5000
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=6000
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=7000
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=8000
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew fixtureFuzzTest -PfixtureFuzzSeed=1592598566 -PfixtureFuzzCount=1000 -PfixtureFuzzIndex=9000

# Standalone Plugin Hub payload
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon clean build
```

## Sync Boundary

The parent `osrs-dps-calc` workspace remains the source of truth for reference fixture generation and high-volume parity work. This standalone repository intentionally contains only the review-ready RuneLite plugin payload and passing local tests.

When the reference calculator changes, update and validate the parent sync workspace first, copy review-ready plugin source changes into this repository, then run:

```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew --no-daemon clean build
```

## Known Parity Gaps

- NPC-vs-player calculations
