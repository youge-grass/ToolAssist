package dev.yogur.toolassist;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.io.StringReader;

public final class YogurToolAssist implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("YogurToolAssist");
    /** 仅节流原版数字键输入，不改动选槽同步或攻击顺序。 */
    public static final boolean CUSTOM_HOTBAR_ADJUSTMENTS_ENABLED = true;
    private static volatile float speed = 0.5f;
    private static volatile double hotbarInterval = 0.05;
    private static volatile float localSpeed = 0.5f;
    private static volatile boolean serverForced;
    private record SyncPayload(int protocol, boolean enabled, float speed, boolean force) implements CustomPayload {
        private static final CustomPayload.Id<SyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("yogurtoolassist", "sync"));
        private static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC = new PacketCodec<>() {
            @Override public SyncPayload decode(RegistryByteBuf buf) {
                if (buf.readableBytes() != 10) throw new IllegalArgumentException("Invalid sync payload length");
                return new SyncPayload(buf.readInt(), buf.readBoolean(), buf.readFloat(), buf.readBoolean());
            }
            @Override public void encode(RegistryByteBuf buf, SyncPayload payload) {
                buf.writeInt(payload.protocol());
                buf.writeBoolean(payload.enabled());
                buf.writeFloat(payload.speed());
                buf.writeBoolean(payload.force());
            }
        };
        @Override public CustomPayload.Id<SyncPayload> getId() { return ID; }
    }

    public static long hotbarIntervalNanos() {
        return Math.round(hotbarInterval * 1_000_000_000.0);
    }

    private static boolean validHotbarInterval(double value) {
        return Double.isFinite(value) && value >= 0.05 && value <= 60.0;
    }
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("yogurtoolassist.properties");

    public static float soundSpeed() {
        return speed;
    }

    public static boolean validSpeed(float value) {
        return Float.isFinite(value) && value >= 0.1f && value <= 2.0f;
    }

    public static boolean setSpeed(float value) {
        if (!validSpeed(value) || serverForced) return false;
        speed = value;
        localSpeed = value;
        saveConfig();
        return true;
    }

    public static boolean isServerForced() { return serverForced; }

    private static void receiveSyncFromValues(int protocol, boolean enabled, float serverSpeed, boolean force) {
        if (protocol != 1 || !validSpeed(serverSpeed)) return;
        boolean wasForced = serverForced;
        serverForced = enabled && force;
        if (!enabled || (wasForced && !force)) speed = localSpeed;
        else speed = serverSpeed;
        LOGGER.info("Server sync applied: enabled={}, speed={}, force={}", enabled, serverSpeed, force);
    }


    @Override
    public void onInitializeClient() {
        loadConfig();
        localSpeed = speed;
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) ->
            context.client().execute(() -> receiveSyncFromValues(payload.protocol(), payload.enabled(), payload.speed(), payload.force())));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverForced = false;
            speed = localSpeed;
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            if (dispatcher == null) return;
            if (CUSTOM_HOTBAR_ADJUSTMENTS_ENABLED) dispatcher.register(ClientCommandManager.literal("hotbarinterval")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("数字键检测间隔：" + hotbarInterval
                        + " 秒（范围 0.05–60；默认 0.05，不改变原版输入时序）"));
                    return 1;
                })
                .then(ClientCommandManager.argument("seconds", DoubleArgumentType.doubleArg(0.05, 60.0))
                    .executes(context -> {
                        double value = DoubleArgumentType.getDouble(context, "seconds");
                        if (!validHotbarInterval(value)) return 0;
                        hotbarInterval = value;
                        boolean saved = saveConfig();
                        context.getSource().sendFeedback(Text.literal("数字键检测间隔已设为 "
                            + hotbarInterval + " 秒"
                            + (saved ? "" : "（配置保存失败，重启后不会保留）")));
                        return 1;
                    })));
            dispatcher.register(ClientCommandManager.literal("glintspeed")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.literal("声音与附魔光效倍率：" + speed + "（0.1–2.0）"));
                    return 1;
                })
                .then(ClientCommandManager.argument("multiplier", FloatArgumentType.floatArg(0.1f, 2.0f))
                    .executes(context -> {
                        float value = FloatArgumentType.getFloat(context, "multiplier");
                        if (!setSpeed(value)) return 0;
                        context.getSource().sendFeedback(Text.literal("声音与附魔光效倍率已设为 " + speed));
                        return 1;
                    })));
            dispatcher.register(ClientCommandManager.literal("soundspeed")
                .executes(context -> {
                    if (context == null || context.getSource() == null) return 0;
                    context.getSource().sendFeedback(Text.literal("声音倍率：" + speed + "（0.1–2.0；1 恢复正常）"));
                    return 1;
                })
                .then(ClientCommandManager.argument("multiplier", FloatArgumentType.floatArg(0.1f, 2.0f))
                    .executes(context -> {
                        if (context == null || context.getSource() == null) return 0;
                        float value = FloatArgumentType.getFloat(context, "multiplier");
                        if (!validSpeed(value)) return 0;
                        if (serverForced) return 0;
                        speed = value;
                        localSpeed = value;
                        boolean saved = saveConfig();
                        context.getSource().sendFeedback(Text.literal("声音倍率已设为 " + speed
                            + (saved ? "" : "（配置保存失败，重启后不会保留）")));
                        return 1;
                    })));
        });
    }

    private static void loadConfig() {
        if (!Files.isRegularFile(CONFIG)) {
            saveConfig();
            return;
        }
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(Files.readString(CONFIG, StandardCharsets.UTF_8)));
            String unifiedRaw = properties.getProperty("speed");
            String legacyRaw = properties.getProperty("soundSpeed");
            if (unifiedRaw == null) unifiedRaw = legacyRaw;
            if (unifiedRaw == null) unifiedRaw = properties.getProperty("glintSpeed");
            if (unifiedRaw != null) {
                try {
                    float value = Float.parseFloat(unifiedRaw);
                    if (validSpeed(value)) { speed = value; localSpeed = value; }
                    else LOGGER.warn("Invalid speed; using 0.5");
                } catch (NumberFormatException exception) {
                    LOGGER.warn("Invalid speed; using 0.5", exception);
                }
            }
            String intervalRaw = properties.getProperty("hotbarInterval");
            if (intervalRaw != null) {
                try {
                    double value = Double.parseDouble(intervalRaw);
                    if (validHotbarInterval(value)) hotbarInterval = value;
                    else LOGGER.warn("Invalid hotbarInterval; using 0.05");
                } catch (NumberFormatException exception) {
                    LOGGER.warn("Invalid hotbarInterval; using 0.05", exception);
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Cannot load configuration; using defaults", exception);
        }
    }

    private static boolean saveConfig() {
        try {
            Path parent = CONFIG.getParent();
            if (parent == null) return false;
            Files.createDirectories(parent);
            Files.writeString(CONFIG, "speed=" + localSpeed
                + "\nhotbarInterval=" + hotbarInterval + "\n", StandardCharsets.UTF_8);
            return true;
        } catch (IOException | SecurityException exception) {
            LOGGER.warn("Cannot save sound speed", exception);
            return false;
        }
    }
}
