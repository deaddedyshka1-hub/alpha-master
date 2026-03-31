package system.alpha.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult;
import system.alpha.api.event.Listener;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.events.render.Render2DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.utils.color.ColorUtil;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.render.display.RectRender;
import org.joml.Vector4f;

import java.awt.*;

@ModuleRegister(name = "Crosshair", category = Category.RENDER, description = "Изменяет ванильный прицел.")
public class CrosshairModule extends Module {
    @Getter private static final CrosshairModule instance = new CrosshairModule();

    // Настройки прицела
    private final SliderSetting thickness = new SliderSetting("Толщина")
            .value(1f)
            .range(0.5f, 3f)
            .step(0.1f);

    private final SliderSetting length = new SliderSetting("Длина")
            .value(3f)
            .range(1f, 8f)
            .step(0.5f);

    private final SliderSetting gap = new SliderSetting("Разрыв")
            .value(2f)
            .range(0f, 5f)
            .step(0.5f);

    private final BooleanSetting dynamicGap = new BooleanSetting("Динамический разрыв")
            .value(false);

    private final BooleanSetting useEntityColor = new BooleanSetting("Цвет при наведении").value(true);

    private final SliderSetting colorSpeed = new SliderSetting("Скорость градиента")
            .value(1f)
            .range(0.1f, 5f)
            .step(0.1f);

    // Настройки скругления
    private final SliderSetting rounding = new SliderSetting("Скругление")
            .value(0f)
            .range(0f, 10f)
            .step(0.5f);

    private final BooleanSetting dynamic = new BooleanSetting("Динамический")
            .value(true)
            .setVisible(() -> dynamicGap.getValue());

    private final RectRender rectRenderer = new RectRender();
    private long startTime = System.currentTimeMillis();

    public CrosshairModule() {
        addSettings(
                thickness, length, gap, dynamicGap,
                useEntityColor, colorSpeed, rounding, dynamic
        );
    }

    @Override
    public void onEvent() {
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            // Проверка режима от третьего лица
            if (!mc.options.getPerspective().isFirstPerson()) {
                return;
            }

            // Получаем размеры экрана
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            float centerX = screenWidth * 0.5f;
            float centerY = screenHeight * 0.5f;

            // Рассчитываем разрыв с динамикой
            float currentGap = gap.getValue();
            if (dynamicGap.getValue()) {
                float cooldown = 1f - mc.player.getAttackCooldownProgress(0);
                currentGap = Math.min(currentGap + 8f * cooldown, 10f);

                // Если включена динамика, добавляем плавность
                if (dynamic.getValue()) {
                    currentGap += Math.sin(System.currentTimeMillis() / 200.0) * 0.5f;
                }
            }

            float lineThickness = thickness.getValue();
            float lineLength = length.getValue();
            float cornerRadius = rounding.getValue();

            // Получаем цвета как в JumpCircle
            Color[] colors = getCrosshairColors();

            MatrixStack matrixStack = event.matrixStack();
            Vector4f radius = new Vector4f(cornerRadius);

            // Рисуем каждую линию своим цветом (как 4 цвета в JumpCircle)

            // Верхняя линия (цвет 1)
            rectRenderer.draw(matrixStack,
                    centerX - lineThickness / 2,
                    centerY - currentGap - lineLength,
                    lineThickness, lineLength, radius, colors[0]);

            // Нижняя линия (цвет 2)
            rectRenderer.draw(matrixStack,
                    centerX - lineThickness / 2,
                    centerY + currentGap,
                    lineThickness, lineLength, radius, colors[1]);

            // Левая линия (цвет 3)
            rectRenderer.draw(matrixStack,
                    centerX - currentGap - lineLength,
                    centerY - lineThickness / 2,
                    lineLength, lineThickness, radius, colors[2]);

            // Правая линия (цвет 4)
            rectRenderer.draw(matrixStack,
                    centerX + currentGap,
                    centerY - lineThickness / 2,
                    lineLength, lineThickness, radius, colors[3]);
        }));

        addEvents(render2DEvent);
    }

    /**
     * Получает 4 цвета для прицела как в JumpCircle
     */
    private Color[] getCrosshairColors() {
        int baseIndex = (int) ((System.currentTimeMillis() - startTime) / 50 * colorSpeed.getValue()) % 360;

        // Если наведены на сущность и включена опция
        if (useEntityColor.getValue() &&
                mc.crosshairTarget != null &&
                mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {

            // Создаем 4 вариации красного с градиентом
            return new Color[]{
                    ColorUtil.setAlpha(new Color(255, 50, 50), 255),     // Светло-красный
                    ColorUtil.setAlpha(new Color(255, 0, 0), 255),       // Красный
                    ColorUtil.setAlpha(new Color(200, 0, 0), 255),       // Темно-красный
                    ColorUtil.setAlpha(new Color(255, 100, 100), 255)    // Розоватый
            };
        }

        // Обычные цвета с градиентом как в JumpCircle
        return new Color[]{
                UIColors.gradient(baseIndex),           // Цвет 1
                UIColors.gradient(baseIndex + 90),      // Цвет 2 (+90°)
                UIColors.gradient(baseIndex + 180),     // Цвет 3 (+180°)
                UIColors.gradient(baseIndex + 240)      // Цвет 4 (+240°)
        };
    }

    /**
     * Альтернативный вариант: один цвет для всего прицела с анимацией
     */
    private Color getAnimatedColor() {
        int index = (int) ((System.currentTimeMillis() - startTime) / 50 * colorSpeed.getValue()) % 360;
        return UIColors.gradient(index);
    }
}