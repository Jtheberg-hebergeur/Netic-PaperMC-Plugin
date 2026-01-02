package cloud.jtheberg.netic;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Client HTTP pour communiquer avec l'API Netic
 */
public class NeticClient {

    private static final String ENDPOINT = "https://netic.jtheberg.cloud/api/v1/chat";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * Envoie une question à l'API avec l'historique complet
     */
    public void askIA(String message, Consumer<String> onSuccess, Consumer<String> onError) {
        String apiKey = NeticPlugin.getInstance().getConfig().getString("api.key");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("METS_TA_CLE_API_ICI")) {
            onError.accept("Clé API non configurée");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(NeticPlugin.getInstance(), () -> {
            try {
                String fullContext = buildContextWithHistory(message);
                String jsonBody = buildJsonBody(fullContext);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .timeout(TIMEOUT)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(TIMEOUT)
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String responseText = parseResponse(response.body());
                    if (responseText != null && !responseText.isEmpty()) {
                        onSuccess.accept(responseText);
                    } else {
                        onError.accept("Réponse vide");
                    }
                } else if (response.statusCode() == 401) {
                    onError.accept("Clé API invalide");
                } else if (response.statusCode() == 429) {
                    onError.accept("Trop de requêtes");
                } else {
                    onError.accept("Erreur serveur (" + response.statusCode() + ")");
                }

            } catch (Exception e) {
                NeticPlugin.getInstance().getLogger().severe("Erreur API: " + e.getMessage());
                onError.accept("Erreur de connexion");
            }
        });
    }

    private String buildContextWithHistory(String currentMessage) {
        StringBuilder context = new StringBuilder();
        String history = HistoryManager.getFormattedHistory();
        if (!history.isEmpty()) {
            context.append(history).append("\n");
        }
        context.append("Joueur: ").append(currentMessage);
        return context.toString();
    }

    private String buildJsonBody(String message) {
        String escapedMessage = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"message\":\"" + escapedMessage + "\"}";
    }

    private String parseResponse(String jsonResponse) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);
            String response = (String) json.get("response");
            return response != null ? response.trim() : null;
        } catch (Exception e) {
            NeticPlugin.getInstance().getLogger().warning("Erreur parsing: " + e.getMessage());
            return null;
        }
    }
}