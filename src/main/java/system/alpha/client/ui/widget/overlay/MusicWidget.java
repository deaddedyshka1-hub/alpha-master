package system.alpha.client.ui.widget.overlay;

import by.bonenaut7.mediatransport4j.api.MediaSession;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.ColorUtil;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.math.MathUtil;
import system.alpha.api.utils.media.MediaUtils;
import system.alpha.api.utils.render.RenderUtil;
import system.alpha.api.utils.render.fonts.Font;
import system.alpha.api.utils.render.fonts.Fonts;
import system.alpha.api.utils.render.fonts.Icons;
import system.alpha.client.features.modules.render.InterfaceModule;
import system.alpha.client.ui.widget.Widget;

import java.awt.*;

public class MusicWidget extends Widget {
    private float[] waveHeights;
    private float[] waveTargets;
    private long lastWaveUpdate = 0L;
    private boolean topCenterPosition = true;
    private float topOffset = 8f;

    private final AnimationUtil hoverAnimation = new AnimationUtil();
    private boolean isHovered = false;
    private long lastHoverTime = 0L;
    private long hoverDelay = 200L;
    private float currentPositionPercent = 0f;
    private long lastProgressTime = 0L;
    private float smoothedPosition = 0f;

    private boolean wasPlaying = false;
    private long simulatedPosition = 0L;
    private long lastSimulationUpdate = 0L;

    private boolean prevWasPressed = false;
    private boolean playPauseWasPressed = false;
    private boolean nextWasPressed = false;

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

        // Обрезаем исполнителей - оставляем только первого если их несколько
        if (artist.contains(",")) {
            artist = artist.split(",")[0].trim();
        }
        if (artist.contains(";")) {
            artist = artist.split(";")[0].trim();
        }
        if (artist.contains(" feat. ")) {
            artist = artist.split(" feat. ")[0].trim();
        }
        if (artist.contains(" ft. ")) {
            artist = artist.split(" ft. ")[0].trim();
        }
        if (artist.contains(" & ")) {
            artist = artist.split(" & ")[0].trim();
        }
        if (artist.contains(" x ")) {
            artist = artist.split(" x ")[0].trim();
        }

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

        float x, y;

        if (topCenterPosition) {
            float screenWidth = mc.getWindow().getScaledWidth();
            x = (screenWidth - panelWidth) / 2.0f;
            y = topOffset;
        } else {
            x = getDraggable().getX();
            y = getDraggable().getY();
        }

        int mouseX = (int) mc.mouse.getX();
        int mouseY = (int) mc.mouse.getY();
        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();
        int scaledMouseX = (int) (mouseX * windowWidth / (double) mc.getWindow().getWidth());
        int scaledMouseY = (int) (mouseY * windowHeight / (double) mc.getWindow().getHeight());

        float panelHeightExpanded = height + scaled(42f);
        boolean currentlyHovered = scaledMouseX >= x && scaledMouseX <= x + panelWidth &&
                scaledMouseY >= y && scaledMouseY <= y + panelHeightExpanded;

        if (currentlyHovered) {
            lastHoverTime = System.currentTimeMillis();
            isHovered = true;
        } else {
            if (System.currentTimeMillis() - lastHoverTime > hoverDelay) {
                isHovered = false;
            }
        }

        hoverAnimation.update();
        hoverAnimation.run(isHovered ? 1.0 : 0.0, 200, Easing.SINE_OUT);

        float hoverAnim = (float) hoverAnimation.getValue();

        if (hoverAnim > 0.01f) {
            float expandedHeight = height + scaled(42f) * hoverAnim;
            RenderUtil.BLUR_RECT.draw(matrixStack, x, y, panelWidth, expandedHeight, radius, UIColors.widgetBlur());
        } else {
            RenderUtil.BLUR_RECT.draw(matrixStack, x, y, panelWidth, height, radius, UIColors.widgetBlur());
        }

        float centerY = y + height / 2.0f;
        float currentX = x + padding;

        drawTexture(matrixStack, artTexture, currentX, centerY - artSize / 2.0f, artSize, artSize, scaled(5.5f));
        currentX += artSize + scaled(3.0f);

        font.drawText(matrixStack, text, currentX, centerY + 1.1f - (fontSize / 2.0f) - scaled(1.0f), fontSize, Color.WHITE);
        currentX += textWidth + scaled(9.0f);

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

        if (hoverAnim > 0.01f) {
            renderControlPanel(matrixStack, x, y + height, panelWidth, hoverAnim);
        }

