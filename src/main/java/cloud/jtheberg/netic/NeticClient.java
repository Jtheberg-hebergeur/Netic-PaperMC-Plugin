package cloud.jtheberg.netic;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Client HTTP pour communiquer avec l'API Netic
 */
public class NeticClient {

    private static final String ENDPOINT = "https://api.neticai.fr/v1/chat";
    private static final String CHAT_ENDPOINT = "https://api.neticai.fr/v1/chat";
    private static final String TRANSCRIBE_ENDPOINT = "https://napi.neticai.fr/v1/transcribe";
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

                URL url = new URL(ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout((int) TIMEOUT.toMillis());
                connection.setReadTimeout((int) TIMEOUT.toMillis());
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int statusCode = connection.getResponseCode();
                String responseBody = new String(connection.getInputStream().readAllBytes());

                if (statusCode == 200) {
                    String responseText = parseResponse(responseBody);
                    if (responseText != null && !responseText.isEmpty()) {
                        onSuccess.accept(responseText);
                    } else {
                        onError.accept("Réponse vide");
                    }
                } else if (statusCode == 401) {
                    onError.accept("Clé API invalide");
                } else if (statusCode == 429) {
                    onError.accept("Trop de requêtes");
                } else {
                    onError.accept("Erreur serveur (" + statusCode + ")");
                }

            } catch (Exception e) {
                NeticPlugin.getInstance().getLogger().severe("Erreur API: " + e.getMessage());
                onError.accept("Erreur de connexion");
            }
        });
    }

    /**
     * Transcrit un fichier audio en texte
     */
    public void transcribeAudio(File audioFile, Consumer<String> onSuccess, Consumer<String> onError) {
        String apiKey = NeticPlugin.getInstance().getConfig().getString("api.key");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("METS_TA_CLE_API_ICI")) {
            onError.accept("Clé API non configurée");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(NeticPlugin.getInstance(), () -> {
            try {
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                URL url = new URL(TRANSCRIBE_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                connection.setConnectTimeout((int) TIMEOUT.toMillis());
                connection.setReadTimeout((int) TIMEOUT.toMillis());
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    // En-tête du fichier audio
                    os.write(("--" + boundary + "\r\n").getBytes());
                    os.write(("Content-Disposition: form-data; name=\"audio\"; filename=\"" + audioFile.getName() + "\"\r\n").getBytes());
                    os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
                    
                    // Contenu du fichier audio
                    Files.copy(audioFile.toPath(), os);
                    
                    // Fin du multipart
                    os.write(("\r\n--" + boundary + "--\r\n").getBytes());
                }

                int statusCode = connection.getResponseCode();
                String responseBody = new String(connection.getInputStream().readAllBytes());

                if (statusCode == 200) {
                    String transcription = parseTranscriptionResponse(responseBody);
                    if (transcription != null && !transcription.isEmpty()) {
                        onSuccess.accept(transcription);
                    } else {
                        onError.accept("Transcription vide");
                    }
                } else if (statusCode == 401) {
                    onError.accept("Clé API invalide");
                } else if (statusCode == 429) {
                    onError.accept("Trop de requêtes");
                } else {
                    onError.accept("Erreur serveur (" + statusCode + ")");
                }

            } catch (IOException e) {
                NeticPlugin.getInstance().getLogger().severe("Erreur transcription audio: " + e.getMessage());
                onError.accept("Erreur de connexion");
            }
        });
    }

    /**
     * Envoie un fichier audio et reçoit une réponse texte
     */
    public void chatWithAudio(File audioFile, BiConsumer<String, String> onSuccess, Consumer<String> onError) {
        String apiKey = NeticPlugin.getInstance().getConfig().getString("api.key");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("METS_TA_CLE_API_ICI")) {
            onError.accept("Clé API non configurée");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(NeticPlugin.getInstance(), () -> {
            try {
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                URL url = new URL(CHAT_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                connection.setConnectTimeout((int) TIMEOUT.toMillis());
                connection.setReadTimeout((int) TIMEOUT.toMillis());
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    // En-tête du fichier audio
                    os.write(("--" + boundary + "\r\n").getBytes());
                    os.write(("Content-Disposition: form-data; name=\"audio\"; filename=\"" + audioFile.getName() + "\"\r\n").getBytes());
                    os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());
                    
                    // Contenu du fichier audio
                    Files.copy(audioFile.toPath(), os);
                    
                    // Fin du multipart
                    os.write(("\r\n--" + boundary + "--\r\n").getBytes());
                }

                int statusCode = connection.getResponseCode();
                String responseBody = new String(connection.getInputStream().readAllBytes());

                if (statusCode == 200) {
                    AudioChatResult result = parseAudioChatResponse(responseBody);
                    if (result != null) {
                        onSuccess.accept(result.response, result.transcription);
                    } else {
                        onError.accept("Réponse vide");
                    }
                } else if (statusCode == 401) {
                    onError.accept("Clé API invalide");
                } else if (statusCode == 429) {
                    onError.accept("Trop de requêtes");
                } else {
                    onError.accept("Erreur serveur (" + statusCode + ")");
                }

            } catch (IOException e) {
                NeticPlugin.getInstance().getLogger().severe("Erreur chat audio: " + e.getMessage());
                onError.accept("Erreur de connexion");
            }
        });
    }

    private String buildContextWithHistory(String message) {
        StringBuilder context = new StringBuilder();
        String history = HistoryManager.getFormattedHistory();
        if (!history.isEmpty()) {
            context.append(history).append("\n");
        }
        context.append("Joueur: ").append(message);
        return context.toString();
    }

    private String buildJsonBody(String context) {
        String escapedMessage = context
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

    private String parseTranscriptionResponse(String jsonResponse) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);
            String transcription = (String) json.get("transcription");
            return transcription != null ? transcription.trim() : null;
        } catch (Exception e) {
            NeticPlugin.getInstance().getLogger().warning("Erreur parsing transcription: " + e.getMessage());
            return null;
        }
    }

    private AudioChatResult parseAudioChatResponse(String jsonResponse) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);
            String response = (String) json.get("response");
            String transcription = (String) json.get("transcription");

            if (response != null && transcription != null) {
                return new AudioChatResult(response.trim(), transcription.trim());
            }
            return null;
        } catch (Exception e) {
            NeticPlugin.getInstance().getLogger().warning("Erreur parsing audio chat: " + e.getMessage());
            return null;
        }
    }

    private static class AudioChatResult {
        final String response;
        final String transcription;

        AudioChatResult(String response, String transcription) {
            this.response = response;
            this.transcription = transcription;
        }
    }
}