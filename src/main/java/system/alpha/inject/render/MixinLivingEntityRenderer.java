package system.alpha.inject.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import system.alpha.api.event.events.render.EntityColorEvent;
import system.alpha.api.system.backend.SharedClass;
import system.alpha.api.utils.color.ColorHit;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.render.other.CustomModelsRenderer;
import system.alpha.api.utils.render.other.RenderStateEntityCache;
import system.alpha.api.utils.rotation.manager.Rotation;
import system.alpha.api.utils.rotation.manager.RotationManager;
import system.alpha.api.utils.rotation.manager.RotationPlan;
import system.alpha.client.features.modules.player.CustomModelType;
import system.alpha.client.features.modules.player.CustomModelsModule;
import system.alpha.client.features.modules.render.HitColorModule;

import java.awt.*;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    private static final ThreadLocal<Boolean> SV_SHOULD_TINT = ColorHit.SHOULD_TINT;
    private static final Identifier SV_WHITE = Identifier.of("minecraft", "textures/misc/white.png");
    private static final ThreadLocal<Boolean> WAS_HURT = ThreadLocal.withInitial(() -> false);

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void storeRenderStateEntity(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        RenderStateEntityCache.put(state, entity);
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float updateVisualPitch(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != SharedClass.player()) {
            return original;
        }

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        Rotation rotationPrev = rotationManager.getPreviousRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return original;
        }

        return MathHelper.lerpAngleDegrees(tickDelta, rotationPrev.getPitch(), rotation.getPitch());
    }

    @Shadow
    @Nullable
    protected abstract RenderLayer getRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        LivingEntity entity = RenderStateEntityCache.get(state);
        CustomModelsModule customModels = CustomModelsModule.getInstance();

        if (entity != null && customModels.shouldApplyTo(entity)) {
            CustomModelType type = customModels.getSelectedType();
            if (type != null) {
                return showOutline ? RenderLayer.getOutline(type.getTexture()) : RenderLayer.getEntityTranslucent(type.getTexture());
            }
        }

        if (!translucent && state.width == 0.6F) {
            int defaultColor = -1;
            EntityColorEvent.EntityColorEventData eventData = new EntityColorEvent.EntityColorEventData(defaultColor);
            boolean modified = EntityColorEvent.getInstance().call(eventData);
            if (modified && eventData.color() != defaultColor) {
                translucent = true;
            }
        }

        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer,
                                 int light, int overlay, int color, @Local(ordinal = 0, argsOnly = true) LivingEntityRenderState renderState) {
        LivingEntity entity = RenderStateEntityCache.get(renderState);
        CustomModelsModule customModels = CustomModelsModule.getInstance();

        EntityColorEvent.EntityColorEventData event = new EntityColorEvent.EntityColorEventData(color);
        EntityColorEvent.getInstance().call(event);
        int finalColor = event.color();

        if (entity != null && customModels.shouldApplyTo(entity)) {
            CustomModelType type = customModels.getSelectedType();
            if (type != null && CustomModelsRenderer.render(type, instance, matrixStack, vertexConsumer, light, overlay, finalColor)) {
                return;
            }
        }

        instance.render(matrixStack, vertexConsumer, light, overlay, finalColor);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    private void prepareTint(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        HitColorModule module = HitColorModule.getInstance();
        if (module != null && module.isEnabled() && state.hurt) {
            SV_SHOULD_TINT.set(true);
            WAS_HURT.set(true);
            state.hurt = false;
        } else {
            WAS_HURT.set(false);
        }
    }

    @Overwrite
    public int getMixColor(S state) {
        HitColorModule module = HitColorModule.getInstance();
        if (module != null && module.isEnabled() && Boolean.TRUE.equals(SV_SHOULD_TINT.get())) {
            Color theme = UIColors.gradient(0);
            int a = (int) (255 * module.alpha.getValue().floatValue());
            return new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), a).getRGB();
        }
        return -1;
    }

    @Inject(method = "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;",
            at = @At("HEAD"), cancellable = true)
    private void forceTranslucentLayer(S state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        HitColorModule module = HitColorModule.getInstance();
        if (module != null && module.isEnabled() && Boolean.TRUE.equals(SV_SHOULD_TINT.get())) {
            cir.setReturnValue(RenderLayer.getEntityTranslucent(SV_WHITE));
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cleanupAfterRender(LivingEntityRenderState state, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SV_SHOULD_TINT.set(false);

        if (Boolean.TRUE.equals(WAS_HURT.get())) {
            state.hurt = true;
            WAS_HURT.set(false);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}