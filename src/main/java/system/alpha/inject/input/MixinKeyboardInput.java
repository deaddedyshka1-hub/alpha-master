package system.alpha.inject.input;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import system.alpha.api.event.events.player.other.MovementInputEvent;
import system.alpha.api.event.events.player.move.SprintEvent;
import system.alpha.api.system.backend.SharedClass;
import system.alpha.api.utils.player.DirectionalInput;
import system.alpha.api.utils.player.MoveUtil;
import system.alpha.api.utils.rotation.manager.Rotation;
import system.alpha.api.utils.rotation.manager.RotationManager;
import system.alpha.api.utils.rotation.manager.RotationPlan;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput onTick(PlayerInput original) {
        MovementInputEvent.MovementInputEventData movementInputEvent = new MovementInputEvent.MovementInputEventData(original, original.jump(), original.sneak(), new DirectionalInput(original));
        MovementInputEvent.getInstance().call(movementInputEvent);

        DirectionalInput untransformedDirectionalInput = movementInputEvent.getDirectionalInput();
        DirectionalInput directionalInput = transformDirection(untransformedDirectionalInput);

        SprintEvent.SprintEventData sprintEvent = new SprintEvent.SprintEventData(directionalInput);
        SprintEvent.getInstance().call(sprintEvent);

        this.untransformed = new PlayerInput(
                untransformedDirectionalInput.isForwards(),
                untransformedDirectionalInput.isBackwards(),
                untransformedDirectionalInput.isLeft(),
                untransformedDirectionalInput.isRight(),
                original.jump(),
                original.sneak(),
                sprintEvent.isSprint()
        );

        return new PlayerInput(
                directionalInput.isForwards(),
                directionalInput.isBackwards(),
                directionalInput.isLeft(),
                directionalInput.isRight(),
                movementInputEvent.isJump(),
                movementInputEvent.isSneak(),
                sprintEvent.isSprint()
        );
    }

    @Unique
    private DirectionalInput transformDirection(DirectionalInput input) {
        ClientPlayerEntity player = SharedClass.player();
        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getCurrentRotation();
        RotationPlan rotationPlan = rotationManager.getCurrentRotationPlan();

        if (rotationPlan == null || rotation == null || player == null || !rotationPlan.moveCorrection()) {
            return input;
        }
        return input;
    }
}
