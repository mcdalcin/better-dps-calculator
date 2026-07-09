package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.*;

import java.util.EnumSet;
import java.util.Set;

/**
 * Test utilities for creating test fixtures.
 * Ported from the web-based osrs-dps-calc test infrastructure.
 */
public class TestUtils {

    /**
     * Creates a standard test monster (Abyssal demon-like stats).
     */
    public static MonsterStats getTestMonster() {
        return new MonsterStatsBuilder()
            .name("Test Monster")
            .defenceLevel(135)
            .magicLevel(1)
            .hitpoints(150)
            .stabDefence(20)
            .slashDefence(20)
            .crushDefence(20)
            .magicDefence(0)
            .standardRangedDefence(20)
            .size(1)
            .build();
    }

    /**
     * Creates a test monster with specific attributes.
     */
    public static MonsterStats getTestMonster(MonsterAttribute... attributes) {
        MonsterStatsBuilder builder = new MonsterStatsBuilder()
            .name("Test Monster")
            .defenceLevel(135)
            .magicLevel(1)
            .hitpoints(150)
            .stabDefence(20)
            .slashDefence(20)
            .crushDefence(20)
            .magicDefence(0)
            .standardRangedDefence(20)
            .size(1);
        
        for (MonsterAttribute attr : attributes) {
            builder.attribute(attr);
        }
        
        return builder.build();
    }

    /**
     * Calculate DPS results for a player against a monster.
     */
    public static DpsResult calculate(PlayerState player, MonsterStats monster) {
        DpsCalculator calc = new DpsCalculator(player, monster);
        return calc.calculate();
    }

    /**
     * Builder for creating test PlayerState objects.
     */
    public static class PlayerStateBuilder {
        private int attackLevel = 99;
        private int strengthLevel = 99;
        private int defenceLevel = 99;
        private int rangedLevel = 99;
        private int magicLevel = 99;
        private int hitpointsLevel = 99;
        private int currentHitpoints = 99;
        
        private int attackBoost = 0;
        private int strengthBoost = 0;
        private int defenceBoost = 0;
        private int rangedBoost = 0;
        private int magicBoost = 0;
        
        private EquipmentStats equipmentStats = new EquipmentStats();
        private String[] equippedItemNames = new String[14];
        private CombatStyle combatStyle = CombatStyle.MELEE_ACCURATE_SLASH;
        private Set<Prayer> activePrayers = EnumSet.noneOf(Prayer.class);
        private int weaponSpeed = 4;
        
        private boolean onSlayerTask = false;
        private boolean inWilderness = false;

        public PlayerStateBuilder attackLevel(int level) {
            this.attackLevel = level;
            return this;
        }

        public PlayerStateBuilder strengthLevel(int level) {
            this.strengthLevel = level;
            return this;
        }

        public PlayerStateBuilder defenceLevel(int level) {
            this.defenceLevel = level;
            return this;
        }

        public PlayerStateBuilder rangedLevel(int level) {
            this.rangedLevel = level;
            return this;
        }

        public PlayerStateBuilder magicLevel(int level) {
            this.magicLevel = level;
            return this;
        }

        public PlayerStateBuilder hitpointsLevel(int level) {
            this.hitpointsLevel = level;
            this.currentHitpoints = level;
            return this;
        }

        public PlayerStateBuilder currentHitpoints(int hp) {
            this.currentHitpoints = hp;
            return this;
        }

        public PlayerStateBuilder attackBoost(int boost) {
            this.attackBoost = boost;
            return this;
        }

        public PlayerStateBuilder strengthBoost(int boost) {
            this.strengthBoost = boost;
            return this;
        }

        public PlayerStateBuilder rangedBoost(int boost) {
            this.rangedBoost = boost;
            return this;
        }

        public PlayerStateBuilder magicBoost(int boost) {
            this.magicBoost = boost;
            return this;
        }

        public PlayerStateBuilder equipmentStats(EquipmentStats stats) {
            this.equipmentStats = stats;
            return this;
        }

        public PlayerStateBuilder slashAttack(int bonus) {
            this.equipmentStats.setSlashAttack(bonus);
            return this;
        }

