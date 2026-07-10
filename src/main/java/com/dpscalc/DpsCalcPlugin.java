package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.combat.CombatTracker;
import com.dpscalc.data.MonsterDataManager;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.AttackType;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.GearSnapshot;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.PlayerStateManager;
import com.dpscalc.state.Prayer;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.callback.ClientThread;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
    name = "Better DPS Calculator",
    description = "Calculates your theoretical DPS against monsters",
    tags = {"dps", "damage", "calculator", "combat", "pvm"}
)
public class DpsCalcPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Getter
    @Inject
    private DpsCalcConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PlayerStateManager playerStateManager;

    @Inject
    private MonsterDataManager monsterDataManager;

    @Inject
    private DpsCalcOverlay overlay;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ItemManager itemManager;

    private NavigationButton navButton;

    @Getter
    @Inject
    private CombatTracker combatTracker;

    @Getter
    private NPC targetNpc;

    @Getter
    private DpsResult currentDpsResult;

    @Getter
    private DpsResult specDpsResult;

    @Getter
    private MonsterStats currentMonsterStats;

    @Getter
    private volatile PlayerState cachedPlayerState;

    public ItemManager getItemManager() {
        return itemManager;
    }

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> targetClearTask;

    @Override
    protected void startUp() {
        log.info("DPS Calculator plugin started");
        monsterDataManager.loadMonsters();
        overlayManager.add(overlay);
        
        if (config.showPanel()) {
            BufferedImage icon;
            try {
                icon = ImageUtil.loadImageResource(getClass(), "icon.png");
            } catch (Exception e) {
                log.warn("Failed to load icon.png, using default", e);
                icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = icon.createGraphics();
                g.setColor(new java.awt.Color(255, 200, 100));
                g.fillRect(0, 0, 16, 16);
                g.setColor(java.awt.Color.WHITE);
                g.drawString("D", 4, 12);
                g.dispose();
            }
            navButton = NavigationButton.builder()
                .tooltip("Better DPS Calculator")
                .icon(icon)
                .priority(5)
                .panel(injector.getInstance(DpsCalcPanel.class))
                .build();
            clientToolbar.addNavigation(navButton);
        }

        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    protected void shutDown() {
        log.info("DPS Calculator plugin stopped");
        overlayManager.remove(overlay);
        
        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }

        cancelTargetClearTask();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        targetNpc = null;
        currentDpsResult = null;
        specDpsResult = null;
        currentMonsterStats = null;
        combatTracker.reset();
    }

    @Provides
    DpsCalcConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DpsCalcConfig.class);
    }

    public DpsResult calculateDps(MonsterStats monster) {
        return calculateDps(monster, config.useBestOffensivePrayer(), config.assumeMaxBoosts());
    }

    public DpsResult calculateDps(MonsterStats monster, boolean useBestPrayer, boolean assumeMaxBoosts) {
        if (monster == null) {
            return null;
        }

        PlayerState playerState = cachedPlayerState;
        if (playerState == null) {
            return null;
        }

        PlayerState calcState = new PlayerState();
        calcState.setAttackLevel(playerState.getAttackLevel());
        calcState.setStrengthLevel(playerState.getStrengthLevel());
        calcState.setDefenceLevel(playerState.getDefenceLevel());
        calcState.setRangedLevel(playerState.getRangedLevel());
        calcState.setMagicLevel(playerState.getMagicLevel());
        calcState.setHitpointsLevel(playerState.getHitpointsLevel());
        calcState.setAttackBoost(playerState.getAttackBoost());
        calcState.setStrengthBoost(playerState.getStrengthBoost());
        calcState.setDefenceBoost(playerState.getDefenceBoost());
        calcState.setRangedBoost(playerState.getRangedBoost());
        calcState.setMagicBoost(playerState.getMagicBoost());
        calcState.setCombatStyle(playerState.getCombatStyle());
        calcState.setActivePrayers(playerState.getActivePrayers());
        calcState.setEquipmentStats(playerState.getEquipmentStats());
        calcState.setEquippedItemIds(playerState.getEquippedItemIds());
        calcState.setEquippedItemNames(playerState.getEquippedItemNames());
        calcState.setWeaponSpeed(playerState.getWeaponSpeed());
        calcState.setCurrentHitpoints(playerState.getCurrentHitpoints());
        
        calcState.setOnSlayerTask(config.onSlayerTask());
        calcState.setChargeSpellActive(config.chargeSpell());

        // Apply "Use Best Offensive Prayer" setting
        if (useBestPrayer) {
            Set<Prayer> enhancedPrayers = EnumSet.copyOf(calcState.getActivePrayers());
            Prayer bestPrayer = getBestOffensivePrayer(calcState.getCombatStyle());
            if (bestPrayer != null) {
                enhancedPrayers.add(bestPrayer);
            }
            calcState.setActivePrayers(enhancedPrayers);
        }

        // Apply "Assume Max Boosts" setting
        if (assumeMaxBoosts) {
            applyMaxBoosts(calcState);
        }

        DpsCalculator calculator = new DpsCalculator(calcState, monster);
        DpsResult result = calculator.calculate();
        result.setMonsterHp(monster.getHitpoints());
        
        return result;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            cancelTargetClearTask();
            targetNpc = null;
            currentDpsResult = null;
            specDpsResult = null;
            currentMonsterStats = null;
        } else if (event.getGameState() == GameState.LOGGED_IN) {
            cachedPlayerState = playerStateManager.getPlayerState();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        cachedPlayerState = playerStateManager.getPlayerState();
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        if (event.getSource() != client.getLocalPlayer()) {
            return;
        }

        Actor target = event.getTarget();
        if (target instanceof NPC) {
            cancelTargetClearTask();
            targetNpc = (NPC) target;
            recalculateDps();
        } else if (target == null && targetNpc != null && !targetNpc.isDead()) {
            scheduleTargetClear();
        }
    }

    private void cancelTargetClearTask() {
        if (targetClearTask != null) {
            targetClearTask.cancel(false);
            targetClearTask = null;
        }
    }

    private void scheduleTargetClear() {
        cancelTargetClearTask();
        int timeout = config.targetTimeout();
        if (timeout > 0 && executor != null) {
            targetClearTask = executor.schedule(this::clearTarget, timeout, TimeUnit.SECONDS);
        }
    }

    private void clearTarget() {
        targetNpc = null;
        currentDpsResult = null;
        specDpsResult = null;
        currentMonsterStats = null;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == 94) {
            recalculateDps();
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        recalculateDps();
    }

    private void recalculateDps() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        PlayerState playerState = playerStateManager.getPlayerState();
        cachedPlayerState = playerState;

        if (targetNpc == null) {
            currentDpsResult = null;
            specDpsResult = null;
            currentMonsterStats = null;
            return;
        }

        int npcId = targetNpc.getId();
        MonsterStats baseMonsterStats = monsterDataManager.getMonster(npcId);
        
        if (baseMonsterStats == null) {
            log.debug("No monster data found for NPC ID: {}", npcId);
            currentDpsResult = null;
            specDpsResult = null;
            currentMonsterStats = null;
            return;
        }

        currentMonsterStats = new LiveMonsterContextProvider(client, targetNpc).enrich(baseMonsterStats);

        playerState.setOnSlayerTask(config.onSlayerTask());
        playerState.setChargeSpellActive(config.chargeSpell());

        DpsCalculator calculator = new DpsCalculator(playerState, currentMonsterStats);
        currentDpsResult = calculator.calculate();
        currentDpsResult.setMonsterHp(currentMonsterStats.getHitpoints());

        DpsCalculator specCalculator = new DpsCalculator(playerState, currentMonsterStats, true);
        specDpsResult = specCalculator.calculate();
        specDpsResult.setMonsterHp(currentMonsterStats.getHitpoints());
        
        log.debug("DPS calculated for {}: {}", currentMonsterStats.getName(), currentDpsResult);
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        Actor target = event.getActor();
        
        if (!(target instanceof NPC)) {
            return;
        }

        if (target != targetNpc) {
            return;
        }

        Hitsplat hitsplat = event.getHitsplat();
        if (hitsplat.isMine()) {
            combatTracker.recordDamage(hitsplat.getAmount());
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        
        if (npc != targetNpc) {
            return;
        }

        if (npc.isDead()) {
            combatTracker.recordKill();
        }
    }

    public void resetCombatTracker() {
        combatTracker.reset();
    }

    public PlayerState snapshotToPlayerState(GearSnapshot snapshot) {
        if (cachedPlayerState == null || snapshot == null) {
            return null;
        }

        PlayerState state = new PlayerState();

        state.setEquippedItemIds(snapshot.getEquippedItemIds().clone());
        state.setEquippedItemNames(snapshot.getEquippedItemNames().clone());

        EquipmentStats equipStats = playerStateManager.calculateEquipmentStatsFromIds(snapshot.getEquippedItemIds());
        state.setEquipmentStats(equipStats);

        int weaponId = snapshot.getEquippedItemIds()[EquipmentSlot.WEAPON.getIndex()];
        state.setWeaponSpeed(playerStateManager.getWeaponSpeed(weaponId));

        Set<Prayer> prayers = EnumSet.copyOf(snapshot.getActivePrayers());
        
        AttackType attackType = determineAttackType(snapshot.getCombatStyleName());
        
        if (snapshot.isUseBestOffensivePrayer()) {
            Prayer bestPrayer = getBestOffensivePrayer(attackType);
            if (bestPrayer != null) {
                prayers.add(bestPrayer);
            }
        }
        state.setActivePrayers(prayers);

        CombatStyle combatStyle = reconstructCombatStyle(snapshot.getCombatStyleName(), snapshot.getCombatStyleStance());
        state.setCombatStyle(combatStyle);

        state.setAttackLevel(cachedPlayerState.getAttackLevel());
        state.setStrengthLevel(cachedPlayerState.getStrengthLevel());
        state.setDefenceLevel(cachedPlayerState.getDefenceLevel());
        state.setRangedLevel(cachedPlayerState.getRangedLevel());
        state.setMagicLevel(cachedPlayerState.getMagicLevel());
        state.setHitpointsLevel(cachedPlayerState.getHitpointsLevel());
        state.setPrayerLevel(cachedPlayerState.getPrayerLevel());
        state.setCurrentHitpoints(cachedPlayerState.getCurrentHitpoints());

        if (snapshot.isAssumeMaxBoosts()) {
            applyMaxBoosts(state, attackType);
        } else {
            state.setAttackBoost(cachedPlayerState.getAttackBoost());
            state.setStrengthBoost(cachedPlayerState.getStrengthBoost());
            state.setDefenceBoost(cachedPlayerState.getDefenceBoost());
            state.setRangedBoost(cachedPlayerState.getRangedBoost());
            state.setMagicBoost(cachedPlayerState.getMagicBoost());
        }

        state.setOnSlayerTask(config.onSlayerTask());
        state.setChargeSpellActive(config.chargeSpell());
        state.setInWilderness(cachedPlayerState.isInWilderness());

        return state;
    }

    private AttackType determineAttackType(String styleName) {
        if (styleName == null) {
            return AttackType.CRUSH;
        }
        
        String upper = styleName.toUpperCase();
        
        if (upper.contains("STAB")) {
            return AttackType.STAB;
        }
        if (upper.contains("SLASH")) {
            return AttackType.SLASH;
        }
        if (upper.contains("CRUSH")) {
            return AttackType.CRUSH;
        }
        
        if (upper.contains("RANGE") || upper.contains("ACCURATE") || 
            upper.contains("RAPID") || upper.contains("LONGRANGE")) {
            return AttackType.RANGED_STANDARD;
        }
        
        if (upper.contains("MAGIC") || upper.contains("AUTOCAST")) {
            return AttackType.MAGIC;
        }
        
        return AttackType.CRUSH;
    }

    private Prayer getBestOffensivePrayer(AttackType attackType) {
        if (attackType == null) {
            return null;
        }
        
        if (attackType.isMelee()) {
            return Prayer.PIETY;
        }
        if (attackType.isRanged()) {
            return Prayer.RIGOUR;
        }
        if (attackType.isMagic()) {
            return Prayer.AUGURY;
        }
        
        return null;
    }

    private void applyMaxBoosts(PlayerState state, AttackType attackType) {
        if (attackType == null) {
            return;
        }
        
        if (attackType.isMelee()) {
            state.setAttackBoost(5);
            state.setStrengthBoost(5);
        } else if (attackType.isRanged()) {
            state.setRangedBoost(5);
        } else if (attackType.isMagic()) {
            state.setMagicBoost(5);
        }
    }

    private CombatStyle reconstructCombatStyle(String name, String stance) {
        if (name == null || stance == null) {
            return CombatStyle.UNARMED_PUNCH;
        }

        CombatStyle matchByBoth = null;
        CombatStyle matchByName = null;

        try {
            for (Field field : CombatStyle.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) &&
                    Modifier.isPublic(field.getModifiers()) &&
                    field.getType() == CombatStyle.class) {

                    CombatStyle style = (CombatStyle) field.get(null);

                    if (name.equals(style.getName()) && stance.equals(style.getStance())) {
                        matchByBoth = style;
                        break;
                    }

                    if (matchByName == null && name.equals(style.getName())) {
                        matchByName = style;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            log.warn("Failed to access CombatStyle fields via reflection", e);
        }

        if (matchByBoth != null) {
            return matchByBoth;
        }
        if (matchByName != null) {
            return matchByName;
        }

        return CombatStyle.UNARMED_PUNCH;
    }

    private Prayer getBestOffensivePrayer(CombatStyle style) {
        if (style == null) return null;
        AttackType attackType = style.getAttackType();
        if (attackType.isMelee()) {
            return Prayer.PIETY;
        } else if (attackType.isRanged()) {
            return Prayer.RIGOUR;
        } else if (attackType.isMagic()) {
            return Prayer.AUGURY;
        }
        return null;
    }

    private void applyMaxBoosts(PlayerState state) {
        if (state.getCombatStyle() == null) return;
        AttackType attackType = state.getCombatStyle().getAttackType();
        if (attackType.isMelee()) {
            state.setAttackBoost(Math.max(state.getAttackBoost(), 5));
            state.setStrengthBoost(Math.max(state.getStrengthBoost(), 5));
        } else if (attackType.isRanged()) {
            state.setRangedBoost(Math.max(state.getRangedBoost(), 5));
        } else if (attackType.isMagic()) {
            state.setMagicBoost(Math.max(state.getMagicBoost(), 5));
        }
    }
}
