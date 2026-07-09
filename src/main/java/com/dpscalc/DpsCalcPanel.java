package com.dpscalc;

import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterDataManager;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.GearSnapshot;
import com.dpscalc.state.PlayerStateManager;
import com.dpscalc.DpsComparison;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DpsCalcPanel extends PluginPanel {

    private static final Color BRAND_ORANGE = ColorScheme.BRAND_ORANGE;
    private static final Color SECTION_TITLE_COLOR = new Color(255, 204, 0);
    private static final Color VALUE_POSITIVE = new Color(0, 255, 100);
    private static final Color MUTED_TEXT = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color PANEL_BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    private static final Color CARD_BACKGROUND = ColorScheme.DARKER_GRAY_COLOR;

    private final Client client;
    private final DpsCalcPlugin plugin;
    private final DpsCalcConfig config;
    private final PlayerStateManager playerStateManager;
    private final MonsterDataManager monsterDataManager;

    private final JLabel monsterNameLabel = new JLabel();
    private final JLabel monsterStatsLabel = new JLabel();
    private final JLabel monsterDefLabel = new JLabel();
    
    private final JLabel dpsLabel = new JLabel();

    private final List<GearSnapshot> snapshots = new ArrayList<>();
    private final List<DpsComparison> comparisons = new ArrayList<>();
    
    private DpsComparisonDialog comparisonDialog;

    private boolean manualMode = false;
    private MonsterStats selectedMonster = null;

    @Inject
    public DpsCalcPanel(Client client, DpsCalcPlugin plugin, DpsCalcConfig config,
                        PlayerStateManager playerStateManager,
                        MonsterDataManager monsterDataManager) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.playerStateManager = playerStateManager;
        this.monsterDataManager = monsterDataManager;

        setLayout(new BorderLayout());
        setBackground(PANEL_BACKGROUND);
        setBorder(null);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(PANEL_BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        contentPanel.add(createHeader());
        contentPanel.add(Box.createVerticalStrut(15));

        JLabel infoLabel = new JLabel("<html><center>Game overlay mirror.<br>Use tool for search & compare.</center></html>");
        infoLabel.setFont(FontManager.getRunescapeSmallFont());
        infoLabel.setForeground(MUTED_TEXT);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(infoLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        contentPanel.add(createSelectedMonsterSection());
        contentPanel.add(Box.createVerticalStrut(15));

        contentPanel.add(createMinimalDpsSection());
        contentPanel.add(Box.createVerticalStrut(15));
        
        JButton compareButton = new JButton("Open Comparison Tool ⚙");
        compareButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        compareButton.setFocusable(false);
        compareButton.setBackground(BRAND_ORANGE.darker());
        compareButton.setForeground(Color.WHITE);
        compareButton.setFont(FontManager.getRunescapeFont());
        compareButton.addActionListener(e -> openComparisonDialog());
        contentPanel.add(compareButton);
        
        contentPanel.add(Box.createVerticalStrut(10));
        
        JButton useTargetBtn = new JButton("Reset to Live Target");
        useTargetBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        useTargetBtn.setFocusable(false);
        useTargetBtn.setBackground(CARD_BACKGROUND);
        useTargetBtn.setForeground(Color.WHITE);
        useTargetBtn.setFont(FontManager.getRunescapeSmallFont());
        useTargetBtn.addActionListener(e -> switchToLiveMode());
        contentPanel.add(useTargetBtn);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        
        add(scrollPane, BorderLayout.CENTER);

        Timer updateTimer = new Timer(600, e -> SwingUtilities.invokeLater(this::updateLoop));
        updateTimer.start();
    }
    
    private void openComparisonDialog() {
        if (comparisonDialog == null || !comparisonDialog.isVisible()) {
            comparisonDialog = new DpsComparisonDialog(plugin, this, snapshots, comparisons, monsterDataManager, SwingUtilities.getWindowAncestor(this));
            comparisonDialog.setVisible(true);
        } else {
            comparisonDialog.toFront();
            comparisonDialog.requestFocus();
        }
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BACKGROUND);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel title = new JLabel("Better DPS Calculator");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(BRAND_ORANGE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        
        panel.setBorder(new MatteBorder(0, 0, 2, 0, BRAND_ORANGE.darker()));
        panel.add(title, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSelectedMonsterSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 4, 0, 0, SECTION_TITLE_COLOR),
            new EmptyBorder(8, 12, 8, 8)
        ));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("TARGET");
        title.setFont(FontManager.getRunescapeSmallFont());
        title.setForeground(SECTION_TITLE_COLOR);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(5));

        monsterNameLabel.setFont(FontManager.getRunescapeBoldFont());
        monsterNameLabel.setForeground(Color.WHITE);
        monsterNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        monsterNameLabel.setText("None");
        panel.add(monsterNameLabel);
        
        panel.add(Box.createVerticalStrut(4));

        monsterStatsLabel.setFont(FontManager.getRunescapeSmallFont());
        monsterStatsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        monsterStatsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        monsterStatsLabel.setText("HP: - | Cmb: -");
        panel.add(monsterStatsLabel);

        panel.add(Box.createVerticalStrut(2));

        monsterDefLabel.setFont(FontManager.getRunescapeSmallFont());
        monsterDefLabel.setForeground(MUTED_TEXT);
        monsterDefLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        monsterDefLabel.setText("Def: -/-/- (S/S/C)");
        panel.add(monsterDefLabel);

        return panel;
    }

    private JPanel createMinimalDpsSection() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD_BACKGROUND);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 4, 0, 0, BRAND_ORANGE),
            new EmptyBorder(8, 12, 8, 8)
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(CARD_BACKGROUND);
        
        JPanel dpsRow = new JPanel(new BorderLayout());
        dpsRow.setBackground(CARD_BACKGROUND);
        dpsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dpsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel dpsTitle = new JLabel("DPS");
        dpsTitle.setForeground(Color.WHITE);
        dpsTitle.setFont(FontManager.getRunescapeFont());
        
        dpsLabel.setForeground(VALUE_POSITIVE);
        dpsLabel.setFont(FontManager.getRunescapeBoldFont()); 
        
        dpsRow.add(dpsTitle, BorderLayout.WEST);
        dpsRow.add(dpsLabel, BorderLayout.EAST);
        content.add(dpsRow);
        
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    public void switchToLiveMode() {
        manualMode = false;
        updateLoop();
    }
    
    // Called by Dialog when a monster is selected
    public void selectMonster(MonsterStats monster) {
        manualMode = true;
        selectedMonster = monster;
        updateDisplay(monster);
    }

    private void updateLoop() {
        if (manualMode) {
            if (selectedMonster != null) {
                updateDisplay(selectedMonster);
            }
            return;
        }

        if (client.getGameState() != GameState.LOGGED_IN) {
            resetDisplay();
            return;
        }

        NPC target = plugin.getTargetNpc();
        if (target == null) {
            resetDisplay();
            return;
        }

        MonsterStats stats = monsterDataManager.getMonster(target.getId());
        if (stats != null) {
            selectedMonster = stats;
            updateDisplay(stats);
        }
    }

    public void updateDisplay(MonsterStats monster) {
        monsterNameLabel.setText(monster.getName());
        int cmb = calculateCombatLevel(monster);
        monsterStatsLabel.setText(String.format("HP: %d | Cmb: %d", monster.getHitpoints(), cmb));
        monsterDefLabel.setText(String.format("Def: %d/%d/%d (S/S/C)", 
            monster.getStabDefence(), monster.getSlashDefence(), monster.getCrushDefence()));

        boolean bestPrayer = config.useBestOffensivePrayer();
        boolean maxBoosts = config.assumeMaxBoosts();
        
        if (comparisonDialog != null && comparisonDialog.isVisible()) {
            bestPrayer = comparisonDialog.isUseBestPrayer();
            maxBoosts = comparisonDialog.isAssumeMaxBoosts();
            comparisonDialog.updateMonster(monster);
        }

        DpsResult result = plugin.calculateDps(monster, bestPrayer, maxBoosts);
         
         if (result != null) {
             dpsLabel.setText(String.format("%.2f", result.getDps()));
         } else {
             dpsLabel.setText("-");
         }
    }

    private void resetDisplay() {
        monsterNameLabel.setText("None");
        monsterStatsLabel.setText("HP: - | Cmb: -");
        monsterDefLabel.setText("Def: -/-/- (S/S/C)");
        dpsLabel.setText("-");
        
        if (comparisonDialog != null && comparisonDialog.isVisible()) {
            comparisonDialog.updateMonster(null);
        }
    }

    private int calculateCombatLevel(MonsterStats m) {
        int base = (int) (0.25 * (m.getDefenceLevel() + m.getHitpoints() + (m.getMagicLevel()/2)));
        int melee = (int) (0.325 * (m.getAttackLevel() + m.getStrengthLevel()));
        int range = (int) (0.325 * (Math.floor(3 * m.getRangedLevel() / 2)));
        int magic = (int) (0.325 * (Math.floor(3 * m.getMagicLevel() / 2)));
        
        int maxOffense = Math.max(melee, Math.max(range, magic));
        return base + maxOffense;
    }

    public MonsterStats getSelectedMonster() {
        return selectedMonster;
    }

    public void replaceSnapshot(GearSnapshot oldSnapshot, GearSnapshot newSnapshot) {
        int index = snapshots.indexOf(oldSnapshot);
        if (index >= 0) {
            snapshots.set(index, newSnapshot);
            if (comparisonDialog != null) {
                comparisonDialog.rebuildSnapshotList();
            }
        }
    }

    public void moveSnapshotUp(GearSnapshot snapshot) {
        int index = snapshots.indexOf(snapshot);
        if (index > 0) {
            Collections.swap(snapshots, index, index - 1);
            if (comparisonDialog != null) {
                comparisonDialog.rebuildSnapshotList();
            }
        }
    }

    public void moveSnapshotDown(GearSnapshot snapshot) {
        int index = snapshots.indexOf(snapshot);
        if (index >= 0 && index < snapshots.size() - 1) {
            Collections.swap(snapshots, index, index + 1);
            if (comparisonDialog != null) {
                comparisonDialog.rebuildSnapshotList();
            }
        }
    }
}
