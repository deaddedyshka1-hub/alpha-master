package system.alpha.inject.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.api.utils.other.ItemEntityRenderStateExt;
import system.alpha.client.features.modules.render.ItemPhysicModule;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {

    @Inject(method={"updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V"}, at={@At(value="TAIL")})
    private void vv$updatePhysicsState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ItemEntityRenderStateExt ext = (ItemEntityRenderStateExt)state;
        if (!ItemPhysicModule.getInstance().isEnabled() || entity == null) {
            ext.vv$setGrounded(false);
            return;
        }
        boolean grounded = !entity.isTouchingWater() && !entity.isSubmergedInWater() && !entity.isInLava();
        ext.vv$setGrounded(grounded);
        ext.vv$setGroundRoll((float)entity.getId() * 31.0f % 360.0f);
        if (grounded) {
            state.age = 0.0f;
            state.uniqueOffset = 0.0f;
        }
    }

    @Inject(method={"render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/ItemEntityRenderer;renderStack(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/ItemStackEntityRenderState;Lnet/minecraft/util/math/random/Random;)V", shift=At.Shift.BEFORE)})
    private void vv$applyGroundTransform(ItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!ItemPhysicModule.getInstance().isEnabled()) {
            return;
        }
        ItemEntityRenderStateExt ext = (ItemEntityRenderStateExt)state;
        if (!ext.vv$isGrounded()) {
            return;
        }
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ext.vv$getGroundRoll()));
    }
}