package system.alpha.client.ui.widget.overlay;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.media.MediaUtils;
import system.alpha.api.utils.render.RenderUtil;
import system.alpha.api.utils.render.fonts.Font;
import system.alpha.api.utils.render.fonts.Fonts;
import system.alpha.client.features.modules.render.InterfaceModule;
import system.alpha.client.ui.widget.Widget;

import java.awt.*;

public class MusicWidget extends Widget {
    private float[] waveHeights;
    private float[] waveTargets;
    private long lastWaveUpdate = 0L;
    private boolean topCenterPosition = true; // Флаг для позиции сверху по центру
    private float topOffset = 8f; // Отступ от верха

    public MusicWidget() {
        super(0f, 8f);
    }

    @Override
    public String getName() {
        return "MusicBar";
    }

    @Override
    public void render(MatrixStack matrixStack) {
        if (!InterfaceModule.getInstance().widgets.isEnabled("MusicBar")) {
            return;
        }

        MediaUtils.MediaInfo mediaInfo = MediaUtils.getCurrentMedia();
        if (mediaInfo == null) return;

        String title = mediaInfo.title == null ? "" : mediaInfo.title;
        String artist = mediaInfo.artist == null ? "" : mediaInfo.artist;
        String label = artist.isEmpty() ? title : (title + " - " + artist);
        if (label.isEmpty()) return;

        AbstractTexture artTexture = mediaInfo.getTexture();
        if (artTexture == null) return;

        Font font = Fonts.SF_REGULAR;
        float fontSize = scaled(8.0f);
        float padding = scaled(7.0f);
        float artSize = scaled(14.0f);
        float radius = scaled(6.6f);

        int bars = 4;
        float barWidth = scaled(3.0f);
        float barGap = scaled(1.7f);
        float waveBlockWidth = bars * barWidth + (bars - 1) * barGap + scaled(4.0f);

        float maxTextWidth = scaled(170.0f);
        String text = trimToWidth(font, label, fontSize, maxTextWidth);
        float textWidth = font.getWidth(text, fontSize);

        float panelWidth = padding + artSize + scaled(7.0f) + textWidth + scaled(9.0f) + waveBlockWidth + padding - scaled(5.9f);
        float height = scaled(19.6f) - scaled(2.1f);

        // Автоматическое позиционирование сверху по центру
        float x, y;

        if (topCenterPosition) {
            // Позиция сверху по центру
            float screenWidth = mc.getWindow().getScaledWidth();
            x = (screenWidth - panelWidth) / 2.0f;
            y = topOffset; // Фиксированный отступ от верха
        } else {
            // Ручное позиционирование (если перетаскивали)
            x = getDraggable().getX();
            y = getDraggable().getY();
        }

        // Фон в стиле ArmorWidget
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, panelWidth, height, radius, UIColors.widgetBlur());

        float centerY = y + height / 2.0f;
        float currentX = x + padding;

        // Отрисовка обложки альбома
        drawTexture(matrixStack, artTexture, currentX, centerY - artSize / 2.0f, artSize, artSize, scaled(5.5f));
        currentX += artSize + scaled(3.0f);

        // Текст песни
        font.drawText(matrixStack, text, currentX, centerY + 1.1f - (fontSize / 2.0f) - scaled(1.0f), fontSize, Color.WHITE);
        currentX += textWidth + scaled(9.0f);

        // Анимация звуковых волн
        updateMusicWave(bars);
        float barBaseY = centerY + scaled(5.0f);
        for (int i = 0; i < bars; i++) {
            float barHeight = waveHeights[i];
            RenderUtil.RECT.draw(matrixStack,
                    currentX + i * (barWidth + barGap),
                    barBaseY - barHeight,
                    barWidth,
                    barHeight,
                    scaled(1.0f),
                    Color.WHITE);
        }

        // Сохраняем позицию в Draggable для перетаскивания
        getDraggable().setX(x);
        getDraggable().setY(y);
        getDraggable().setWidth(panelWidth);
        getDraggable().setHeight(height);
    }

    private void drawTexture(MatrixStack matrixStack, AbstractTexture texture, float x, float y, float w, float h, float radius) {
        RenderUtil.TEXTURE_RECT.draw(matrixStack, x, y, w, h, radius, Color.WHITE, 0f, 0f, 1f, 1f, texture.getGlId());
    }

    private String trimToWidth(Font font, String text, float size, float maxWidth) {
        if (font.getWidth(text, size) <= maxWidth) return text;
        String trimmed = text;
        while (trimmed.length() > 3 && font.getWidth(trimmed + "...", size) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.length() > 3 ? trimmed + "..." : trimmed;
    }

    private void updateMusicWave(int bars) {
        if (waveHeights == null || waveHeights.length != bars) {
            waveHeights = new float[bars];
            waveTargets = new float[bars];
            for (int i = 0; i < bars; i++) {
                waveHeights[i] = scaled(5.0f);
                waveTargets[i] = scaled(5.0f);
            }
        }

        if (System.currentTimeMillis() - lastWaveUpdate > 150) {
            lastWaveUpdate = System.currentTimeMillis();
            for (int i = 0; i < bars; i++) {
                waveTargets[i] = scaled(3.0f) + (float) (Math.random() * scaled(10.0f));
            }
        }

        for (int i = 0; i < bars; i++) {
            waveHeights[i] += (waveTargets[i] - waveHeights[i]) * 0.5f;
        }
    }

    // Метод для включения/выключения автоматической позиции
    public void setTopCenterPosition(boolean enabled) {
        this.topCenterPosition = enabled;
    }

    // Метод для установки отступа от верха
    public void setTopOffset(float offset) {
        this.topOffset = offset;
    }
}