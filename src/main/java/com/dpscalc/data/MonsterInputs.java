package com.dpscalc.data;

public class MonsterInputs {
    
    private int toaInvocationLevel;
    private int toaPathLevel;
    private int monsterCurrentHp;
    private String phase;
    private int partySize = 1;
    private int demonbaneVulnerability = 100;
    private DefenceReductions defenceReductions = new DefenceReductions();
    
    public MonsterInputs() {}
    
    public MonsterInputs(MonsterInputs other) {
        this.toaInvocationLevel = other.toaInvocationLevel;
        this.toaPathLevel = other.toaPathLevel;
        this.monsterCurrentHp = other.monsterCurrentHp;
        this.phase = other.phase;
        this.partySize = other.partySize;
        this.demonbaneVulnerability = other.demonbaneVulnerability;
        this.defenceReductions = new DefenceReductions(other.defenceReductions);
    }

    public int getToaInvocationLevel() {
        return toaInvocationLevel;
    }

    public void setToaInvocationLevel(int toaInvocationLevel) {
        this.toaInvocationLevel = toaInvocationLevel;
    }

    public int getToaPathLevel() {
        return toaPathLevel;
    }

    public void setToaPathLevel(int toaPathLevel) {
        this.toaPathLevel = toaPathLevel;
    }

    public int getMonsterCurrentHp() {
        return monsterCurrentHp;
    }

    public void setMonsterCurrentHp(int monsterCurrentHp) {
        this.monsterCurrentHp = monsterCurrentHp;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = Math.max(1, partySize);
    }

    public int getDemonbaneVulnerability() {
        return demonbaneVulnerability;
    }

    public void setDemonbaneVulnerability(int demonbaneVulnerability) {
        this.demonbaneVulnerability = demonbaneVulnerability;
    }

    public DefenceReductions getDefenceReductions() {
        return defenceReductions;
    }

    public void setDefenceReductions(DefenceReductions defenceReductions) {
        this.defenceReductions = defenceReductions;
    }

    public static class DefenceReductions {
        private int dwh;
        private int bgs;
        private int arclight;
        private int tonalztic;
        private int elderMaul;
        private int vulnerability;
        private int accursedSceptre;
        
        public DefenceReductions() {}
        
        public DefenceReductions(DefenceReductions other) {
            this.dwh = other.dwh;
            this.bgs = other.bgs;
            this.arclight = other.arclight;
            this.tonalztic = other.tonalztic;
            this.elderMaul = other.elderMaul;
            this.vulnerability = other.vulnerability;
            this.accursedSceptre = other.accursedSceptre;
        }

        public int getDwh() { return dwh; }
        public void setDwh(int dwh) { this.dwh = dwh; }

        public int getBgs() { return bgs; }
        public void setBgs(int bgs) { this.bgs = bgs; }

        public int getArclight() { return arclight; }
        public void setArclight(int arclight) { this.arclight = arclight; }

        public int getTonalztic() { return tonalztic; }
        public void setTonalztic(int tonalztic) { this.tonalztic = tonalztic; }

        public int getElderMaul() { return elderMaul; }
        public void setElderMaul(int elderMaul) { this.elderMaul = elderMaul; }

        public int getVulnerability() { return vulnerability; }
        public void setVulnerability(int vulnerability) { this.vulnerability = vulnerability; }

        public int getAccursedSceptre() { return accursedSceptre; }
        public void setAccursedSceptre(int accursedSceptre) { this.accursedSceptre = accursedSceptre; }
    }
}
