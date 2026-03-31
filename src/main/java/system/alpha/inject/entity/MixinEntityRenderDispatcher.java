package system.alpha.inject.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.api.utils.render.display.hitbox.HitboxQueue;
import system.alpha.client.features.modules.render.CustomHitboxesModule;

import java.awt.*;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {

    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void alpha$replaceVanillaHitboxes(
            MatrixStack matrices,
            VertexConsumer vertices,
            Entity entity,
            float red, float green, float blue, float alpha,
            CallbackInfo ci
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) {
            return;
        }

        CustomHitboxesModule module = CustomHitboxesModule.getInstance();
        if (module == null || !module.isEnabled()) return;

        ci.cancel();

        if (!module.shouldRenderEntityInHitbox(entity)) return;

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        Box worldBox = entity.getBoundingBox().expand(0.0020000000949949026);
        Vec3d lerped = entity.getLerpedPos(tickDelta);
        Vec3d current = entity.getPos();
        worldBox = worldBox.offset(lerped.x - current.x, lerped.y - current.y, lerped.z - current.z);

        Color fillColor = module.getFillColorForRendering();
        Color outlineColor = module.getOutlineColorForRendering();
        boolean fill = module.shouldFill();
        float lineWidth = module.getLineWidth();

        if (fill) {
            HitboxQueue.addHitbox(worldBox, fillColor, outlineColor, lineWidth);
        } else {
            HitboxQueue.addHitboxOutline(worldBox, outlineColor, lineWidth);
        }
    }
}