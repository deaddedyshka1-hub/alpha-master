package system.alpha.inject.other;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import system.alpha.client.features.modules.player.AspectRatioModule;
import system.alpha.client.features.modules.render.HitColorModule;

@Mixin(GameRenderer.class)
public class MixinAspectRatio {
    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    private void modifyProjectionMatrix(float fov, @NotNull CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatioModule module = AspectRatioModule.getInstance();

        if (module.isEnabled()) {
            float aspect = module.ratio.getValue();
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.peek().getPositionMatrix().identity();
            matrixStack.peek().getPositionMatrix().mul(
                    new Matrix4f().setPerspective(
                            (float) (fov * 0.017453292), // радианы
                            aspect,
                            0.05f,
                            256.0f
                    )
            );
            cir.setReturnValue(matrixStack.peek().getPositionMatrix());
        }
    }
}
