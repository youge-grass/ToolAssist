package dev.yogur.toolassist.mixin;

import dev.yogur.toolassist.YogurToolAssist;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureTransform.class)
public abstract class TextureTransformMixin {
    @Unique private static final long yogur$glintEpochNanos = System.nanoTime();

    @Redirect(method = "setupGlintTexturing(F)Lorg/joml/Matrix4f;", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J"), require = 1, allow = 1)
    private static long yogur$virtualGlintTime() {
        return (long) (((System.nanoTime() - yogur$glintEpochNanos) / 1_000_000.0)
            * YogurToolAssist.soundSpeed());
    }
}