        public PlayerStateBuilder stabAttack(int bonus) {
            this.equipmentStats.setStabAttack(bonus);
            return this;
        }

        public PlayerStateBuilder crushAttack(int bonus) {
            this.equipmentStats.setCrushAttack(bonus);
            return this;
        }

        public PlayerStateBuilder rangedAttack(int bonus) {
            this.equipmentStats.setRangedAttack(bonus);
            return this;
        }

        public PlayerStateBuilder magicAttack(int bonus) {
            this.equipmentStats.setMagicAttack(bonus);
            return this;
        }

        public PlayerStateBuilder meleeStrength(int bonus) {
            this.equipmentStats.setMeleeStrength(bonus);
            return this;
        }

        public PlayerStateBuilder rangedStrength(int bonus) {
            this.equipmentStats.setRangedStrength(bonus);
            return this;
        }

        public PlayerStateBuilder magicDamage(int bonus) {
            this.equipmentStats.setMagicDamage(bonus);
            return this;
        }

        public PlayerStateBuilder weapon(String weaponName) {
            this.equippedItemNames[EquipmentSlot.WEAPON.getIndex()] = weaponName;
            return this;
        }

        public PlayerStateBuilder head(String itemName) {
            this.equippedItemNames[EquipmentSlot.HEAD.getIndex()] = itemName;
            return this;
        }

        public PlayerStateBuilder body(String itemName) {
            this.equippedItemNames[EquipmentSlot.BODY.getIndex()] = itemName;
            return this;
        }

        public PlayerStateBuilder legs(String itemName) {
            this.equippedItemNames[EquipmentSlot.LEGS.getIndex()] = itemName;
            return this;
        }

        public PlayerStateBuilder neck(String itemName) {
            this.equippedItemNames[EquipmentSlot.AMULET.getIndex()] = itemName;
            return this;
        }

        public PlayerStateBuilder gloves(String itemName) {
            this.equippedItemNames[EquipmentSlot.GLOVES.getIndex()] = itemName;
            return this;
        }

        public PlayerStateBuilder ammo(String itemName) {
            this.equippedItemNames[EquipmentSlot.AMMO.getIndex()] = itemName;
            return this;
        }

        public PlayerStateBuilder combatStyle(CombatStyle style) {
            this.combatStyle = style;
            return this;
        }

        public PlayerStateBuilder prayer(Prayer prayer) {
            this.activePrayers.add(prayer);
            return this;
        }

        public PlayerStateBuilder prayers(Prayer... prayers) {
            for (Prayer p : prayers) {
                this.activePrayers.add(p);
            }
            return this;
        }

        public PlayerStateBuilder weaponSpeed(int speed) {
            this.weaponSpeed = speed;
            return this;
        }

        public PlayerStateBuilder onSlayerTask(boolean onTask) {
            this.onSlayerTask = onTask;
            return this;
        }

        public PlayerStateBuilder inWilderness(boolean inWild) {
            this.inWilderness = inWild;
            return this;
        }

        public PlayerState build() {
            PlayerState state = new PlayerState();
            state.setAttackLevel(attackLevel);
            state.setStrengthLevel(strengthLevel);
            state.setDefenceLevel(defenceLevel);
            state.setRangedLevel(rangedLevel);
            state.setMagicLevel(magicLevel);
            state.setHitpointsLevel(hitpointsLevel);
            state.setCurrentHitpoints(currentHitpoints);
            
            state.setAttackBoost(attackBoost);
            state.setStrengthBoost(strengthBoost);
            state.setDefenceBoost(defenceBoost);
            state.setRangedBoost(rangedBoost);
            state.setMagicBoost(magicBoost);
            
            state.setEquipmentStats(equipmentStats);
            state.setEquippedItemNames(equippedItemNames);
            state.setCombatStyle(combatStyle);
            state.setActivePrayers(activePrayers);
            state.setWeaponSpeed(weaponSpeed);
            state.setOnSlayerTask(onSlayerTask);
            state.setInWilderness(inWilderness);
            
            return state;
        }
    }

    /**
     * Builder for creating test MonsterStats objects.
     */
    public static class MonsterStatsBuilder {
        private int id = 1;
        private String name = "Test Monster";
        private String version = "";
        private int size = 1;
        private int speed = 4;
        
