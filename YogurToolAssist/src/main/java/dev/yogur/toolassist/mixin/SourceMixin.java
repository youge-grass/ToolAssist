package dev.yogur.toolassist.mixin;

import dev.yogur.toolassist.YogurToolAssist;
import net.minecraft.client.sound.Source;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Source.class)
public abstract class SourceMixin {
    @Shadow @Final private int pointer;
    @Shadow @Final private java.util.concurrent.atomic.AtomicBoolean playing;
    @Unique private float yogur$originalPitch = 1.0f;
    @Unique private float yogur$appliedSpeed = Float.NaN;

    // playing 表示声源尚未关闭，并非 OpenAL 的正在播放状态。
    @Inject(method = "setPitch", at = @At("HEAD"), cancellable = true)
    private void yogur$guardPitch(float pitch, CallbackInfo ci) {
        if (ci != null && (pointer <= 0 || playing == null || !playing.get())) ci.cancel();
    }

    @ModifyVariable(method = "setPitch", at = @At("HEAD"), argsOnly = true)
    private float yogur$scalePitch(float pitch) {
        yogur$originalPitch = Float.isFinite(pitch) && pitch > 0 ? pitch : 1.0f;
        yogur$appliedSpeed = YogurToolAssist.soundSpeed();
        return yogur$originalPitch * yogur$appliedSpeed;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void yogur$updateSpeed(CallbackInfo ci) {
        if (ci == null || pointer <= 0 || playing == null || !playing.get()) return;
        float speed = YogurToolAssist.soundSpeed();
        if (Float.compare(speed, yogur$appliedSpeed) == 0) return;
        yogur$appliedSpeed = speed;
        AL10.alSourcef(pointer, AL10.AL_PITCH, yogur$originalPitch * speed);
    }
}
