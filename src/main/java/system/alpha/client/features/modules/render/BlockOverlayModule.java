package system.alpha.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.render.Render3DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.utils.color.ColorUtil;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.render.RenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "BlockOverlay", category = Category.RENDER, description = "Рисует грани блока.")
public class BlockOverlayModule extends Module {
    @Getter
    private static final BlockOverlayModule instance = new BlockOverlayModule();

    // Настройки обводки
    private final SliderSetting lineWidth = new SliderSetting("Толщина линии")
            .value(2.0f)
            .range(1.0f, 5.0f)
            .step(0.1f);

    private final SliderSetting outlineAlpha = new SliderSetting("Прозрачность обводки")
            .value(150f)
            .range(0f, 255f)
            .step(1f);

    // Настройки заливки
    private final BooleanSetting fill = new BooleanSetting("Заливка")
            .value(false);

    private final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки")
            .value(50f)
            .range(0f, 255f)
            .step(1f)
            .setVisible(fill::getValue);

    public BlockOverlayModule() {
        addSettings(lineWidth, outlineAlpha, fill, fillAlpha);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null || mc.crosshairTarget == null) return;

            HitResult hitResult = mc.crosshairTarget;
            if (hitResult.getType() != HitResult.Type.BLOCK) return;

            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();
            float tickDelta = event.partialTicks();

            // Проверяем расстояние
            Vec3d playerPos = mc.player.getLerpedPos(tickDelta);
            Vec3d blockCenter = Vec3d.ofCenter(blockPos);
            double distance = playerPos.distanceTo(blockCenter);

            if (distance > 100.0f) return;

            // Получаем цвета из темы
            Color outlineColor = getOutlineColor();
            Color outlineColorSecondary = getOutlineColorSecondary();

            // Получаем форму контура блока
            VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

            MatrixStack matrixStack = event.matrixStack();
            RenderUtil.WORLD.startRender(matrixStack);

            // Если форма пустая, используем полный блок
            if (shape.isEmpty()) {
                Box blockBox = new Box(blockPos).expand(0.001);
                renderBlockOverlay(matrixStack, blockBox, outlineColor, outlineColorSecondary, tickDelta);
            } else {
                // Рендерим все хитбоксы формы
                List<Box> boxes = new ArrayList<>();
                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    boxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ)
                            .offset(blockPos)
                            .expand(0.001));
                });

                for (Box box : boxes) {
                    renderBlockOverlay(matrixStack, box, outlineColor, outlineColorSecondary, tickDelta);
                }
            }

            RenderUtil.WORLD.endRender(matrixStack);
        }));

        addEvents(renderEvent);
    }

    private Color getOutlineColor() {
        return UIColors.gradient(0, outlineAlpha.getValue().intValue());
    }

    private Color getOutlineColorSecondary() {
        return UIColors.gradient(90, outlineAlpha.getValue().intValue());
    }

    private Color getFillColor() {
        return UIColors.gradient(0, fillAlpha.getValue().intValue());
    }

    private Color getFillColorSecondary() {
        return UIColors.gradient(90, fillAlpha.getValue().intValue());
    }

    private void renderBlockOverlay(MatrixStack matrices, Box box, Color outlineColor, Color outlineColorSecondary, float tickDelta) {
        // Настройки для рендеринга сквозь стены
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Рендерим заливку
        if (fill.getValue()) {
            Color fillColor = getFillColor();
            Color fillColorSecondary = getFillColorSecondary();
            renderBoxFillGradient(matrices, box, fillColor, fillColorSecondary);
        }

        // Рендерим обводку
        renderBoxOutlineGradient(matrices, box, outlineColor, outlineColorSecondary, lineWidth.getValue().floatValue());

        // Восстанавливаем настройки
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void renderBoxFillGradient(MatrixStack matrices, Box box, Color startColor, Color endColor) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.getEntityRenderDispatcher().camera.getPos();

        // Переводим в локальные координаты
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);

        float r1 = startColor.getRed() / 255.0f;
        float g1 = startColor.getGreen() / 255.0f;
        float b1 = startColor.getBlue() / 255.0f;
        float a1 = startColor.getAlpha() / 255.0f;

        float r2 = endColor.getRed() / 255.0f;
        float g2 = endColor.getGreen() / 255.0f;
        float b2 = endColor.getBlue() / 255.0f;
        float a2 = endColor.getAlpha() / 255.0f;

        // Верхняя грань
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);

        // Нижняя грань
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);

        // Передняя грань
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);

        // Задняя грань
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);

        // Левая грань
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);

        // Правая грань
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderBoxOutlineGradient(MatrixStack matrices, Box box, Color startColor, Color endColor, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.getEntityRenderDispatcher().camera.getPos();

        // Вычисляем расстояние до центра блока
        Vec3d boxCenter = box.getCenter();
        double distance = camera.distanceTo(boxCenter);

        // Корректируем толщину линии
        float adjustedLineWidth = Math.max(0.5f, lineWidth * (float)(10.0 / Math.max(distance, 1.0)));

        // Устанавливаем ширину линии
        RenderSystem.lineWidth(adjustedLineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Переводим в локальные координаты
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);

        float r1 = startColor.getRed() / 255.0f;
        float g1 = startColor.getGreen() / 255.0f;
        float b1 = startColor.getBlue() / 255.0f;
        float a1 = startColor.getAlpha() / 255.0f;

        float r2 = endColor.getRed() / 255.0f;
        float g2 = endColor.getGreen() / 255.0f;
        float b2 = endColor.getBlue() / 255.0f;
        float a2 = endColor.getAlpha() / 255.0f;

        // Нижнее основание
        buffer.vertex(matrix, minX, minY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);

        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);

        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r1, g1, b1, a1);

        // Верхнее основание
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);

        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);

        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);

        // Вертикальные ребра
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}