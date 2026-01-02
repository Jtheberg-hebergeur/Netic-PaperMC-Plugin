package cloud.jtheberg.netic;

import cloud.jtheberg.netic.api.NeticAPI;
import cloud.jtheberg.netic.api.NeticAPIImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NeticAI v1.0-b1-beta - Plugin principal
 * Plugin d'intelligence artificielle pour Minecraft avec API publique
 *
 * Nouvelles fonctionnalités v1.0-b1-beta:
 * - API publique pour autres plugins
 * - Rate limiting avancé (par joueur + global)
 * - Système de cache intelligent
 * - Statistiques détaillées
 * - Vérification automatique des mises à jour
 *
 * @author Kiz, S Jtheberg
 * @version v1.0-b1-beta
 */
public class NeticPlugin extends JavaPlugin {

    private static NeticPlugin instance;

    // Managers
    private DatabaseManager databaseManager;
    private RateLimitManager rateLimitManager;
    private CacheManager cacheManager;
    private NeticClient neticClient;
    private UpdateChecker updateChecker;

    // API publique
    private NeticAPIImpl neticAPI;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║  NeticAI v1.0-b1-beta - Chargement...  ║");
        getLogger().info("╚════════════════════════════════════════╝");

        // 1. Charger la configuration
        saveDefaultConfig();
        checkApiKey();

        // 2. Initialiser la base de données
        try {
            databaseManager = new DatabaseManager();
            databaseManager.initialize();
            String dbType = getConfig().getString("database.type", "sqlite");
            getLogger().info("✅ Base de données: " + dbType.toUpperCase());
        } catch (Exception e) {
            getLogger().severe("❌ ERREUR CRITIQUE: Initialisation BDD");
            getLogger().severe("Message: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Initialiser les managers
        cacheManager = new CacheManager(this);
        rateLimitManager = new RateLimitManager(this);
        neticClient = new NeticClient();

        // 4. Initialiser l'historique
        HistoryManager.initialize();

        // 5. Initialiser l'API publique (si activée)
        if (getConfig().getBoolean("api.public-enabled", true)) {
            initializePublicAPI();
        }

        // 6. Initialiser le vérificateur de mise à jour
        updateChecker = new UpdateChecker(this);
        Bukkit.getPluginManager().registerEvents(updateChecker, this);

        // Vérifier les mises à jour de manière asynchrone
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            updateChecker.checkForUpdates().thenAccept(hasUpdate -> {
                if (hasUpdate) {
                    getLogger().info("⚡ Tapez /netic update pour plus d'infos");
                }
            });
        });

        // 7. Enregistrer les listeners et commandes
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        AdminCommand adminCommand = new AdminCommand();
        getCommand("netic").setExecutor(adminCommand);
        getCommand("netic").setTabCompleter(adminCommand);

        // 8. Message de démarrage
        long loadTime = System.currentTimeMillis() - startTime;
        printStartupMessage(loadTime);
    }

    @Override
    public void onDisable() {
        getLogger().info("Désactivation de NeticAI...");

        // Fermer la base de données
        if (databaseManager != null) {
            databaseManager.close();
            getLogger().info("✅ Base de données fermée");
        }

        // Dé-enregistrer l'API publique
        if (neticAPI != null) {
            getServer().getServicesManager().unregister(NeticAPI.class);
            getLogger().info("✅ API publique dé-enregistrée");
        }

        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║  NeticAI désactivé                    ║");
        getLogger().info("╚════════════════════════════════════════╝");
    }

    /**
     * Initialise l'API publique pour les autres plugins
     */
    private void initializePublicAPI() {
        neticAPI = new NeticAPIImpl(this);

        getServer().getServicesManager().register(
                NeticAPI.class,
                neticAPI,
                this,
                ServicePriority.Normal
        );

        getLogger().info("✅ API publique enregistrée");
        getLogger().info("   Les autres plugins peuvent maintenant utiliser NeticAI");
    }

    /**
     * Recharge la configuration et réinitialise les managers
     */
    public void reloadConfiguration() {
        reloadConfig();

        // Recharger le cache
        if (cacheManager != null) {
            cacheManager.reload();
        }

        // Recharger le rate limiter
        if (rateLimitManager != null) {
            rateLimitManager.reload();
        }

        // Réinitialiser l'historique
        HistoryManager.reset();
        HistoryManager.initialize();

        getLogger().info("✅ Configuration rechargée");
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
            getLogger().warning("║  et ajoutez votre clé API            ║");
            getLogger().warning("║                                       ║");
            getLogger().warning("║  Obtenez votre clé sur:              ║");
            getLogger().warning("║  https://netic.jtheberg.cloud        ║");
            getLogger().warning("╚════════════════════════════════════════╝");
        } else {
            getLogger().info("✅ Clé API configurée");
        }
    }

    /**
     * Affiche le message de démarrage avec les informations du plugin
     */
    private void printStartupMessage(long loadTime) {
        String iaName = getConfig().getString("ia.name", "NETIC");
        String trigger = getConfig().getString("ia.trigger", "!ia");
        String dbType = getConfig().getString("database.type", "sqlite");
        int maxHistory = getConfig().getInt("history.max-messages", 20);
        int dbMessageCount = databaseManager != null ? databaseManager.getMessageCount() : 0;

        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║  ✅ NeticAI v1.0-b1-beta activé       ║");
        getLogger().info("║                                       ║");
        getLogger().info("║  Nom IA: " + String.format("%-29s", iaName) + "║");
        getLogger().info("║  Trigger: " + String.format("%-28s", trigger) + "║");
        getLogger().info("║  Historique: " + String.format("%-24s", maxHistory + " messages") + "║");
        getLogger().info("║  BDD: " + String.format("%-32s", dbType.toUpperCase()) + "║");
        getLogger().info("║  Messages BDD: " + String.format("%-22s", dbMessageCount) + "║");
        getLogger().info("║  API publique: " + String.format("%-22s", neticAPI != null ? "Activée" : "Désactivée") + "║");
        getLogger().info("║  Cache: " + String.format("%-30s", cacheManager.isEnabled() ? "Activé" : "Désactivé") + "║");
        getLogger().info("║  Chargement: " + String.format("%-25s", loadTime + "ms") + "║");
        getLogger().info("║                                       ║");
        getLogger().info("║  Développé par Jtheberg (Kiz, S)     ║");
        getLogger().info("╚════════════════════════════════════════╝");
    }

    // ==================== GETTERS ====================

    /**
     * Obtient l'instance du plugin
     * @return L'instance de NeticPlugin
     */
    public static NeticPlugin getInstance() {
        return instance;
    }

    /**
     * Obtient le gestionnaire de base de données
     * @return Le DatabaseManager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Obtient le gestionnaire de rate limiting
     * @return Le RateLimitManager
     */
    public RateLimitManager getRateLimitManager() {
        return rateLimitManager;
    }

    /**
     * Obtient le gestionnaire de cache
     * @return Le CacheManager
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Obtient le client HTTP pour l'API Netic
     * @return Le NeticClient
     */
    public NeticClient getNeticClient() {
        return neticClient;
    }

    /**
     * Obtient l'API publique de NeticAI
     * @return L'interface NeticAPI ou null si désactivée
     */
    public NeticAPI getNeticAPI() {
        return neticAPI;
    }

    /**
     * Obtient le vérificateur de mise à jour
     * @return L'UpdateChecker
     */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}