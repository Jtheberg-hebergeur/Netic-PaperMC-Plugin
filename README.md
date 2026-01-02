# NeticAI – L’IA intelligente pour Minecraft

**Description :**  
NeticAI intègre l’intelligence artificielle **Netic** directement dans votre serveur Minecraft (Paper 1.21.x). Grâce à ce plugin, vos joueurs peuvent discuter avec une IA capable de comprendre le contexte du jeu, donner des conseils, expliquer des mécaniques ou simplement discuter de manière naturelle et engageante.  

## Fonctionnalités principales
- 🤖 **IA conversationnelle** : Discutez avec Netic via un simple trigger dans le chat (`!ia`).  
- ⏰ **Anti-spam intégré** : Définissez un délai entre les messages pour éviter les abus.  
- 📝 **Personnalisation du comportement de l’IA** : Modifiez le `system-prompt` pour ajuster le style et le ton de Netic.  
- 📚 **Historique des conversations** : Gardez jusqu’à 20 messages en mémoire et en base de données.  
- 💾 **Base de données flexible** : Supporte SQLite (simple) ou MariaDB (performant pour gros serveurs).  
- 🔑 **API Netic sécurisée** : Chaque serveur utilise sa propre clé API (à obtenir sur [netic.jtheberg.cloud](https://netic.jtheberg.cloud)).

## Installation
1. Téléchargez et placez le plugin dans votre dossier `plugins`.  
2. Remplissez votre clé API dans `config.yml`.  
3. Configurez les options selon vos besoins (nom de l’IA, trigger, délai, type de base de données).  
4. Redémarrez votre serveur et utilisez la commande de chat pour parler à Netic.

## Notes importantes
- Tous les joueurs partagent la même conversation.  
- L’historique est sauvegardé en mémoire et dans la base de données.  
- Le plugin fonctionne sur **Paper 1.21.x**.
