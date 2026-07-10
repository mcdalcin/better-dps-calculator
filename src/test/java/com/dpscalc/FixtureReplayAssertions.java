package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.data.MonsterScaling;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.EquipmentStats;
import com.google.gson.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

final class FixtureReplayAssertions {
    private static final double DPS_TOLERANCE = 0.001;
    private static final double ACCURACY_TOLERANCE = 0.0001;

    private FixtureReplayAssertions() {}

    static void assertFixture(JsonObject fixture, String context) {
        if (fixture == null) {
            fail(context + ": fixture data not loaded");
        }

        String fixtureId = fixture.get("id").getAsString();
        JsonObject inputs = fixture.getAsJsonObject("inputs");
        JsonObject outputs = fixture.getAsJsonObject("outputs");
        String prefix = context + " id=" + fixtureId;

        try {
            MonsterStats rawMonster = FixtureStateFactory.buildMonster(inputs.getAsJsonObject("monster"));
            PlayerState player = FixtureStateFactory.buildPlayer(inputs.getAsJsonObject("player"), rawMonster.getId());
            MonsterStats monster = MonsterScaling.scale(rawMonster);
            DpsCalculator calc = new DpsCalculator(player, monster, isUsingSpecialAttack(inputs));
            assertEquipment(outputs, player.getEquipmentStats(), calc.getAttackSpeed(), prefix);

            int actualMaxHit = calc.getMaxHit();
            int actualMaxAttackRoll = calc.getMaxAttackRoll();
            int actualDefenceRoll = calc.getNpcDefenceRoll();
            double actualAccuracy = calc.calculateHitChance(actualMaxAttackRoll, actualDefenceRoll);
            double actualDps = calc.calculate().getDps();

            if (outputs.has("maxHit")) {
                assertEquals(prefix + ": Max hit mismatch", outputs.get("maxHit").getAsInt(), actualMaxHit);
            }
            if (outputs.has("maxAttackRoll")) {
                assertEquals(prefix + ": Max attack roll mismatch", outputs.get("maxAttackRoll").getAsInt(), actualMaxAttackRoll);
            }
            if (outputs.has("npcDefRoll")) {
                assertEquals(prefix + ": NPC defence roll mismatch", outputs.get("npcDefRoll").getAsInt(), actualDefenceRoll);
            }
            if (outputs.has("accuracy")) {
                double expectedAccuracy = outputs.get("accuracy").getAsDouble();
                assertEquals(prefix + ": Accuracy mismatch", expectedAccuracy, actualAccuracy, ACCURACY_TOLERANCE);
            }
            if (outputs.has("dps")) {
                double expectedDps = outputs.get("dps").getAsDouble();
                assertEquals(prefix + ": DPS mismatch", expectedDps, actualDps, DPS_TOLERANCE);
            }
        } catch (Exception e) {
            fail(prefix + ": Exception during test: " + e.getMessage());
        }
    }

    private static void assertEquipment(JsonObject outputs, EquipmentStats stats, int attackSpeed, String prefix) {
        if (!outputs.has("equipment")) return;
        JsonObject equipment = outputs.getAsJsonObject("equipment");
        JsonObject bonuses = equipment.getAsJsonObject("bonuses");
        JsonObject offensive = equipment.getAsJsonObject("offensive");
        JsonObject defensive = equipment.getAsJsonObject("defensive");
        assertEquals(prefix + ": equipment str", bonuses.get("str").getAsInt(), stats.getMeleeStrength());
        assertEquals(prefix + ": equipment ranged str", bonuses.get("ranged_str").getAsInt(), stats.getRangedStrength());
        assertEquals(prefix + ": equipment magic str", bonuses.get("magic_str").getAsInt(), stats.getMagicDamage());
        assertEquals(prefix + ": equipment prayer", bonuses.get("prayer").getAsInt(), stats.getPrayerBonus());
        assertEquals(prefix + ": equipment stab", offensive.get("stab").getAsInt(), stats.getStabAttack());
        assertEquals(prefix + ": equipment slash", offensive.get("slash").getAsInt(), stats.getSlashAttack());
        assertEquals(prefix + ": equipment crush", offensive.get("crush").getAsInt(), stats.getCrushAttack());
        assertEquals(prefix + ": equipment magic", offensive.get("magic").getAsInt(), stats.getMagicAttack());
        assertEquals(prefix + ": equipment ranged", offensive.get("ranged").getAsInt(), stats.getRangedAttack());
        assertEquals(prefix + ": defence stab", defensive.get("stab").getAsInt(), stats.getStabDefence());
        assertEquals(prefix + ": defence slash", defensive.get("slash").getAsInt(), stats.getSlashDefence());
        assertEquals(prefix + ": defence crush", defensive.get("crush").getAsInt(), stats.getCrushDefence());
        assertEquals(prefix + ": defence magic", defensive.get("magic").getAsInt(), stats.getMagicDefence());
        assertEquals(prefix + ": defence ranged", defensive.get("ranged").getAsInt(), stats.getRangedDefence());
        assertEquals(prefix + ": attack speed", equipment.get("attackSpeed").getAsInt(), attackSpeed);
    }

    private static boolean isUsingSpecialAttack(JsonObject inputs) {
        JsonObject options = inputs.getAsJsonObject("options");
        return options != null && options.has("usingSpecialAttack") && options.get("usingSpecialAttack").getAsBoolean();
    }
}
