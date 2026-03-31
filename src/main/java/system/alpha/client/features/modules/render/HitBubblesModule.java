package system.alpha.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.player.world.AttackEvent;
import system.alpha.api.event.events.render.Render3DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.ModeSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.system.files.FileUtil;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.math.TimerUtil;
import system.alpha.api.utils.render.RenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ModuleRegister(name = "Hit Bubbles", category = Category.RENDER, description = "При ударе создаёт пузырь под углом.")
public class HitBubblesModule extends Module {
    @Getter
    private static final HitBubblesModule instance = new HitBubblesModule();

    // Настройки
    private final SliderSetting size = new SliderSetting("Размер")
            .value(0.5f)
            .range(0.2f, 2.0f)
            .step(0.05f);

    private final ModeSetting texture = new ModeSetting("Текстура").value("Обычный").values("Обычный", "Тонкий");

    private final SliderSetting lifeTime = new SliderSetting("Время жизни")
            .value(0.8f)
            .range(0.2f, 2.0f)
            .step(0.05f);

    private final SliderSetting spawnDuration = new SliderSetting("Длительность появления")
            .value(400f)
            .range(100f, 1000f)
            .step(50f);


    private String texture() {
        return "circle/" + (texture.is("Обычный") ? "bubble" : "glow_fat");
    }

    private final List<HitBubble> hitBubbles = new ArrayList<>();
    private final TimerUtil timerUtil = new TimerUtil();

    public HitBubblesModule() {
        addSettings(texture, size, lifeTime, spawnDuration);
    }

    @Override
    public void onEvent() {
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;

            Entity target = event.entity();
            if (target == null) return;

            if (!(target instanceof LivingEntity livingEntity)) return;
            if (!livingEntity.isAlive()) return;

            if (livingEntity instanceof PlayerEntity) {
                return;
            }

            Vec3d hitPos = getHitPosition(livingEntity);
            if (hitPos == null) {
                hitPos = livingEntity.getPos().add(0, livingEntity.getHeight() / 2f, 0);
            }

            Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
            Vec3d toCamera = cameraPos.subtract(hitPos).normalize();

            float yaw = (float) Math.toDegrees(Math.atan2(toCamera.z, toCamera.x));
            float pitch = (float) Math.toDegrees(Math.atan2(-toCamera.y,
                    Math.sqrt(toCamera.x * toCamera.x + toCamera.z * toCamera.z)));

            hitBubbles.add(new HitBubble(
                    hitPos,
                    (long) (lifeTime.getValue() * 1000),
                    yaw,
                    pitch,
                    spawnDuration.getValue().intValue()
            ));
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;

            hitBubbles.removeIf(HitBubble::shouldRemove);

            MatrixStack matrixStack = event.matrixStack();
            RenderUtil.WORLD.startRender(matrixStack);

            for (HitBubble bubble : hitBubbles) {
                float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
                renderBubble(matrixStack, bubble, tickDelta);
            }

            RenderUtil.WORLD.endRender(matrixStack);
        }));

        addEvents(attackEvent, renderEvent);
    }

    private Vec3d getHitPosition(LivingEntity entity) {
        HitResult hitResult = mc.crosshairTarget;
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            if (entityHitResult.getEntity() == entity) {
                return entityHitResult.getPos();
            }
        }

        return computeHitOnEntityAABB(entity);
    }

    private Vec3d computeHitOnEntityAABB(LivingEntity entity) {
        if (mc.player == null) return null;

        Vec3d start = mc.player.getEyePos();
        Vec3d dir = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(6.0));

        Box bb = entity.getBoundingBox();

        Optional<Vec3d> result = bb.raycast(start, end);
        return result.orElse(null);
    }

    private void renderBubble(MatrixStack matrices, HitBubble bubble, float tickDelta) {
        long age = timerUtil.getElapsedTime() - bubble.spawnTime;
        long totalLifetime = bubble.lifetime;

        if (age > totalLifetime) return;

        bubble.growAnimation.update();
        bubble.growAnimation.run(1.0, bubble.spawnDuration, Easing.SINE_OUT);

        float growValue = (float) bubble.growAnimation.getValue();

        float timeFade = (float) Math.pow(1.0 - (age / (float) totalLifetime), 0.8);
        float alphaFactor = Math.max(0f, Math.min(1f, growValue * timeFade));
        int alpha = (int) (255 * alphaFactor);

        if (alpha <= 2) return;

        float bubbleSize = size.getValue() * (0.6f + 0.4f * growValue);

        Color bubbleColor = UIColors.gradient(0, alpha);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, FileUtil.getImage(texture()));
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO
        );
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();

        double yawRad = Math.toRadians(bubble.rotationYaw);
        double pitchRad = Math.toRadians(bubble.rotationPitch);

        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        Vec3d forward = new Vec3d(
                cosYaw * cosPitch,
                -sinPitch,
                sinYaw * cosPitch
        ).normalize();

        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = worldUp.crossProduct(forward).normalize();
        Vec3d up = forward.crossProduct(right).normalize();

        Vec3d scaledRight = right.multiply(bubbleSize);
        Vec3d scaledUp = up.multiply(bubbleSize);

        Vec3d topLeft = bubble.position.add(scaledUp).subtract(scaledRight);
        Vec3d topRight = bubble.position.add(scaledUp).add(scaledRight);
        Vec3d bottomLeft = bubble.position.subtract(scaledUp).subtract(scaledRight);
        Vec3d bottomRight = bubble.position.subtract(scaledUp).add(scaledRight);

        topLeft = topLeft.subtract(cameraPos);
        topRight = topRight.subtract(cameraPos);
        bottomLeft = bottomLeft.subtract(cameraPos);
        bottomRight = bottomRight.subtract(cameraPos);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float red = bubbleColor.getRed() / 255.0f;
        float green = bubbleColor.getGreen() / 255.0f;
        float blue = bubbleColor.getBlue() / 255.0f;
        float alphaValue = bubbleColor.getAlpha() / 255.0f;

        buffer.vertex(matrix, (float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z)
                .texture(0.0f, 1.0f).color(red, green, blue, alphaValue);
        buffer.vertex(matrix, (float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z)
                .texture(1.0f, 1.0f).color(red, green, blue, alphaValue);
        buffer.vertex(matrix, (float) topRight.x, (float) topRight.y, (float) topRight.z)
                .texture(1.0f, 0.0f).color(red, green, blue, alphaValue);
        buffer.vertex(matrix, (float) topLeft.x, (float) topLeft.y, (float) topLeft.z)
                .texture(0.0f, 0.0f).color(red, green, blue, alphaValue);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private class HitBubble {
        final Vec3d position;
        final long spawnTime;
        final long lifetime;
        final float rotationYaw;
        final float rotationPitch;
        final int spawnDuration;
        final AnimationUtil growAnimation;

        HitBubble(Vec3d position, long lifetime, float rotationYaw, float rotationPitch, int spawnDuration) {
            this.position = position;
            this.spawnTime = timerUtil.getElapsedTime();
            this.lifetime = lifetime;
            this.rotationYaw = rotationYaw;
            this.rotationPitch = rotationPitch;
            this.spawnDuration = spawnDuration;
            this.growAnimation = new AnimationUtil();
        }

        boolean shouldRemove() {
            return timerUtil.getElapsedTime() - spawnTime > lifetime;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        timerUtil.reset();
        hitBubbles.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hitBubbles.clear();
    }
}