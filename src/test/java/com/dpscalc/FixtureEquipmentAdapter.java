package com.dpscalc;

import com.dpscalc.equipment.EquipmentCalculator;
import com.dpscalc.equipment.EquipmentCatalogItem;
import com.dpscalc.equipment.EquipmentCombatStyle;
import com.dpscalc.equipment.EquipmentContext;
import com.dpscalc.equipment.EquipmentDomainCatalog;
import com.dpscalc.equipment.EquipmentDomainException;
import com.dpscalc.equipment.EquipmentItem;
import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.EquipmentResult;
import com.dpscalc.equipment.EquipmentStatTotals;
import com.dpscalc.equipment.ItemVariable;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

final class FixtureEquipmentAdapter {
    private static final int SLOT_COUNT = 14;
    private static final EquipmentDomainCatalog CATALOG = loadCatalog();
    private static final EquipmentCalculator CALCULATOR = new EquipmentCalculator(CATALOG);

    private FixtureEquipmentAdapter() {}

    static void apply(PlayerState state, JsonObject playerJson, int monsterId) {
        JsonObject equipment = playerJson.getAsJsonObject("equipment");
        if (equipment.has("stats")) {
            applyLegacy(state, equipment, playerJson.getAsJsonObject("style"));
            return;
        }
        EquipmentLoadout loadout = rawLoadout(equipment);
        JsonObject style = playerJson.getAsJsonObject("style");
        EquipmentContext context = EquipmentContext.of(
            monsterId,
            EquipmentCombatStyle.of(style.get("type").getAsString(), style.get("stance").getAsString()),
            spellbook(playerJson.get("spell"))
        );
        EquipmentResult result = CALCULATOR.calculate(loadout, context);
        state.setEquipmentStats(toMutableStats(result.getStats()));
        applyIdentity(state, result.getCanonicalLoadout());
        int speed = result.getAttackSpeed();
        if ("Rapid".equals(style.get("stance").getAsString())) speed += 1;
        state.setWeaponSpeed(speed);
    }

    private static EquipmentLoadout rawLoadout(JsonObject equipment) {
        Map<com.dpscalc.equipment.EquipmentSlot, EquipmentItem> items = new EnumMap<>(com.dpscalc.equipment.EquipmentSlot.class);
        for (com.dpscalc.equipment.EquipmentSlot slot : com.dpscalc.equipment.EquipmentSlot.values()) {
            JsonElement element = equipment.get(slot.name().toLowerCase(java.util.Locale.ROOT));
            if (element == null || element.isJsonNull()) continue;
            JsonObject item = element.getAsJsonObject();
            items.put(slot, EquipmentItem.raw(item.get("id").getAsInt(), itemVariables(item.getAsJsonObject("itemVars"))));
        }
        return EquipmentLoadout.of(items);
    }

