package dev.yogur.toolassist.server;

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
    private final Method getHandle;
    private final Field eatStartTime;
    private boolean active;

    UseDurationLimiter(JavaPlugin plugin, boolean configured) {
        this.plugin = plugin;
        this.configured = configured;
        Method handle = null;
        Field field = null;
        try {
            handle = Player.class.getMethod("getHandle");
            Class<?> type = handle.getReturnType();
            while (type != null && field == null) {
                try { field = type.getDeclaredField("eatStartTime"); }
                catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
            }
            if (field == null || field.getType() != long.class) throw new NoSuchFieldException("LivingEntity.eatStartTime");
            field.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning("无法找到 Paper 1.21.11 LivingEntity.eatStartTime，已禁用进食补偿禁用功能: " + ex.getMessage());
            handle = null;
            field = null;
        }
        getHandle = handle;
        eatStartTime = field;
        active = configured && getHandle != null && eatStartTime != null;
        if (!configured) plugin.getLogger().info("disable-eating-compensation=false，进食补偿禁用功能未启用");
    }

    @EventHandler
    public void onServerTickStart(ServerTickStartEvent event) {
        if (!active || plugin.getServer().getServerTickManager().getTickRate() >= 20.0f) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.isOnline() || !player.isHandRaised() || !player.hasActiveItem()) continue;
            try {
                Object handle = getHandle.invoke(player);
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
    }

    void restoreModifiedPlayers() {
        if (eatStartTime == null) return;
        long now = System.nanoTime();
        for (Object handle : modified.keySet()) {
            try { eatStartTime.setLong(handle, now); }
            catch (ReflectiveOperationException | RuntimeException ex) {
                plugin.getLogger().warning("恢复 eatStartTime 失败: " + ex.getMessage());
            }
        }
        modified.clear();
    }
}
