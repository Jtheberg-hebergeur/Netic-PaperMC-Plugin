package cloud.jtheberg.netic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                String prompt = NeticPlugin.getInstance().getConfig().getString("ia.system-prompt", "");
                HistoryManager.updateSystemPrompt(prompt + "\n\nTon nom est " + newName);
                sender.sendMessage(Component.text("✅ Nom changé: " + newName).color(NamedTextColor.GREEN));
                break;

            case "reload":
                NeticPlugin.getInstance().reloadConfiguration();
                sender.sendMessage(Component.text("✅ Configuration rechargée").color(NamedTextColor.GREEN));
                break;

            case "status":
                String iaName = NeticPlugin.getInstance().getConfig().getString("ia.name");
                String trigger = NeticPlugin.getInstance().getConfig().getString("ia.trigger");
                int historySize = HistoryManager.size();
                int maxHistory = NeticPlugin.getInstance().getConfig().getInt("history.max-messages");
                sender.sendMessage(Component.text("═══ Statut NeticAI ═══").color(NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Nom: " + iaName).color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Trigger: " + trigger).color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Historique: " + historySize + "/" + maxHistory).color(NamedTextColor.YELLOW));
                break;

            case "clearcooldown":
                CooldownManager.reset();
                sender.sendMessage(Component.text("✅ Cooldown réinitialisé").color(NamedTextColor.GREEN));
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("═══ NeticAI Admin ═══").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/netic reset - Réinitialise l'historique").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/netic setname <nom> - Change le nom").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/netic reload - Recharge la config").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/netic status - Affiche le statut").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/netic clearcooldown - Reset cooldown").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reset", "setname", "reload", "status", "clearcooldown");
        }
        return new ArrayList<>();
    }
}