    private static Map<String, ItemVariable> itemVariables(JsonObject json) {
        Map<String, ItemVariable> values = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive()) throw new EquipmentDomainException("itemVars." + entry.getKey(), "expected primitive");
            if (value.getAsJsonPrimitive().isBoolean()) values.put(entry.getKey(), ItemVariable.ofBoolean(value.getAsBoolean()));
            else if (value.getAsJsonPrimitive().isNumber()) values.put(entry.getKey(), ItemVariable.ofNumber(value.getAsDouble()));
            else if (value.getAsJsonPrimitive().isString()) values.put(entry.getKey(), ItemVariable.ofString(value.getAsString()));
            else throw new EquipmentDomainException("itemVars." + entry.getKey(), "unsupported primitive");
        }
        return values;
    }

    private static void applyIdentity(PlayerState state, EquipmentLoadout loadout) {
        int[] ids = new int[SLOT_COUNT];
        String[] names = new String[SLOT_COUNT];
        String[] versions = new String[SLOT_COUNT];
        String[] categories = new String[SLOT_COUNT];
        for (Map.Entry<com.dpscalc.equipment.EquipmentSlot, EquipmentItem> entry : loadout.asMap().entrySet()) {
            com.dpscalc.state.EquipmentSlot stateSlot = stateSlot(entry.getKey());
            EquipmentCatalogItem facts = CATALOG.getItem(entry.getValue());
            ids[stateSlot.getIndex()] = entry.getValue().getOriginalId();
            names[stateSlot.getIndex()] = facts.getName();
            versions[stateSlot.getIndex()] = facts.getVersion();
            categories[stateSlot.getIndex()] = facts.getCategory();
        }
        state.setEquippedItemIds(ids);
        state.setEquippedItemNames(names);
        state.setEquippedItemVersions(versions);
        state.setEquippedItemCategories(categories);
    }

    private static com.dpscalc.state.EquipmentSlot stateSlot(com.dpscalc.equipment.EquipmentSlot slot) {
        switch (slot) {
            case HEAD: return com.dpscalc.state.EquipmentSlot.HEAD;
            case CAPE: return com.dpscalc.state.EquipmentSlot.CAPE;
            case NECK: return com.dpscalc.state.EquipmentSlot.AMULET;
            case WEAPON: return com.dpscalc.state.EquipmentSlot.WEAPON;
            case BODY: return com.dpscalc.state.EquipmentSlot.BODY;
            case SHIELD: return com.dpscalc.state.EquipmentSlot.SHIELD;
            case LEGS: return com.dpscalc.state.EquipmentSlot.LEGS;
            case HANDS: return com.dpscalc.state.EquipmentSlot.GLOVES;
            case FEET: return com.dpscalc.state.EquipmentSlot.BOOTS;
            case RING: return com.dpscalc.state.EquipmentSlot.RING;
            case AMMO: return com.dpscalc.state.EquipmentSlot.AMMO;
            default: throw new AssertionError(slot);
        }
    }

    private static EquipmentStats toMutableStats(EquipmentStatTotals totals) {
        EquipmentStats stats = new EquipmentStats();
        stats.setStabAttack(totals.getStabAttack()); stats.setSlashAttack(totals.getSlashAttack());
        stats.setCrushAttack(totals.getCrushAttack()); stats.setMagicAttack(totals.getMagicAttack());
        stats.setRangedAttack(totals.getRangedAttack()); stats.setMeleeStrength(totals.getMeleeStrength());
        stats.setRangedStrength(totals.getRangedStrength()); stats.setMagicDamage(totals.getMagicDamage());
        stats.setPrayerBonus(totals.getPrayerBonus()); stats.setStabDefence(totals.getStabDefence());
        stats.setSlashDefence(totals.getSlashDefence()); stats.setCrushDefence(totals.getCrushDefence());
        stats.setMagicDefence(totals.getMagicDefence()); stats.setRangedDefence(totals.getRangedDefence());
        return stats;
    }

    private static String spellbook(JsonElement spell) {
        if (spell == null || spell.isJsonNull()) return "";
        if (!spell.isJsonObject()) throw new EquipmentDomainException("spell", "expected raw spell object");
        return spell.getAsJsonObject().get("spellbook").getAsString();
    }

    private static void applyLegacy(PlayerState state, JsonObject equipment, JsonObject style) {
        state.setEquipmentStats(legacyStats(equipment.getAsJsonObject("stats")));
        state.setEquippedItemIds(legacyIds(equipment.getAsJsonObject("itemIds")));
        state.setEquippedItemNames(legacyStrings(equipment.getAsJsonObject("itemNames")));
        state.setEquippedItemVersions(legacyItemField(equipment.getAsJsonObject("slots"), "version"));
        state.setEquippedItemCategories(legacyItemField(equipment.getAsJsonObject("slots"), "category"));
        int speed = equipment.get("weaponSpeed").getAsInt();
        if ("Rapid".equals(style.get("stance").getAsString())) speed += 1;
        state.setWeaponSpeed(speed);
    }

    private static EquipmentStats legacyStats(JsonObject json) {
        JsonObject bonuses = json.getAsJsonObject("bonuses"); JsonObject offensive = json.getAsJsonObject("offensive");
        JsonObject defensive = json.getAsJsonObject("defensive"); EquipmentStats stats = new EquipmentStats();
        stats.setStabAttack(value(offensive, "stab")); stats.setSlashAttack(value(offensive, "slash"));
        stats.setCrushAttack(value(offensive, "crush")); stats.setMagicAttack(value(offensive, "magic"));
        stats.setRangedAttack(value(offensive, "ranged")); stats.setMeleeStrength(value(bonuses, "str"));
        stats.setRangedStrength(value(bonuses, "ranged_str")); stats.setMagicDamage(value(bonuses, "magic_str"));
        stats.setPrayerBonus(value(bonuses, "prayer")); stats.setStabDefence(value(defensive, "stab"));
        stats.setSlashDefence(value(defensive, "slash")); stats.setCrushDefence(value(defensive, "crush"));
        stats.setMagicDefence(value(defensive, "magic")); stats.setRangedDefence(value(defensive, "ranged"));
        return stats;
    }

    private static int[] legacyIds(JsonObject json) {
        int[] values = new int[SLOT_COUNT];
        slotNames().forEach((name, slot) -> values[slot.getIndex()] = value(json, name));
        return values;
    }

    private static String[] legacyStrings(JsonObject json) {
        String[] values = new String[SLOT_COUNT];
        slotNames().forEach((name, slot) -> values[slot.getIndex()] = nullableString(json.get(name)));
        return values;
    }

    private static String[] legacyItemField(JsonObject slots, String field) {
        String[] values = new String[SLOT_COUNT];
        slotNames().forEach((name, slot) -> {
            JsonElement item = slots.get(name);
            values[slot.getIndex()] = item == null || item.isJsonNull() ? null : nullableString(item.getAsJsonObject().get(field));
        });
        return values;
    }

    private static Map<String, com.dpscalc.state.EquipmentSlot> slotNames() {
        Map<String, com.dpscalc.state.EquipmentSlot> slots = new java.util.LinkedHashMap<>();
        slots.put("head", com.dpscalc.state.EquipmentSlot.HEAD); slots.put("cape", com.dpscalc.state.EquipmentSlot.CAPE);
        slots.put("neck", com.dpscalc.state.EquipmentSlot.AMULET); slots.put("weapon", com.dpscalc.state.EquipmentSlot.WEAPON);
        slots.put("body", com.dpscalc.state.EquipmentSlot.BODY); slots.put("shield", com.dpscalc.state.EquipmentSlot.SHIELD);
        slots.put("legs", com.dpscalc.state.EquipmentSlot.LEGS); slots.put("hands", com.dpscalc.state.EquipmentSlot.GLOVES);
        slots.put("feet", com.dpscalc.state.EquipmentSlot.BOOTS); slots.put("ring", com.dpscalc.state.EquipmentSlot.RING);
        slots.put("ammo", com.dpscalc.state.EquipmentSlot.AMMO); return slots;
    }

    private static int value(JsonObject json, String key) { JsonElement value = json.get(key); return value == null || value.isJsonNull() ? 0 : value.getAsInt(); }
    private static String nullableString(JsonElement value) { return value == null || value.isJsonNull() ? null : value.getAsString(); }

    private static EquipmentDomainCatalog loadCatalog() {
        try (InputStream stream = FixtureEquipmentAdapter.class.getResourceAsStream("/equipment-domain.json")) {
            return EquipmentDomainCatalog.load(stream, "b6bc098dc0d742b2b763375d2e78e1b611a22070", "070a34ce7f6267be1ae8c84cfdd3757100f5a027898ba34937eaba16536ea951");
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
