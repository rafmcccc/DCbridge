# DCbridge

A Paper plugin that runs your Discord bot **inside** the Minecraft server.
No second process, no hosting a bot separately, it starts with the server and
shuts down with it. Handles whitelisting, live server-status embeds, presence,
and a couple of lightweight anti-exploit/webhook checks.

The name is the whole pitch: **DC ↔ bridge**.

---

## Why run the bot in the server

- Reads stats straight from Bukkit, online count, version, average ping, TPS.
  No polling a remote status API.
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
  authorized-user-id: ""       # owner for the message-deletion tool
```

4. Restart the server. Run `/status setup` in the channel you want the live
   stats embed in, and `/whitelist-setup` to post the verification button.

## Commands

| Command | Permission | What it does |
|---|---|---|
| `/status setup` | dcbridge.admin | Posts the auto-updating status embed in the current channel |
| `/status remove` | dcbridge.admin | Removes the status embed |
| `/whitelist-setup` | dcbridge.admin | Posts the whitelist verify embed + button |

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
| `stats.embed-interval-seconds` | 30 | How often the status embed refreshes |
| `stats.color-online/offline` | — | Embed accent colors |
| `presence.format` | `{emoji} {name} \| {online}/{max} Players \| {ping}ms` | Bot "playing" text; tokens get swapped |
| `admin.remove-keywords` | `remove ts,delete ts` | Mention the bot + keyword to delete a message |
| `admin.user-remove-keywords` | `user remove ts` | Delete the bot's reply to a message |
| `admin.auto-delete-seconds` | 5 | Auto-cleanup delay for confirmations |
| `data.sqlite-file` | `whitelist.db` | Where requests/players are stored |
| `data.stats-file` | `stats.json` | Guild → {channel, message} mapping |

Message strings (`form-title`, `approved-dm`, `denied-dm`, …) are all in
`config.yml`. DM templates take `{username}` and `{platform}`.

## CheckHacks integration (anti-exploit)

This bundles a small, self-contained module that watches for a couple of cheap
server-side signals  sign-text tampering and teleport-distance moves, rate
limited per player, and forwards hits to a Discord webhook if
`checkhacks.webhook-url` is set. It's a lightweight heuristic, **not** a
replacement for a real anti-cheat.

```yaml
checkhacks:
  enabled: false
  threshold: 0.6        # blocks travelled between moves above this are flagged
  cooldown-ms: 5000
  webhook-url: ""       # Discord webhook for alerts
  alert-template: "🚨 CheckHacks alert: {player} triggered {reason} | {details}"
```

Only triggers for players lacking `dcbridge.checkhacks.bypass` and ops are always
ignored.

### Credit

The detection approach here is inspired by the ideas in
**[branduzzo/CheckHacks](https://github.com/branduzzo/CheckHacks)**   go check it
out. If you want a full anti-cheat this repo doesn't claim to be one; use the
real thing.

## Tech

- [Paper](https://papermc.io) / Spigot, `api-version: 1.16` (Java 17+)
- [JDA](https://github.com/discord-jda/JDA) for the Discord client
- `sqlite-jdbc` for persistence
- Maven Shade to ship one jar