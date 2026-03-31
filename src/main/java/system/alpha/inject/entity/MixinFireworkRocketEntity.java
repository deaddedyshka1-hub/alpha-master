package system.alpha.inject.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import system.alpha.api.system.backend.Pair;
import system.alpha.api.system.backend.SharedClass;
import system.alpha.api.utils.rotation.manager.Rotation;
import system.alpha.api.utils.rotation.manager.RotationManager;
import system.alpha.api.utils.rotation.manager.RotationPlan;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {
    @Shadow
    private LivingEntity shooter;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d fixFireworkVelocity(LivingEntity instance) {
        if (instance != SharedClass.player()) {
            return instance.getRotationVector();
        }

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return instance.getRotationVector();
        }

        return rotation.getVector();
    }

}
