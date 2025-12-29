package cloud.jtheberg.netic;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Écoute les messages du chat pour détecter le trigger de l'IA
 *
 * @author Jtheberg
 */
public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        // Convertir le message en texte simple
        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());

        // Récupérer le trigger
        String trigger = NeticPlugin.getInstance().getConfig().getString("ia.trigger", "!ia");

        // Vérifier si le message commence par le trigger
        if (!message.startsWith(trigger + " ")) {
            return;
        }

        // NE PAS ANNULER L'EVENT - Le message reste visible dans le chat

        // Vérifier la clé API
        String apiKey = NeticPlugin.getInstance().getConfig().getString("api.key", "");
        if (apiKey.equals("METS_TA_CLE_API_ICI") || apiKey.isEmpty()) {
            event.getPlayer().sendMessage(
                    Component.text("❌ Clé API non configurée. Contactez un administrateur.")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        // Vérifier le cooldown
        long cooldownSeconds = NeticPlugin.getInstance().getConfig().getLong("ia.cooldown-seconds", 5);
        long cooldownMs = cooldownSeconds * 1000;

        if (!CooldownManager.canSend(cooldownMs)) {
            int remaining = CooldownManager.getRemainingSeconds(cooldownMs);

            if (remaining == -1) {
                event.getPlayer().sendMessage(
                        Component.text("⏳ L'IA réfléchit déjà, patiente un instant...")
                                .color(NamedTextColor.YELLOW)
                );
            } else {
                event.getPlayer().sendMessage(
                        Component.text("⏰ Attends encore " + remaining + " seconde(s)")
                                .color(NamedTextColor.GOLD)
                );
            }
            return;
        }

        // Extraire la question
        String question = message.substring(trigger.length()).trim();

        if (question.isEmpty()) {
            event.getPlayer().sendMessage(
                    Component.text("💬 Usage: " + trigger + " <ta question>")
                            .color(NamedTextColor.GRAY)
            );
            CooldownManager.releaseRequest();
            return;
        }

        // Récupérer le nom de l'IA
        String iaName = NeticPlugin.getInstance().getConfig().getString("ia.name", "NETIC");
        String playerName = event.getPlayer().getName();

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
        NeticClient.askIA(
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
                    CooldownManager.releaseRequest();
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
                    CooldownManager.releaseRequest();
                }
        );
    }
}