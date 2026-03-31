package system.alpha.client.ui.clickgui.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import system.alpha.api.module.Module;
import system.alpha.api.module.setting.*;
import system.alpha.api.system.backend.KeyStorage;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.math.MouseUtil;
import system.alpha.api.utils.color.ColorUtil;
import system.alpha.api.utils.math.TimerUtil;
import system.alpha.api.utils.render.RenderUtil;
import system.alpha.api.utils.render.ScissorUtil;
import system.alpha.api.utils.render.fonts.Fonts;
import system.alpha.client.ui.clickgui.module.settings.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static system.alpha.api.utils.other.Wrapper.mc;

@Getter
public class ModuleComponent extends ExpandableComponent {
    private final List<SettingComponent> settings = new ArrayList<>();
    private final Module module;
    @Setter private float round;
    @Setter private boolean last;
    @Setter private int index;

    private boolean bind;

    // Изменяем модификатор доступа на public
    public boolean showTooltip = false;
    public final TimerUtil tooltipTimer = new TimerUtil();

    private final AnimationUtil enableAnimation = new AnimationUtil();
    private final AnimationUtil bindAnimation = new AnimationUtil();
    private final AnimationUtil hoverAnimation = new AnimationUtil();

    public ModuleComponent(Module module) {
        this.module = module;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                settings.add(new BooleanComponent(bool));
            }

            if (setting instanceof MultiBooleanSetting multi) {
                settings.add(new MultiBooleanComponent(multi));
            }

            if (setting instanceof ModeSetting mode) {
                settings.add(new ModeComponent(mode));
            }

            if (setting instanceof SliderSetting slider) {
                settings.add(new SliderComponent(slider));
            }

            if (setting instanceof ColorSetting color) {
                settings.add(new ColorComponent(color));
            }

            if (setting instanceof RunSetting DoniKuni) {
                settings.add(new ButtonComponent(DoniKuni));
            }

