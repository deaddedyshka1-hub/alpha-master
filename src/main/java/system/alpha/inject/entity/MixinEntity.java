package system.alpha.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import system.alpha.api.event.events.player.move.VelocityEvent;
import system.alpha.api.system.backend.SharedClass;
import system.alpha.client.features.modules.render.RemovalsModule;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public void cancelGlowingHook(CallbackInfoReturnable<Boolean> cir) {
        if (RemovalsModule.getInstance().isGlowEffect()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    private boolean fixFallDistance(boolean original) {
        if ((Object) this == SharedClass.player()) {
            return false;
        }

        return original;
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d updateVelocityHook(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == SharedClass.player()) {
            VelocityEvent.VelocityEventData event = new VelocityEvent.VelocityEventData(movementInput, speed, yaw, Entity.movementInputToVelocity(movementInput, speed, yaw));
            VelocityEvent.getInstance().call(event);
            return event.getVelocity();
        }


        return Entity.movementInputToVelocity(movementInput, speed, yaw);
    }
}
