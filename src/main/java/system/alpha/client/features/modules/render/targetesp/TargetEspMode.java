package system.alpha.client.features.modules.render.targetesp;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import system.alpha.api.event.events.render.Render3DEvent;
import system.alpha.api.system.interfaces.QuickImports;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.math.MathUtil;

import java.util.List;

public abstract class TargetEspMode implements QuickImports {
    public static final AnimationUtil showAnimation = new AnimationUtil();
    public static final AnimationUtil sizeAnimation = new AnimationUtil();
    public static LivingEntity currentTarget = null;
    public float prevShowAnimation = 0f;
    public float prevSizeAnimation = 0f;

    private double maxRange = 10.0;

    public void updateTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();

        currentTarget = null;

        HitResult hitResult = mc.crosshairTarget;

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;

                if (isValidTarget(livingEntity, mc) && canSeeDirectly(livingEntity, mc)) {
                    currentTarget = livingEntity;
                    return;
                }
            }
        }
    }

    private boolean isValidTarget(LivingEntity entity, MinecraftClient mc) {
        if (entity == null || !entity.isAlive() || mc.player == null) return false;
        if (entity == mc.player) return false;

        if (TargetEspModule.getInstance().shouldIgnoreInvisible()) {
            if (entity.isInvisible()) return false;
            if (entity.hasStatusEffect(StatusEffects.INVISIBILITY)) return false;
        }

        double distance = mc.player.distanceTo(entity);
        if (distance > maxRange) return false;

        return true;
    }

    private boolean canSeeDirectly(LivingEntity entity, MinecraftClient mc) {
        if (mc.player == null || mc.cameraEntity == null) return false;

        Vec3d cameraPos = mc.cameraEntity.getCameraPosVec(1.0f);
        Vec3d entityPos = entity.getBoundingBox().getCenter();

        return mc.world.raycast(
                new RaycastContext(
                        cameraPos,
                        entityPos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        ).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    public void updateAnimation(long duration, String mode, float size, float in, float out) {
        prevShowAnimation = (float) showAnimation.getValue();
        prevSizeAnimation = (float) sizeAnimation.getValue();

        sizeAnimation.update();
        double dyingSize = switch (mode) {
            case "In" -> in;
            case "Out" -> out;
            default -> size;
        };
        sizeAnimation.run(reason() ? size : dyingSize, duration, Easing.SINE_OUT);

        showAnimation.update();
        showAnimation.run(reason() ? 1.0 : 0.0, duration, Easing.SINE_OUT);
    }

    public boolean reason() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (currentTarget == null) return false;
        if (mc.player == null || mc.world == null) return false;

        if (TargetEspModule.getInstance().shouldIgnoreInvisible()) {
            if (currentTarget.isInvisible()) return false;
            if (currentTarget.hasStatusEffect(StatusEffects.INVISIBILITY)) return false;
        }

        HitResult hitResult = mc.crosshairTarget;

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();

            if (entity == currentTarget && isValidTarget(currentTarget, mc) && canSeeDirectly(currentTarget, mc)) {
                return true;
            }
        }

        return false;
    }

    public boolean canDraw() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return false;
        if (currentTarget == null) return false;

        if (TargetEspModule.getInstance().shouldIgnoreInvisible()) {
            if (currentTarget.isInvisible()) return false;
            if (currentTarget.hasStatusEffect(StatusEffects.INVISIBILITY)) return false;
        }

        HitResult hitResult = mc.crosshairTarget;

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();

            if (entity == currentTarget) {
                return showAnimation.getValue() > 0.0;
            }
        }

        return false;
    }

    public static void updatePositions() {
        float animationValue = (float) showAnimation.getValue();
        float animationTarget = (float) showAnimation.getToValue();

        boolean useLastPosition = TargetEspModule.getInstance().lastPosition.getValue();
        boolean preventUpdate = useLastPosition && animationTarget == 0.0 && animationValue <= 0.9f;

        if (currentTarget != null && !preventUpdate) {
            lastTargetX = MathUtil.interpolate((float) currentTarget.prevX, (float) currentTarget.getX());
            lastTargetY = MathUtil.interpolate((float) currentTarget.prevY, (float) currentTarget.getY());
            lastTargetZ = MathUtil.interpolate((float) currentTarget.prevZ, (float) currentTarget.getZ());
        }

        targetX = lastTargetX;
        targetY = lastTargetY;
        targetZ = lastTargetZ;
    }

    @Getter private static double targetX = -1;
    @Getter private static double targetY = -1;
    @Getter private static double targetZ = -1;

    private static double lastTargetX = -1;
    private static double lastTargetY = -1;
    private static double lastTargetZ = -1;

    public abstract void onUpdate();
    public abstract void onRender3D(Render3DEvent.Render3DEventData event);
}