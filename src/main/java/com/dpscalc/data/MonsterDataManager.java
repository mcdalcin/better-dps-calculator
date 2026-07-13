package com.dpscalc.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.runelite.client.RuneLite;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class MonsterDataManager {
    private static final Logger log = LoggerFactory.getLogger(MonsterDataManager.class);
    
    private static final String CACHE_FILENAME = "dpscalc-monsters.json";
    private static final String ETAG_FILENAME = "dpscalc-monsters.etag";
    
    private volatile MonsterCatalog catalog = MonsterCatalog.empty();
    private final Gson gson = new Gson();
    
    @Getter
    private volatile boolean loaded = false;
    
    @Getter
    private volatile boolean updatedFromRemote = false;
    
    private volatile String currentEtag = null;
    
    @Inject
    private OkHttpClient okHttpClient;

    public void loadMonsters() {
        if (loaded) return;
        
        loadFromCache();
        
        if (!loaded) {
            loadFromBundledResource();
        }
        
        fetchFromGitHub();
    }
    
    private void loadFromBundledResource() {
        try (InputStream is = getClass().getResourceAsStream("/com/dpscalc/monsters.json")) {
            if (is == null) {
                log.error("Could not find bundled monsters.json resource");
                return;
            }
            
            JsonArray monsters = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
            parseAndStoreMonsters(monsters);
            loaded = true;
            log.info("Loaded {} monster IDs from bundled resource", catalog.uniqueIdCount());
        } catch (Exception e) {
            log.error("Failed to load bundled monsters.json", e);
        }
    }
    
    private void loadFromCache() {
        File cacheFile = getCacheFile();
        File etagFile = getEtagFile();
        
        if (!cacheFile.exists()) {
            log.debug("No cached monsters.json found");
            return;
        }
        
        try (FileReader reader = new FileReader(cacheFile, StandardCharsets.UTF_8)) {
            String content = java.nio.file.Files.readString(cacheFile.toPath());
            if (!MonsterDataProvenance.matches(content)) {
                log.warn("Ignoring monster cache without pinned-reference provenance");
                return;
            }
            JsonArray monsters = gson.fromJson(content, JsonArray.class);
            parseAndStoreMonsters(monsters);
            loaded = true;
            log.info("Loaded {} monster IDs from pinned cache", catalog.uniqueIdCount());
            
            if (etagFile.exists()) {
                currentEtag = java.nio.file.Files.readString(etagFile.toPath()).trim();
            }
        } catch (Exception e) {
            log.warn("Failed to load cached monsters.json, will use bundled", e);
        }
    }
    
    private void fetchFromGitHub() {
        if (okHttpClient == null) {
            log.warn("OkHttpClient not injected, skipping remote fetch");
            return;
        }
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(MonsterDataProvenance.RAW_URL)
            .header("User-Agent", "RuneLite-DpsCalc");
        
        if (currentEtag != null) {
            requestBuilder.header("If-None-Match", currentEtag);
        }
        
        Request request = requestBuilder.build();
        
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.warn("Failed to fetch monsters.json from GitHub: {}", e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                try (response) {
                    if (response.code() == 304) {
                        log.debug("Monster data unchanged (304 Not Modified)");
                        return;
                    }
                    
                    if (!response.isSuccessful()) {
                        log.warn("GitHub returned error: {}", response.code());
                        return;
                    }
                    
                    String body = response.body() != null ? response.body().string() : null;
                    if (body == null || body.isEmpty()) {
                        log.warn("Empty response from GitHub");
                        return;
                    }
                    if (!MonsterDataProvenance.matches(body)) {
                        log.error("Rejected monster data that does not match pinned reference {}",
                            MonsterDataProvenance.REFERENCE_SHA);
                        return;
                    }
                    
                    JsonArray monsters = gson.fromJson(body, JsonArray.class);
                    int previousCount = catalog.uniqueIdCount();
                    parseAndStoreMonsters(monsters);
                    
                    String newEtag = response.header("ETag");
                    saveToCache(body, newEtag);
                    
                    loaded = true;
                    updatedFromRemote = true;
                    log.info("Updated monster data from GitHub: {} monsters (was {})", 
                        catalog.uniqueIdCount(), previousCount);
                } catch (Exception e) {
                    log.error("Failed to process GitHub response", e);
                }
            }
        });
    }
    
    private void saveToCache(String jsonContent, String etag) {
        try {
            File cacheFile = getCacheFile();
            File etagFile = getEtagFile();
            
            cacheFile.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(cacheFile, StandardCharsets.UTF_8)) {
                writer.write(jsonContent);
            }
            
            if (etag != null) {
                try (FileWriter writer = new FileWriter(etagFile, StandardCharsets.UTF_8)) {
                    writer.write(etag);
                }
                currentEtag = etag;
            }
            
            log.debug("Saved monster data to cache");
        } catch (IOException e) {
            log.warn("Failed to save monster cache", e);
        }
    }
    
    private void parseAndStoreMonsters(JsonArray monsters) {
        List<MonsterStats> newMonsters = new ArrayList<>();
        
        for (JsonElement element : monsters) {
            JsonObject obj = element.getAsJsonObject();
            MonsterStats stats = parseMonster(obj);
            if (stats != null && stats.getId() > 0) {
                newMonsters.add(stats);
            }
        }
        
        catalog = MonsterCatalog.from(newMonsters);
    }
    
    private File getCacheFile() {
        return new File(RuneLite.RUNELITE_DIR, CACHE_FILENAME);
    }
    
    private File getEtagFile() {
        return new File(RuneLite.RUNELITE_DIR, ETAG_FILENAME);
    }

    private MonsterStats parseMonster(JsonObject obj) {
        MonsterStats stats = new MonsterStats();

        stats.setId(getIntSafe(obj, "id"));
        stats.setName(getStringSafe(obj, "name"));
        stats.setVersion(getStringSafe(obj, "version"));
        stats.setSize(getIntSafe(obj, "size"));
        stats.setSpeed(getIntSafe(obj, "speed"));

        JsonObject skills = obj.getAsJsonObject("skills");
        if (skills != null) {
            stats.setAttackLevel(getIntSafe(skills, "atk"));
            stats.setStrengthLevel(getIntSafe(skills, "str"));
            stats.setDefenceLevel(getIntSafe(skills, "def"));
            stats.setHitpoints(getIntSafe(skills, "hp"));
            stats.setMagicLevel(getIntSafe(skills, "magic"));
            stats.setRangedLevel(getIntSafe(skills, "ranged"));
        }

        JsonObject defensive = obj.getAsJsonObject("defensive");
        if (defensive != null) {
            stats.setStabDefence(getIntSafe(defensive, "stab"));
            stats.setSlashDefence(getIntSafe(defensive, "slash"));
            stats.setCrushDefence(getIntSafe(defensive, "crush"));
            stats.setMagicDefence(getIntSafe(defensive, "magic"));
            stats.setLightRangedDefence(getIntSafe(defensive, "light"));
            stats.setStandardRangedDefence(getIntSafe(defensive, "standard"));
            stats.setHeavyRangedDefence(getIntSafe(defensive, "heavy"));
            stats.setFlatArmour(getIntSafe(defensive, "flat_armour"));
        }

        JsonArray attributes = obj.getAsJsonArray("attributes");
        if (attributes != null) {
            for (JsonElement attr : attributes) {
                MonsterAttribute monsterAttr = MonsterAttribute.fromJson(attr.getAsString());
                stats.addAttribute(monsterAttr);
            }
        }

        JsonElement weaknessEl = obj.get("weakness");
        if (weaknessEl != null && !weaknessEl.isJsonNull() && weaknessEl.isJsonObject()) {
            JsonObject weakness = weaknessEl.getAsJsonObject();
            stats.setWeaknessElement(WeaknessElement.fromJson(getStringSafe(weakness, "element")));
            stats.setWeaknessSeverity(getIntSafe(weakness, "severity"));
        }

        return stats;
    }

    private int getIntSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return 0;
        try {
            return el.getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    private String getStringSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        try {
            return el.getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    public MonsterStats getMonster(int npcId) {
        if (!loaded) {
            loadMonsters();
        }
        return catalog.get(npcId, null);
    }

    public MonsterStats getMonster(int npcId, String version) {
        if (!loaded) {
            loadMonsters();
        }
        return catalog.get(npcId, version);
    }

    public List<MonsterStats> getMonsterVersions(int npcId) {
        if (!loaded) {
            loadMonsters();
        }
        return catalog.versions(npcId);
    }

    public boolean hasMonster(int npcId) {
        if (!loaded) {
            loadMonsters();
        }
        return catalog.contains(npcId);
    }

    public int getMonsterCount() {
        if (!loaded) {
            loadMonsters();
        }
        return catalog.uniqueIdCount();
    }
    
    public Collection<MonsterStats> getAllMonsters() {
        if (!loaded) {
            loadMonsters();
        }
        return catalog.all();
    }
    
    public void forceRefresh() {
        currentEtag = null;
        fetchFromGitHub();
    }
}
