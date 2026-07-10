package com.dpscalc.calc;

import com.dpscalc.calc.distribution.AttackDistribution;

public class DpsResult {
    private double dps;
    private int maxHit;
    private double accuracy;
    private int attackRoll;
    private int defenceRoll;
    private int attackSpeed;
    private AttackDistribution attackDistribution;
    
    // Monster info for TTK calculations
    private int monsterHp;

    public double getDps() { return dps; }
    public void setDps(double dps) { this.dps = dps; }

    public int getMaxHit() { return maxHit; }
    public void setMaxHit(int maxHit) { this.maxHit = maxHit; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public int getAttackRoll() { return attackRoll; }
    public void setAttackRoll(int attackRoll) { this.attackRoll = attackRoll; }

    public int getDefenceRoll() { return defenceRoll; }
    public void setDefenceRoll(int defenceRoll) { this.defenceRoll = defenceRoll; }

    public int getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(int attackSpeed) { this.attackSpeed = attackSpeed; }

    public AttackDistribution getAttackDistribution() { return attackDistribution; }
    public void setAttackDistribution(AttackDistribution attackDistribution) {
        this.attackDistribution = attackDistribution;
    }

    public int getMonsterHp() { return monsterHp; }
    public void setMonsterHp(int monsterHp) { this.monsterHp = monsterHp; }

    // ==================== FORMATTED GETTERS ====================

    public String getFormattedDps() {
        return String.format("%.2f", dps);
    }

    public String getFormattedAccuracy() {
        return String.format("%.1f%%", accuracy * 100);
    }

    // ==================== ADVANCED CALCULATIONS ====================

    /**
     * Calculate average damage per successful hit.
     * This is (maxHit + 1) / 2 for uniform distribution from 0 to maxHit.
     */
    public double getAverageHit() {
        if (maxHit <= 0) return 0;
        return (maxHit + 1) / 2.0;
    }

    public String getFormattedAverageHit() {
        return String.format("%.1f", getAverageHit());
    }

    /**
     * Calculate average damage per attack (including misses).
     */
    public double getAverageDamagePerAttack() {
        return getAverageHit() * accuracy;
    }

    /**
     * Calculate damage per game tick.
     */
    public double getDamagePerTick() {
        if (attackSpeed <= 0) return 0;
        return getAverageDamagePerAttack() / attackSpeed;
    }

    public String getFormattedDamagePerTick() {
        return String.format("%.2f", getDamagePerTick());
    }

    /**
     * Calculate expected number of hits to kill the monster.
     * This accounts for accuracy (misses don't count as hits).
     */
    public double getExpectedHitsToKill() {
        double avgDmgPerAttack = getAverageDamagePerAttack();
        if (avgDmgPerAttack <= 0 || monsterHp <= 0) return 0;
        return monsterHp / avgDmgPerAttack;
    }

    public String getFormattedExpectedHits() {
        double hits = getExpectedHitsToKill();
        if (hits <= 0) return "N/A";
        return String.format("%.1f", hits);
    }

    /**
     * Calculate time to kill in seconds.
     */
    public double getTimeToKill() {
        if (dps <= 0 || monsterHp <= 0) return 0;
        return monsterHp / dps;
    }

    public String getFormattedTimeToKill() {
        double ttk = getTimeToKill();
        if (ttk <= 0) return "N/A";
        
        if (ttk < 60) {
            return String.format("%.1fs", ttk);
        } else {
            int minutes = (int) (ttk / 60);
            double seconds = ttk % 60;
            return String.format("%dm %.0fs", minutes, seconds);
        }
    }

    /**
     * Calculate time to kill in game ticks.
     */
    public double getTimeToKillTicks() {
        double hitsNeeded = getExpectedHitsToKill();
        if (hitsNeeded <= 0 || attackSpeed <= 0) return 0;
        return hitsNeeded * attackSpeed;
    }

    public String getFormattedTimeToKillTicks() {
        double ticks = getTimeToKillTicks();
        if (ticks <= 0) return "N/A";
        return String.format("%.0f ticks", ticks);
    }

    /**
     * Calculate estimated kills per hour.
     */
    public double getKillsPerHour() {
        double ttk = getTimeToKill();
        if (ttk <= 0) return 0;
        return 3600.0 / ttk;
    }

    public String getFormattedKillsPerHour() {
        double kph = getKillsPerHour();
        if (kph <= 0) return "N/A";
        if (kph >= 1000) {
            return String.format("%.1fk", kph / 1000);
        }
        return String.format("%.0f", kph);
    }

    @Override
    public String toString() {
        return String.format("DPS: %.2f | Max Hit: %d | Accuracy: %.1f%% | Speed: %d ticks",
            dps, maxHit, accuracy * 100, attackSpeed);
    }
}
