package dev.yogur.toolassist.mixin;

import dev.yogur.toolassist.YogurToolAssist;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Unique private long yogur$lastHotbarPoll;
    @Unique private boolean yogur$hasHotbarPoll;

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 2))
    private boolean yogur$limitVanillaHotbar(KeyBinding binding) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (!yogur$handlesHotbar(client)) return binding.wasPressed();
        yogur$pollHotbar();
        return false;
    }

    @Unique
    private boolean yogur$handlesHotbar(MinecraftClient client) {
        return client.player != null && client.world != null && client.options != null
            && client.currentScreen == null && client.getOverlay() == null
            && client.isWindowFocused() && !client.player.isSpectator()
            && client.options.hotbarKeys != null
            && client.options.saveToolbarActivatorKey != null
            && client.options.loadToolbarActivatorKey != null
            && !client.options.saveToolbarActivatorKey.isPressed()
            && !client.options.loadToolbarActivatorKey.isPressed();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void yogur$responsiveHotbar(boolean tick, CallbackInfo ci) {
        if (ci != null) yogur$pollHotbar();
    }

    @Unique
    private void yogur$pollHotbar() {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (!yogur$handlesHotbar(client)) return;
        PlayerInventory inventory = client.player.getInventory();
        if (inventory == null) return;
        long now = System.nanoTime();
        // 配置间隔最低为 50ms；帧率再高也不额外消费输入。
        if (yogur$hasHotbarPoll
            && now - yogur$lastHotbarPoll < YogurToolAssist.hotbarIntervalNanos()) return;
        yogur$hasHotbarPoll = true;
        yogur$lastHotbarPoll = now;
        for (int slot = 0; slot < Math.min(9, client.options.hotbarKeys.length); slot++) {
            KeyBinding binding = client.options.hotbarKeys[slot];
            if (binding == null) continue;
            while (binding.wasPressed()) inventory.setSelectedSlot(slot);
        }
    }
}
