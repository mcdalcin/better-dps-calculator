package com.dpscalc.calc;

import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterConstants;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.AttackType;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.Prayer;

import java.util.Set;

public class DpsCalculator extends BaseCalc {

    private static final double SECONDS_PER_TICK = 0.6;

    private boolean usingSpecialAttack;

    public DpsCalculator(PlayerState player, MonsterStats monster) {
        super(player, monster);
    }

    public DpsCalculator(PlayerState player, MonsterStats monster, boolean usingSpecialAttack) {
        super(player, monster);
        this.usingSpecialAttack = usingSpecialAttack;
    }

    public DpsResult calculate() {
        DpsResult result = new DpsResult();
        
        if (monster == null) {
            return result;
        }

        if (isImmune()) {
            result.setMaxHit(0);
            result.setAccuracy(0);
            result.setDps(0);
            return result;
        }

        int attackRoll = getMaxAttackRoll();
        int defenceRoll = getNpcDefenceRoll();
        double hitChance = calculateHitChance(attackRoll, defenceRoll);
        int maxHit = getMaxHit();
        int attackSpeed = getAttackSpeed();
        
        double avgDamage;
        
        if (isSunspearSpec()) {
            avgDamage = hitChance * Math.floor(maxHit * 7 / 10.0);
        } else if (isWearingScythe() && monster.getSize() >= 2) {
            avgDamage = calculateScytheDps(hitChance, maxHit);
        } else if (isWearingVeracs()) {
            avgDamage = calculateVeracsDps(hitChance, maxHit, defenceRoll);
        } else if (isWearingFang()) {
            int minHit = (int) Math.floor(maxHit * 0.15);
            avgDamage = hitChance * ((maxHit + minHit) / 2.0);
        } else {
            avgDamage = hitChance * getAverageHit(maxHit);
        }
        
        double attacksPerSecond = 1.0 / (attackSpeed * SECONDS_PER_TICK);
        double dps = avgDamage * attacksPerSecond;

        result.setMaxHit(maxHit);
        result.setAccuracy(hitChance);
        result.setAttackRoll(attackRoll);
        result.setDefenceRoll(defenceRoll);
        result.setAttackSpeed(attackSpeed);
        result.setDps(dps);
        
        return result;
    }
    
    /**
     * Get the average damage per successful hit, accounting for the "min hit 1" mechanic.
     * In OSRS, when you pass the accuracy check, if you roll 0 damage it becomes 1.
     * This changes the average from maxHit/2 to maxHit/2 + 1/(maxHit+1).
     */
    private double getAverageHit(int maxHit) {
        if (maxHit <= 0) return 0;
        return maxHit / 2.0 + 1.0 / (maxHit + 1);
    }
    
    private double calculateScytheDps(double hitChance, int maxHit) {
        int size = monster.getSize();
        double totalDamage = 0;
        
        totalDamage += hitChance * getAverageHit(maxHit);
        
        if (size >= 2) {
            int hit2Max = maxHit / 2;
            totalDamage += hitChance * getAverageHit(hit2Max);
        }
        
        if (size >= 3) {
            int hit3Max = maxHit / 4;
            totalDamage += hitChance * getAverageHit(hit3Max);
        }
        
        return totalDamage;
    }
    
    private double calculateVeracsDps(double hitChance, int maxHit, int defenceRoll) {
        double normalDamage = hitChance * getAverageHit(maxHit);
        // Verac's special effect: 25% chance to ignore defense and hit 1 to maxHit+1
        double veracDamage = (maxHit + 2) / 2.0;
        return 0.75 * normalDamage + 0.25 * veracDamage;
    }

    public int getMaxAttackRoll() {
        CombatStyle style = player.getCombatStyle();
        AttackType attackType = style.getAttackType();

        if (attackType.isMelee()) {
            return getMeleeAttackRoll();
        } else if (attackType.isRanged()) {
            return getRangedAttackRoll();
        } else if (attackType.isMagic()) {
            return getMagicAttackRoll();
        }
        return 0;
    }

    private int getMeleeAttackRoll() {
        CombatStyle style = player.getCombatStyle();
        
        int effectiveLevel = player.getBoostedAttack();
        effectiveLevel = applyPrayerBonus(effectiveLevel, true);
        
        int stanceBonus = 8;
        if ("Accurate".equals(style.getStance())) {
            stanceBonus += 3;
        } else if ("Controlled".equals(style.getStance())) {
            stanceBonus += 1;
        }
        effectiveLevel += stanceBonus;

        if (isWearingMeleeVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 11, 10);
        }

