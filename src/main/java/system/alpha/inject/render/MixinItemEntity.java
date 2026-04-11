package system.alpha.inject.render;

import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.client.features.modules.render.ItemPhysicModule;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity {

    @Unique
    private int vv_ticksLived;

    @Inject(method = "tick", at = @At("HEAD"))
    private void vv_onTickHead(CallbackInfo ci) {
        ++this.vv_ticksLived;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void vv_onTickTail(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;

        if (!ItemPhysicModule.getInstance().isEnabled()) {
            return;
        }
        if (this.vv_ticksLived < 40 || self.isRemoved()) {
            return;
        }
        if (self.hasNoGravity() || self.isTouchingWater() || self.isSubmergedInWater() || self.isInLava()) {
            return;
        }
        Vec3d vel = self.getVelocity();
        if (vel.y <= 0.001) {
            return;
        }
        self.setVelocity(vel.x, 0.0, vel.z);
    }
}