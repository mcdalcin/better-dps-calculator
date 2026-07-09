package com.dpscalc;

import com.dpscalc.state.CombatStyle;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class CombatStyleTest {
    @Test
    public void findsExactStyleWhenNameAndStanceMatch() {
        CombatStyle style = CombatStyle.findByNameAndStance("Slash", "Aggressive");

        assertSame(CombatStyle.MELEE_AGGRESSIVE_SLASH, style);
    }

    @Test
    public void fallsBackToFirstNameMatchWhenStanceIsUnknown() {
        CombatStyle style = CombatStyle.findByNameAndStance("Slash", "Unknown");

        assertSame(CombatStyle.MELEE_ACCURATE_SLASH, style);
    }

    @Test
    public void fallsBackToUnarmedWhenNameOrStanceIsMissing() {
        assertSame(CombatStyle.UNARMED_PUNCH, CombatStyle.findByNameAndStance(null, "Accurate"));
        assertSame(CombatStyle.UNARMED_PUNCH, CombatStyle.findByNameAndStance("Slash", null));
    }
}