        int gearBonus = getAttackBonusForStyle() + 64;
        int baseRoll = effectiveLevel * gearBonus;
        int attackRoll = baseRoll;

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            int factor = player.isForinthrySurgeActive() ? 27 : 24;
            attackRoll = applyFactor(attackRoll, factor, 20);
        } else if (player.isWearingAny("Salve amulet (e)", "Salve amulet(ei)") 
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            attackRoll = applyFactor(attackRoll, 6, 5);
        } else if (player.isWearingAny("Salve amulet", "Salve amulet(i)") 
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            attackRoll = applyFactor(attackRoll, 7, 6);
        } else if (isWearingBlackMask() && player.isOnSlayerTask()) {
            attackRoll = applyFactor(attackRoll, 7, 6);
        }

        if (isWearingTzhaarWeapon() && isWearingObsidian()) {
            int obsidianBonus = applyFactor(baseRoll, 1, 10);
            attackRoll += obsidianBonus;
        }

        if (isRevWeaponApplicable()) {
            attackRoll = applyFactor(attackRoll, 3, 2);
        }

        if (player.isWearingAny("Arclight", "Emberlight") && hasAttribute(MonsterAttribute.DEMON)) {
            attackRoll = addFactor(attackRoll, demonbaneFactor(70), 100);
        }
        
        if (player.isWearingAny("Bone claws", "Burning claws") && hasAttribute(MonsterAttribute.DEMON)) {
            attackRoll = addFactor(attackRoll, demonbaneFactor(5), 100);
        }

        if (player.isWearing("Dragon hunter lance") && hasAttribute(MonsterAttribute.DRAGON)) {
            attackRoll = applyFactor(attackRoll, 6, 5);
        } else if (player.isWearing("Dragon hunter wand") && hasAttribute(MonsterAttribute.DRAGON)) {
            attackRoll = applyFactor(attackRoll, 7, 4);
        }

        if (player.isWearing("Keris partisan of breaching") && hasAttribute(MonsterAttribute.KALPHITE)) {
            attackRoll = applyFactor(attackRoll, 133, 100);
        }
        
        if (player.isWearing("Keris partisan of the sun") 
            && MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId())
            && monster.getInputs().getMonsterCurrentHp() < monster.getHitpoints() / 4) {
            attackRoll = applyFactor(attackRoll, 5, 4);
        }

        if (player.isWearingAny("Blisterwood flail", "Blisterwood sickle") && isVampyre()) {
            attackRoll = applyFactor(attackRoll, 21, 20);
        }
        
        if (isWearingSilverWeapon() && player.isWearing("Efaritay's aid") && isVampyre()) {
            attackRoll = applyFactor(attackRoll, 23, 20);
        }
        
        if (player.isWearing("Granite hammer") && hasAttribute(MonsterAttribute.GOLEM)) {
            attackRoll = applyFactor(attackRoll, 13, 10);
        }

        if (style.getAttackType() == AttackType.CRUSH) {
            int inqPieces = countInquisitorPieces();
            if (inqPieces > 0) {
                if (player.isWearing("Inquisitor's mace")) {
                    inqPieces *= 5;
                } else if (inqPieces == 3) {
                    inqPieces = 5;
                }
                attackRoll = applyFactor(attackRoll, 200 + inqPieces, 200);
            }
        }

        if (usingSpecialAttack) {
            attackRoll = applyMeleeSpecAttackRollBonus(attackRoll);
        }

        return attackRoll;
    }
    
    private int applyMeleeSpecAttackRollBonus(int attackRoll) {
        if (isWearingGodsword()) {
            return applyFactor(attackRoll, 2, 1);
        } else if (isWearingFang() || player.isWearingAny("Arkan blade", "Granite hammer")) {
            return applyFactor(attackRoll, 3, 2);
        } else if (player.isWearingAny("Elder maul", "Dragon mace", "Dragon sword", "Dragon scimitar", "Abyssal whip")) {
            return applyFactor(attackRoll, 5, 4);
        } else if (player.isWearing("Dragon dagger")) {
            return applyFactor(attackRoll, 23, 20);
        } else if (player.isWearing("Abyssal dagger")) {
            return applyFactor(attackRoll, 5, 4);
        } else if (player.isWearing("Soulreaper axe")) {
            int stacks = Math.max(0, Math.min(5, player.getSoulreaperStacks()));
            return applyFactor(attackRoll, 100 + 6 * stacks, 100);
        } else if (player.isWearing("Brine sabre")) {
            return applyFactor(attackRoll, 2, 1);
        } else if (player.isWearing("Barrelchest anchor")) {
            return applyFactor(attackRoll, 2, 1);
        } else if (isSunspearFinisher()) {
            return applyFactor(attackRoll, 7, 10);
        }
        return attackRoll;
    }

    private int getRangedAttackRoll() {
        CombatStyle style = player.getCombatStyle();
        
        int effectiveLevel = player.getBoostedRanged();
        effectiveLevel = applyPrayerBonus(effectiveLevel, true);
        
        if ("Accurate".equals(style.getStance())) {
            effectiveLevel += 3;
        }
        effectiveLevel += 8;

        if (isWearingRangedVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 11, 10);
        }

        EquipmentStats stats = player.getEquipmentStats();
        int attackRoll = effectiveLevel * (stats.getRangedAttack() + 64);

        if (isWearingCrystalBow()) {
            int crystalPieces = countCrystalPieces();
            attackRoll = applyFactor(attackRoll, 20 + crystalPieces, 20);
        }

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            int factor = player.isForinthrySurgeActive() ? 27 : 24;
            attackRoll = applyFactor(attackRoll, factor, 20);
        } else if (player.isWearing("Salve amulet(ei)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            attackRoll = applyFactor(attackRoll, 6, 5);
        } else if (player.isWearing("Salve amulet(i)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            attackRoll = applyFactor(attackRoll, 7, 6);
        } else if (isWearingImbuedBlackMask() && player.isOnSlayerTask()) {
            attackRoll = applyFactor(attackRoll, 23, 20);
        }

        if (player.isWearing("Twisted bow")) {
            attackRoll = applyTwistedBowScaling(attackRoll, true);
            if (MonsterConstants.isP2Warden(monster.getId())) {
                attackRoll = applyTwistedBowScaling(attackRoll, true);
            }
        }

        if (player.isWearing("Dragon hunter crossbow") && hasAttribute(MonsterAttribute.DRAGON)) {
            attackRoll = applyFactor(attackRoll, 13, 10);
        }

        if (isRevWeaponApplicable()) {
            attackRoll = applyFactor(attackRoll, 3, 2);
        }
        
        if (player.isWearing("Scorching bow") && hasAttribute(MonsterAttribute.DEMON)) {
            attackRoll = addFactor(attackRoll, demonbaneFactor(30), 100);
        }

        if (usingSpecialAttack) {
            attackRoll = applyRangedSpecAttackRollBonus(attackRoll);
        }
        
        if (MonsterConstants.isTitanBoss(monster.getId()) && "Out of Melee Range".equals(monster.getInputs().getPhase())) {
            attackRoll = applyFactor(attackRoll, 6, 1);
        }

        return attackRoll;
    }
    
    private int applyRangedSpecAttackRollBonus(int attackRoll) {
        if (player.isWearingAny("Zaryte crossbow", "Webweaver bow") || isWearingBlowpipe()) {
            return applyFactor(attackRoll, 2, 1);
        } else if (isWearingMsb()) {
            return applyFactor(attackRoll, 10, 7);
        } else if (player.isWearingAny("Heavy ballista", "Light ballista")) {
            return applyFactor(attackRoll, 5, 4);
        } else if (player.isWearing("Rosewood blowpipe")) {
            return applyFactor(attackRoll, 4, 5);
        }
        return attackRoll;
    }

    private int getMagicAttackRoll() {
        CombatStyle style = player.getCombatStyle();
        
        int effectiveLevel = player.getBoostedMagic();
        effectiveLevel = applyPrayerBonus(effectiveLevel, true);
        
        if ("Accurate".equals(style.getStance())) {
            effectiveLevel += 2;
        }
        effectiveLevel += 9;

        if (isWearingMagicVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 29, 20);
        }

        EquipmentStats stats = player.getEquipmentStats();
        int baseRoll = effectiveLevel * (stats.getMagicAttack() + 64);
        int attackRoll = baseRoll;

        int additiveBonus = 0;
        boolean blackMaskBonus = false;
        
        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            additiveBonus += player.isForinthrySurgeActive() ? 35 : 20;
        } else if (player.isWearing("Salve amulet(ei)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            additiveBonus += 20;
        } else if (player.isWearing("Salve amulet(i)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            additiveBonus += 15;
        } else if (isWearingImbuedBlackMask() && player.isOnSlayerTask()) {
            blackMaskBonus = true;
        }
        
        if (player.isWearing("Efaritay's aid") && isVampyre() && isWearingSilverWeapon()) {
            additiveBonus += 15;
        }

        if (isWearingSmokeStaff()) {
            additiveBonus += 10;
        }

        if (additiveBonus > 0) {
            attackRoll = applyFactor(attackRoll, 100 + additiveBonus, 100);
        }

        if (hasAttribute(MonsterAttribute.DRAGON)) {
            if (player.isWearing("Dragon hunter crossbow")) {
                attackRoll = applyFactor(attackRoll, 13, 10);
            } else if (player.isWearing("Dragon hunter lance")) {
                attackRoll = applyFactor(attackRoll, 6, 5);
            } else if (player.isWearing("Dragon hunter wand")) {
                attackRoll = applyFactor(attackRoll, 7, 4);
            }
        }
        
        if (blackMaskBonus) {
            attackRoll = applyFactor(attackRoll, 23, 20);
        }

        if (isRevWeaponApplicable()) {
            attackRoll = applyFactor(attackRoll, 3, 2);
        }
        
        if (player.isWearing("Tome of water")) {
            attackRoll = applyFactor(attackRoll, 6, 5);
        }

        if (usingSpecialAttack) {
            attackRoll = applyMagicSpecAttackRollBonus(attackRoll);
        }

        return attackRoll;
    }
    
    private int applyMagicSpecAttackRollBonus(int attackRoll) {
        if (isWearingAccursedSceptre()) {
            return applyFactor(attackRoll, 3, 2);
        } else if (player.isWearing("Volatile nightmare staff")) {
            return applyFactor(attackRoll, 3, 2);
        } else if (player.isWearing("Eye of ayak")) {
            return applyFactor(attackRoll, 2, 1);
        }
        return attackRoll;
    }

    public int getNpcDefenceRoll() {
        if (monster == null) return 0;
        
        CombatStyle style = player.getCombatStyle();
        AttackType defenceStyle = getDefenceStyleForAttack(style.getAttackType());
        
        int defenceLevel;
        if (defenceStyle.isMagic() && !monster.usesDefenceForMagicDefence()) {
            defenceLevel = monster.getMagicLevel();
        } else {
            defenceLevel = monster.getDefenceLevel();
        }
        
        int effectiveLevel = defenceLevel + 9;
        int defenceBonus = monster.getDefenceForStyle(defenceStyle.getKey()) + 64;
        int defenceRoll = effectiveLevel * defenceBonus;
        
        boolean isCustomMonster = monster.getId() == -1;
        int toaInvocationLevel = monster.getInputs().getToaInvocationLevel();
        
        if (toaInvocationLevel > 0) {
            boolean isToaMonster = MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId());
            boolean isKephriOverlord = MonsterConstants.isKephriOverlord(monster.getId());
            
            if ((isToaMonster && !isKephriOverlord) || isCustomMonster) {
                defenceRoll = applyFactor(defenceRoll, 250 + toaInvocationLevel, 250);
            }
        }
        
        return defenceRoll;
    }
    
    private AttackType getDefenceStyleForAttack(AttackType attackType) {
        if (!usingSpecialAttack) {
            return attackType;
        }
        
        if (player.isWearingAny("Dragon claws", "Dragon dagger", "Dragon halberd", "Dragon longsword",
                "Dragon scimitar", "Crystal halberd", "Abyssal dagger", "Saradomin sword", "Arkan blade")
            || isWearingGodsword()) {
            return AttackType.SLASH;
        } else if (player.isWearingAny("Arclight", "Emberlight", "Dragon sword")) {
            return AttackType.STAB;
        } else if (player.isWearingAny("Voidwaker", "Saradomin's blessed sword")) {
            return AttackType.MAGIC;
        } else if (player.isWearing("Dragon mace")) {
            return AttackType.CRUSH;
        }
        
        return attackType;
    }

    public double calculateHitChance(int attackRoll, int defenceRoll) {
        if (isGuaranteedAccuracy()) {
            return 1.0;
        }

        if (isSunspearFinisher()) {
            return BaseCalc.getFixedAttackHitChance(attackRoll, defenceRoll);
        }
        
        if (isWearingFang() && player.getCombatStyle().getAttackType() == AttackType.STAB) {
            double normalAcc = BaseCalc.getNormalAccuracyRoll(attackRoll, defenceRoll);
            if (MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId())) {
                return 1 - Math.pow(1 - normalAcc, 2);
            }
            return BaseCalc.getFangAccuracyRoll(attackRoll, defenceRoll);
        }
        
        return BaseCalc.getNormalAccuracyRoll(attackRoll, defenceRoll);
    }
    
    private boolean isGuaranteedAccuracy() {
        int monsterId = monster.getId();
        AttackType attackType = player.getCombatStyle().getAttackType();
        String phase = monster.getInputs().getPhase();
        
        if (MonsterConstants.GUARANTEED_ACCURACY_MONSTERS.contains(monsterId)) {
            return true;
        }
        
        if (MonsterConstants.isDoomOfMokhaiotl(monsterId) && !"Normal".equals(phase)) {
            return true;
        }
        
        if (MonsterConstants.isVerzikP1(monsterId) && player.isWearing("Dawnbringer")) {
            return true;
        }
        
        if (MonsterConstants.isP2Warden(monsterId)) {
            return true;
        }
        
        if (monsterId == 7223) {
            return true;
        }
        
        if (MonsterConstants.isTormentedDemon(monsterId) && !"Shielded".equals(phase)) {
            return true;
        }
        
        if (MonsterConstants.isTitanElemental(monsterId) && attackType.isMagic()) {
            return false;
        }
        
        if (MonsterConstants.isEclipseMoon(monsterId) && "Clone".equals(monster.getVersion()) && isUsingMeleeStyle()) {
            return true;
        }
        
        if (attackType.isMagic() && MonsterConstants.ALWAYS_MAX_HIT_MAGIC.contains(monsterId)) {
            return true;
        }
        if (attackType.isRanged() && MonsterConstants.ALWAYS_MAX_HIT_RANGED.contains(monsterId)) {
            return true;
        }
        if (isUsingMeleeStyle() && MonsterConstants.ALWAYS_MAX_HIT_MELEE.contains(monsterId)) {
            return true;
        }
        
        if (usingSpecialAttack && player.isWearingAny("Voidwaker", "Dawnbringer")) {
            return true;
        }
        
        if (usingSpecialAttack && (player.isWearing("Seercull") || isWearingMlb())) {
            return true;
        }
        
        return false;
    }
    
    public double getTitanElementalMagicAccuracy() {
        int magicAttack = player.getEquipmentStats().getMagicAttack();
        double accuracy = Math.min(1.0, Math.max(0, magicAttack) / 100.0 + 0.3);
        if (isWearingEliteMagicVoid() || isWearingMagicVoid()) {
            accuracy = Math.min(1.0, accuracy * 1.45);
        }
        return accuracy;
    }

    public int getMaxHit() {
        CombatStyle style = player.getCombatStyle();
        AttackType attackType = style.getAttackType();

        if (MonsterConstants.ONE_HIT_MONSTERS.contains(monster.getId())) {
            return monster.getHitpoints();
        }

        if (attackType.isMelee()) {
            return getMeleeMaxHit();
        } else if (attackType.isRanged()) {
            return getRangedMaxHit();
        } else if (attackType.isMagic()) {
            return getMagicMaxHit();
        }
        return 0;
    }

    private int getMeleeMaxHit() {
        CombatStyle style = player.getCombatStyle();
        
        int baseLevel = player.getBoostedStrength();
        int effectiveLevel = baseLevel;
        effectiveLevel = applyPrayerBonus(effectiveLevel, false);
        
        if (player.isWearing("Soulreaper axe") && !usingSpecialAttack) {
            int stacks = Math.max(0, Math.min(5, player.getSoulreaperStacks()));
            int bonus = applyFactor(baseLevel, stacks * 6, 100);
            effectiveLevel += bonus;
        }
        
        int stanceBonus = 8;
        if ("Aggressive".equals(style.getStance())) {
            stanceBonus += 3;
        } else if ("Controlled".equals(style.getStance())) {
            stanceBonus += 1;
        }
        effectiveLevel += stanceBonus;

        if (isWearingMeleeVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 11, 10);
        }

        EquipmentStats stats = player.getEquipmentStats();
        int gearBonus = stats.getMeleeStrength() + 64;
        int baseMax = (effectiveLevel * gearBonus + 320) / 640;
        int maxHit = baseMax;

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            int factor = player.isForinthrySurgeActive() ? 27 : 24;
            maxHit = applyFactor(maxHit, factor, 20);
        } else if (player.isWearingAny("Salve amulet (e)", "Salve amulet(ei)") 
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            maxHit = applyFactor(maxHit, 6, 5);
        } else if (player.isWearingAny("Salve amulet", "Salve amulet(i)") 
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            maxHit = applyFactor(maxHit, 7, 6);
        } else if (isWearingBlackMask() && player.isOnSlayerTask()) {
            maxHit = applyFactor(maxHit, 7, 6);
        }

        if (player.isWearingAny("Arclight", "Emberlight") && hasAttribute(MonsterAttribute.DEMON)) {
            maxHit = addFactor(maxHit, demonbaneFactor(70), 100);
        }
        
        if (player.isWearingAny("Bone claws", "Burning claws") && hasAttribute(MonsterAttribute.DEMON)) {
            maxHit = addFactor(maxHit, demonbaneFactor(5), 100);
        }

        if (isWearingTzhaarWeapon() && isWearingObsidian()) {
            int obsidianBonus = applyFactor(baseMax, 1, 10);
            maxHit += obsidianBonus;
        }
        
        if (isWearingTzhaarWeapon() && isWearingBerserkerNecklace()) {
            maxHit = applyFactor(maxHit, 6, 5);
        }

        if (player.isWearing("Dragon hunter lance") && hasAttribute(MonsterAttribute.DRAGON)) {
            maxHit = applyFactor(maxHit, 6, 5);
        }
        
        if (player.isWearing("Dragon hunter wand") && hasAttribute(MonsterAttribute.DRAGON)) {
            maxHit = applyFactor(maxHit, 7, 5);
        }

        if (isWearingKeris() && hasAttribute(MonsterAttribute.KALPHITE)) {
            if (player.isWearing("Keris partisan of amascut")) {
                maxHit = applyFactor(maxHit, 115, 100);
            } else {
                maxHit = applyFactor(maxHit, 133, 100);
            }
        }
        
        if (player.isWearing("Barronite mace") && hasAttribute(MonsterAttribute.GOLEM)) {
            maxHit = applyFactor(maxHit, 23, 20);
        }
        
        if (player.isWearing("Granite hammer") && hasAttribute(MonsterAttribute.GOLEM)) {
            maxHit = applyFactor(maxHit, 13, 10);
        }

        if (isRevWeaponApplicable()) {
            maxHit = applyFactor(maxHit, 3, 2);
        }
        
        if (player.isWearingAny("Silverlight", "Darklight", "Silverlight (dyed)") && hasAttribute(MonsterAttribute.DEMON)) {
            maxHit = addFactor(maxHit, demonbaneFactor(60), 100);
        }

        if (player.isWearing("Leaf-bladed battleaxe") && hasAttribute(MonsterAttribute.LEAFY)) {
            maxHit = applyFactor(maxHit, 47, 40);
        }
        
        if (player.isWearing("Colossal blade") && monster != null) {
            maxHit += Math.min(monster.getSize() * 2, 10);
        }
        
        if (isWearingRatBoneWeapon() && hasAttribute(MonsterAttribute.RAT)) {
            maxHit += 10;
        }

        if (style.getAttackType() == AttackType.CRUSH) {
            int inqPieces = countInquisitorPieces();
            if (inqPieces > 0) {
                if (player.isWearing("Inquisitor's mace")) {
                    inqPieces *= 5;
                } else if (inqPieces == 3) {
                    inqPieces = 5;
                }
                maxHit = applyFactor(maxHit, 200 + inqPieces, 200);
            }
        }

        if (isWearingDharok()) {
            int missingHp = player.getHitpointsLevel() - player.getCurrentHitpoints();
            double dharokBonus = 1.0 + (missingHp * player.getHitpointsLevel()) / 10000.0;
            maxHit = (int) Math.floor(maxHit * dharokBonus);
        }

        if (usingSpecialAttack) {
            maxHit = applyMeleeSpecMaxHitBonus(maxHit);
        }

        return Math.max(0, maxHit);
    }
    
    private int applyMeleeSpecMaxHitBonus(int maxHit) {
        if (isWearingGodsword()) {
            maxHit = applyFactor(maxHit, 11, 10);
        }
        
        if (player.isWearingAny("Bandos godsword", "Saradomin sword")) {
            maxHit = applyFactor(maxHit, 11, 10);
        } else if (player.isWearingAny("Armadyl godsword", "Dragon sword", "Dragon longsword", "Saradomin's blessed sword")) {
            maxHit = applyFactor(maxHit, 5, 4);
        } else if (player.isWearingAny("Dragon mace", "Dragon warhammer", "Arkan blade")) {
            maxHit = applyFactor(maxHit, 3, 2);
        } else if (player.isWearing("Voidwaker")) {
            int minHit = applyFactor(maxHit, 1, 2);
            maxHit += minHit;
        } else if (player.isWearingAny("Dragon halberd", "Crystal halberd")) {
            maxHit = applyFactor(maxHit, 11, 10);
        } else if (player.isWearing("Dragon dagger")) {
            maxHit = applyFactor(maxHit, 23, 20);
        } else if (player.isWearing("Abyssal dagger")) {
            maxHit = applyFactor(maxHit, 17, 20);
        } else if (player.isWearing("Barrelchest anchor")) {
            maxHit = applyFactor(maxHit, 110, 100);
        } else if (player.isWearing("Soulreaper axe")) {
            int stacks = Math.max(0, Math.min(5, player.getSoulreaperStacks()));
            maxHit = applyFactor(maxHit, 100 + 6 * stacks, 100);
        }
        
        return maxHit;
    }

    private int getRangedMaxHit() {
        CombatStyle style = player.getCombatStyle();
        
        int effectiveLevel = player.getBoostedRanged();
        
        boolean scalesWithStr = player.isWearingAny("Eclipse atlatl", "Hunter's spear");
        if (scalesWithStr) {
            effectiveLevel = player.getBoostedStrength();
        }
        
        effectiveLevel = applyPrayerBonus(effectiveLevel, false);
        
        if ("Accurate".equals(style.getStance())) {
            effectiveLevel += 3;
        }
        effectiveLevel += 8;

        if (isWearingEliteRangedVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 9, 8);
        } else if (isWearingRangedVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 11, 10);
        }

        EquipmentStats stats = player.getEquipmentStats();
        int bonusStr = scalesWithStr ? stats.getMeleeStrength() : stats.getRangedStrength();
        int gearBonus = bonusStr + 64;
        int baseMax = (effectiveLevel * gearBonus + 320) / 640;
        int maxHit = baseMax;

        if (isWearingCrystalBow()) {
            int crystalPieces = countCrystalPieces();
            maxHit = applyFactor(maxHit, 40 + crystalPieces, 40);
        }

        boolean needRevWeaponBonus = isRevWeaponApplicable();
        boolean needDragonbane = player.isWearing("Dragon hunter crossbow") && hasAttribute(MonsterAttribute.DRAGON);
        boolean needDemonbane = player.isWearing("Scorching bow") && hasAttribute(MonsterAttribute.DEMON);

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            int factor = player.isForinthrySurgeActive() ? 27 : 24;
            maxHit = applyFactor(maxHit, factor, 20);
        } else if ((player.isWearing("Salve amulet(ei)") || (scalesWithStr && player.isWearing("Salve amulet (e)"))) 
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            maxHit = applyFactor(maxHit, 6, 5);
        } else if ((player.isWearing("Salve amulet(i)") || (scalesWithStr && player.isWearing("Salve amulet"))) 
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            maxHit = applyFactor(maxHit, 7, 6);
        } else if (scalesWithStr && isWearingBlackMask() && player.isOnSlayerTask()) {
            maxHit = applyFactor(maxHit, 7, 6);
        } else if (isWearingImbuedBlackMask() && player.isOnSlayerTask()) {
            int numerator = 23;
            if (needRevWeaponBonus) {
                needRevWeaponBonus = false;
                numerator += 10;
            }
            if (needDragonbane) {
                needDragonbane = false;
                numerator += 5;
            }
            if (needDemonbane) {
                needDemonbane = false;
                numerator += 6;
            }
            maxHit = applyFactor(maxHit, numerator, 20);
        }

        if (player.isWearing("Twisted bow")) {
            maxHit = applyTwistedBowScaling(maxHit, false);
        }

        if (needRevWeaponBonus) {
            maxHit = applyFactor(maxHit, 3, 2);
        }
        if (needDragonbane) {
            maxHit = applyFactor(maxHit, 5, 4);
        }
        if (needDemonbane) {
            maxHit = addFactor(maxHit, demonbaneFactor(30), 100);
        }
        
        if (isWearingRatBoneWeapon() && hasAttribute(MonsterAttribute.RAT)) {
            maxHit += 10;
        }
        
        if (player.isWearing("Tonalztics of ralos")) {
            maxHit = applyFactor(maxHit, 3, 4);
        }

        if (usingSpecialAttack) {
            maxHit = applyRangedSpecMaxHitBonus(maxHit);
        }
        
        if (MonsterConstants.isP2Warden(monster.getId())) {
            maxHit = applyP2WardensMaxHitModifier(maxHit);
        }

        return Math.max(0, maxHit);
    }
    
    private int applyRangedSpecMaxHitBonus(int maxHit) {
        if (isWearingBlowpipe()) {
            return applyFactor(maxHit, 3, 2);
        } else if (player.isWearing("Webweaver bow")) {
            int maxReduction = applyFactor(maxHit, 6, 10);
            return maxHit - maxReduction;
        } else if (player.isWearingAny("Heavy ballista", "Light ballista")) {
            return applyFactor(maxHit, 5, 4);
        } else if (player.isWearing("Rosewood blowpipe")) {
            return applyFactor(maxHit, 11, 10);
        }
        return maxHit;
    }

    private int getMagicMaxHit() {
        int maxHit = getBaseMagicMaxHit();
        if (maxHit == 0) return 0;
        
        if (usingSpecialAttack && player.isWearing("Eye of ayak")) {
            maxHit = applyFactor(maxHit, 13, 10);
        }

        int magicDmgBonus = player.getEquipmentStats().getMagicDamage();

        if (isWearingSmokeStaff()) {
            magicDmgBonus += 100;
        }
        
        boolean blackMaskBonus = false;

        if (player.isWearing("Salve amulet(ei)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            magicDmgBonus += 200;
        } else if (player.isWearing("Salve amulet(i)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            magicDmgBonus += 150;
        } else if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            magicDmgBonus += player.isForinthrySurgeActive() ? 350 : 200;
        } else if (isWearingImbuedBlackMask() && player.isOnSlayerTask()) {
            blackMaskBonus = true;
        }

        maxHit = addFactor(maxHit, magicDmgBonus, 1000);
        
        if (blackMaskBonus) {
            maxHit = applyFactor(maxHit, 23, 20);
        }

        if (hasAttribute(MonsterAttribute.DRAGON)) {
            if (player.isWearing("Dragon hunter lance")) {
                maxHit = applyFactor(maxHit, 6, 5);
            } else if (player.isWearing("Dragon hunter wand")) {
                maxHit = applyFactor(maxHit, 7, 5);
            } else if (player.isWearing("Dragon hunter crossbow")) {
                maxHit = applyFactor(maxHit, 5, 4);
            }
        }

        if (isRevWeaponApplicable()) {
            maxHit = applyFactor(maxHit, 3, 2);
        }
        
        if (usingSpecialAttack && isWearingAccursedSceptre()) {
            maxHit = applyFactor(maxHit, 3, 2);
        }

        if (player.isWearingAny("Tome of fire", "Tome of water", "Tome of earth")) {
            maxHit = applyFactor(maxHit, 11, 10);
        }

        for (Prayer prayer : player.getActivePrayers()) {
            if (prayer.isMagicPrayer()) {
                maxHit = addFactor(maxHit, prayer.getMagicDamageBonus(), 100);
            }
        }
        
        if (MonsterConstants.isP2Warden(monster.getId())) {
            maxHit = applyP2WardensMaxHitModifier(maxHit);
        }

        return Math.max(0, maxHit);
    }

    private int getBaseMagicMaxHit() {
        int magicLevel = player.getBoostedMagic();
        String weapon = player.getWeaponName();
        
        if (weapon == null) return 0;

        if (weapon.contains("Trident of the seas")) {
            return Math.max(1, magicLevel / 3 - 5);
        }
        if (weapon.equals("Thammaron's sceptre")) {
            return Math.max(1, magicLevel / 3 - 8);
        }
        if (weapon.contains("Accursed sceptre")) {
            return Math.max(1, magicLevel / 3 - 6);
        }
        if (weapon.contains("Trident of the swamp")) {
            return Math.max(1, magicLevel / 3 - 2);
        }
        if (weapon.contains("Sanguinesti staff") || weapon.contains("Holy sanguinesti staff")) {
            return Math.max(1, magicLevel / 3 - 1);
        }
        if (weapon.contains("Tumeken's shadow")) {
            return Math.max(1, magicLevel / 3 + 1);
        }
        if (weapon.equals("Eye of ayak")) {
            return Math.max(1, magicLevel / 3 - 6);
        }
        if (weapon.contains("Warped sceptre")) {
            return Math.max(1, (8 * magicLevel + 96) / 37);
        }
        if (weapon.equals("Bone staff")) {
            return Math.max(1, magicLevel / 3 - 5) + 10;
        }
        if (weapon.equals("Starter staff")) {
            return 8;
        }
        if (weapon.equals("Dawnbringer")) {
            return Math.max(1, magicLevel / 6 - 1);
        }
        if (weapon.contains("Crystal staff (basic)") || weapon.contains("Corrupted staff (basic)")) {
            return 23;
        }
        if (weapon.contains("Crystal staff (attuned)") || weapon.contains("Corrupted staff (attuned)")) {
            return 31;
        }
        if (weapon.contains("Crystal staff (perfected)") || weapon.contains("Corrupted staff (perfected)")) {
            return 39;
        }
        if (weapon.equals("Swamp lizard")) {
            return (magicLevel * (56 + 64) + 320) / 640;
        }
        if (weapon.equals("Orange salamander")) {
            return (magicLevel * (59 + 64) + 320) / 640;
        }
        if (weapon.equals("Red salamander")) {
            return (magicLevel * (77 + 64) + 320) / 640;
        }
        if (weapon.equals("Black salamander")) {
            return (magicLevel * (92 + 64) + 320) / 640;
        }
        if (weapon.equals("Tecu salamander")) {
            return (magicLevel * (104 + 64) + 320) / 640;
        }
        
        return 0;
    }
    
    private int applyP2WardensMaxHitModifier(int maxHit) {
        int defenceRoll = getNpcDefenceRoll();
        int attackRoll = getMaxAttackRoll();
        
        int reducedNpcDefence = defenceRoll / 3;
        int accuracyDelta = Math.max(attackRoll - reducedNpcDefence, 0);
        
        int modifier = (int) Math.max(15, Math.min(40, 15 + (40 - 15) * accuracyDelta / 42000.0));
        int maxPctRange = 20;
        
        return (maxHit * (modifier + maxPctRange)) / 100;
    }

    public int getAttackSpeed() {
        int speed = player.getWeaponSpeed();
        if (speed <= 0) {
            speed = 4;
        }
        
        CombatStyle style = player.getCombatStyle();
        if ("Rapid".equals(style.getStance())) {
            speed = Math.max(1, speed - 1);
        }

        if (usingSpecialAttack && player.isWearing("Eye of ayak")) {
            return 5;
        }
        
        return speed;
    }
    
    public boolean isImmune() {
        int monsterId = monster.getId();
        AttackType attackType = player.getCombatStyle().getAttackType();
        
        if (usingSpecialAttack && player.isWearing("Voidwaker")) {
            attackType = AttackType.MAGIC;
        }
        
        if (MonsterConstants.IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS.contains(monsterId) && attackType.isMagic()) {
            return true;
        }
        if (MonsterConstants.IMMUNE_TO_RANGED_DAMAGE_NPC_IDS.contains(monsterId) && attackType.isRanged()) {
            return true;
        }
        if (MonsterConstants.IMMUNE_TO_MELEE_DAMAGE_NPC_IDS.contains(monsterId) && isUsingMeleeStyle()) {
            if (MonsterConstants.isZulrah(monsterId) && isWearingPolearm()) {
                return false;
            }
            return true;
        }
        if (hasAttribute(MonsterAttribute.FLYING) && isUsingMeleeStyle()) {
            if (MonsterConstants.isVespula(monsterId)) {
                return true;
            }
            if (isWearingPolearm() || isWearingSalamander()) {
                return false;
            }
            return true;
        }
        if (MonsterConstants.IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS.contains(monsterId)
            && isUsingMeleeStyle() && !isWearingSalamander()) {
            return true;
        }
        if (hasAttribute(MonsterAttribute.VAMPYRE_3) && !wearingVampyrebane(MonsterAttribute.VAMPYRE_3)) {
            return true;
        }
        if (hasAttribute(MonsterAttribute.VAMPYRE_2) && !wearingVampyrebane(MonsterAttribute.VAMPYRE_2) 
            && !player.isWearing("Efaritay's aid") && !isWearingSilverWeapon()) {
            return true;
        }
        if (MonsterConstants.isGuardian(monsterId) && (!isUsingMeleeStyle() || !isWearingPickaxe())) {
            return true;
        }
        if (hasAttribute(MonsterAttribute.LEAFY) && !isWearingLeafBladedWeapon()) {
            return true;
        }
        if (MonsterConstants.isDoomOfMokhaiotl(monsterId) && "Shielded".equals(monster.getInputs().getPhase()) && !isUsingDemonbane()) {
            return true;
        }
        if (!hasAttribute(MonsterAttribute.RAT) && isWearingRatBoneWeapon()) {
            return true;
        }
        if (MonsterConstants.isEclipseMoon(monsterId) && "Clone".equals(monster.getVersion()) && !isUsingMeleeStyle()) {
            return true;
        }
        
        return false;
    }

    private int applyPrayerBonus(int level, boolean isAttack) {
        Set<Prayer> prayers = player.getActivePrayers();
        double multiplier = 1.0;
        int flatBonus = 0;

        for (Prayer prayer : prayers) {
            if (isAttack) {
                if (isUsingMeleeStyle() && prayer.isMeleePrayer()) {
                    multiplier *= prayer.getMeleeAttackMultiplier();
                } else if (player.getCombatStyle().getAttackType().isRanged() && prayer.isRangedPrayer()) {
                    multiplier *= prayer.getRangedAttackMultiplier();
                } else if (player.getCombatStyle().getAttackType().isMagic() && prayer.isMagicPrayer()) {
                    multiplier *= prayer.getMagicAttackMultiplier();
                }
            } else {
                if (isUsingMeleeStyle() && prayer.isMeleePrayer()) {
                    if (prayer == Prayer.BURST_OF_STRENGTH && level <= 20) {
                        flatBonus += 1;
                    } else {
                        multiplier *= prayer.getMeleeStrengthMultiplier();
                    }
                } else if (player.getCombatStyle().getAttackType().isRanged() && prayer.isRangedPrayer()) {
                    if (prayer == Prayer.SHARP_EYE && level <= 20) {
                        flatBonus += 1;
                    } else {
                        multiplier *= prayer.getRangedStrengthMultiplier();
                    }
                }
            }
        }

        return (int) Math.floor(level * multiplier) + flatBonus;
    }

    private int getAttackBonusForStyle() {
        EquipmentStats stats = player.getEquipmentStats();
        AttackType type = player.getCombatStyle().getAttackType();
        return stats.getAttackBonusForType(type);
    }

    private int countInquisitorPieces() {
        int count = 0;
        if (player.isWearing("Inquisitor's great helm")) count++;
        if (player.isWearing("Inquisitor's hauberk")) count++;
        if (player.isWearing("Inquisitor's plateskirt")) count++;
        return count;
    }

    private int countCrystalPieces() {
        int count = 0;
        if (player.isWearing("Crystal helm")) count++;
        if (player.isWearing("Crystal legs")) count += 2;
        if (player.isWearing("Crystal body")) count += 3;
        return count;
    }

    private boolean isRevWeaponApplicable() {
        if (!player.isInWilderness()) {
            return false;
        }
        
        AttackType type = player.getCombatStyle().getAttackType();
        if (type.isMagic()) {
            return player.isWearingAny("Accursed sceptre", "Accursed sceptre (a)", 
                "Thammaron's sceptre", "Thammaron's sceptre (a)");
        } else if (type.isRanged()) {
            return player.isWearingAny("Craw's bow", "Webweaver bow");
        } else {
            return player.isWearingAny("Ursine chainmace", "Viggora's chainmace");
        }
    }
    
    private int applyTwistedBowScaling(int value, boolean isAccuracy) {
        if (monster == null) return value;
        
        int monsterMagic = Math.max(monster.getMagicLevel(), monster.getOffensiveMagic());
        int cap = hasAttribute(MonsterAttribute.XERICIAN) ? 350 : 250;
        monsterMagic = Math.min(cap, monsterMagic);

        int factor;
        int base;
        
        if (isAccuracy) {
            factor = 10;
            base = 140;
        } else {
            factor = 14;
            base = 250;
        }
        
        int t2 = (3 * monsterMagic - factor) / 100;
        int t3 = (int) Math.pow((3 * monsterMagic / 10) - (10 * factor), 2) / 100;
        
        int bonus = base + t2 - t3;
        return (value * bonus) / 100;
    }
    
    private int demonbaneFactor(int weaponDemonbane) {
        int vulnerability = monster.getInputs().getDemonbaneVulnerability();
        if (vulnerability == 0) {
            vulnerability = getDemonbaneVulnerability();
        }
        return (weaponDemonbane * vulnerability) / 100;
    }
    
    private int getDemonbaneVulnerability() {
        if (monster.getId() == -1 && monster.getInputs().getDemonbaneVulnerability() > 0) {
            return monster.getInputs().getDemonbaneVulnerability();
        }
        if ("Duke Sucellus".equals(monster.getName())) {
            return 70;
        }
        if (MonsterConstants.isYama(monster.getId())) {
            return 120;
        }
        if (MonsterConstants.isYamaVoidFlare(monster.getId())) {
            return 200;
        }
        return 100;
    }
    
    protected boolean isWearingAccursedSceptre() {
        return player.isWearingAny("Accursed sceptre", "Accursed sceptre (a)");
    }
    
    protected boolean isWearingBlowpipe() {
        return player.isWearingAny("Toxic blowpipe", "Blazing blowpipe");
    }
    
    protected boolean isWearingMsb() {
        return player.isWearingAny("Magic shortbow", "Magic shortbow (i)");
    }
    
    protected boolean isWearingMlb() {
        return player.isWearingAny("Magic longbow", "Magic comp bow");
    }
    
    protected boolean isWearingSmokeStaff() {
        return player.isWearingAny("Smoke battlestaff", "Mystic smoke staff", "Twinflame staff");
    }
    
    protected boolean isWearingRatBoneWeapon() {
        return player.isWearingAny("Bone mace", "Bone shortbow", "Bone staff");
    }
    
    protected boolean isWearingPolearm() {
        String weapon = player.getWeaponName();
        return weapon != null && (weapon.contains("halberd") || weapon.contains("spear"));
    }
    
    protected boolean isWearingSalamander() {
        String weapon = player.getWeaponName();
        return weapon != null && weapon.toLowerCase().contains("salamander");
    }
    
    protected boolean isWearingPickaxe() {
        String weapon = player.getWeaponName();
        return weapon != null && weapon.toLowerCase().contains("pickaxe");
    }
    
    protected boolean isWearingLeafBladedWeapon() {
        if (isUsingMeleeStyle() && player.isWearingAny("Leaf-bladed battleaxe", "Leaf-bladed spear", "Leaf-bladed sword")) {
            return true;
        }
        if (player.isWearingAny("Broad arrows", "Broad bolts", "Amethyst broad bolts") 
            && player.getCombatStyle().getAttackType().isRanged()) {
            return true;
        }
        return false;
    }
    
    protected boolean isWearingSilverWeapon() {
        if (player.isWearingAny("Silver bolts") && player.getCombatStyle().getAttackType().isRanged()) {
            return true;
        }
        return isUsingMeleeStyle() && player.isWearingAny(
            "Blessed axe", "Ivandis flail", "Blisterwood flail", "Silver sickle", "Silver sickle (b)",
            "Emerald sickle", "Emerald sickle (b)", "Enchanted emerald sickle (b)", "Ruby sickle (b)",
            "Enchanted ruby sickle (b)", "Blisterwood sickle", "Silverlight", "Darklight", "Arclight",
            "Rod of ivandis", "Wolfbane"
        );
    }
    
    protected boolean wearingVampyrebane(MonsterAttribute tier) {
        boolean t2 = tier == MonsterAttribute.VAMPYRE_2;
        return (t2 || isUsingMeleeStyle()) && player.isWearingAny(
            t2 ? "Rod of ivandis" : "",
            "Ivandis flail", "Blisterwood sickle", "Blisterwood flail", "Hallowed flail", "Sunspear"
        );
    }

    private boolean isSunspearSpec() {
        return usingSpecialAttack && player.isWearing("Sunspear");
    }

    private boolean isSunspearFinisher() {
        if (!isSunspearSpec()) {
            return false;
        }

        int currentHp = monster.getInputs().getMonsterCurrentHp() > 0
            ? monster.getInputs().getMonsterCurrentHp()
            : monster.getHitpoints();
        int finisherDamage = applyFactor(getMaxHit(), 7, 10);
        return currentHp <= finisherDamage;
    }
    
    protected boolean isVampyre() {
        return hasAttribute(MonsterAttribute.VAMPYRE_1) 
            || hasAttribute(MonsterAttribute.VAMPYRE_2) 
            || hasAttribute(MonsterAttribute.VAMPYRE_3);
    }
    
    protected boolean isUsingDemonbane() {
        AttackType type = player.getCombatStyle().getAttackType();
        if (type.isRanged()) {
            return player.isWearing("Scorching bow");
        }
        return player.isWearingAny("Silverlight", "Darklight", "Arclight", "Emberlight", "Bone claws", "Burning claws");
    }
    
    protected boolean isUsingAbyssal() {
        return isUsingMeleeStyle() && player.isWearingAny("Abyssal bludgeon", "Abyssal dagger", "Abyssal whip", "Abyssal tentacle");
    }
    
    public boolean isUsingSpecialAttack() {
        return usingSpecialAttack;
    }
    
    public void setUsingSpecialAttack(boolean usingSpecialAttack) {
        this.usingSpecialAttack = usingSpecialAttack;
    }
}
