package system.alpha.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.api.event.events.player.move.MotionEvent;
import system.alpha.api.event.events.player.other.CloseScreenEvent;
import system.alpha.api.event.events.player.other.UpdateEvent;
import system.alpha.api.event.events.player.move.MoveEvent;
import system.alpha.api.event.events.player.move.SprintEvent;
import system.alpha.api.system.backend.SharedClass;
import system.alpha.api.utils.player.DirectionalInput;
import system.alpha.api.utils.rotation.manager.Rotation;
import system.alpha.api.utils.rotation.manager.RotationManager;
import system.alpha.api.utils.rotation.manager.RotationPlan;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    public Input input;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHook(CallbackInfo ci) {
        UpdateEvent.getInstance().call();
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void moveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        MoveEvent.MoveEventData event = new MoveEvent.MoveEventData(movement.x, movement.y, movement.z);
        if (MoveEvent.getInstance().call(event)) {
            super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float silentRotationYaw(float original) {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return original;
        }

        Rotation rotation = rotationManager.getRotation();

        if (SharedClass.player() != null) {
            float yaw = rotation.getYaw();

            this.setHeadYaw(yaw);
            this.setBodyYaw(yaw);
        }

        return rotation.getYaw();
    }

    @ModifyExpressionValue(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float silentRotationPitch(float original) {
        RotationPlan rotation = RotationManager.getInstance().getCurrentRotationPlan();
        if (rotation == null) {
            return original;
        }

        Rotation rot = RotationManager.getInstance().getRotation();

        return rot.getPitch();
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "HEAD"), cancellable = true)
    private void preMotion(CallbackInfo ci) {
        MotionEvent.MotionEventData event = new MotionEvent.MotionEventData(getX(), getY(), getZ(), getYaw(1), getPitch(1), isOnGround());
        if (MotionEvent.getInstance().call(event)) ci.cancel();
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;canSprint()Z"))
    private boolean sprintEventTick(boolean original) {
        return sprintHook(original);
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean sprintEventInput(boolean original) {
        return sprintHook(original);
    }

    @Inject(method = "closeHandledScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"), cancellable = true)
    private void onCloseHandledScreen(CallbackInfo ci) {
        if (CloseScreenEvent.getInstance().call()) ci.cancel();
    }

    @Unique
    private boolean sprintHook(boolean origin) {
        return SprintEvent.getInstance().call(new SprintEvent.SprintEventData(new DirectionalInput(input)));
    }
}
