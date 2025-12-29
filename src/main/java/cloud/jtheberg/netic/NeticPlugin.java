package cloud.jtheberg.netic;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin principal NeticAI
 * Permet aux joueurs de parler avec une IA via le chat Minecraft
 * Historique persistant en base de données (SQLite ou MariaDB)
 *
 * @author Jtheberg
 * @version 1.0
 */
public class NeticPlugin extends JavaPlugin {

    private static NeticPlugin instance;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // Sauvegarder la configuration par défaut
        saveDefaultConfig();

        // Initialiser la base de données
        try {
            databaseManager = new DatabaseManager();
            databaseManager.initialize();

            String dbType = getConfig().getString("database.type", "sqlite");
            getLogger().info("✅ Base de données initialisée: " + dbType.toUpperCase());

        } catch (Exception e) {
            getLogger().severe("════════════════════════════════════════");
            getLogger().severe("❌ ERREUR CRITIQUE: Initialisation BDD");
            getLogger().severe("Message: " + e.getMessage());
            getLogger().severe("════════════════════════════════════════");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Vérifier la configuration de la clé API
        checkApiKey();

        // Initialiser l'historique avec le prompt système
        HistoryManager.initialize();

        // Enregistrer les listeners
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        // Enregistrer les commandes
        getCommand("netic").setExecutor(new AdminCommand());
        getCommand("netic").setTabCompleter(new AdminCommand());

        // Message de démarrage
        printStartupMessage();
    }

    @Override
    public void onDisable() {
        // Fermer proprement la base de données
        if (databaseManager != null) {
            databaseManager.close();
            getLogger().info("✅ Base de données fermée proprement");
        }

        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║  NeticAI désactivé                    ║");
        getLogger().info("║  Historique conservé en BDD           ║");
        getLogger().info("╚════════════════════════════════════════╝");
    }

    /**
     * Obtient l'instance du plugin
     */
    public static NeticPlugin getInstance() {
        return instance;
    }

    /**
     * Obtient le gestionnaire de base de données
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Recharge la configuration et réinitialise l'historique
     */
    public void reloadConfiguration() {
        reloadConfig();
        HistoryManager.reset();
        HistoryManager.initialize();
        getLogger().info("✅ Configuration rechargée et historique réinitialisé");
    }

    /**
     * Vérifie si la clé API est configurée
     */
    private void checkApiKey() {
        String apiKey = getConfig().getString("api.key", "");

        if (apiKey.equals("METS_TA_CLE_API_ICI") || apiKey.isEmpty()) {
            getLogger().warning("╔════════════════════════════════════════╗");
            getLogger().warning("║  ⚠️  CLÉ API NON CONFIGURÉE  ⚠️        ║");
            getLogger().warning("║                                       ║");
            getLogger().warning("║  Éditez plugins/NeticAI/config.yml   ║");
            getLogger().warning("║  et ajoutez votre clé API Netic      ║");
            getLogger().warning("║  pour activer le plugin              ║");
            getLogger().warning("║                                       ║");
            getLogger().warning("╚════════════════════════════════════════╝");
        } else {
            getLogger().info("✅ Clé API détectée et configurée");
        }
    }

    /**
     * Affiche le message de démarrage avec les infos importantes
     */
    private void printStartupMessage() {
        String iaName = getConfig().getString("ia.name", "NETIC");
        String trigger = getConfig().getString("ia.trigger", "!ia");
        String dbType = getConfig().getString("database.type", "sqlite");
        int maxHistory = getConfig().getInt("history.max-messages", 20);

        int dbMessageCount = 0;
        if (databaseManager != null) {
            dbMessageCount = databaseManager.getMessageCount();
        }

        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║  ✅  NeticAI activé                    ║");
        getLogger().info("║                                       ║");
        getLogger().info("║  Nom IA: " + String.format("%-29s", iaName) + "║");
        getLogger().info("║  Trigger: " + String.format("%-28s", trigger) + "║");
        getLogger().info("║  Historique: " + String.format("%-24s", maxHistory + " messages") + "║");
        getLogger().info("║  BDD: " + String.format("%-32s", dbType.toUpperCase()) + "║");
        getLogger().info("║  Messages BDD: " + String.format("%-22s", dbMessageCount) + "║");
        getLogger().info("║                                       ║");
        getLogger().info("║  Développé par Jtheberg              ║");
        getLogger().info("╚════════════════════════════════════════╝");
    }
}