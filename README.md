# DCbridge

A Paper plugin that runs your Discord bot **inside** the Minecraft server.
No second process, no hosting a bot separately, it starts with the server and
shuts down with it. Handles Discord-driven whitelisting with automatic
persistence and recovery.

The name is the whole pitch: **DC ↔ bridge**.

---

## Why run the bot in the server

- Approving a whitelist request calls the real `setWhitelisted(true)`. The
  vanilla whitelist is the source of truth.
- One process, one config file, one place to restart.
- If your server is down, the bot is down — which is usually the honest state
  anyway.

---

## Build

You need JDK 17+ and Maven.

```bash
cd dcbridge-plugin
mvn package
```

You'll get `target/DCbridge.jar`. Drop it into your server's `plugins/` folder,
start the server once to generate `DCbridge/config.yml`, then fill it in.

## Setup

1. Create a bot in the [Discord Developer Portal](https://discord.com/developers/applications)
   and copy the token. Enable the **Message Content** intent.
2. Invite the bot to your server with the `applications.commands` and
   `bot` scopes.
3. Edit `config.yml`:

```yaml
discord:
  token: ""                    # your bot token
  client-id: ""
  guild-id: ""                 # optional, scopes slash commands to one guild
  channels:
    whitelist: ""              # where the verify embed goes
    whitelist-log: ""
    whitelist-queue: ""
  roles:
    whitelist: ""
    whitelist-admin: ""
  authorized-user-id: ""       # owner for admin commands
```

4. Restart the server. Run `/whitelist-setup` to run the interactive wizard
   (picks channels/roles), which also posts the verification button.

## Commands

| Command | Permission | What it does |
|---|---|---|
| `/whitelist-setup` | dcbridge.admin | Interactive Discord wizard to configure channels/roles and post the verify embed |
| `/wl-remove` | dcbridge.admin | Revokes a player's whitelist by username |

Serve a role named `dcbridge.admin` (or set it via LuckPerms) to use these.

## How the whitelist flow works

1. Player clicks the **Verify** button in the whitelist channel.
2. A modal asks for platform (Java/Bedrock) and their Minecraft username.
3. The request shows up in the queue channel with **Done/Cancel** buttons.
4. **Done** → the plugin whitelists that username on the server, grants the
   Discord whitelist role, and DMs the player. **Cancel** → denies and DMs.

A few deliberate decisions:

- **Re-submission:** if someone already holds the whitelist role they can submit
  again (e.g. to change username). The old active entry is retired, not deleted,
  so there's never two active records for one user.
- **Geyser:** Bedrock players join with a prefix (default `.`), so `Steve` joins
  as `.Steve`. Set `whitelist.geyser-prefix` and the plugin strips it when
  matching, but whitelists the *actual* name.
- **Role idempotency:** re-adding the role to someone who already has it is a no-op.

## config.yml reference

| Key | Default | Notes |
|---|---|---|
| `whitelist.mode` | `strict` | `strict` kicks non-whitelisted; `notify` lets them in but warns |
| `whitelist.geyser-prefix` | `.` | Strip this prefix for Bedrock names |
| `whitelist.username-min/max` | 3 / 16 | Minecraft username length bounds |
| `admin.remove-keywords` | `remove ts,delete ts` | Mention the bot + keyword to delete a message |
| `admin.user-remove-keywords` | `user remove ts` | Delete the bot's reply to a message |
| `admin.auto-delete-seconds` | 5 | Auto-cleanup delay for confirmations |
| `data.sqlite-file` | `whitelist.db` | Where requests/players are stored |

Message strings (`form-title`, `approved-dm`, `denied-dm`, …) are all in
`config.yml`. DM templates take `{username}` and `{platform}`.

## Reliability features (v3.1.0+)

- **Whitelist restore on startup** — the SQLite database is the durable source of
  truth. On every server start, all active whitelisted players are re-applied to
  the vanilla whitelist, surviving restarts even if `whitelist.json` wasn't saved.
- **SQLite auto-recovery** — if the database connection drops (lock, corruption,
  disk hiccup), the plugin transparently reconnects and retries the query once
  before failing closed. Legitimate whitelisted players are never kicked due to
  transient DB errors.
- **Async bot startup** — the Discord bot connects in the background without
  blocking the server main thread. A bad token or network issue will never hang
  the server boot.

## Tech

- [Paper](https://papermc.io) / Spigot, `api-version: 1.21` (Java 17+)
- [JDA](https://github.com/discord-jda/JDA) for the Discord client
- `sqlite-jdbc` for persistence
- Maven Shade to ship one jar