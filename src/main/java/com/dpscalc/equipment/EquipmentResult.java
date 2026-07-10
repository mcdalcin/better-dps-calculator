package com.dpscalc.equipment;

import java.util.Objects;

public final class EquipmentResult {
    private final EquipmentLoadout canonicalLoadout;
    private final EquipmentStatTotals stats;
    private final int attackSpeed;

    EquipmentResult(EquipmentLoadout canonicalLoadout, EquipmentStatTotals stats, int attackSpeed) {
        this.canonicalLoadout = Objects.requireNonNull(canonicalLoadout, "canonicalLoadout");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.attackSpeed = attackSpeed;
    }

    public EquipmentLoadout getCanonicalLoadout() { return canonicalLoadout; }
    public EquipmentStatTotals getStats() { return stats; }
    public int getAttackSpeed() { return attackSpeed; }
}
