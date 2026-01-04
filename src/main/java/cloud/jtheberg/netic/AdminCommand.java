package cloud.jtheberg.netic;

import cloud.jtheberg.netic.api.NeticAPI;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commandes administrateur NeticAI v1.0-c1-beta
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("netic.admin")) {
            sender.sendMessage(Component.text("❌ Permission refusée").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reset":
                HistoryManager.reset();
                HistoryManager.initialize();
                sender.sendMessage(Component.text("✅ Historique réinitialisé").color(NamedTextColor.GREEN));
                break;

            case "setname":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("❌ Usage: /netic setname <nom>").color(NamedTextColor.RED));
                    return true;
                }

                String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                NeticPlugin.getInstance().getConfig().set("ia.name", newName);
                NeticPlugin.getInstance().saveConfig();

                String prompt = NeticPlugin.getInstance()
                        .getConfig()
                        .getString("ia.system-prompt", "");

                HistoryManager.updateSystemPrompt(prompt + "\n\nTon nom est " + newName);

                sender.sendMessage(Component.text("✅ Nom changé: " + newName)
                        .color(NamedTextColor.GREEN));
                break;

            case "reload":
                NeticPlugin.getInstance().reloadConfiguration();
                sender.sendMessage(Component.text("✅ Configuration rechargée")
                        .color(NamedTextColor.GREEN));
                break;

            case "status":
                sendStatus(sender);
                break;

            case "stats":
                sendStats(sender);
                break;

            case "cache":
                handleCacheCommand(sender, args);
                break;

            case "clearcooldown":
                NeticPlugin.getInstance().getRateLimitManager().reset();
                sender.sendMessage(Component.text("✅ Rate limits réinitialisés")
                        .color(NamedTextColor.GREEN));
                break;

            case "update":
                handleUpdateCommand(sender);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    /* ========================= UPDATE ========================= */

    private void handleUpdateCommand(CommandSender sender) {

        UpdateChecker updateChecker = NeticPlugin.getInstance().getUpdateChecker();

        sender.sendMessage(Component.text("🔄 Vérification des mises à jour...")
                .color(NamedTextColor.YELLOW));

        updateChecker.checkForUpdates().thenAccept(updateAvailable -> {
            Bukkit.getScheduler().runTask(NeticPlugin.getInstance(), () -> {

                if (!updateAvailable) {
                    sender.sendMessage(Component.text("✅ NeticAI est à jour (v"
                                    + updateChecker.getCurrentVersion() + ")")
                            .color(NamedTextColor.GREEN));
                    return;
                }

                sender.sendMessage(Component.empty());
                sender.sendMessage(Component.text("🆕 Mise à jour NeticAI disponible !")
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));

                sender.sendMessage(Component.text("Version actuelle: ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(updateChecker.getCurrentVersion())
                                .color(NamedTextColor.RED)));

                sender.sendMessage(Component.text("Nouvelle version: ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(updateChecker.getLatestVersion())
                                .color(NamedTextColor.GREEN)
                                .decorate(TextDecoration.BOLD)));

                sender.sendMessage(Component.empty());

                sender.sendMessage(Component.text("➤ Télécharger sur GitHub")
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(updateChecker.getDownloadUrl())));
            });
        });
    }

    /* ========================= STATUS ========================= */

    private void sendStatus(CommandSender sender) {
        String iaName = NeticPlugin.getInstance().getConfig().getString("ia.name");
        String trigger = NeticPlugin.getInstance().getConfig().getString("ia.trigger");
        int historySize = HistoryManager.size();
        int maxHistory = NeticPlugin.getInstance().getConfig().getInt("history.max-messages");
        int dbCount = NeticPlugin.getInstance().getDatabaseManager().getMessageCount();

        sender.sendMessage(Component.text("═══ NeticAI v1.0-c1-beta - Statut ═══")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        sender.sendMessage(Component.text("Nom: ").color(NamedTextColor.GRAY)
                .append(Component.text(iaName).color(NamedTextColor.AQUA)));

        sender.sendMessage(Component.text("Trigger: ").color(NamedTextColor.GRAY)
                .append(Component.text(trigger).color(NamedTextColor.YELLOW)));

        sender.sendMessage(Component.text("Historique: ").color(NamedTextColor.GRAY)
                .append(Component.text(historySize + "/" + maxHistory)
                        .color(NamedTextColor.GREEN)));

        sender.sendMessage(Component.text("Messages BDD: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(dbCount))
                        .color(NamedTextColor.GREEN)));

        sender.sendMessage(Component.text("Cache: ").color(NamedTextColor.GRAY)
                .append(Component.text(
                                NeticPlugin.getInstance().getCacheManager().isEnabled()
                                        ? "Activé" : "Désactivé")
                        .color(NeticPlugin.getInstance().getCacheManager().isEnabled()
                                ? NamedTextColor.GREEN
                                : NamedTextColor.RED)));

        sender.sendMessage(Component.text("API publique: ").color(NamedTextColor.GRAY)
                .append(Component.text(
                                NeticPlugin.getInstance().getNeticAPI() != null
                                        ? "Activée" : "Désactivée")
                        .color(NeticPlugin.getInstance().getNeticAPI() != null
                                ? NamedTextColor.GREEN
                                : NamedTextColor.RED)));
    }

    /* ========================= STATS ========================= */

    private void sendStats(CommandSender sender) {

        NeticAPI api = NeticPlugin.getInstance().getNeticAPI();

        sender.sendMessage(Component.text("═══ NeticAI - Statistiques ═══")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        if (api == null) {
            sender.sendMessage(Component.text("❌ API publique désactivée")
                    .color(NamedTextColor.RED));
            return;
        }

        NeticAPI.ApiStats stats = api.getStats();

        sender.sendMessage(Component.text("📊 Requêtes API:")
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));

        sender.sendMessage(Component.text("  Total: ").color(NamedTextColor.GRAY)
                .append(Component.text(stats.getTotalRequests())
                        .color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("  Réussies: ").color(NamedTextColor.GRAY)
                .append(Component.text(stats.getSuccessfulRequests())
                        .color(NamedTextColor.GREEN)));

        sender.sendMessage(Component.text("  Échouées: ").color(NamedTextColor.GRAY)
                .append(Component.text(stats.getFailedRequests())
                        .color(NamedTextColor.RED)));

        sender.sendMessage(Component.text("  Rate limited: ").color(NamedTextColor.GRAY)
                .append(Component.text(stats.getRateLimitedRequests())
                        .color(NamedTextColor.YELLOW)));

        sender.sendMessage(Component.text("  Taux succès: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f%%", stats.getSuccessRate() * 100))
                        .color(NamedTextColor.GREEN)));

        CacheManager cacheManager = NeticPlugin.getInstance().getCacheManager();
        if (cacheManager.isEnabled()) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("💾 Cache:")
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD));

            sender.sendMessage(Component.text("  Entrées: ").color(NamedTextColor.GRAY)
                    .append(Component.text(cacheManager.size())
                            .color(NamedTextColor.WHITE)));

            sender.sendMessage(Component.text("  Depuis cache: ").color(NamedTextColor.GRAY)
                    .append(Component.text(stats.getCachedResponses())
                            .color(NamedTextColor.YELLOW)));

            CacheStats cacheStats = cacheManager.getStats();
            if (cacheStats != null) {
                sender.sendMessage(Component.text("  Hits: ").color(NamedTextColor.GRAY)
                        .append(Component.text(cacheStats.hitCount())
                                .color(NamedTextColor.GREEN)));

                sender.sendMessage(Component.text("  Misses: ").color(NamedTextColor.GRAY)
                        .append(Component.text(cacheStats.missCount())
                                .color(NamedTextColor.YELLOW)));
            }
        }
    }

    /* ========================= CACHE ========================= */

    private void handleCacheCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /netic cache <clear|stats>")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        CacheManager cacheManager = NeticPlugin.getInstance().getCacheManager();

        switch (args[1].toLowerCase()) {
            case "clear":
                cacheManager.clear();
                sender.sendMessage(Component.text("✅ Cache vidé")
                        .color(NamedTextColor.GREEN));
                break;

            case "stats":
                if (!cacheManager.isEnabled()) {
                    sender.sendMessage(Component.text("❌ Cache désactivé")
                            .color(NamedTextColor.RED));
                    return;
                }

                CacheStats stats = cacheManager.getStats();
                if (stats == null) {
                    sender.sendMessage(Component.text("❌ Aucune statistique disponible")
                            .color(NamedTextColor.RED));
                    return;
                }

                sender.sendMessage(Component.text("═══ Cache - Statistiques ═══")
                        .color(NamedTextColor.GOLD));

                sender.sendMessage(Component.text("Taille: " + cacheManager.size())
                        .color(NamedTextColor.YELLOW));

                sender.sendMessage(Component.text("Hits: " + stats.hitCount())
                        .color(NamedTextColor.GREEN));

                sender.sendMessage(Component.text("Misses: " + stats.missCount())
                        .color(NamedTextColor.RED));

                sender.sendMessage(Component.text("Taux hit: "
                                + String.format("%.1f%%", stats.hitRate() * 100))
                        .color(NamedTextColor.AQUA));
                break;

            default:
                sender.sendMessage(Component.text("❌ Sous-commande inconnue")
                        .color(NamedTextColor.RED));
                break;
        }
    }

    /* ========================= HELP ========================= */

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("═══ NeticAI v1.0-c1-beta - Admin ═══")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        sender.sendMessage(Component.text("/netic reset").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Réinitialise l'historique").color(NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("/netic setname <nom>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Change le nom de l'IA").color(NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("/netic reload").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Recharge la config").color(NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("/netic status").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Affiche le statut").color(NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("/netic stats").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Statistiques détaillées").color(NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("/netic cache <clear|stats>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Gestion du cache").color(NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("/netic clearcooldown").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Reset rate limits").color(NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("/netic update").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Vérifie les mises à jour").color(NamedTextColor.GRAY)));
    }

    /* ========================= TAB ========================= */

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return Arrays.asList(
                    "reset",
                    "setname",
                    "reload",
                    "status",
                    "stats",
                    "cache",
                    "clearcooldown",
                    "update"
            );
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("cache")) {
            return Arrays.asList("clear", "stats");
        }

        return new ArrayList<>();
    }
}
