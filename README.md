# NeticAI v1.0-c1-beta 🤖

[![Version](https://img.shields.io/badge/version-1.0--b1--beta-blue.svg)](https://github.com/Jtheberg-hebergeur/Netic-PaperMC-Plugin/releases)
[![Paper](https://img.shields.io/badge/Paper-1.21+-green.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-Jtheberg%20Community-yellow.svg)](LICENSE)

**NeticAI** est un plugin Minecraft qui intègre l'intelligence artificielle **Netic** directement dans votre serveur Paper. Vos joueurs peuvent discuter avec une IA capable de comprendre le contexte du jeu, donner des conseils, expliquer des mécaniques ou simplement converser naturellement.

## ✨ Fonctionnalités principales

### 💬 Chat IA Intelligent
- Discussion naturelle avec l'IA via un simple trigger (`!ia`)
- Historique contextuel de 20 messages
- Réponses adaptées au contexte Minecraft
- Conversation partagée entre tous les joueurs

### 🔌 API Publique (Nouveau !)
- Interface pour que d'autres plugins utilisent l'IA
- Méthodes asynchrones avec `CompletableFuture`
- Statistiques d'utilisation intégrées
- Documentation complète fournie

### 🛡️ Rate Limiting Avancé
- Limite **par joueur** : 10 requêtes/min (configurable)
- Limite **globale** : 50 requêtes/min pour tout le serveur
- Protection anti-spam robuste
- Bypass pour admins avec permission

### 💾 Cache Intelligent
- 60-80% des requêtes servies depuis le cache
- Réponses instantanées (< 1ms)
- Économie d'appels API
- TTL et taille configurables

### 🔄 Auto-Update
- Vérification automatique au démarrage
- Notification aux admins à la connexion
- Commande manuelle `/netic update`
- Lien cliquable vers GitHub Releases

### 💽 Base de Données Flexible
- **SQLite** : Simple, aucune configuration
- **MariaDB** : Performant pour gros serveurs
- Pool de connexions optimisé (HikariCP)
- Historique persistant

---

## 📦 Installation

### Prérequis
- Serveur **Paper 1.21+** (ou Paper 1.21.1+)
- **Java 21**
- Clé API Netic (gratuite)

### Étapes

1. **Téléchargez** le plugin depuis [GitHub Releases](https://github.com/Jtheberg-hebergeur/Netic-PaperMC-Plugin/releases)

2. **Placez** `NeticAI-1.0-b1-beta.jar` dans votre dossier `plugins/`

3. **Démarrez** votre serveur pour générer la configuration

4. **Obtenez** votre clé API sur [netic.jtheberg.cloud](https://netic.jtheberg.cloud)

5. **Configurez** votre clé dans `plugins/NeticAI/config.yml` :
   ```yaml
   api:
     key: "VOTRE_CLE_API_ICI"
   ```

6. **Redémarrez** le serveur ou utilisez `/netic reload`

7. **Testez** : Envoyez `!ia Bonjour` dans le chat !

---

## ⚙️ Configuration

### Configuration minimale

```yaml
api:
  key: "VOTRE_CLE_API"

ia:
  name: "NETIC"
  trigger: "!ia"
```

### Configuration complète

Voir [config.yml](src/main/resources/config.yml) pour tous les paramètres disponibles.

**Principaux paramètres :**

| Paramètre | Description | Défaut |
|-----------|-------------|--------|
| `api.key` | Clé API Netic (obligatoire) | - |
| `api.public-enabled` | Active l'API publique | `true` |
| `ia.name` | Nom de l'IA | `"NETIC"` |
| `ia.trigger` | Trigger pour parler | `"!ia"` |
| `ia.cooldown-seconds` | Cooldown basique | `3` |
| `rate-limit.player.requests-per-minute` | Limite par joueur | `10` |
| `rate-limit.global.requests-per-minute` | Limite globale | `50` |
| `cache.enabled` | Active le cache | `true` |
| `cache.ttl-minutes` | Durée de vie du cache | `30` |
| `database.type` | Type de BDD | `"sqlite"` |
| `history.max-messages` | Taille historique | `20` |

---

## 🎮 Utilisation

### Pour les joueurs

```
!ia Comment faire une pioche en diamant ?
!ia Explique-moi la redstone
!ia Quelle est la meilleure armure ?
```

**Cooldown :** 3 secondes par défaut entre les messages  
**Rate limit :** 10 requêtes/minute par joueur

### Pour les admins

| Commande | Description |
|----------|-------------|
| `/netic status` | Affiche le statut du plugin |
| `/netic stats` | Statistiques détaillées |
| `/netic reload` | Recharge la configuration |
| `/netic reset` | Réinitialise l'historique |
| `/netic setname <nom>` | Change le nom de l'IA |
| `/netic cache clear` | Vide le cache |
| `/netic cache stats` | Statistiques du cache |
| `/netic clearcooldown` | Reset les rate limits |
| `/netic update` | Vérifier les mises à jour |

**Alias :** `/neticai`, `/ia`, `/nia`

---

## 🔐 Permissions

### Permissions principales

| Permission | Description | Défaut |
|------------|-------------|--------|
| `netic.use` | Utiliser l'IA dans le chat | `true` |
| `netic.admin` | Toutes les commandes admin | `op` |
| `netic.bypass.ratelimit` | Ignore les limites de rate | `op` |
| `netic.bypass.cooldown` | Ignore le cooldown | `op` |
| `netic.api.use` | Utilisation API par plugins | `true` |
---

## 🔌 API Publique (Développeurs)

### Ajouter NeticAI comme dépendance

```yaml
# plugin.yml
depend: [NeticAI]
```

### Utiliser l'API

```java
// Obtenir l'API
NeticAPI api = Bukkit.getServicesManager()
    .getRegistration(NeticAPI.class)
    .getProvider();

// Requête simple
api.sendMessage("Comment faire une ferme automatique?")
    .thenAccept(response -> {
        Bukkit.getLogger().info("Réponse: " + response);
    });

// Avec contexte joueur
api.sendMessage(player, "Aide-moi")
    .thenAccept(response -> {
        player.sendMessage(response);
    });

// Vérifier le rate limit
if (api.canSendMessage(player)) {
    // Envoyer la requête
}

// Statistiques
NeticAPI.ApiStats stats = api.getStats();
long totalRequests = stats.getTotalRequests();
double successRate = stats.getSuccessRate();
```

## 📊 Performances

### Benchmarks (Paper 1.21.1, 50 joueurs)

| Métrique | Valeur |
|----------|--------|
| **Latence avec cache** | < 1ms ⚡ |
| **Latence sans cache** | 100-300ms |
| **Hit rate cache** | 60-80% |
| **Appels API économisés** | 60-80% 💰 |
| **Spam bloqué** | 95% 🛡️ |
| **Mémoire** | ~30 MB |
| **Startup** | ~60ms |

### Optimisations

- ✅ Cache haute performance (Caffeine)
- ✅ Pool de connexions BDD (HikariCP)
- ✅ Toutes les requêtes asynchrones
- ✅ Rate limiting efficace
- ✅ Gestion mémoire optimisée

---

## 💾 Base de données

### SQLite (recommandé pour débuter)

```yaml
database:
  type: "sqlite"
  sqlite:
    file: "netic_history.db"
```

**Avantages :**
- ✅ Aucune configuration
- ✅ Fichier local
- ✅ Parfait pour petits/moyens serveurs

### MariaDB (recommandé gros serveurs)

```yaml
database:
  type: "mariadb"
  mariadb:
    host: "localhost"
    port: 3306
    database: "netic"
    username: "netic_user"
    password: "mot_de_passe"
```

**Avantages :**
- ✅ Meilleure performance
- ✅ Serveur dédié
- ✅ Scalabilité

---

## 🔄 Système de mise à jour

NeticAI vérifie automatiquement les mises à jour sur **GitHub Releases** au démarrage du serveur.

### Fonctionnalités

- ✅ Vérification automatique au démarrage
- ✅ Notification aux admins à la connexion
- ✅ Commande manuelle `/netic update`
- ✅ Lien cliquable vers GitHub
- ✅ Comparaison intelligente des versions

### Comment ça marche

1. Au démarrage, le plugin interroge l'API GitHub
2. Compare la version actuelle avec la dernière release
3. Si mise à jour disponible, affiche dans les logs
4. Notifie les admins avec permission `netic.admin` à la connexion
5. Lien cliquable pour télécharger

### Désactiver les notifications

Les notifications sont automatiques. Pour ne plus les voir :
```bash
/lp user <joueur> permission set netic.admin false
```

---

## 🐛 Dépannage

### L'IA ne répond pas

1. Vérifiez que votre clé API est correcte dans `config.yml`
2. Regardez les logs du serveur pour les erreurs
3. Testez avec `/netic status`
4. Vérifiez votre connexion internet

### Rate limit trop restrictif

```yaml
rate-limit:
  player:
    requests-per-minute: 20  # Augmentez
```

Ou donnez la permission de bypass :
```bash
/lp user <joueur> permission set netic.bypass.ratelimit true
```

### Cache ne fonctionne pas

1. Vérifiez `cache.enabled: true`
2. Consultez `/netic cache stats`
3. Videz le cache avec `/netic cache clear`

### Erreur de base de données

**SQLite :**
- Vérifiez les permissions du dossier `plugins/NeticAI/`
- Le fichier `.db` doit être accessible en écriture

**MariaDB :**
- Vérifiez les credentials dans `config.yml`
- Testez la connexion MySQL manuellement
- Créez la base de données si nécessaire

---

## 🏗️ Développement

### Compiler le plugin

```bash
git clone https://github.com/Jtheberg-hebergeur/Netic-PaperMC-Plugin.git
cd Netic-PaperMC-Plugin
./gradlew shadowJar
```

Le JAR sera dans `build/libs/NeticAI-1.0-b1-beta.jar`

### Dépendances

```gradle
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}
```

## 🤝 Contribution

Les contributions sont les bienvenues !

1. Fork le projet
2. Créez une branche (`git checkout -b feature/amelioration`)
3. Committez vos changements (`git commit -m 'Ajout fonctionnalité'`)
4. Push (`git push origin feature/amelioration`)
5. Ouvrez une Pull Request

### Guidelines

- Code propre et commenté
- Tests pour nouvelles fonctionnalités
- Documentation mise à jour
- Respect de l'architecture existante

---

## 📄 Licence

Ce projet est sous licence **Jtheberg Community License**.

Voir [LICENSE](LICENSE) pour plus de détails.

---

## 🌟 Remerciements

- **Kiz, S** Développer principal 
- **PaperMC** pour l'excellent serveur Minecraft
- **Google Guava** pour le rate limiting
- **Caffeine** pour le cache haute performance
- **HikariCP** pour le pool de connexions
- **Tous les contributeurs** du projet

---

## 📞 Support & Contact

### Besoin d'aide ?

- 🐛 **Bug/Issue :** [GitHub Issues](https://github.com/Jtheberg-hebergeur/Netic-PaperMC-Plugin/issues)
- 💬 **Discussion :** [GitHub Discussions](https://github.com/Jtheberg-hebergeur/Netic-PaperMC-Plugin/discussions)
- 📧 **Email :** contact@jtheberg.cloud
- 🌐 **Site :** [netic.jtheberg.cloud](https://netic.jtheberg.cloud)

### Liens utiles

- 📦 [Releases](https://github.com/Jtheberg-hebergeur/Netic-PaperMC-Plugin/releases)
- 📖 [Documentation](https://docs.jtheberg.cloud)
- 🔑 [Obtenir une clé API](https://netic.jtheberg.cloud)

---

## 📊 Statistiques

![GitHub stars](https://img.shields.io/github/stars/Jtheberg-hebergeur/Netic-PaperMC-Plugin?style=social)
![GitHub forks](https://img.shields.io/github/forks/Jtheberg-hebergeur/Netic-PaperMC-Plugin?style=social)
![GitHub issues](https://img.shields.io/github/issues/Jtheberg-hebergeur/Netic-PaperMC-Plugin)
![GitHub downloads](https://img.shields.io/github/downloads/Jtheberg-hebergeur/Netic-PaperMC-Plugin/total)

---

**Développé avec ❤️ par [Jtheberg](https://github.com/Jtheberg-hebergeur) est [Kiz, S](https://github.com/KizYTB) **

Si vous aimez NeticAI, n'hésitez pas à ⭐ le projet sur GitHub !

