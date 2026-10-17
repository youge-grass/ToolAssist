package dev.yogur.toolassist.server;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

final class SyncListener implements Listener {
    private final YogurToolAssistServer plugin;
    SyncListener(YogurToolAssistServer plugin) { this.plugin = plugin; }
    @EventHandler public void onJoin(PlayerJoinEvent event) { plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.sync(event.getPlayer()), 1L); }
}
