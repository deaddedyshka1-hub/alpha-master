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

    public void loadConfig() {
        WidgetConfigManager configManager = WidgetConfigManager.getInstance();
        showBPS = configManager.getBoolean("Watermark", "showBPS", false);
        showFPS = configManager.getBoolean("Watermark", "showFPS", true);
        showXYZ = configManager.getBoolean("Watermark", "showXYZ", false);
    }

    private void saveConfig() {
        WidgetConfigManager configManager = WidgetConfigManager.getInstance();
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

        if (chatOpen && !wasChatOpen) {
            chatIndicatorAnimation.run(1.0, 300, Easing.BACK_OUT);
        } else if (!chatOpen && wasChatOpen) {
            chatIndicatorAnimation.run(0.0, 200, Easing.CUBIC_IN);
            settingsOpen = false;
            settingsScaleAnimation.reset();
        }

        wasChatOpen = chatOpen;

        if (chatOpen && !chatIndicatorAnimation.isActive()) {
            chatIndicatorAnimation.setValue(1.0);
        }

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float gap = getGap();

        float headSize = scaled(29f);
        boolean isRight = x > mc.getWindow().getScaledWidth() / 2f;

        // Рассчитываем ширину виджета
        updateWatermarkSize(matrixStack, x, y, headSize, isRight);
        float totalWidth = getDraggable().getWidth();

        // Для правой стороны голова должна быть справа
        float headX = isRight ? x + totalWidth - headSize : x;

        // Текст начинается слева для правой стороны, и справа от головы для левой
        float pillsStartX = isRight ? x : x + headSize + gap;

        matrixStack.push();
        float centerX = headX + headSize / 2;
        float centerY = y + headSize / 2;
        matrixStack.translate(centerX, centerY, 0);
        float scale = 1.0f + (float) chatIndicatorAnimation.getValue() * 0.1f;
        matrixStack.scale(scale, scale, 1);
        matrixStack.translate(-centerX, -centerY, 0);

        RenderUtil.TEXTURE_RECT.drawHead(matrixStack, mc.player, headX, y, headSize, headSize,
                getGap() / 2f, 0f, Color.WHITE);
        matrixStack.pop();

        // Для правой стороны рендерим справа налево
        if (isRight) {
            renderRightSide(matrixStack, pillsStartX, y, headSize);
        } else {
            renderLeftSide(matrixStack, pillsStartX, y, headSize);
        }

        if (lastWatermarkX != x || lastWatermarkY != y || lastWatermarkWidth != totalWidth) {
            updateWidgetPositions();
            lastWatermarkX = x;
            lastWatermarkY = y;
            lastWatermarkWidth = totalWidth;
        }

        if (chatOpen) {
            handleInteraction(matrixStack, x, y, totalWidth, headSize);
        }
    }

    private void renderRightSide(MatrixStack matrixStack, float startX, float startY, float headSize) {
        float gap = getGap();

        // Первая строка (верхняя) - Название клиента и XYZ
        String clientName = getClientName() + getClientVersion();
        float clientWidth = drawPillRight(matrixStack, startX, startY, clientName, 0)[2];

        float secondRowX = startX;
        if (showXYZ && mc.player != null) {
            renderXYZ(matrixStack, startX + clientWidth + gap, startY);
        }

        // Вторая строка (нижняя) - Приветствие и виджеты
        float secondRowY = startY + headSize - watermarkHeight;
        float widgetX = startX;

        // Сначала виджеты FPS/BPS
        if (showFPS) {
            renderFPS(matrixStack, widgetX, secondRowY);
            widgetX += fpsWidth + gap;
        }
        if (showBPS) {
            renderBPS(matrixStack, widgetX, secondRowY);
            widgetX += bpsWidth + gap;
        }

        // Затем приветствие
        String greeting = getGreeting();
        if (!greeting.isEmpty()) {
            drawPillRight(matrixStack, widgetX, secondRowY, greeting, 0);
        }
    }

    private void renderLeftSide(MatrixStack matrixStack, float startX, float startY, float headSize) {
        float gap = getGap();

        // Первая строка (верхняя) - Название клиента и XYZ
        String clientName = getClientName() + getClientVersion();
        float[] namePill = drawPill(matrixStack, startX, startY, clientName, 0);
        float namePillWidth = namePill[2];
        watermarkWidth = namePillWidth;
        watermarkHeight = namePill[3];

        if (showXYZ && mc.player != null) {
            renderXYZ(matrixStack, startX + namePillWidth + gap, startY);
        }

        // Вторая строка (нижняя) - Приветствие и виджеты
        float secondRowY = startY + headSize - watermarkHeight;
        float[] greetingPill = drawPill(matrixStack, startX, secondRowY, getGreeting(), 0);
        float greetingWidth = greetingPill[2];

        float widgetX = startX + greetingWidth + gap;
        if (showFPS) {
            renderFPS(matrixStack, widgetX, secondRowY);
            if (showBPS) {
                renderBPS(matrixStack, widgetX + fpsWidth + gap, secondRowY);
            }
        } else if (showBPS) {
            renderBPS(matrixStack, widgetX, secondRowY);
        }
    }

    private void updateWatermarkSize(MatrixStack matrixStack, float x, float y, float headSize, boolean isRight) {
        float gap = getGap();

        // Считаем ширину первой строки (клиент + XYZ)
        String clientName = getClientName() + getClientVersion();
        float clientWidth = getSemiBoldFont().getWidth(clientName, scaled(7.5f)) + gap * 1.8f;

        float firstRowWidth = clientWidth;
        if (showXYZ && mc.player != null) {
            firstRowWidth += gap + xyzWidth;
        }

        // Считаем ширину второй строки (приветствие + FPS/BPS)
        String greeting = getGreeting();
        float greetingWidth = getMediumFont().getWidth(greeting, scaled(7.5f)) + gap * 1.8f;

        float widgetsWidth = 0;
        if (showFPS) widgetsWidth += fpsWidth;
        if (showBPS) {
            if (showFPS) widgetsWidth += gap;
            widgetsWidth += bpsWidth;
        }

        float secondRowWidth = greetingWidth;
        if (widgetsWidth > 0) {
            secondRowWidth += gap + widgetsWidth;
        }

        // Общая ширина контента = максимальная из двух строк
        float contentWidth = Math.max(firstRowWidth, secondRowWidth);

        // Общая ширина виджета = голова + отступ + контент
        float totalWidth = headSize + gap + contentWidth;
        float totalHeight = headSize;

        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(totalHeight);
    }

    private void renderClientNameWithXYZ(MatrixStack matrixStack, float startX, float startY, float headSize, boolean isRight) {
        String clientName = getClientName() + getClientVersion();
        float namePillWidth;

        if (isRight) {
            if (showXYZ && mc.player != null) {
                renderXYZ(matrixStack, startX, startY);
                namePillWidth = drawPillRight(matrixStack, startX + xyzWidth + getGap(), startY, clientName, 0)[2];
            } else {
                namePillWidth = drawPillRight(matrixStack, startX, startY, clientName, 0)[2];
            }
        } else {
            float[] namePill = drawPill(matrixStack, startX, startY, clientName, 0);
            namePillWidth = namePill[2];
            watermarkWidth = namePillWidth;
            watermarkHeight = namePill[3];

            if (showXYZ && mc.player != null) {
                renderXYZ(matrixStack, startX + namePillWidth + getGap(), startY);
            }
        }
    }

    private void renderGreetingWithWidgets(MatrixStack matrixStack, float startX, float startY, boolean isRight) {
        String greeting = getGreeting();

        if (isRight) {
            float widgetsWidth = 0;
            if (showFPS) widgetsWidth += fpsWidth;
            if (showBPS) {
                if (showFPS) widgetsWidth += getGap();
                widgetsWidth += bpsWidth;
            }

            float greetingX = startX;
            if (widgetsWidth > 0) {
                greetingX = startX + widgetsWidth + getGap();
            }

            drawPillRight(matrixStack, greetingX, startY, greeting, 0);

            float widgetX = startX;
            if (showFPS) {
                renderFPS(matrixStack, widgetX, startY);
                widgetX += fpsWidth + getGap();
            }
            if (showBPS) {
                renderBPS(matrixStack, widgetX, startY);
            }
        } else {
            float[] greetingPill = drawPill(matrixStack, startX, startY, greeting, 0);
            float greetingWidth = greetingPill[2];

            float widgetsX = startX + greetingWidth + getGap();
            float widgetsY = startY;

            if (showFPS) {
                renderFPS(matrixStack, widgetsX, widgetsY);
                if (showBPS) {
                    renderBPS(matrixStack, widgetsX + fpsWidth + getGap(), widgetsY);
                }
            } else if (showBPS) {
                renderBPS(matrixStack, widgetsX, widgetsY);
            }
        }
    }

    private float[] drawPillRight(MatrixStack matrixStack, float x, float y, String content, float extraHeight) {
        boolean watermark = content.contains(ClientInfo.NAME);
        Font font = !watermark ? getMediumFont() : getSemiBoldFont();

        float fontSize = scaled(7.5f);
        float contentWidth = font.getWidth(content, fontSize);
        float contentHeight = fontSize;

        float gap = getGap() * 0.9f;
        float backgroundWidth = contentWidth + gap * 2f;
        float backgroundHeight = contentHeight + gap * 2f + extraHeight;
        float round = backgroundHeight * 0.3f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round, UIColors.widgetBlur());

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
            font.drawText(matrixStack, pro, textX + preWidth, textY, fontSize, UIColors.inactiveTextColor());
        }

        watermarkWidth = backgroundWidth;
        watermarkHeight = backgroundHeight;

        return new float[]{x, y, backgroundWidth, backgroundHeight};
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

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round, UIColors.widgetBlur());

        float textX = x + gap;
        float textY = y + gap;

        getSemiBoldFont().drawGradientText(matrixStack, "XYZ: ", textX, textY, fontSize,
                UIColors.primary(), UIColors.secondary(), textWidth / 4f);
        getSemiBoldFont().drawText(matrixStack, value, textX + getSemiBoldFont().getWidth("XYZ: ", fontSize),
                textY, fontSize, UIColors.textColor());

        xyzWidth = backgroundWidth;
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

    private float getBPS() {
        if (mc.player == null) return 0;
        double deltaX = mc.player.getX() - mc.player.prevX;
        double deltaZ = mc.player.getZ() - mc.player.prevZ;
        double bps = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20;
        return (float) bps;
    }

    private enum WidgetInfo {
        BPS, FPS, XYZ
    }
}