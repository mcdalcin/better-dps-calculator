#!/bin/bash
cd "$(dirname "$0")"
if [ -n "$1" ]; then
    ./gradlew dpsTest -Pscenario="$1" --console=plain -q
else
    ./gradlew dpsTest --console=plain -q
fi
