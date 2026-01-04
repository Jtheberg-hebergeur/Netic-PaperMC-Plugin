package cloud.jtheberg.netic.api;

import org.bukkit.entity.Player;
import java.io.File;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface NeticAPI {

    CompletableFuture<String> sendMessage(String message);
    CompletableFuture<String> sendMessage(Player player, String message);
    void sendMessage(String message, Consumer<String> onSuccess, Consumer<Throwable> onError);
    
    // Méthodes audio
    CompletableFuture<String> transcribeAudio(File audioFile);
    CompletableFuture<String> transcribeAudio(Player player, File audioFile);
    void transcribeAudio(File audioFile, Consumer<String> onSuccess, Consumer<Throwable> onError);
    
    CompletableFuture<AudioChatResponse> chatWithAudio(File audioFile);
    CompletableFuture<AudioChatResponse> chatWithAudio(Player player, File audioFile);
    void chatWithAudio(File audioFile, Consumer<AudioChatResponse> onSuccess, Consumer<Throwable> onError);
    
    boolean canSendMessage(Player player);
    boolean canSendAudio(Player player);
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
    
    class AudioChatResponse {
        private final String response;
        private final String transcription;
        private final long timestamp;
        
        public AudioChatResponse(String response, String transcription, long timestamp) {
            this.response = response;
            this.transcription = transcription;
            this.timestamp = timestamp;
        }
        
        public String getResponse() {
            return response;
        }
        
        public String getTranscription() {
            return transcription;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}