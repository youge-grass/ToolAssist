package dev.yogur.toolassist.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.yogur.toolassist.YogurToolAssist;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Unique private long yogur$lastHotbarPoll;
    @Unique private boolean yogur$hasHotbarPoll;
    @Unique private boolean yogur$allowHotbarPass;

    // 1.21.11 bytecode: hotbarKeys -> AALOAD -> wasPressed, before socialInteractionsKey.
    // This slice contains exactly one wasPressed call site; never intercept attack/use keys.
    @WrapOperation(method = "handleInputEvents", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z"),
        slice = @Slice(from = @At(value = "FIELD",
            target = "Lnet/minecraft/client/option/GameOptions;hotbarKeys:[Lnet/minecraft/client/option/KeyBinding;"),
            to = @At(value = "FIELD",
            target = "Lnet/minecraft/client/option/GameOptions;socialInteractionsKey:Lnet/minecraft/client/option/KeyBinding;")),
        require = 1, allow = 1)
    private boolean yogur$limitVanillaHotbar(KeyBinding binding, Operation<Boolean> original) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        long interval = YogurToolAssist.hotbarIntervalNanos();
        // Default is a true passthrough, including catch-up ticks and queued key presses.
        if (interval <= 50_000_000L || client.player == null || client.player.isSpectator()
            || client.options.saveToolbarActivatorKey.isPressed()
            || client.options.loadToolbarActivatorKey.isPressed()) {
            yogur$hasHotbarPoll = false;
            return original.call(binding);
        }
        // Vanilla visits all nine bindings once per input pass. Decide once for the whole pass.
        if (binding == client.options.hotbarKeys[0]) {
            long now = System.nanoTime();
            yogur$allowHotbarPass = !yogur$hasHotbarPoll || now - yogur$lastHotbarPoll >= interval;
            if (yogur$allowHotbarPass) {
                yogur$lastHotbarPoll = now;
                yogur$hasHotbarPoll = true;
            }
        }
        // A blocked pass leaves vanilla's press counters untouched. No custom slot or packets.
        return yogur$allowHotbarPass && original.call(binding);
    }
}
