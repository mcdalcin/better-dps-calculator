package com.dpscalc.calc;

public final class SyncVersion {
    public static final String WEB_CALC_COMMIT = "1bebf1330bc3a81394819e8ae6ba8a4d4ae80328";
    public static final String WEB_CALC_DATE = "2026-07-07";
    public static final String LAST_VERIFIED = "2026-07-07";
    
    public static final String WEB_CALC_REPO = "https://github.com/weirdgloop/osrs-dps-calc";
    public static final String WEB_CALC_COMMIT_URL = WEB_CALC_REPO + "/commit/" + WEB_CALC_COMMIT;
    
    private SyncVersion() {}
    
    public static String getSyncInfo() {
        return String.format("Synced to %s (%s)", WEB_CALC_COMMIT, WEB_CALC_DATE);
    }
}
