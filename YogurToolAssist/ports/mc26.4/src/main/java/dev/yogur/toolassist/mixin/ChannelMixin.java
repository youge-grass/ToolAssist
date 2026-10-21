package dev.yogur.toolassist.mixin;

import com.mojang.blaze3d.audio.Channel;
import dev.yogur.toolassist.YogurToolAssist;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelMixin {
    @Shadow @Final private int source;
    @Shadow @Final private AtomicBoolean initialized;
    @Unique private float yogur$originalPitch = 1.0f;
    @Unique private float yogur$appliedSpeed = Float.NaN;

    @Inject(method = "setPitch(F)V", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void yogur$guardPitch(float pitch, CallbackInfo ci) {
        if (source <= 0 || !initialized.get()) ci.cancel();
    }

    @ModifyVariable(method = "setPitch(F)V", at = @At("HEAD"), argsOnly = true, require = 1, allow = 1)
    private float yogur$scalePitch(float pitch) {
        yogur$originalPitch = Float.isFinite(pitch) && pitch > 0 ? pitch : 1.0f;
        yogur$appliedSpeed = YogurToolAssist.soundSpeed();
        return yogur$originalPitch * yogur$appliedSpeed;
    }

    // ChannelAccess schedules updateStream on the sound executor, including static channels.
    @Inject(method = "updateStream()V", at = @At("HEAD"), require = 1, allow = 1)
    private void yogur$updateSpeed(CallbackInfo ci) {
        if (source <= 0 || !initialized.get()) return;
        float speed = YogurToolAssist.soundSpeed();
        if (Float.compare(speed, yogur$appliedSpeed) == 0) return;
        yogur$appliedSpeed = speed;
        AL10.alSourcef(source, AL10.AL_PITCH, yogur$originalPitch * speed);
    }
}
