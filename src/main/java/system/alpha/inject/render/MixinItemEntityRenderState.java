package system.alpha.inject.render;

import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import system.alpha.api.utils.other.ItemEntityRenderStateExt;

@Mixin(value={ItemEntityRenderState.class})
public abstract class MixinItemEntityRenderState
        implements ItemEntityRenderStateExt {
    @Unique
    private boolean vv$grounded;
    @Unique
    private float vv$groundRoll;

    @Override
    public boolean vv$isGrounded() {
        return this.vv$grounded;
    }

    @Override
    public void vv$setGrounded(boolean grounded) {
        this.vv$grounded = grounded;
    }

    @Override
    public float vv$getGroundRoll() {
        return this.vv$groundRoll;
    }

    @Override
    public void vv$setGroundRoll(float roll) {
        this.vv$groundRoll = roll;
    }
}
