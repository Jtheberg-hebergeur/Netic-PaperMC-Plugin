# NeticAI – The Smart AI for Minecraft

![Demo](https://cdn.modrinth.com/data/cached_images/450f3ec8db4543a6eb3c9e729964691934fe7e4f.png)

## Description
NeticAI brings the **Netic** artificial intelligence directly into your Minecraft server (Paper 1.21.x).  
With this plugin, your players can chat with an AI that understands the game context, gives useful tips, explains game mechanics, or simply engages in natural and immersive conversations.

## Key Features
- 🤖 **Conversational AI**: Chat with Netic using a simple chat trigger (`!ia`).
- ⏰ **Built-in anti-spam**: Configure a cooldown between messages to prevent abuse.
- 📝 **Custom AI behavior**: Modify the `system-prompt` to control Netic’s personality, tone, and role.
- 📚 **Conversation history**: Stores up to 20 messages in memory and in the database.
- 💾 **Flexible database support**: Supports SQLite (easy setup) and MariaDB (recommended for large servers).
- 🔑 **Secure Netic API**: Each server uses its own API key (available at [netic.jtheberg.cloud](https://netic.jtheberg.cloud)).

## Installation
1. Download the plugin and place it in your `plugins` folder.
2. Add your Netic API key to `config.yml`.
3. Configure the plugin options (AI name, chat trigger, cooldown, database type).
4. Restart your server and use the chat trigger to talk with Netic.

## Important Notes
- All players share the same conversation.
- Conversation history is saved both in memory and in the database.
- Compatible with **Paper 1.21.x**.
