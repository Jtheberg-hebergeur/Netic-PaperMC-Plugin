package cloud.jtheberg.netic.api;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface NeticAPI {

    CompletableFuture<String> sendMessage(String message);
    CompletableFuture<String> sendMessage(Player player, String message);
    void sendMessage(String message, Consumer<String> onSuccess, Consumer<Throwable> onError);
    boolean canSendMessage(Player player);
    long getRemainingCooldown(Player player);
    void clearHistory();
    boolean isAvailable();
    ApiStats getStats();

    interface ApiStats {
        long getTotalRequests();
        long getSuccessfulRequests();
        long getFailedRequests();
        long getCachedResponses();
        double getAverageResponseTime();
        long getRateLimitedRequests();

        default double getSuccessRate() {
            long total = getTotalRequests();
            if (total == 0) return 1.0;
            return (double) getSuccessfulRequests() / total;
        }

        default double getCacheHitRate() {
            long total = getTotalRequests();
            if (total == 0) return 0.0;
            return (double) getCachedResponses() / total;
        }
    }
}