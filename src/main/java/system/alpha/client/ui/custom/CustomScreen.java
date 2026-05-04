package system.alpha.client.ui.custom;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import system.alpha.api.system.backend.ClientInfo;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.render.RenderUtil;
import system.alpha.api.utils.render.fonts.Fonts;
import system.alpha.client.services.UpdateService;

import java.awt.*;
import java.net.URI;

public class CustomScreen extends Screen {
    private AnimationUtil fadeAnimation = new AnimationUtil();
    private AnimationUtil buttonHover = new AnimationUtil();
    private float time = 0;
    private boolean closing = false;

    public CustomScreen() {
        super(Text.literal("Update Required"));
        fadeAnimation.setValue(0);
        fadeAnimation.run(1.0, 800, Easing.CUBIC_OUT);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        time += delta;

        fadeAnimation.update();
        buttonHover.update();

        float fade = (float) fadeAnimation.getValue();

        renderDarkBackground(context, fade);

        int centerX = width / 2;
        int centerY = height / 2;

        int intAlpha = (int)(255 * Math.min(1.0f, Math.max(0.0f, fade)));
        intAlpha = Math.max(0, Math.min(255, intAlpha));

        float panelW = Math.min(450, width - 40);
        float panelH = Math.min(300, height - 40);
        float panelX = centerX - panelW / 2;
        float panelY = centerY - panelH / 2;

        float fontSizeTitle = Math.min(28, panelH / 10);
        float fontSizeMedium = Math.min(18, panelH / 17);
        float fontSizeTiny = Math.min(14, panelH / 22);

        float offsetY = panelY + 30;

        RenderUtil.BLUR_RECT.draw(context.getMatrices(), panelX, panelY, panelW, panelH, 16, new Color(25, 25, 45, Math.min(240, intAlpha)));

        RenderUtil.RECT.draw(context.getMatrices(), panelX, panelY, panelW, 3, 16, new Color(255, 80, 80, intAlpha));
        RenderUtil.RECT.draw(context.getMatrices(), panelX, panelY + panelH - 3, panelW, 3, 16, new Color(255, 80, 80, intAlpha));

        Fonts.PS_BOLD.drawCenteredText(context.getMatrices(), "ОБНОВЛЕНИЕ ТРЕБУЕТСЯ", centerX, offsetY, fontSizeTitle,
                new Color(255, 80, 80, intAlpha));
        offsetY += fontSizeTitle + 20;

        Fonts.PS_MEDIUM.drawCenteredText(context.getMatrices(), "Ваша версия: " + ClientInfo.VERSION, centerX, offsetY, fontSizeMedium,
                new Color(255, 255, 255, intAlpha));
        offsetY += fontSizeMedium + 8;

        Fonts.PS_MEDIUM.drawCenteredText(context.getMatrices(), "Новая версия: " + UpdateService.getInstance().getLatestVersion(), centerX, offsetY, fontSizeMedium,
                new Color(100, 255, 100, intAlpha));
        offsetY += fontSizeMedium + 25;

        Fonts.PS_MEDIUM.drawCenteredText(context.getMatrices(), "Для продолжения использования клиента", centerX, offsetY, fontSizeTiny,
                new Color(255, 220, 100, intAlpha));
        offsetY += fontSizeTiny + 5;

        Fonts.PS_MEDIUM.drawCenteredText(context.getMatrices(), "необходимо обновление!", centerX, offsetY, fontSizeTiny,
                new Color(255, 220, 100, intAlpha));

        renderCustomButton(context, mouseX, mouseY, delta, centerX, (int)(panelY + panelH - 50), intAlpha);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderDarkBackground(DrawContext context, float fade) {
        int alpha = (int)(180 * fade);
        alpha = Math.max(0, Math.min(255, alpha));
        context.fill(0, 0, width, height, new Color(0, 0, 0, alpha).getRGB());
    }

    private void openBrowser(String url) {
        System.out.println("[CustomScreen] Trying to open: " + url);
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;

                if (os.contains("win")) {
                    pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", url);
                } else {
                    pb = new ProcessBuilder("xdg-open", url);
                }

                pb.start();
                System.out.println("[CustomScreen] Browser opened successfully");
            } catch (Exception e) {
                System.err.println("[CustomScreen] Failed to open browser: " + e.getMessage());
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    System.err.println("[CustomScreen] Desktop.browse also failed: " + ex.getMessage());
                }
            }
        }).start();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = width / 2;
            int centerY = height / 2;
            float panelH = Math.min(300, height - 40);
            int buttonY = (int)(centerY - panelH / 2 + panelH - 50);
            int buttonW = 220;
            int buttonH = 40;
            int buttonX = centerX - buttonW / 2;

            if (mouseX >= buttonX && mouseX <= buttonX + buttonW && mouseY >= buttonY && mouseY <= buttonY + buttonH) {
                System.out.println("[CustomScreen] Button clicked!");
                openBrowser(UpdateService.getInstance().getDownloadUrl());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderCustomButton(DrawContext context, int mouseX, int mouseY, float delta, int centerX, int buttonY, int intAlpha) {
        int buttonW = 220;
        int buttonH = 40;
        int buttonX = centerX - buttonW / 2;

        if (buttonY + buttonH > height - 10) {
            buttonY = height - 50;
        }

        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonW && mouseY >= buttonY && mouseY <= buttonY + buttonH;

        if (hovered) {
            buttonHover.run(1.0, 200, Easing.CUBIC_OUT);
        } else {
            buttonHover.run(0.0, 200, Easing.CUBIC_IN);
        }

        float hoverAnim = (float) buttonHover.getValue();

        Color bgColor = new Color(60 + (int)(40 * hoverAnim), 70 + (int)(50 * hoverAnim), 140 + (int)(80 * hoverAnim), intAlpha);
        Color borderColor = new Color(200 + (int)(55 * hoverAnim), 150 + (int)(50 * hoverAnim), 255, intAlpha);

        RenderUtil.BLUR_RECT.draw(context.getMatrices(), buttonX, buttonY, buttonW, buttonH, 8, bgColor);
        RenderUtil.RECT.draw(context.getMatrices(), buttonX, buttonY, buttonW, 2, 8, borderColor);
        RenderUtil.RECT.draw(context.getMatrices(), buttonX, buttonY + buttonH - 2, buttonW, 2, 8, borderColor);

        Fonts.PS_MEDIUM.drawCenteredText(context.getMatrices(), "СКАЧАТЬ ОБНОВЛЕНИЕ", centerX, buttonY + buttonH / 2 - 4, 14,
                new Color(255, 255, 255, intAlpha));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        fadeAnimation.run(0.0, 500, Easing.CUBIC_IN);
    }

    @Override
    public void tick() {
        super.tick();
        if (closing && fadeAnimation.isFinished() && fadeAnimation.getValue() <= 0.01) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void applyBlur() {
    }
}