package net.slqmy.rank_system;

import net.slqmy.rank_system.managers.NameTagManager;
import net.slqmy.rank_system.managers.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.UUID;

public final class EventListener implements Listener {
	private final Main plugin;
	private final RankManager rankManager;
	private final NameTagManager nameTagManager;
	private final HashMap<UUID, PermissionAttachment> permissions;

	EventListener(final Main plugin) {
		this.plugin = plugin;
		this.rankManager = plugin.getRankManager();
		this.nameTagManager = plugin.getNameTagManager();
		this.permissions = rankManager.getPermissions();
	}

	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event) {
		final YamlConfiguration config = (YamlConfiguration) plugin.getConfig();
		final String defaultRank = config.getString("defaultRank");
		final Player player = event.getPlayer();
		final UUID playerUUID = player.getUniqueId();

		// Attempt to assign the player the default rank, if it exists.
		if (defaultRank != null && !player.hasPlayedBefore()) {
			final boolean success = rankManager.setRank(playerUUID, defaultRank, true);

			if (!success) {
				System.out.println("[Rank-System] Invalid configuration! Default rank does not exist in rank list.");
			}
		}

		// Give the player a scoreboard with nametags of other players.
		nameTagManager.setNameTags(player);
		// Add the player to everyone else's scoreboard.
		nameTagManager.addNewNameTag(player);

		// Add permissions to the player.
		final PermissionAttachment attachment;

		if (permissions.containsKey(playerUUID)) {
			attachment = permissions.get(playerUUID);
		} else {
			attachment = player.addAttachment(plugin);
			permissions.put(playerUUID, attachment);
		}

		for (final String permission : rankManager.getRank(playerUUID).getPermissions()) {
			attachment.setPermission(permission, true);
		}
	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		final UUID playerUUID = player.getUniqueId();

		// Remove player from other player's scoreboard and from the hashmap.
		nameTagManager.removeNameTag(player);
		permissions.remove(playerUUID, permissions.get(playerUUID));
	}

	@EventHandler
	public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
		// Custom chat message:
		// (rank formatting) <rank name> (white) <player> » (grey) <message>
		event.setCancelled(true);

		final Player player = event.getPlayer();

		Bukkit.broadcastMessage(rankManager.getRank(player.getUniqueId()).getDisplayName() + " " + player.getName() + ChatColor.BOLD +  " » " + ChatColor.GRAY + event.getMessage());
	}
}