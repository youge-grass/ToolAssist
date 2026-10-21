package dev.yogur.toolassist.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.yogur.toolassist.YogurToolAssist;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Unique private long yogur$lastHotbarPoll;
    @Unique private boolean yogur$hasHotbarPoll;
    @Unique private boolean yogur$allowHotbarPass;

    // Snapshot 5: bytecode 188 hotbar field, 193 consumeClick, 285 inventory field.
    @WrapOperation(method = "handleKeybinds()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"),
        slice = @Slice(from = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Options;keyHotbarSlots:[Lnet/minecraft/client/KeyMapping;"),
            to = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Options;keyInventory:Lnet/minecraft/client/KeyMapping;")),
        require = 1, allow = 1)
    private boolean yogur$limitVanillaHotbar(KeyMapping binding, Operation<Boolean> original) {
        Minecraft client = (Minecraft) (Object) this;
        long interval = YogurToolAssist.hotbarIntervalNanos();
        if (interval <= 50_000_000L || client.player == null || client.player.isSpectator()
            || client.options.keySaveHotbarActivator.isDown()
            || client.options.keyLoadHotbarActivator.isDown()) {
            yogur$hasHotbarPoll = false;
            return original.call(binding);
        }
        if (binding == client.options.keyHotbarSlots[0]) {
            long now = System.nanoTime();
            yogur$allowHotbarPass = !yogur$hasHotbarPoll || now - yogur$lastHotbarPoll >= interval;
            if (yogur$allowHotbarPass) {
                yogur$lastHotbarPoll = now;
                yogur$hasHotbarPoll = true;
            }
        }
        return yogur$allowHotbarPass && original.call(binding);
    }
}
