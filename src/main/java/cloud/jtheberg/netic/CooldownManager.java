package cloud.jtheberg.netic;

/**
 * Gestionnaire de cooldown global pour éviter le spam
 * Un seul cooldown pour tout le serveur
 *
 * @author Jtheberg
 */
public class CooldownManager {

    private static long lastRequestTime = 0;
    private static boolean isProcessing = false;

    /**
     * Vérifie si une nouvelle requête peut être envoyée
     */
    public static synchronized boolean canSend(long delayMs) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;

        // Si une requête est déjà en cours
        if (isProcessing) {
            return false;
        }

        // Vérifier le délai minimum
        if (timeSinceLastRequest < delayMs) {
            return false;
        }

        // Autoriser la requête
        lastRequestTime = currentTime;
        isProcessing = true;
        return true;
    }

    /**
     * Libère le verrou après traitement d'une requête
     */
    public static synchronized void releaseRequest() {
        isProcessing = false;
    }

    /**
     * Obtient le temps restant avant la prochaine requête
     */
    public static synchronized int getRemainingSeconds(long delayMs) {
        if (isProcessing) {
            return -1; // En cours de traitement
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        long remaining = delayMs - timeSinceLastRequest;

        if (remaining <= 0) {
            return 0;
        }

        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Réinitialise complètement le cooldown
     */
    public static synchronized void reset() {
        lastRequestTime = 0;
        isProcessing = false;
    }
}