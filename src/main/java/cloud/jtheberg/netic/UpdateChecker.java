package cloud.jtheberg.netic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Vérificateur de mise à jour pour NeticAI
 * Vérifie automatiquement sur GitHub Releases
 */
public class UpdateChecker implements Listener {

    private static final String GITHUB_API = "https://api.github.com/repos/Jtheberg-hebergeur/Netic-PaperMC-Plugin/releases/latest";
    private static final String GITHUB_RELEASES = "https://github.com/Jtheberg-hebergeur/Netic-PaperMC-Plugin/releases";

    private final NeticPlugin plugin;
    private String latestVersion = null;
    private boolean updateAvailable = false;
    private String downloadUrl = null;
    private String releaseNotes = null;

    public UpdateChecker(NeticPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Vérifie les mises à jour de manière asynchrone
     */
    public CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(GITHUB_API);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "NeticAI-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("⚠️ Impossible de vérifier les mises à jour (HTTP " + responseCode + ")");
                    return false;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                latestVersion = extractJsonValue(json, "tag_name");
                downloadUrl = extractJsonValue(json, "html_url");
                releaseNotes = extractJsonValue(json, "body");

                if (latestVersion == null) {
                    plugin.getLogger().warning("⚠️ Impossible de parser la version depuis GitHub");
                    return false;
                }

                // Nettoyer le tag (enlever le 'v' si présent)
                latestVersion = latestVersion.replace("v", "");

                String currentVersion = plugin.getDescription().getVersion();
                updateAvailable = isNewerVersion(currentVersion, latestVersion);

                if (updateAvailable) {
                    logUpdateAvailable(currentVersion);
                } else {
                    plugin.getLogger().info("✅ Plugin à jour (version " + currentVersion + ")");
                }

                return updateAvailable;

            } catch (Exception e) {
                plugin.getLogger().warning("⚠️ Erreur lors de la vérification des mises à jour: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Log une mise à jour disponible dans la console
     */
    private void logUpdateAvailable(String currentVersion) {
        plugin.getLogger().info("╔════════════════════════════════════════════════════╗");
        plugin.getLogger().info("║  🆕 MISE À JOUR DISPONIBLE!                       ║");
        plugin.getLogger().info("║                                                   ║");
        plugin.getLogger().info("║  Version actuelle: " + String.format("%-28s", currentVersion) + "║");
        plugin.getLogger().info("║  Nouvelle version: " + String.format("%-28s", latestVersion) + "║");
        plugin.getLogger().info("║                                                   ║");
        plugin.getLogger().info("║  Télécharger sur GitHub Releases:                ║");
        plugin.getLogger().info("║  " + GITHUB_RELEASES);
        plugin.getLogger().info("╚════════════════════════════════════════════════════╝");
    }

    /**
     * Extrait une valeur d'un JSON simple
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;

            startIndex += searchKey.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) return null;

            return json.substring(startIndex, endIndex)
                    .replace("\\n", "\n")
                    .replace("\\r", "")
                    .replace("\\t", "  ");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Compare deux versions
     * Format supporté: X.Y-ZN.type (ex: 1.0-c1-beta)
     */
    private boolean isNewerVersion(String currentVersion, String newVersion) {
        try {
            currentVersion = currentVersion.replace("v", "").trim();
            newVersion = newVersion.replace("v", "").trim();

            // Séparer version principale et suffixe
            String[] currentSplit = currentVersion.split("-");
            String[] newSplit = newVersion.split("-");

            // Comparer version principale (ex: 1.0)
            String[] currentParts = currentSplit[0].split("\\.");
            String[] newParts = newSplit[0].split("\\.");

            for (int i = 0; i < Math.max(currentParts.length, newParts.length); i++) {
                int current = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
                int newer = i < newParts.length ? parseVersionPart(newParts[i]) : 0;

                if (newer > current) return true;
                if (newer < current) return false;
            }

            // Si version principale identique, comparer suffixe (ex: c1-beta vs c2-beta)
            if (currentSplit.length > 1 && newSplit.length > 1) {
                return compareSuffix(currentSplit[1], newSplit[1]);
            }

            // Si l'une a un suffixe et l'autre non, celle sans suffixe est plus récente
            if (currentSplit.length > 1 && newSplit.length == 1) return true;
            if (currentSplit.length == 1 && newSplit.length > 1) return false;

            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Erreur comparaison versions: " + e.getMessage());
            return false;
        }
    }

    /**
     * Compare les suffixes (ex: a1.beta vs c1-beta)
     */
    private boolean compareSuffix(String current, String newer) {
        try {
            // Extraire lettre et numéro (ex: c1)
            char currentLetter = current.charAt(0);
            char newerLetter = newer.charAt(0);

            if (newerLetter > currentLetter) return true;
            if (newerLetter < currentLetter) return false;

            // Même lettre, comparer numéro
            int currentNum = parseVersionPart(current.substring(1).split("\\.")[0]);
            int newerNum = parseVersionPart(newer.substring(1).split("\\.")[0]);

            return newerNum > currentNum;
        } catch (Exception e) {
            return newer.compareTo(current) > 0;
        }
    }

    /**
     * Parse une partie de version en nombre
     */
    private int parseVersionPart(String part) {
        try {
            StringBuilder numbers = new StringBuilder();
            for (char c : part.toCharArray()) {
                if (Character.isDigit(c)) {
                    numbers.append(c);
                }
            }
            return numbers.length() > 0 ? Integer.parseInt(numbers.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Notifie un joueur admin d'une mise à jour disponible
     */
    public void notifyPlayer(Player player) {
        if (!updateAvailable || latestVersion == null) {
            return;
        }

        if (!player.hasPermission("netic.admin")) {
            return;
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("════════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        player.sendMessage(Component.text("🆕 ")
                .color(NamedTextColor.GOLD)
                .append(Component.text("Mise à jour NeticAI disponible!")
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)));

        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("  Version actuelle: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(plugin.getDescription().getVersion())
                        .color(NamedTextColor.RED)));

        player.sendMessage(Component.text("  Nouvelle version: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(latestVersion)
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)));

        player.sendMessage(Component.empty());

        // Lien cliquable
        player.sendMessage(Component.text("  ➤ ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("[Télécharger sur GitHub]")
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(GITHUB_RELEASES))));

        player.sendMessage(Component.text("════════════════════════════════════")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.empty());
    }

    /**
     * Event: Notifie les admins à la connexion
     */
    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("netic.admin")) {
            return;
        }

        if (!updateAvailable) {
            return;
        }

        // Délai de 3 secondes pour ne pas spammer au login
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            notifyPlayer(player);
        }, 60L);
    }

    // Getters

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    public String getDownloadUrl() {
        return downloadUrl != null ? downloadUrl : GITHUB_RELEASES;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }
}