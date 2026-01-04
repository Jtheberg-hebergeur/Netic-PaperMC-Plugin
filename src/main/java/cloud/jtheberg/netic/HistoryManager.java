package cloud.jtheberg.netic;

import java.util.LinkedList;
import java.util.List;

/**
 * Gestionnaire de l'historique des conversations
 * Gère la mémoire (rapide) ET la base de données (persistant)
 */
public class HistoryManager {

    private static final LinkedList<String> memoryHistory = new LinkedList<>();
    private static int maxMessages;
    private static boolean systemPromptAdded = false;

    /**
     * Initialise l'historique au démarrage du plugin
     */
    public static void initialize() {
        maxMessages = NeticPlugin.getInstance().getConfig().getInt("history.max-messages", 20);

        // Ajouter le prompt système
        if (!systemPromptAdded) {
            String systemPrompt = NeticPlugin.getInstance().getConfig().getString("ia.system-prompt", "");
            if (!systemPrompt.isEmpty()) {
                synchronized (memoryHistory) {
                    memoryHistory.clear();
                    memoryHistory.add("SYSTEM: " + systemPrompt.trim());
                    systemPromptAdded = true;
                }
            }
        }

        // Charger l'historique depuis la base de données
        loadFromDatabase();
    }

    /**
     * Charge l'historique depuis la base de données au démarrage
     */
    private static void loadFromDatabase() {
        DatabaseManager db = NeticPlugin.getInstance().getDatabaseManager();
        if (db == null) {
            NeticPlugin.getInstance().getLogger().warning("⚠️ DatabaseManager non disponible, historique non chargé");
            return;
        }

        List<DatabaseManager.HistoryEntry> dbMessages = db.getRecentMessages(maxMessages);

        synchronized (memoryHistory) {
            for (DatabaseManager.HistoryEntry entry : dbMessages) {
                memoryHistory.add(entry.toString());
            }
        }

        if (!dbMessages.isEmpty()) {
            NeticPlugin.getInstance().getLogger().info("📚 " + dbMessages.size() + " messages chargés depuis la BDD");
        }
    }

    /**
     * Ajoute un message à l'historique (mémoire + BDD)
     */
    public static void add(String role, String message) {
        String playerName = null;

        // Extraire le nom du joueur si c'est un message joueur
        if (role.startsWith("Joueur ")) {
            playerName = role.substring(7);
            role = "Joueur";
        }

        // Ajouter en mémoire (synchrone, rapide)
        synchronized (memoryHistory) {
            String formattedMessage = (playerName != null ? "Joueur " + playerName : role) + ": " + message.trim();
            memoryHistory.add(formattedMessage);

            // Supprimer les messages les plus anciens si on dépasse la limite
            while (memoryHistory.size() > maxMessages + 1) {
                if (memoryHistory.size() > 1) {
                    memoryHistory.remove(1);
                }
            }
        }

        // Sauvegarder en BDD (asynchrone pour ne pas bloquer)
        DatabaseManager db = NeticPlugin.getInstance().getDatabaseManager();
        if (db != null && !role.equals("SYSTEM")) {
            final String finalRole = role;
            final String finalPlayerName = playerName;
            final String finalMessage = message.trim();

            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                    NeticPlugin.getInstance(),
                    () -> db.addMessage(finalRole, finalPlayerName, finalMessage)
            );
        }
    }

    /**
     * Récupère l'historique formaté pour l'envoi à l'API
     */
    public static String getFormattedHistory() {
        synchronized (memoryHistory) {
            return String.join("\n", memoryHistory);
        }
    }

    /**
     * Obtient le nombre de messages en mémoire (sans le prompt système)
     */
    public static int size() {
        synchronized (memoryHistory) {
            return Math.max(0, memoryHistory.size() - 1);
        }
    }

    /**
     * Réinitialise complètement l'historique (mémoire + BDD)
     */
    public static void reset() {
        synchronized (memoryHistory) {
            memoryHistory.clear();
            systemPromptAdded = false;
        }

        // Supprimer de la BDD (asynchrone)
        DatabaseManager db = NeticPlugin.getInstance().getDatabaseManager();
        if (db != null) {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                    NeticPlugin.getInstance(),
                    db::clearHistory
            );
        }
    }

    /**
     * Met à jour le prompt système (utilisé quand le nom de l'IA change)
     */
    public static void updateSystemPrompt(String newPrompt) {
        synchronized (memoryHistory) {
            memoryHistory.removeIf(msg -> msg.startsWith("SYSTEM:"));
            memoryHistory.addFirst("SYSTEM: " + newPrompt.trim());
            systemPromptAdded = true;
        }
    }
}