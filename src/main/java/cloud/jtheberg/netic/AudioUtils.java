package cloud.jtheberg.netic;

import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utilitaires pour la gestion des fichiers audio
 */
public class AudioUtils {

    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList(
        "webm", "mp3", "wav", "ogg", "m4a", "flac"
    ));

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB

    /**
     * Vérifie si le format de fichier est supporté
     */
    public static boolean isSupportedFormat(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(fileName);
        return extension != null && SUPPORTED_FORMATS.contains(extension.toLowerCase());
    }

    /**
     * Vérifie si la taille du fichier est valide
     */
    public static boolean isValidFileSize(File file) {
        return file != null && file.exists() && file.length() <= MAX_FILE_SIZE;
    }

    /**
     * Obtient l'extension d'un fichier
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }
        
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * Crée un répertoire pour les fichiers audio si nécessaire
     */
    public static Path createAudioDirectory() throws IOException {
        Path audioDir = Paths.get(NeticPlugin.getInstance().getDataFolder().getPath(), "audio");
        if (!Files.exists(audioDir)) {
            Files.createDirectories(audioDir);
        }
        return audioDir;
    }

    /**
     * Génère un nom de fichier unique pour un audio
     */
    public static String generateUniqueFileName(Player player, String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String playerName = player != null ? player.getName() : "unknown";
        String extension = getFileExtension(originalFileName);
        
        return String.format("audio_%s_%s_%s.%s", 
            playerName, 
            player != null ? player.getUniqueId().toString().substring(0, 8) : UUID.randomUUID().toString().substring(0, 8),
            timestamp,
            extension != null ? extension : "webm"
        );
    }

    /**
     * Sauvegarde un fichier audio dans le répertoire audio du plugin
     */
    public static File saveAudioFile(Player player, File sourceFile) throws IOException {
        if (!isValidFileSize(sourceFile)) {
            throw new IOException("Fichier audio invalide ou trop volumineux (max 25MB)");
        }

        if (!isSupportedFormat(sourceFile.getName())) {
            throw new IOException("Format de fichier non supporté: " + getFileExtension(sourceFile.getName()));
        }

        Path audioDir = createAudioDirectory();
        String uniqueFileName = generateUniqueFileName(player, sourceFile.getName());
        Path targetPath = audioDir.resolve(uniqueFileName);

        Files.copy(sourceFile.toPath(), targetPath);
        
        NeticPlugin.getInstance().getLogger().info("🎵 Fichier audio sauvegardé: " + targetPath);
        
        return targetPath.toFile();
    }

    /**
     * Supprime les anciens fichiers audio (plus de 7 jours)
     */
    public static void cleanupOldAudioFiles() {
        try {
            Path audioDir = createAudioDirectory();
            long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
            
            Files.list(audioDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < sevenDaysAgo;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        NeticPlugin.getInstance().getLogger().info("🗑️ Ancien fichier audio supprimé: " + path.getFileName());
                    } catch (IOException e) {
                        NeticPlugin.getInstance().getLogger().warning("❌ Impossible de supprimer l'ancien fichier audio: " + path.getFileName());
                    }
                });
                
        } catch (IOException e) {
            NeticPlugin.getInstance().getLogger().warning("❌ Erreur lors du nettoyage des anciens fichiers audio: " + e.getMessage());
        }
    }

    /**
     * Obtient des informations sur un fichier audio
     */
    public static AudioFileInfo getAudioFileInfo(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        return new AudioFileInfo(
            file.getName(),
            getFileExtension(file.getName()),
            file.length(),
            file.lastModified()
        );
    }

    /**
     * Classe pour stocker les informations d'un fichier audio
     */
    public static class AudioFileInfo {
        private final String fileName;
        private final String format;
        private final long size;
        private final long lastModified;

        public AudioFileInfo(String fileName, String format, long size, long lastModified) {
            this.fileName = fileName;
            this.format = format;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFormat() {
            return format;
        }

        public long getSize() {
            return size;
        }

        public String getFormattedSize() {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else {
                return String.format("%.1f MB", size / (1024.0 * 1024.0));
            }
        }

        public long getLastModified() {
            return lastModified;
        }

        @Override
        public String toString() {
            return String.format("%s (%s, %s)", fileName, format, getFormattedSize());
        }
    }

    /**
     * Formate la durée en millisecondes en format lisible
     */
    public static String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60 * 1000) {
            return String.format("%.1fs", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / (60 * 1000);
            long seconds = (milliseconds % (60 * 1000)) / 1000;
            return String.format("%dm%ds", minutes, seconds);
        }
    }

    /**
     * Valide complètement un fichier audio
     */
    public static ValidationResult validateAudioFile(File file) {
        if (file == null) {
            return new ValidationResult(false, "Fichier null");
        }

        if (!file.exists()) {
            return new ValidationResult(false, "Le fichier n'existe pas");
        }

        if (!file.isFile()) {
            return new ValidationResult(false, "Le chemin n'est pas un fichier");
        }

        if (!isValidFileSize(file)) {
            return new ValidationResult(false, "Fichier trop volumineux (max 25MB)");
        }

        if (!isSupportedFormat(file.getName())) {
            return new ValidationResult(false, "Format non supporté. Formats supportés: " + String.join(", ", SUPPORTED_FORMATS));
        }

        return new ValidationResult(true, "Fichier valide");
    }

    /**
     * Résultat de validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
