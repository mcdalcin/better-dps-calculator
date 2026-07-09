package com.dpscalc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DpsComparison {
    private final String targetName;
    private final String weaponName;
    private final double dps;
    private final int maxHit;
    private final double accuracy;
    private final int attackSpeed;
    private final long timestamp;
}