            if (setting instanceof BindSetting sex) {
                settings.add(new BindComponent(sex));
            }
        }

        enableAnimation.setValue(module.isEnabled() ? 1.0 : 0.0);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();
        hoverAnimation.update();
        enableAnimation.update();
        bindAnimation.update();
        bindAnimation.run(bind ? 1.0 : 0.0, 400, Easing.EXPO_OUT);

        // Проверяем, наведена ли мышь на элемент
        boolean isHovered = MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getDefaultHeight());
        hoverAnimation.run(isHovered ? 1.0 : 0.0, 300, Easing.QUINT_OUT);
        enableAnimation.run(module.isEnabled() ? 1.0 : 0.0, 200, Easing.EXPO_OUT);

        // Управление показом подсказки
        if (isHovered) {
            if (!showTooltip) {
                if (tooltipTimer.getElapsedTime() >= 500) {
                    showTooltip = true;
                }
            }
        } else {
            showTooltip = false;
            tooltipTimer.reset();
        }

        int fullAlpha = (int) (getAlpha() * 255f);

        Color rectColor1 = ColorUtil.setAlpha(ColorUtil.interpolate(UIColors.gradient(index), UIColors.blur(), enableAnimation.getValue()), fullAlpha);
        Color rectColor2 = ColorUtil.setAlpha(ColorUtil.interpolate(UIColors.gradient(index + 45), UIColors.blur(), enableAnimation.getValue()), fullAlpha);

        String bindText = (bind ? "Binding: " : "Bind: ") + KeyStorage.getBind(module.getBind());
        String defaultText = module.getName();

        float fontSize = getDefaultHeight() * 0.47f + scaled((float) hoverAnimation.getValue());

        float openAnim = getAnim();
        float finalRound = isLast() ? (getRound() * 2f) * (1f - openAnim) : 0f;
        Vector4f round = new Vector4f(0f, 0f, finalRound, finalRound);

        float nameAnim = 1f - (float) bindAnimation.getValue();
        float bindAnim = (float) bindAnimation.getValue();

        int bindAlpha1 = (int) (nameAnim * getAlpha() * 255f);
        int bindAlpha2 = (int) (bindAnim * getAlpha() * 255f);

        Color textColor1 = ColorUtil.interpolate(UIColors.textColor(bindAlpha1), UIColors.inactiveTextColor(bindAlpha1), enableAnimation.getValue());
        Color textColor2 = ColorUtil.interpolate(UIColors.textColor(bindAlpha2), UIColors.inactiveTextColor(bindAlpha2), enableAnimation.getValue());

        boolean huesos = bindAnim > 0;

        if (openAnim > 0.0) moduleSetting(context, mouseX, mouseY, delta);
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getDefaultHeight(), round, rectColor1, rectColor1, rectColor2, rectColor2);

        if (huesos) ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getDefaultHeight());
        if (nameAnim > 0) Fonts.PS_BOLD.drawCenteredText(matrixStack, defaultText, getX() + (getWidth() / 2f - offset() * openAnim) * nameAnim, getY() + getDefaultHeight() / 2f - fontSize / 2f, fontSize, textColor1);
        if (bindAnim > 0) Fonts.PS_BOLD.drawCenteredText(matrixStack, bindText, getX() + (getWidth() / 2f) + getWidth() * nameAnim, getY() + getDefaultHeight() / 2f - fontSize / 2f, fontSize, textColor2);
        if (huesos) ScissorUtil.stop(matrixStack);

        // Убираем отрисовку подсказки отсюда
        // Она будет отрисовываться в Panel/ScreenClickGUI
    }

    // Добавляем геттер для описания
    public String getDescription() {
        return module.getDescription();
    }

    // Метод для получения размеров подсказки
    public float[] getTooltipDimensions(float fontSize) {
        String description = module.getDescription();
        List<String> lines = wrapText(description, 200, fontSize);

        float padding = 5f;
        float lineHeight = 10f;
        float tooltipWidth = 0f;

        for (String line : lines) {
            float lineWidth = Fonts.PS_REGULAR.getWidth(line, fontSize);
            if (lineWidth > tooltipWidth) {
                tooltipWidth = lineWidth;
            }
        }

        float tooltipHeight = lines.size() * lineHeight + padding * 2f;

        return new float[]{tooltipWidth + padding * 2, tooltipHeight};
    }

    // Метод для отрисовки подсказки (будет вызываться из Panel)
    public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrixStack = context.getMatrices();
        String description = module.getDescription();

        List<String> lines = wrapText(description, 200, 9f);

        float padding = 5f;
        float lineHeight = 10f;
        float tooltipWidth = 0f;

        for (String line : lines) {
            float lineWidth = Fonts.PS_REGULAR.getWidth(line, 9f);
            if (lineWidth > tooltipWidth) {
                tooltipWidth = lineWidth;
            }
        }

        float tooltipHeight = lines.size() * lineHeight + padding * 2f;

        // Позиционируем подсказку рядом с курсором
        float tooltipX = mouseX + 10f;
        float tooltipY = mouseY + 10f;

        // Проверяем, чтобы подсказка не выходила за границы экрана
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        if (tooltipX + tooltipWidth > screenWidth) {
            tooltipX = mouseX - tooltipWidth - 10f;
        }

        if (tooltipY + tooltipHeight > screenHeight) {
            tooltipY = mouseY - tooltipHeight - 10f;
        }

        // Рисуем фон подсказки
        RenderUtil.BLUR_RECT.draw(matrixStack, tooltipX, tooltipY, tooltipWidth + padding * 2, tooltipHeight,
                new Vector4f(5f, 5f, 5f, 5f),
                UIColors.blur(230),
                UIColors.blur(230),
                UIColors.blur(180),
                UIColors.blur(180)
        );

        // Рисуем текст
        float textY = tooltipY + padding;
        for (String line : lines) {
            Fonts.PS_REGULAR.drawText(matrixStack, line, tooltipX + padding, textY, 9f, UIColors.textColor(255));
            textY += lineHeight;
        }
    }

    private List<String> wrapText(String text, float maxWidth, float fontSize) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            float testWidth = Fonts.PS_REGULAR.getWidth(testLine, fontSize);

            if (testWidth <= maxWidth) {
                if (!currentLine.isEmpty()) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    // ... остальной код без изменений
    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bind) {
            boolean deleteButton = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE;
            module.setBind(deleteButton ? -999 : keyCode);
            bind = false;
        }

        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.9) continue;
            setting.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean hoveredToDefault = MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getDefaultHeight());

        if (bind && button != 1 && button != 2 && button != 0) {
            module.setBind(-100 + button);
            bind = false;
            return;
        }

        if (hoveredToDefault) {
            switch (button) {
                case 0 -> module.toggle();
                case 1 -> {
                    if (!settings.isEmpty()) {
                        toggleOpen();
                    }

                    if (!isOpen()) {
                        for (SettingComponent setting : settings) {
                            if (setting instanceof ExpandableSettingComponent e) {
                                e.setOpen(false);
                            }
                        }
                    }
                }

                case 2 -> bind = !bind;
            }

            return;
        }

        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.9) continue;
            setting.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (isNotOver()) return;

        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.9) continue;
            setting.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    }

    private void moduleSetting(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        float openAnim = getAnim();
        float reverseAnim = 1f - openAnim;
        float animPad = gap() * reverseAnim;

        float finalRound = isLast() ? (getRound() * 2f) * openAnim : 0f;
        Vector4f round = new Vector4f(0f, 0f, finalRound, finalRound);

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY() + getDefaultHeight(), getWidth(), getHeight() - getDefaultHeight(), round, UIColors.widgetBlur((int) (getAlpha() * openAnim * 255f)));

        float govnarik = offset() * (1f + 0.7f * reverseAnim);

        float doni = gap() - animPad;
        float componentY = getY() + (getDefaultHeight() * openAnim) + doni;

        for (SettingComponent setting : settings) {
            setting.getVisibleAnimation().update();
            setting.getVisibleAnimation().run(setting.getSetting().isVisible() ? 1.0 : 0.0, 120, Easing.SINE_OUT);
            float visibleAnim = (float) setting.getVisibleAnimation().getValue();
            if (setting.getVisibleAnimation().getValue() > 0.0) {
                setting.setX(getX() + govnarik);
                setting.setY(componentY);
                setting.setWidth(getWidth() - govnarik * 2f);
                setting.setAlpha(visibleAnim * openAnim * getAlpha());

                setting.render(context, mouseX, mouseY, delta);
                componentY += ((setting.getHeight() + gap()) * visibleAnim) * openAnim;
            }
        }
    }

    public float getDefaultHeight() {
        return scaled(17f);
    }
}