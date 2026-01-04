package cloud.jtheberg.netic;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Écoute les messages du chat pour détecter le trigger de l'IA
 */
public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        String trigger = NeticPlugin.getInstance().getConfig().getString("ia.trigger", "!ia");

        if (!message.startsWith(trigger + " ")) {
            return;
        }

        Player player = event.getPlayer();

        // Vérifier la clé API
        String apiKey = NeticPlugin.getInstance().getConfig().getString("api.key", "");
        if (apiKey.equals("METS_TA_CLE_API_ICI") || apiKey.isEmpty()) {
            player.sendMessage(
                    Component.text("❌ Clé API non configurée. Contactez un administrateur.")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        // Vérifier le rate limit (v2.0)
        if (!NeticPlugin.getInstance().getRateLimitManager().tryAcquire(player)) {
            long remaining = NeticPlugin.getInstance().getRateLimitManager().getRemainingCooldown(player);

            if (remaining > 0) {
                player.sendMessage(
                        Component.text("⏰ Attendez encore " + remaining + " seconde(s)")
                                .color(NamedTextColor.GOLD)
                );
            } else {
                player.sendMessage(
                        Component.text("⏳ Trop de requêtes, ralentissez un peu!")
                                .color(NamedTextColor.RED)
                );
            }
            return;
        }

        // Extraire la question
        String question = message.substring(trigger.length()).trim();

        if (question.isEmpty()) {
            player.sendMessage(
                    Component.text("💬 Usage: " + trigger + " <ta question>")
                            .color(NamedTextColor.GRAY)
            );
            return;
        }

        String iaName = NeticPlugin.getInstance().getConfig().getString("ia.name", "NETIC");
        String playerName = player.getName();

        // Ajouter la question à l'historique
        HistoryManager.add("Joueur " + playerName, question);

        // Message d'attente
        Bukkit.broadcast(
                Component.text("💭 [")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(iaName).color(NamedTextColor.AQUA))
                        .append(Component.text("] Réflexion en cours...").color(NamedTextColor.GRAY))
        );

        // Appeler l'API
        NeticPlugin.getInstance().getNeticClient().askIA(
                question,
                // Callback succès
                response -> {
                    HistoryManager.add("IA", response);
                    Bukkit.broadcast(
                            Component.text("[")
                                    .color(NamedTextColor.GREEN)
                                    .append(Component.text(iaName).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                                    .append(Component.text("] ").color(NamedTextColor.GREEN))
                                    .append(Component.text(response).color(NamedTextColor.WHITE))
                    );
                },
                // Callback erreur
                error -> {
                    Bukkit.broadcast(
                            Component.text("❌ [")
                                    .color(NamedTextColor.RED)
                                    .append(Component.text(iaName).color(NamedTextColor.AQUA))
                                    .append(Component.text("] ").color(NamedTextColor.RED))
                                    .append(Component.text(error).color(NamedTextColor.DARK_RED))
                    );
                }
        );
    }
}