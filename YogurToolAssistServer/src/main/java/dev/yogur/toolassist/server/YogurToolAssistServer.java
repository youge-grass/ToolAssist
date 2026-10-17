package dev.yogur.toolassist.server;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class YogurToolAssistServer extends JavaPlugin {
    private static final String CHANNEL = "yogurtoolassist:sync";
    private static final int PROTOCOL = 1;
    private boolean enabled;
    private float speed;
    private boolean force;

    @Override public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        loadSettings();
        getServer().getPluginManager().registerEvents(new SyncListener(this), this);
        getLogger().info("YogurToolAssistServer enabled");
    }
    @Override public void onDisable() {
        enabled = false;
        force = false;
        syncAll();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
    }
    void loadSettings() {
        saveDefaultConfig(); reloadConfig();
        enabled = getConfig().getBoolean("enabled", true);
        double configuredSpeed = getConfig().getDouble("speed", 0.5);
        speed = Double.isFinite(configuredSpeed) ? (float) Math.max(0.1, Math.min(2.0, configuredSpeed)) : 0.5f;
        force = getConfig().getBoolean("force", false);
    }
    void sync(Player player) {
        if (!player.isOnline()) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(10);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(PROTOCOL); out.writeBoolean(enabled); out.writeFloat(speed); out.writeBoolean(force);
            player.sendPluginMessage(this, CHANNEL, bytes.toByteArray());
        } catch (IOException ex) { getLogger().warning("无法发送同步数据: " + ex.getMessage()); }
    }
    void syncAll() { for (Player player : getServer().getOnlinePlayers()) sync(player); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("yogurtoolassist.admin")) { sender.sendMessage("§c你没有权限。"); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("§aYogurToolAssist: enabled=" + enabled + ", speed=" + speed + ", force=" + force); return true;
        }
        if (args[0].equalsIgnoreCase("reload")) { loadSettings(); syncAll(); sender.sendMessage("§a配置已重载并同步。"); return true; }
        if (args[0].equalsIgnoreCase("set") && args.length >= 3) {
            try {
                if (args[1].equalsIgnoreCase("enabled")) enabled = Boolean.parseBoolean(args[2]);
                else if (args[1].equalsIgnoreCase("speed")) {
                    float value = Float.parseFloat(args[2]);
                    if (!Float.isFinite(value)) throw new NumberFormatException("Non-finite speed");
                    speed = Math.max(0.1f, Math.min(2.0f, value));
                }
                else if (args[1].equalsIgnoreCase("force")) force = Boolean.parseBoolean(args[2]);
                else { sender.sendMessage("§c可设置 enabled、speed、force"); return true; }
                getConfig().set(args[1].toLowerCase(), args[1].equalsIgnoreCase("speed") ? speed : args[1].equalsIgnoreCase("enabled") ? enabled : force);
                saveConfig(); syncAll(); sender.sendMessage("§a已更新并同步。"); return true;
            } catch (NumberFormatException ex) { sender.sendMessage("§cspeed 必须是 0.1 到 2.0 的数字。"); return true; }
        }
        sender.sendMessage("§e用法: /" + label + " <status|reload|set enabled|speed|force 值>"); return true;
    }
}