        private int attackLevel = 1;
        private int strengthLevel = 1;
        private int defenceLevel = 1;
        private int hitpoints = 100;
        private int magicLevel = 1;
        private int rangedLevel = 1;
        
        private int stabDefence = 0;
        private int slashDefence = 0;
        private int crushDefence = 0;
        private int magicDefence = 0;
        private int lightRangedDefence = 0;
        private int standardRangedDefence = 0;
        private int heavyRangedDefence = 0;
        
        private Set<MonsterAttribute> attributes = EnumSet.noneOf(MonsterAttribute.class);

        public MonsterStatsBuilder id(int id) {
            this.id = id;
            return this;
        }

        public MonsterStatsBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MonsterStatsBuilder version(String version) {
            this.version = version;
            return this;
        }

        public MonsterStatsBuilder size(int size) {
            this.size = size;
            return this;
        }

        public MonsterStatsBuilder attackLevel(int level) {
            this.attackLevel = level;
            return this;
        }

        public MonsterStatsBuilder strengthLevel(int level) {
            this.strengthLevel = level;
            return this;
        }

        public MonsterStatsBuilder defenceLevel(int level) {
            this.defenceLevel = level;
            return this;
        }

        public MonsterStatsBuilder hitpoints(int hp) {
            this.hitpoints = hp;
            return this;
        }

        public MonsterStatsBuilder magicLevel(int level) {
            this.magicLevel = level;
            return this;
        }

        public MonsterStatsBuilder rangedLevel(int level) {
            this.rangedLevel = level;
            return this;
        }

        public MonsterStatsBuilder stabDefence(int defence) {
            this.stabDefence = defence;
            return this;
        }

        public MonsterStatsBuilder slashDefence(int defence) {
            this.slashDefence = defence;
            return this;
        }

        public MonsterStatsBuilder crushDefence(int defence) {
            this.crushDefence = defence;
            return this;
        }

        public MonsterStatsBuilder magicDefence(int defence) {
            this.magicDefence = defence;
            return this;
        }

        public MonsterStatsBuilder lightRangedDefence(int defence) {
            this.lightRangedDefence = defence;
            return this;
        }

        public MonsterStatsBuilder standardRangedDefence(int defence) {
            this.standardRangedDefence = defence;
            return this;
        }

        public MonsterStatsBuilder heavyRangedDefence(int defence) {
            this.heavyRangedDefence = defence;
            return this;
        }

        public MonsterStatsBuilder attribute(MonsterAttribute attr) {
            this.attributes.add(attr);
            return this;
        }

        public MonsterStatsBuilder attributes(MonsterAttribute... attrs) {
            for (MonsterAttribute attr : attrs) {
                this.attributes.add(attr);
            }
            return this;
        }

        public MonsterStats build() {
            MonsterStats stats = new MonsterStats();
            stats.setId(id);
            stats.setName(name);
            stats.setVersion(version);
            stats.setSize(size);
            stats.setSpeed(speed);
            
            stats.setAttackLevel(attackLevel);
            stats.setStrengthLevel(strengthLevel);
            stats.setDefenceLevel(defenceLevel);
            stats.setHitpoints(hitpoints);
            stats.setMagicLevel(magicLevel);
            stats.setRangedLevel(rangedLevel);
            
            stats.setStabDefence(stabDefence);
            stats.setSlashDefence(slashDefence);
            stats.setCrushDefence(crushDefence);
            stats.setMagicDefence(magicDefence);
            stats.setLightRangedDefence(lightRangedDefence);
            stats.setStandardRangedDefence(standardRangedDefence);
            stats.setHeavyRangedDefence(heavyRangedDefence);
            
            stats.setAttributes(attributes);
            
            return stats;
        }
    }

    /**
     * Creates a new PlayerStateBuilder.
     */
    public static PlayerStateBuilder player() {
        return new PlayerStateBuilder();
    }

    /**
     * Creates a new MonsterStatsBuilder.
     */
    public static MonsterStatsBuilder monster() {
        return new MonsterStatsBuilder();
    }
}
