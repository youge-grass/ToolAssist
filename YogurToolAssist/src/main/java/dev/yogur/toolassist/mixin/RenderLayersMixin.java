package dev.yogur.toolassist.mixin;

import dev.yogur.toolassist.YogurToolAssist;
import net.minecraft.client.render.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureTransform.class)
public abstract class RenderLayersMixin {
    @Unique private static final long yogur$glintEpochNanos = System.nanoTime();

    /**
     * TextureTransform.getGlintTransformation(float) calls Util.getMeasuringTimeMs()
     * (official: bhs.c()J), then multiplies that millisecond clock by the glint-speed
     * option and applies the modulo/translation math. Redirect the proven source clock,
     * leaving the transformation scale argument untouched.
     */
    @Redirect(method = "getGlintTransformation", at = @org.spongepowered.asm.mixin.injection.At(
        value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"))
    private static long yogur$virtualGlintTime() {
        return (long) (((System.nanoTime() - yogur$glintEpochNanos) / 1_000_000.0)
            * YogurToolAssist.soundSpeed());
    }
}

