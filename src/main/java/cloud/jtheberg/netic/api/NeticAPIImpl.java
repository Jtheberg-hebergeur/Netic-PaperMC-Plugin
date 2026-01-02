package cloud.jtheberg.netic.api;

import cloud.jtheberg.netic.NeticPlugin;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class NeticAPIImpl implements NeticAPI {

    private final NeticPlugin plugin;
    private final ApiStatsImpl stats;

    public NeticAPIImpl(NeticPlugin plugin) {
        this.plugin = plugin;
        this.stats = new ApiStatsImpl();
    }

    @Override
    public CompletableFuture<String> sendMessage(String message) {
        return sendMessage(null, message);
    }

    @Override
    public CompletableFuture<String> sendMessage(Player player, String message) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (player != null && !canSendMessage(player)) {
            stats.rateLimited.incrementAndGet();
            future.completeExceptionally(new RateLimitException("Rate limit exceeded"));
            return future;
        }

        stats.totalRequests.incrementAndGet();
        long startTime = System.currentTimeMillis();

        String cached = plugin.getCacheManager().get(message);
        if (cached != null) {
            stats.cachedResponses.incrementAndGet();
            stats.successfulRequests.incrementAndGet();
            future.complete(cached);
            return future;
        }

        plugin.getNeticClient().askIA(message,
                response -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    stats.addResponseTime(responseTime);
                    stats.successfulRequests.incrementAndGet();
                    plugin.getCacheManager().put(message, response);
                    future.complete(response);
                },
                error -> {
                    stats.failedRequests.incrementAndGet();
                    future.completeExceptionally(new ApiException(error));
                }
        );

        return future;
    }

    @Override
    public void sendMessage(String message, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        sendMessage(message).thenAccept(onSuccess).exceptionally(ex -> {
            onError.accept(ex);
            return null;
        });
    }

    @Override
    public boolean canSendMessage(Player player) {
        return plugin.getRateLimitManager().tryAcquire(player);
    }

    @Override
    public long getRemainingCooldown(Player player) {
        return plugin.getRateLimitManager().getRemainingCooldown(player);
    }

    @Override
    public void clearHistory() {
        cloud.jtheberg.netic.HistoryManager.reset();
        cloud.jtheberg.netic.HistoryManager.initialize();
    }

    @Override
    public boolean isAvailable() {
        String apiKey = plugin.getConfig().getString("api.key", "");
        return !apiKey.isEmpty() && !apiKey.equals("METS_TA_CLE_API_ICI");
    }

    @Override
    public ApiStats getStats() {
        return stats;
    }

    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
    }

    public static class RateLimitException extends Exception {
        public RateLimitException(String message) {
            super(message);
        }
    }

    private static class ApiStatsImpl implements ApiStats {
        final AtomicLong totalRequests = new AtomicLong(0);
        final AtomicLong successfulRequests = new AtomicLong(0);
        final AtomicLong failedRequests = new AtomicLong(0);
        final AtomicLong cachedResponses = new AtomicLong(0);
        final AtomicLong rateLimited = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong responseCount = new AtomicLong(0);

        void addResponseTime(long time) {
            totalResponseTime.addAndGet(time);
            responseCount.incrementAndGet();
        }

        @Override
        public long getTotalRequests() {
            return totalRequests.get();
        }

        @Override
        public long getSuccessfulRequests() {
            return successfulRequests.get();
        }

        @Override
        public long getFailedRequests() {
            return failedRequests.get();
        }

        @Override
        public long getCachedResponses() {
            return cachedResponses.get();
        }

        @Override
        public double getAverageResponseTime() {
            long count = responseCount.get();
            if (count == 0) return 0.0;
            return (double) totalResponseTime.get() / count;
        }

        @Override
        public long getRateLimitedRequests() {
            return rateLimited.get();
        }
    }
}