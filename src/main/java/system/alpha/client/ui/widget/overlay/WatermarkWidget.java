package system.alpha.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import system.alpha.api.system.backend.ClientInfo;
import system.alpha.api.system.configs.WidgetConfigManager;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.render.RenderUtil;
import system.alpha.api.utils.render.fonts.Font;
import system.alpha.client.ui.widget.Widget;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class WatermarkWidget extends Widget {
    private float watermarkWidth = 0f;
    private float watermarkHeight = 0f;

    private boolean showBPS = false;
    private boolean showFPS = true;
    private boolean showXYZ = false;

    private float settingsAnim;
    private boolean settingsOpen;
    private float settingsX, settingsY;
    private boolean wasRightClick;
    private boolean wasLeftClick;

    private final AnimationUtil settingsScaleAnimation = new AnimationUtil();
    private final AnimationUtil chatIndicatorAnimation = new AnimationUtil();

    private boolean wasChatOpen = false;
    private float lastWatermarkX, lastWatermarkY, lastWatermarkWidth;

    // Переменные для размеров виджетов
    private float fpsWidth = 0;
    private float bpsWidth = 0;
    private float xyzWidth = 0;

    @Override
    public String getName() {
        return "Watermark";
    }

    public WatermarkWidget() {
        super(3f, 3f);
        updateWidgetPositions();
    }

    // Геттеры и сеттеры для настроек
    public boolean isShowBPS() {
        return showBPS;
    }

    public void setShowBPS(boolean showBPS) {
        this.showBPS = showBPS;
        saveConfig();
    }

    public boolean isShowFPS() {
        return showFPS;
    }

    public void setShowFPS(boolean showFPS) {
        this.showFPS = showFPS;
        saveConfig();
    }

    public boolean isShowXYZ() {
        return showXYZ;
    }

    public void setShowXYZ(boolean showXYZ) {
        this.showXYZ = showXYZ;
        saveConfig();
    }

    // Загрузка конфигурации
    public void loadConfig() {
        WidgetConfigManager configManager =
                WidgetConfigManager.getInstance();

        showBPS = configManager.getBoolean("Watermark", "showBPS", false);
        showFPS = configManager.getBoolean("Watermark", "showFPS", true);
        showXYZ = configManager.getBoolean("Watermark", "showXYZ", false);
    }

    // Сохранение конфигурации
    private void saveConfig() {
        WidgetConfigManager configManager =
                WidgetConfigManager.getInstance();

        configManager.setValue("Watermark", "showBPS", showBPS);
        configManager.setValue("Watermark", "showFPS", showFPS);
        configManager.setValue("Watermark", "showXYZ", showXYZ);

        configManager.save();
    }

    @Override
    public void render(MatrixStack matrixStack) {
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        settingsScaleAnimation.update();
        chatIndicatorAnimation.update();

        // Проверяем, изменилось ли состояние чата
        if (chatOpen && !wasChatOpen) {
            // Чат только что открылся - запускаем анимацию
            chatIndicatorAnimation.run(1.0, 300, Easing.BACK_OUT);
        } else if (!chatOpen && wasChatOpen) {
            // Чат только что закрылся - запускаем обратную анимацию
            chatIndicatorAnimation.run(0.0, 200, Easing.CUBIC_IN);
            settingsOpen = false;
            // Также сбрасываем анимацию настроек
            settingsScaleAnimation.reset();
        }

        // Обновляем флаг
        wasChatOpen = chatOpen;

        // Также можно добавить защиту на случай, если анимация не активна, но чат открыт
        if (chatOpen && !chatIndicatorAnimation.isActive()) {
            chatIndicatorAnimation.setValue(1.0);
        }

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float gap = getGap();

        float width = 0f;
        float height = 0f;

        float headSize = scaled(29f);
        boolean isRight = x > mc.getWindow().getScaledWidth() / 2f;

        float headX = !isRight ? x : x + getDraggable().getWidth() - headSize;

        matrixStack.push();
        float centerX = headX + headSize / 2;
        float centerY = y + headSize / 2;
        matrixStack.translate(centerX, centerY, 0);
        float scale = 1.0f + (float)chatIndicatorAnimation.getValue() * 0.1f;
        matrixStack.scale(scale, scale, 1);
        matrixStack.translate(-centerX, -centerY, 0);

        RenderUtil.TEXTURE_RECT.drawHead(matrixStack, mc.player, headX, y, headSize, headSize,
                getGap() / 2f, 0f, Color.WHITE);
        matrixStack.pop();

        width += headSize + gap;
        height += headSize;

        float pillsStartX = !isRight ? x + headSize + gap : x;

        // Рендерим название клиента и XYZ рядом с ним
        renderClientNameWithXYZ(matrixStack, pillsStartX, y, headSize);

        float pillsBottomStartY = y + headSize - watermarkHeight;

        // Рендерим приветствие и рядом FPS/BPS
        renderGreetingWithWidgets(matrixStack, pillsStartX, pillsBottomStartY);

        // Обновляем размеры ватермарки
        updateWatermarkSize(matrixStack, x, y, headSize);

        // Проверяем, изменилась ли позиция ватермарки
        if (lastWatermarkX != x || lastWatermarkY != y || lastWatermarkWidth != width) {
            updateWidgetPositions();
            lastWatermarkX = x;
            lastWatermarkY = y;
            lastWatermarkWidth = width;
        }

        if (chatOpen) {
            handleInteraction(matrixStack, x, y, getDraggable().getWidth(), getDraggable().getHeight());
        }
    }

    private void renderClientNameWithXYZ(MatrixStack matrixStack, float startX, float startY, float headSize) {
        String clientName = getClientName() + getClientVersion();

        // Рендерим название клиента
        float[] namePill = drawPill(matrixStack, startX, startY, clientName, 0);
        float namePillWidth = namePill[2];
        watermarkWidth = namePillWidth;
        watermarkHeight = namePill[3];

        // Рендерим XYZ справа от названия клиента, если включен
        if (showXYZ && mc.player != null) {
            renderXYZ(matrixStack, startX + namePillWidth + getGap(), startY);
        }
    }

    private void renderGreetingWithWidgets(MatrixStack matrixStack, float startX, float startY) {
        String greeting = getGreeting();

        // Сначала рендерим приветствие
        float[] greetingPill = drawPill(matrixStack, startX, startY, greeting, 0);
        float greetingWidth = greetingPill[2];
        float greetingHeight = greetingPill[3];

        // Позиция для FPS/BPS справа от приветствия
        float widgetsX = startX + greetingWidth + getGap();
        float widgetsY = startY;

        // Рендерим FPS и BPS в строку
        if (showFPS) {
            renderFPS(matrixStack, widgetsX, widgetsY);
            if (showBPS) {
                // Если BPS тоже включен, рендерим его справа от FPS
                renderBPS(matrixStack, widgetsX + fpsWidth + getGap(), widgetsY);
            }
        } else if (showBPS) {
            // Если только BPS включен
            renderBPS(matrixStack, widgetsX, widgetsY);
        }
    }

    private void updateWatermarkSize(MatrixStack matrixStack, float x, float y, float headSize) {
        float gap = getGap();
        float totalWidth = headSize + gap + watermarkWidth;
        float totalHeight = headSize;

        // Добавляем ширину XYZ, если он включен
        if (showXYZ && mc.player != null) {
            totalWidth += getGap() + xyzWidth;
        }

        // Рассчитываем ширину строки с приветствием и виджетами
        String greeting = getGreeting();
        float greetingWidth = getMediumFont().getWidth(greeting, scaled(7.5f)) + getGap() * 1.8f;

        // Рассчитываем ширину виджетов FPS/BPS
        float widgetsWidth = 0;
        if (showFPS) {
            fpsWidth = getWidgetWidth(WidgetInfo.FPS);
            widgetsWidth += fpsWidth;
        }
        if (showBPS) {
            bpsWidth = getWidgetWidth(WidgetInfo.BPS);
            if (showFPS) widgetsWidth += getGap();
            widgetsWidth += bpsWidth;
        }

        // Общая ширина строки с приветствием и виджетами
        float greetingRowWidth = greetingWidth;
        if (widgetsWidth > 0) {
            greetingRowWidth += getGap() + widgetsWidth;
        }

        // Берем максимальную ширину из двух строк (клиент+XYZ и приветствие+FPS/BPS)
        float firstRowWidth = watermarkWidth;
        if (showXYZ && mc.player != null) {
            firstRowWidth += getGap() + xyzWidth;
        }

        totalWidth = Math.max(firstRowWidth, greetingRowWidth);

        // Высота остается как высота головы (обе строки на одной высоте)
        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(totalHeight);
    }

    private void renderBPS(MatrixStack matrixStack, float x, float y) {
        if (!showBPS || mc.player == null) return;

        float gap = getGap();
        float fontSize = scaled(7.5f);

        float bpsValue = getBPS();
        String value = String.format("%.2f", bpsValue);
        String text = "BPS: " + value;

        float textWidth = getSemiBoldFont().getWidth(text, fontSize);
        float backgroundWidth = textWidth + gap * 2f;
        float backgroundHeight = fontSize + gap * 2f;
        float round = backgroundHeight * 0.3f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round, UIColors.widgetBlur());

        float textX = x + gap;
        float textY = y + gap;

        getSemiBoldFont().drawGradientText(matrixStack, "BPS: ", textX, textY, fontSize,
                UIColors.primary(), UIColors.secondary(), textWidth / 4f);
        getSemiBoldFont().drawText(matrixStack, value, textX + getSemiBoldFont().getWidth("BPS: ", fontSize),
                textY, fontSize, UIColors.textColor());

        bpsWidth = backgroundWidth;
    }

    private void renderFPS(MatrixStack matrixStack, float x, float y) {
        if (!showFPS) return;

        float gap = getGap();
        float fontSize = scaled(7.5f);

        int fpsValue = mc.getCurrentFps();
        String value = String.valueOf(fpsValue);
        String text = "FPS: " + value;

        float textWidth = getSemiBoldFont().getWidth(text, fontSize);
        float backgroundWidth = textWidth + gap * 2f;
        float backgroundHeight = fontSize + gap * 2f;
        float round = backgroundHeight * 0.3f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round, UIColors.widgetBlur());

        float textX = x + gap;
        float textY = y + gap;

        getSemiBoldFont().drawGradientText(matrixStack, "FPS: ", textX, textY, fontSize,
                UIColors.primary(), UIColors.secondary(), textWidth / 4f);
        getSemiBoldFont().drawText(matrixStack, value, textX + getSemiBoldFont().getWidth("FPS: ", fontSize),
                textY, fontSize, UIColors.textColor());

        fpsWidth = backgroundWidth;
    }

    private void renderXYZ(MatrixStack matrixStack, float x, float y) {
        if (!showXYZ || mc.player == null) return;

        float gap = getGap();
        float fontSize = scaled(7.5f);

        String xVal = String.format("%.1f", mc.player.getX());
        String yVal = String.format("%.1f", mc.player.getY());
        String zVal = String.format("%.1f", mc.player.getZ());
        String value = xVal + ", " + yVal + ", " + zVal;
        String text = "XYZ: " + value;

        float textWidth = getSemiBoldFont().getWidth(text, fontSize);
        float backgroundWidth = textWidth + gap * 2f;
        float backgroundHeight = fontSize + gap * 2f;
        float round = backgroundHeight * 0.3f;

        // XYZ рендерится справа от названия клиента (на той же высоте)
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round, UIColors.widgetBlur());

        float textX = x + gap;
        float textY = y + gap;

        getSemiBoldFont().drawGradientText(matrixStack, "XYZ: ", textX, textY, fontSize,
                UIColors.primary(), UIColors.secondary(), textWidth / 4f);
        getSemiBoldFont().drawText(matrixStack, value, textX + getSemiBoldFont().getWidth("XYZ: ", fontSize),
                textY, fontSize, UIColors.textColor());

        xyzWidth = backgroundWidth;
    }

    private float getWidgetWidth(WidgetInfo widget) {
        float fontSize = scaled(7.5f);
        float gap = getGap();
        String text = getWidgetText(widget);
        return getSemiBoldFont().getWidth(text, fontSize) + gap * 2f;
    }


    private String getWidgetText(WidgetInfo widget) {
        switch (widget) {
            case BPS -> {
                float bpsValue = getBPS();
                return "BPS: " + String.format("%.2f", bpsValue);
            }
            case FPS -> {
                return "FPS: " + mc.getCurrentFps();
            }
            case XYZ -> {
                if (mc.player == null) return "XYZ: 0.0, 0.0, 0.0";
                return String.format("XYZ: %.1f, %.1f, %.1f",
                        mc.player.getX(), mc.player.getY(), mc.player.getZ());
            }
            default -> {
                return "";
            }
        }
    }

    private float getBPS() {
        if (mc.player == null) return 0;
        double deltaX = mc.player.getX() - mc.player.prevX;
        double deltaZ = mc.player.getZ() - mc.player.prevZ;
        double bps = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20;
        return (float) bps;
    }

    private void updateWidgetPositions() {
    }

    private void handleInteraction(MatrixStack ms, float x, float y, float w, float h) {
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;

        boolean rightClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (rightClick && !wasRightClick && hover) {
            settingsOpen = !settingsOpen;
            settingsX = (float) mx;
            settingsY = (float) my;

            if (settingsOpen) {
                settingsScaleAnimation.reset();
                settingsScaleAnimation.run(1.0, 300, Easing.ELASTIC_OUT);
            } else {
                settingsScaleAnimation.reset();
                settingsScaleAnimation.run(0.0, 200, Easing.CUBIC_IN);
            }
        }
        wasRightClick = rightClick;

        if (settingsOpen) {
            renderSettings(ms, mx, my);
        }
    }

    // В методе renderSettings используйте сеттеры
    private void renderSettings(MatrixStack ms, double mx, double my) {
        settingsScaleAnimation.update();

        settingsAnim += (settingsOpen ? 0.15f : -0.15f);
        settingsAnim = Math.max(0, Math.min(1, settingsAnim));

        if (settingsAnim <= 0.05f && !settingsOpen) return;

        float scaleAnim = (float) settingsScaleAnimation.getValue();

        float pad = scaled(6);
        float gap = scaled(5);
        float font = scaled(6);
        float toggle = scaled(7);

        String[] opts = {"Показывать BPS", "Показывать FPS", "Показывать XYZ"};

        float maxW = 0;
        for (String opt : opts) {
            float w = getMediumFont().getWidth(opt, font);
            if (w > maxW) maxW = w;
        }

        float w = pad + toggle + gap + maxW + pad;
        float h = pad + (font + gap) * opts.length + pad;

        float x = settingsX + scaled(10);
        float y = settingsY;

        if (x + w > mc.getWindow().getScaledWidth()) x = settingsX - w - scaled(10);
        if (y + h > mc.getWindow().getScaledHeight()) y = mc.getWindow().getScaledHeight() - h - scaled(10);

        ms.push();
        float centX = x + w / 2;
        float centY = y + h / 2;

        ms.translate(centX, centY, 0);
        ms.scale(scaleAnim, scaleAnim, 1);
        ms.translate(-centX, -centY, 0);

        int alpha = (int) (255 * settingsAnim);
        alpha = Math.max(0, Math.min(255, alpha));

        Color blurColor = new Color(UIColors.widgetBlur().getRed(),
                UIColors.widgetBlur().getGreen(),
                UIColors.widgetBlur().getBlue(),
                alpha);
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, scaled(3), blurColor);

        float cy = y + pad;
        boolean[] states = {showBPS, showFPS, showXYZ};

        boolean leftClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        for (int i = 0; i < opts.length; i++) {
            float tx = x + pad + toggle + gap;
            int textAlpha = (int) (255 * settingsAnim);
            textAlpha = Math.max(0, Math.min(255, textAlpha));

            getMediumFont().drawText(ms, opts[i], tx, cy, font, new Color(200, 200, 200, textAlpha));

            float toggleX = x + pad;
            float toggleY = cy;

            boolean hoverToggle = mx >= toggleX && mx <= toggleX + toggle && my >= toggleY && my <= toggleY + toggle;

            if (hoverToggle && leftClick && !wasLeftClick) {
                switch (i) {
                    case 0 -> setShowBPS(!showBPS);
                    case 1 -> setShowFPS(!showFPS);
                    case 2 -> setShowXYZ(!showXYZ);
                }
                states[i] = !states[i];
            }

            Color toggleColor = states[i] ?
                    new Color(100, 255, 100, textAlpha) :
                    new Color(255, 100, 100, textAlpha);
            RenderUtil.RECT.draw(ms, toggleX, toggleY, toggle, toggle, toggle * 0.3f, toggleColor);

            cy += font + gap;
        }

        wasLeftClick = leftClick;
        ms.pop();
    }

    private float[] drawPill(MatrixStack matrixStack, float x, float y, String content, float extraHeight) {
        boolean watermark = content.contains(ClientInfo.NAME);

        Font font = !watermark ? getMediumFont() : getSemiBoldFont();

        float fontSize = scaled(7.5f);
        float contentWidth = font.getWidth(content, fontSize);
        float contentHeight = fontSize;

        float gap = getGap() * 0.9f;
        float backgroundWidth = contentWidth + gap * 2f;
        float backgroundHeight = contentHeight + gap * 2f + extraHeight;
        float round = backgroundHeight * 0.3f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round,
                UIColors.widgetBlur());

        float textX = x + gap;
        float textY = y + gap;

        if (!watermark) {
            font.drawText(matrixStack, content, textX, textY, fontSize, UIColors.textColor());
        } else {
            String pre = getClientName();
            String pro = getClientVersion();
            float preWidth = font.getWidth(pre, fontSize);

            font.drawGradientText(matrixStack, pre, textX, textY, fontSize,
                    UIColors.primary(), UIColors.secondary(), contentWidth / 4f);
            font.drawText(matrixStack, pro, textX + preWidth, textY, fontSize,
                    UIColors.inactiveTextColor());
        }

        return new float[]{x, y, backgroundWidth, backgroundHeight};
    }

    private String getGreeting() {
        java.time.ZonedDateTime moscowTime = java.time.ZonedDateTime.now(
                java.time.ZoneId.of("Europe/Moscow")
        );

        int hour = moscowTime.getHour();

        if (hour >= 5 && hour < 12) {
            return "Доброе утро";
        } else if (hour >= 12 && hour < 17) {
            return "Добрый день";
        } else if (hour >= 17 && hour < 21) {
            return "Добрый вечер";
        } else {
            return "Доброй ночи";
        }
    }

    private String getClientVersion() {
        return " v" + ClientInfo.VERSION;
    }

    private String getClientName() {
        return ClientInfo.NAME;
    }

    private enum WidgetInfo {
        BPS, FPS, XYZ
    }
}