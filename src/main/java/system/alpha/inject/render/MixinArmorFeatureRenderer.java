package system.alpha.inject.render;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.api.utils.entity.ArmorTintContext;
import system.alpha.api.utils.render.other.RenderStateEntityCache;
import system.alpha.client.features.modules.player.CustomModelsModule;
import system.alpha.client.features.modules.render.RemovalsModule;

@Mixin(value={ArmorFeatureRenderer.class})
public class MixinArmorFeatureRenderer {
    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true)
    private void removeArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        LivingEntity entity = RenderStateEntityCache.get((LivingEntityRenderState)state);
        ArmorTintContext.set(entity);
        if (RemovalsModule.getInstance().isArmor() || entity != null && CustomModelsModule.getInstance().shouldApplyTo(entity)) {
            ArmorTintContext.clear();
            ci.cancel();
        }
    }

    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void clearArmorContext(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        ArmorTintContext.clear();
    }
}
