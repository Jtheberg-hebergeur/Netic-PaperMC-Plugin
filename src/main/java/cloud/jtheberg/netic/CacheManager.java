package cloud.jtheberg.netic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.concurrent.TimeUnit;

public class CacheManager {

    private final NeticPlugin plugin;
    private Cache<String, String> responseCache;

    private boolean enabled;
    private long ttlMinutes;
    private int maxSize;

    public CacheManager(NeticPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        initializeCache();
    }

    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("cache.enabled", true);
        this.ttlMinutes = plugin.getConfig().getLong("cache.ttl-minutes", 30);
        this.maxSize = plugin.getConfig().getInt("cache.max-size", 1000);
    }

    private void initializeCache() {
        if (!enabled) {
            plugin.getLogger().info("ℹ️ Cache désactivé");
            return;
        }

        responseCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        plugin.getLogger().info("✅ Cache initialisé: " + maxSize + " entrées max, TTL " + ttlMinutes + "m");
    }

    public String get(String question) {
        if (!enabled || responseCache == null) {
            return null;
        }

        String normalized = normalizeQuestion(question);
        return responseCache.getIfPresent(normalized);
    }

    public void put(String question, String response) {
        if (!enabled || responseCache == null) {
            return;
        }

        String normalized = normalizeQuestion(question);
        responseCache.put(normalized, response);
    }

    private String normalizeQuestion(String question) {
        return question.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[?!.,;:]", "");
    }

    public void clear() {
        if (responseCache != null) {
            long size = responseCache.estimatedSize();
            responseCache.invalidateAll();
            plugin.getLogger().info("🗑️ Cache vidé (" + size + " entrées)");
        }
    }

    public CacheStats getStats() {
        if (responseCache == null) {
            return null;
        }
        return responseCache.stats();
    }

    public long size() {
        if (responseCache == null) {
            return 0;
        }
        return responseCache.estimatedSize();
    }

    public double getHitRate() {
        if (responseCache == null) {
            return 0.0;
        }
        CacheStats stats = responseCache.stats();
        return stats.hitRate();
    }

    public void reload() {
        clear();
        loadConfig();
        initializeCache();
    }

    public boolean isEnabled() {
        return enabled;
    }
}