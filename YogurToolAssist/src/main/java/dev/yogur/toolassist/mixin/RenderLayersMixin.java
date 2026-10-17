package dev.yogur.toolassist.mixin;

import dev.yogur.toolassist.YogurToolAssist;
import net.minecraft.client.render.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextureTransform.class)
public abstract class RenderLayersMixin {
    @ModifyVariable(method = "getGlintTransformation", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), argsOnly = true)
    private static float yogur$scaleGlintSpeed(float speed) {
        return speed * YogurToolAssist.soundSpeed();
    }
}
