package dev.yogur.toolassist.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

/** Paper 1.21.11 eating compensation switch; the remaining-use duration is never changed. */
final class UseDurationLimiter implements Listener {
    private final JavaPlugin plugin;
    private final boolean configured;
    private final Map<Object, Boolean> modified = new IdentityHashMap<>();
    private Method getHandle;
    private Field eatStartTime;
    private boolean active;

    UseDurationLimiter(JavaPlugin plugin, boolean configured) {
        this.plugin = plugin;
        this.configured = configured;
        resolveReflection();
        active = configured && getHandle != null && eatStartTime != null;
        if (!configured) plugin.getLogger().info("disable-eating-compensation=false，进食补偿禁用功能未启用");
    }

    private void resolveReflection() {
        try {
            String craftPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftPlayer = Class.forName(craftPackage + ".entity.CraftPlayer");
            getHandle = craftPlayer.getMethod("getHandle");
            Class<?> type = getHandle.getReturnType();
            while (type != null && eatStartTime == null) {
                try { eatStartTime = type.getDeclaredField("eatStartTime"); }
                catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
            }
            if (eatStartTime == null || eatStartTime.getType() != long.class) throw new NoSuchFieldException("LivingEntity.eatStartTime");
            eatStartTime.setAccessible(true);
            plugin.getLogger().info("已从运行时 " + craftPlayer.getName() + " 解析 getHandle，并定位 Paper 1.21.11 LivingEntity.eatStartTime");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning("无法从运行时 CraftPlayer 解析 getHandle 或 Paper 1.21.11 LivingEntity.eatStartTime，已禁用进食补偿禁用功能: " + ex.getMessage());
            getHandle = null;
            eatStartTime = null;
        }
    }

    @EventHandler
    public void onServerTickStart(ServerTickStartEvent event) {
        if (!active || plugin.getServer().getServerTickManager().getTickRate() >= 20.0f) return;
        Map<Object, Boolean> current = new IdentityHashMap<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.isOnline() || !player.isHandRaised() || !player.hasActiveItem()) continue;
            try {
                Object handle = getHandle.invoke(player);
                current.put(handle, Boolean.TRUE);
                if (eatStartTime.getLong(handle) != -1L) {
                    eatStartTime.setLong(handle, -1L);
                    modified.put(handle, Boolean.TRUE);
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                active = false;
                plugin.getLogger().warning("写入 eatStartTime 失败，已禁用进食补偿禁用功能: " + ex.getMessage());
                return;
            }
        }
        modified.keySet().removeIf(handle -> !current.containsKey(handle));
    }

    void restoreModifiedPlayers() {
        if (eatStartTime == null) return;
        Map<Object, Boolean> current = new IdentityHashMap<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.isOnline() || !player.isHandRaised() || !player.hasActiveItem()) continue;
            try { current.put(getHandle.invoke(player), Boolean.TRUE); }
            catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        for (Object handle : modified.keySet()) {
            if (!current.containsKey(handle)) continue;
            try {
                if (eatStartTime.getLong(handle) == -1L) eatStartTime.setLong(handle, System.nanoTime());
            } catch (ReflectiveOperationException | RuntimeException ex) {
                plugin.getLogger().warning("恢复 eatStartTime 失败: " + ex.getMessage());
            }
        }
        modified.clear();
    }
}
