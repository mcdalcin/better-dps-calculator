package com.dpscalc;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Development test launcher for the DPS Calculator plugin.
 * 
 * Run this class with assertions enabled (-ea) to test the plugin in RuneLite.
 * 
 * Make sure to add the following VM options:
 * -ea
 */
public class DpsCalcPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(DpsCalcPlugin.class);
        RuneLite.main(args);
    }
}
