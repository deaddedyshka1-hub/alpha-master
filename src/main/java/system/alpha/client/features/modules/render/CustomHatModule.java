package system.alpha.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.render.Render3DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.ModeSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.system.configs.FriendManager;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.ColorUtil;
import system.alpha.api.utils.color.UIColors;

import java.awt.*;

@ModuleRegister(
        name = "CustomHat",
        category = Category.RENDER,
        description = "Конус или нимб над головой."
)
public class CustomHatModule extends Module {

    @Getter private static final CustomHatModule instance = new CustomHatModule();

    private final ModeSetting mode = new ModeSetting("Режим").value("Конус").values("Конус", "Нимб");
    private final ModeSetting coneAnimation = new ModeSetting("Анимация конуса").value("Вращение").values("Вращение", "Пульсация", "Нет").setVisible(() -> mode.is("Конус"));
    private final SliderSetting size = new SliderSetting("Размер").value(0.45f).range(0.1f, 1f).step(0.05f);
    private final SliderSetting offset = new SliderSetting("Отступ от головы").value(0.2f).range(0f, 1f).step(0.05f);
    private final SliderSetting opacity = new SliderSetting("Прозрачность").value(0.85f).range(0f, 1f).step(0.01f);
    private final SliderSetting thickness = new SliderSetting("Толщина нимба").value(2.5f).range(1f, 6f).step(0.5f).setVisible(() -> mode.is("Нимб"));
    private final SliderSetting colorSpeed = new SliderSetting("Скорость градиента").value(1f).range(0.1f, 5f).step(0.1f);
    private final SliderSetting coneHeight = new SliderSetting("Высота конуса").value(0.25f).range(0.1f, 0.5f).step(0.05f).setVisible(() -> mode.is("Конус"));
    private final SliderSetting haloHeight3D = new SliderSetting("3D высота нимба").value(0.3f).range(0.1f, 1f).step(0.05f).setVisible(() -> mode.is("Нимб"));
    private final SliderSetting rotationSpeed = new SliderSetting("Скорость вращения").value(1f).range(0.1f, 3f).step(0.1f).setVisible(() -> mode.is("Конус") && coneAnimation.is("Вращение"));
    private final SliderSetting pulseSpeed = new SliderSetting("Скорость пульсации").value(1f).range(0.1f, 3f).step(0.1f).setVisible(() -> mode.is("Конус") && coneAnimation.is("Пульсация"));
    private final SliderSetting floatSpeed = new SliderSetting("Скорость плавания").value(1f).range(0.1f, 3f).step(0.1f).setVisible(() -> mode.is("Нимб"));
    private final SliderSetting floatAmount = new SliderSetting("Высота плавания").value(0.05f).range(0f, 0.2f).step(0.01f);
    private final BooleanSetting showSelf = new BooleanSetting("Показывать себя").value(true);
    private final BooleanSetting showFriends = new BooleanSetting("Показывать друзей").value(true);
    private final BooleanSetting disableFirstPerson = new BooleanSetting("Отключить от 1 лица").value(false);
    private final BooleanSetting enableFloat = new BooleanSetting("Анимация плавания").value(true);
    private final BooleanSetting enableAnimation = new BooleanSetting("Включить анимацию").value(true).setVisible(() -> mode.is("Конус") && !coneAnimation.is("Нет"));

    private static final int SEGMENTS = 96;
    private final long startTime = System.currentTimeMillis();
    private final AnimationUtil floatAnimation = new AnimationUtil();
    private final AnimationUtil pulseAnimation = new AnimationUtil();
    private float rotationAngle = 0f;

