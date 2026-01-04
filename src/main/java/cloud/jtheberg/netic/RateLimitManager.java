package cloud.jtheberg.netic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RateLimitManager {

    private final NeticPlugin plugin;
    private final Cache<UUID, RateLimiter> playerLimiters;
    private final ConcurrentHashMap<UUID, Long> lastRequestTime;
    private final RateLimiter globalLimiter;

    private double playerRatePerMinute;
    private double globalRatePerMinute;
    private long cooldownSeconds;

    public RateLimitManager(NeticPlugin plugin) {
        this.plugin = plugin;
        this.lastRequestTime = new ConcurrentHashMap<>();

        loadConfig();

        this.playerLimiters = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();

        this.globalLimiter = RateLimiter.create(globalRatePerMinute / 60.0);

        plugin.getLogger().info("✅ Rate limiting: " + playerRatePerMinute + "/min par joueur, "
                + globalRatePerMinute + "/min global");
    }

    private void loadConfig() {
        this.playerRatePerMinute = plugin.getConfig().getDouble("rate-limit.player.requests-per-minute", 10.0);
        this.globalRatePerMinute = plugin.getConfig().getDouble("rate-limit.global.requests-per-minute", 50.0);
        this.cooldownSeconds = plugin.getConfig().getLong("ia.cooldown-seconds", 3);
    }

    public boolean tryAcquire(Player player) {
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("netic.bypass.ratelimit")) {
            lastRequestTime.put(uuid, System.currentTimeMillis());
            return true;
        }

        if (!checkCooldown(uuid)) {
            return false;
        }

        if (!globalLimiter.tryAcquire()) {
            plugin.getLogger().warning("⚠️ Rate limit global atteint!");
            return false;
        }

        RateLimiter playerLimiter = getPlayerLimiter(uuid);
        if (!playerLimiter.tryAcquire()) {
            return false;
        }

        lastRequestTime.put(uuid, System.currentTimeMillis());
        return true;
    }

    private boolean checkCooldown(UUID uuid) {
        Long lastTime = lastRequestTime.get(uuid);
        if (lastTime == null) {
            return true;
        }

        long elapsed = System.currentTimeMillis() - lastTime;
        return elapsed >= (cooldownSeconds * 1000);
    }

    public long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        Long lastTime = lastRequestTime.get(uuid);

        if (lastTime == null) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - lastTime;
        long remaining = (cooldownSeconds * 1000) - elapsed;

        if (remaining <= 0) {
            return 0;
        }

        return (long) Math.ceil(remaining / 1000.0);
    }

    private RateLimiter getPlayerLimiter(UUID uuid) {
        RateLimiter limiter = playerLimiters.getIfPresent(uuid);
        if (limiter == null) {
            limiter = RateLimiter.create(playerRatePerMinute / 60.0);
            playerLimiters.put(uuid, limiter);
        }
        return limiter;
    }

    public void reset() {
        lastRequestTime.clear();
        playerLimiters.invalidateAll();
        plugin.getLogger().info("✅ Rate limits réinitialisés");
    }

    public void reload() {
        loadConfig();
        reset();
    }
}