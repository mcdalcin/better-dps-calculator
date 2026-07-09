package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.data.MonsterScaling;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.PlayerState;
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
            PlayerState player = FixtureStateFactory.buildPlayer(inputs.getAsJsonObject("player"));
            MonsterStats monster = MonsterScaling.scale(FixtureStateFactory.buildMonster(inputs.getAsJsonObject("monster")));
            DpsCalculator calc = new DpsCalculator(player, monster, isUsingSpecialAttack(inputs));

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

    private static boolean isUsingSpecialAttack(JsonObject inputs) {
        JsonObject options = inputs.getAsJsonObject("options");
        return options != null && options.has("usingSpecialAttack") && options.get("usingSpecialAttack").getAsBoolean();
    }
}