    public CustomHatModule() {
        addSettings(mode, coneAnimation, size, offset, opacity, thickness, colorSpeed, coneHeight, haloHeight3D,
                rotationSpeed, pulseSpeed, floatSpeed, floatAmount, showSelf, showFriends,
                disableFirstPerson, enableFloat, enableAnimation);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;
            if (disableFirstPerson.getValue() && mc.options.getPerspective().isFirstPerson()) return;
            if (mc.options.getPerspective().isFirstPerson() && !showSelf.getValue()) return;

            updateAnimations();

            float tickDelta = event.partialTicks();
            MatrixStack matrices = event.matrixStack();

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!shouldRender(player)) continue;
                renderCustom(matrices, player, tickDelta);
            }
        }));

        addEvents(renderEvent);
    }

    private void updateAnimations() {
        floatAnimation.update();
        pulseAnimation.update();

        if (enableFloat.getValue() && floatAnimation.isFinished()) {
            floatAnimation.run(floatAnimation.getValue() > 0.5f ? 0.0 : 1.0,
                    (long)(1000 / floatSpeed.getValue()), Easing.SINE_BOTH);
        }

        if (mode.is("Конус") && coneAnimation.is("Пульсация") && enableAnimation.getValue() && pulseAnimation.isFinished()) {
            pulseAnimation.run(pulseAnimation.getValue() > 0.5f ? 0.0 : 1.0,
                    (long)(1000 / pulseSpeed.getValue()), Easing.SINE_OUT);
        }

        if (mode.is("Конус") && coneAnimation.is("Вращение") && enableAnimation.getValue()) {
            rotationAngle += rotationSpeed.getValue() * 0.5f;
            if (rotationAngle > 360f) rotationAngle -= 360f;
        }
    }

    private boolean shouldRender(PlayerEntity player) {
        if (player == mc.player) return showSelf.getValue();
        return showFriends.getValue() && FriendManager.getInstance().getData().contains(player.getName().getString());
    }

    private void renderCustom(MatrixStack matrices, PlayerEntity player, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta);
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);

        float eye = player.getEyeHeight(player.getPose());
        float floatOffset = enableFloat.getValue() ? (float) (floatAnimation.getValue() * floatAmount.getValue()) : 0f;
        matrices.translate(0, eye + offset.getValue() + floatOffset, 0);

        float yaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));

        if (mode.is("Конус") && coneAnimation.is("Вращение") && enableAnimation.getValue()) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));
        }

        if (player.isInSneakingPose()) matrices.translate(0, -0.15f, 0);

        int baseIndex = (int) ((System.currentTimeMillis() - startTime) / 45 * colorSpeed.getValue()) % 360;

        if (mode.is("Конус")) {
            renderCone(matrices, baseIndex, player.getId());
        } else {
            renderHalo(matrices, baseIndex);
        }

        matrices.pop();
    }

    private void renderCone(MatrixStack matrices, int baseIndex, int playerId) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_CULL_FACE);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int alpha = (int) (opacity.getValue() * 255);
        float radius = size.getValue();

        float pulseMultiplier = 1f;
        if (coneAnimation.is("Пульсация") && enableAnimation.getValue()) {
            pulseMultiplier = 0.8f + (float)pulseAnimation.getValue() * 0.4f;
        }

        radius *= pulseMultiplier;
        float height = coneHeight.getValue() * pulseMultiplier;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float tipAlpha = MathHelper.clamp(opacity.getValue(), 0.0f, 1.0f);
        float baseAlpha = MathHelper.clamp(tipAlpha * 0.7f, 0.0f, 1.0f);

        for (int i = 0; i <= SEGMENTS; i++) {
            double a = i / (double) SEGMENTS * Math.PI * 2;
            float x = (float) Math.cos(a) * radius;
            float z = (float) Math.sin(a) * radius;

            int colorShift = enableAnimation.getValue() ? (int)(rotationAngle / 360 * 180) : 0;
            Color baseColor = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i + playerId * 25 + colorShift), (int)(baseAlpha * 255));
            Color tipColor = ColorUtil.setAlpha(UIColors.gradient(baseIndex + colorShift), (int)(tipAlpha * 255));

            buffer.vertex(matrix, x, 0, z).color(baseColor.getRGB());
            buffer.vertex(matrix, 0, height, 0).color(tipColor.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        GL11.glEnable(GL11.GL_CULL_FACE);
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    private void renderHalo(MatrixStack matrices, int baseIndex) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        RenderSystem.disableCull();

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int alpha = (int) (opacity.getValue() * 255);
        float radius = size.getValue();
        float thicknessValue = thickness.getValue() * 0.01f;
        float height = thicknessValue * 0.5f * haloHeight3D.getValue();

        Tessellator tess = Tessellator.getInstance();
        renderHaloRing(matrices, baseIndex, radius, thicknessValue, 0.0f, height, alpha, 1.0f, tess);
        renderHaloRing(matrices, baseIndex, radius, thicknessValue, height, 0.0f, alpha, 0.8f, tess);
        renderHaloVolume(matrices, baseIndex, radius, thicknessValue, height, alpha, tess);
        renderHaloTopCap(matrices, baseIndex, radius, thicknessValue, height, alpha, tess);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void renderHaloRing(MatrixStack matrices, int baseIndex, float radius, float thickness, float yPos, float yOffset, int alpha, float alphaMultiplier, Tessellator tess) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = i / (double) SEGMENTS * Math.PI * 2.0;
            float xInner = (float) (Math.cos(angle) * (radius - thickness));
            float zInner = (float) (Math.sin(angle) * (radius - thickness));
            float xOuter = (float) (Math.cos(angle) * (radius + thickness));
            float zOuter = (float) (Math.sin(angle) * (radius + thickness));

            Color colorInner = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i * 3), (int)(alpha * alphaMultiplier * 0.9f));
            Color colorOuter = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i * 3 + 120), (int)(alpha * alphaMultiplier * 0.7f));

            buffer.vertex(matrix, xInner, yPos + yOffset, zInner).color(colorInner.getRGB());
            buffer.vertex(matrix, xOuter, yPos + yOffset, zOuter).color(colorOuter.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderHaloVolume(MatrixStack matrices, int baseIndex, float radius, float thickness, float height, int alpha, Tessellator tess) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = i / (double) SEGMENTS * Math.PI * 2.0;
            float x = (float) (Math.cos(angle) * (radius - thickness));
            float z = (float) (Math.sin(angle) * (radius - thickness));

            Color colorBottom = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i * 2), (int)(alpha * 0.6f));
            Color colorTop = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i * 2 + 90), (int)(alpha * 0.8f));

            buffer.vertex(matrix, x, 0.0f, z).color(colorBottom.getRGB());
            buffer.vertex(matrix, x, height, z).color(colorTop.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = i / (double) SEGMENTS * Math.PI * 2.0;
            float x = (float) (Math.cos(angle) * (radius + thickness));
            float z = (float) (Math.sin(angle) * (radius + thickness));

            Color colorBottom = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i * 2 + 180), (int)(alpha * 0.5f));
            Color colorTop = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i * 2 + 270), (int)(alpha * 0.7f));

            buffer.vertex(matrix, x, 0.0f, z).color(colorBottom.getRGB());
            buffer.vertex(matrix, x, height, z).color(colorTop.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderHaloTopCap(MatrixStack matrices, int baseIndex, float radius, float thickness, float height, int alpha, Tessellator tess) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = i / (double) SEGMENTS * Math.PI * 2.0;
            float xInner = (float) (Math.cos(angle) * (radius - thickness * 0.3f));
            float zInner = (float) (Math.sin(angle) * (radius - thickness * 0.3f));
            float xOuter = (float) (Math.cos(angle) * (radius + thickness * 0.7f));
            float zOuter = (float) (Math.sin(angle) * (radius + thickness * 0.7f));

            Color color = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i * 4), (int)(alpha * 0.9f));
            buffer.vertex(matrix, xInner, height, zInner).color(color.getRGB());
            buffer.vertex(matrix, xOuter, height, zOuter).color(color.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Color centerColor = ColorUtil.setAlpha(UIColors.gradient(baseIndex), (int)(alpha * 0.7f));
        buffer.vertex(matrix, 0, height + 0.01f, 0).color(centerColor.getRGB());

        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = i / (double) SEGMENTS * Math.PI * 2.0;
            float x = (float) (Math.cos(angle) * thickness * 0.3f);
            float z = (float) (Math.sin(angle) * thickness * 0.3f);
            Color edgeColor = ColorUtil.setAlpha(UIColors.gradient(baseIndex + i * 2), (int)(alpha * 0.4f));
            buffer.vertex(matrix, x, height, z).color(edgeColor.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}