        getDraggable().setX(x);
        getDraggable().setY(y);
        getDraggable().setWidth(panelWidth);
        getDraggable().setHeight(height);
    }

    private void renderControlPanel(MatrixStack matrixStack, float x, float y, float panelWidth, float hoverAnim) {
        float padding = scaled(8f);

        MediaSession session = MediaUtils.getCurrentSession();

        long duration = 0L;
        long position = 0L;
        boolean isPlaying = false;

        if (session != null) {
            try {
                long durationRaw = session.getDuration();
                long positionRaw = session.getPosition();

                // Значения уже в секундах!
                duration = durationRaw;
                position = positionRaw;

                isPlaying = session.isPlaying();
            } catch (Exception e) {
                duration = 0L;
                position = 0L;
                isPlaying = false;
            }
        }

        if (duration <= 0) {
            duration = 1L;
        }

        if (position > duration) position = duration;
        if (position < 0) position = 0;

        if (isPlaying) {
            long currentTime = System.currentTimeMillis();
            if (wasPlaying) {
                long delta = currentTime - lastSimulationUpdate;
                simulatedPosition += delta;
                if (simulatedPosition > duration * 1000) {
                    simulatedPosition = duration * 1000;
                }
            } else {
                simulatedPosition = position * 1000;
            }
            lastSimulationUpdate = currentTime;
            position = simulatedPosition / 1000;
        } else {
            simulatedPosition = position * 1000;
        }
        wasPlaying = isPlaying;

        if (duration > 0) {
            currentPositionPercent = (float) position / duration;
        }

        String timeText = formatTime(position) + " / " + formatTime(duration);
        Fonts.SF_REGULAR.drawCenteredText(matrixStack, timeText, x + panelWidth / 2f, y + scaled(2f),
                scaled(9f), ColorUtil.setAlpha(Color.WHITE, (int)(255 * hoverAnim)));

        float progressBarWidth = panelWidth - padding * 2;
        float progressBarY = y + scaled(14f);
        float progressBarHeight = scaled(3f);
        float progressBarRadius = scaled(1.5f);

        RenderUtil.BLUR_RECT.draw(matrixStack, x + padding, progressBarY, progressBarWidth, progressBarHeight,
                progressBarRadius, ColorUtil.setAlpha(Color.DARK_GRAY, (int)(150 * hoverAnim)));

        float filledWidth = progressBarWidth * currentPositionPercent;
        if (filledWidth > 0 && duration > 1) {
            RenderUtil.GRADIENT_RECT.draw(matrixStack, x + padding, progressBarY, filledWidth, progressBarHeight,
                    new Vector4f(progressBarRadius, 0, 0, progressBarRadius),
                    ColorUtil.setAlpha(UIColors.gradient(0), (int)(255 * hoverAnim)),
                    ColorUtil.setAlpha(UIColors.gradient(0), (int)(255 * hoverAnim)),
                    ColorUtil.setAlpha(UIColors.gradient(0), (int)(255 * hoverAnim)),
                    ColorUtil.setAlpha(UIColors.gradient(0), (int)(255 * hoverAnim)));
        }

        float controlsY = progressBarY + scaled(10f);
        float buttonSize = scaled(12f);
        float iconSize = scaled(7f);
        float totalButtonsWidth = buttonSize * 3 + scaled(20f);
        float buttonsStartX = x + (panelWidth - totalButtonsWidth) / 2f;

        float prevX = buttonsStartX;
        float playPauseX = prevX + buttonSize + scaled(10f);
        float nextX = playPauseX + buttonSize + scaled(10f);

        boolean leftClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        renderButton(matrixStack, prevX, controlsY - buttonSize / 2f, buttonSize, buttonSize,
                Icons.STEP_B.getLetter(), iconSize, hoverAnim, leftClick, !prevWasPressed && leftClick, () -> {
                    if (session != null) {
                        session.switchToPrevious();
                        simulatedPosition = 0;
                        lastSimulationUpdate = System.currentTimeMillis();
                    }
                });
        prevWasPressed = leftClick;

        String playPauseIcon = isPlaying ? Icons.PAUSE.getLetter() : Icons.PLAY.getLetter();
        renderButton(matrixStack, playPauseX, controlsY - buttonSize / 2f, buttonSize, buttonSize,
                playPauseIcon, iconSize, hoverAnim, leftClick, !playPauseWasPressed && leftClick, () -> {
                    if (session != null) {
                        session.togglePlay();
                        lastSimulationUpdate = System.currentTimeMillis();
                    }
                });
        playPauseWasPressed = leftClick;

        renderButton(matrixStack, nextX, controlsY - buttonSize / 2f, buttonSize, buttonSize,
                Icons.STEP_F.getLetter(), iconSize, hoverAnim, leftClick, !nextWasPressed && leftClick, () -> {
                    if (session != null) {
                        session.switchToNext();
                        simulatedPosition = 0;
                        lastSimulationUpdate = System.currentTimeMillis();
                    }
                });
        nextWasPressed = leftClick;
    }

    private void renderButton(MatrixStack matrixStack, float x, float y, float width, float height,
                              String icon, float iconSize, float alpha, boolean leftClick, boolean justPressed, Runnable action) {
        int mouseX = (int) mc.mouse.getX();
        int mouseY = (int) mc.mouse.getY();
        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();
        int scaledMouseX = (int) (mouseX * windowWidth / (double) mc.getWindow().getWidth());
        int scaledMouseY = (int) (mouseY * windowHeight / (double) mc.getWindow().getHeight());

        boolean buttonHovered = scaledMouseX >= x && scaledMouseX <= x + width &&
                scaledMouseY >= y && scaledMouseY <= y + height;

        Color buttonColor = buttonHovered ?
                (leftClick ? ColorUtil.setAlpha(UIColors.widgetBlur(), (int)(180 * alpha)) :
                        ColorUtil.setAlpha(UIColors.widgetBlur(), (int)(240 * alpha))) :
                ColorUtil.setAlpha(UIColors.widgetBlur(), (int)(200 * alpha));

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, scaled(4f), buttonColor);

        Fonts.ICONS.drawCenteredText(matrixStack, icon, x + width / 2f, y + height / 2f - iconSize / 2f,
                iconSize, ColorUtil.setAlpha(Color.WHITE, (int)(255 * alpha)), 0.1f);

        if (buttonHovered && justPressed) {
            action.run();
        }
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
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
}