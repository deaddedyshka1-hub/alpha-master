package system.alpha.client.ui.widget.overlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import system.alpha.api.event.events.render.Render2DEvent;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.ColorUtil;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.render.RenderUtil;
import system.alpha.api.utils.render.fonts.Font;
import system.alpha.client.ui.widget.Widget;

import java.awt.*;

public class HotbarWidget extends Widget {
    private static final int HOTBAR_SLOTS = 9;
    private float itemSize;
    private float gap;
    private boolean isVertical;
    private float barWidth;
    private float barHeight;
    private boolean initialized = false;

    private final AnimationUtil healthAnim = new AnimationUtil();
    private final AnimationUtil absorptionAnim = new AnimationUtil();
    private final AnimationUtil selectAnim = new AnimationUtil();
    private int lastSlot = -1;

    public HotbarWidget() {
        super(100f, 100f);
        getDraggable().setDragging(false);
        updateSizes();
    }

    @Override
    public String getName() {
        return "Hotbar";
    }

    private void updateSizes() {
        itemSize = scaled(16f);
        gap = scaled(3f);
        barWidth = scaled(80f);
        barHeight = scaled(5f);
    }


    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        if (mc.player == null) return;

        MatrixStack matrixStack = event.matrixStack();
        DrawContext context = event.context();

        updateSizes();

        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        float hotbarWidth = HOTBAR_SLOTS * itemSize + (HOTBAR_SLOTS - 1) * gap;
        float halfBarWidth = (hotbarWidth - gap) / 2f;

        float barsHeight = barHeight * 2 + gap;
        float expHeight = barHeight;

        float width = hotbarWidth + gap * 2;
        float height = barsHeight + expHeight + itemSize + scaled(6f) + gap * 2.5f;

        float x = (screenWidth - width) / 2f;
        float bottomMargin = scaled(6f);
        float y = screenHeight - height - bottomMargin;

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);
        getDraggable().setX(x);
        getDraggable().setY(y);

        RenderUtil.BLUR_RECT.draw(
                matrixStack,
                x, y,
                width, height,
                scaled(6f),
                UIColors.widgetBlur()
        );

        float baseX = x + gap;
        float baseY = y + gap;

        PlayerEntity p = mc.player;

        boolean isCreative = mc.player.isCreative();
        boolean isSpectator = mc.player.isSpectator();
        boolean showCreativeMode = isCreative || isSpectator;


        float targetHealth = p.getHealth();
        float targetAbs = p.getAbsorptionAmount();

        if (healthAnim.isFinished())
            healthAnim.run(targetHealth, 300, Easing.QUAD_OUT);

        if (absorptionAnim.isFinished())
            absorptionAnim.run(targetAbs, 300, Easing.QUAD_OUT);

        healthAnim.update();
        absorptionAnim.update();

        if (showCreativeMode) {
            String modeText = isCreative ? "CREATIVE" : "SPECTATOR";

            float totalHeight = barsHeight;

            renderModeText(matrixStack, baseX, baseY, hotbarWidth, totalHeight, modeText);

            float expY = baseY + totalHeight + gap;
            renderExpBar(matrixStack, baseX, expY, hotbarWidth);

            float hotbarY = expY + barHeight + gap * 2;
            renderItems(matrixStack, context, baseX, hotbarY);
        } else {
            renderHealthBar(matrixStack, baseX, baseY, halfBarWidth);

            renderHungerBar(matrixStack,
                    baseX + halfBarWidth + gap,
                    baseY,
                    halfBarWidth);

            float expY = baseY + barHeight + gap;
            renderExpBar(matrixStack, baseX, expY, hotbarWidth);

            float hotbarY = expY + barHeight + gap * 2;
            renderItems(matrixStack, context, baseX, hotbarY);
        }
    }

    private void renderModeText(MatrixStack matrixStack, float x, float y, float width, float height, String text) {
        Color textColor;
        if (text.equals("CREATIVE")) {
            textColor = new Color(85, 255, 85);
        } else {
            textColor = new Color(170, 0, 170);
        }

        float mainFontSize = scaled(12f);
        getSmallFont().drawCenteredText(
                matrixStack,
                text,
                x + width / 2f,
                y + height / 2f - mainFontSize / 2f,
                mainFontSize,
                textColor
        );
    }

    private void renderItems(MatrixStack matrixStack, DrawContext context, float startX, float startY) {
        PlayerEntity player = mc.player;
        if (player == null) return;

        float currentX = startX;
        float currentY = startY;

        float scale = itemSize / 16f;

        int selected = player.getInventory().selectedSlot;

        if (selected != lastSlot) {
            lastSlot = selected;
            selectAnim.run(1.2, 120, Easing.BACK_OUT);
        }

        selectAnim.update();

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            ItemStack item = player.getInventory().getStack(i);

            if (i == selected) {
                renderSlotSelection(matrixStack, currentX, currentY);
            }

            matrixStack.push();
            matrixStack.translate(currentX, currentY, 0);
            matrixStack.scale(scale, scale, 1f);

            context.drawItem(item, 0, 0);
            matrixStack.pop();

            renderItemOverlayCustom(matrixStack, item, currentX, currentY);

            if (isVertical) {
                currentY += itemSize + gap;
            } else {
                currentX += itemSize + gap;
            }
        }
    }

    private void renderItemOverlayCustom(MatrixStack matrixStack, ItemStack stack, float x, float y) {
        float padding = scaled(1.5f);

        if (stack.getCount() > 1) {
            String count = String.valueOf(stack.getCount());
            float size = scaled(6f);

            float textWidth = getSmallFont().getWidth(count, size);

            float textX = x + itemSize - textWidth - padding;
            float textY = y + itemSize - size - padding;

            // маленькая тень
            getSmallFont().drawText(
                    matrixStack,
                    count,
                    textX + 0.8f,
                    textY + 0.8f,
                    size,
                    new Color(0, 0, 0, 160)
            );

            getSmallFont().drawText(
                    matrixStack,
                    count,
                    textX,
                    textY,
                    size,
                    Color.WHITE
            );
        }

        if (stack.isDamageable()) {
            float durability = 1f - (float) stack.getDamage() / stack.getMaxDamage();

            float barWidth = itemSize - scaled(4f);
            float barHeightLocal = scaled(2.2f);

            float barX = x + scaled(2f);
            float barY = y + itemSize + scaled(1.5f);

            RenderUtil.RECT.draw(
                    matrixStack,
                    barX,
                    barY,
                    barWidth,
                    barHeightLocal,
                    barHeightLocal / 2f,
                    new Color(0, 0, 0, 170)
            );

            Color durColor = ColorUtil.interpolate(
                    new Color(210, 60, 60),
                    new Color(60, 220, 120),
                    durability
            );

            // сама полоска
            RenderUtil.RECT.draw(
                    matrixStack,
                    barX,
                    barY,
                    barWidth * durability,
                    barHeightLocal,
                    barHeightLocal / 2f,
                    durColor
            );
        }
    }

    private void renderSlotSelection(MatrixStack matrixStack, float x, float y) {
        float scale = (float) selectAnim.getValue();
        if (scale <= 0) scale = 1f;

        float size = itemSize * scale;
        float offset = (size - itemSize) / 2f;

        Color color = UIColors.primary();

        float thickness = scaled(1.5f);

        RenderUtil.RECT.draw(matrixStack,
                x - offset,
                y - offset,
                size,
                thickness,
                0,
                color);

        RenderUtil.RECT.draw(matrixStack,
                x - offset,
                y - offset + size - thickness,
                size,
                thickness,
                0,
                color);

        RenderUtil.RECT.draw(matrixStack,
                x - offset,
                y - offset,
                thickness,
                size,
                0,
                color);

        RenderUtil.RECT.draw(matrixStack,
                x - offset + size - thickness,
                y - offset,
                thickness,
                size,
                0,
                color);
    }

    private void renderHealthBar(MatrixStack matrixStack, float x, float y, float width) {
        PlayerEntity player = mc.player;

        float health = (float) healthAnim.getValue();
        float absorption = (float) absorptionAnim.getValue();
        float maxHealth = player.getMaxHealth();

        float healthPercent = MathHelper.clamp(health / maxHealth, 0, 1);
        float absorptionPercent = MathHelper.clamp(absorption / maxHealth, 0, 1 - healthPercent);

        RenderUtil.RECT.draw(matrixStack, x, y, width, barHeight,
                barHeight * 0.5f, new Color(0,0,0,100));

        RenderUtil.RECT.draw(matrixStack, x, y, width * healthPercent, barHeight,
                barHeight * 0.5f, getHealthColor(healthPercent));

        if (absorption > 0) {
            RenderUtil.RECT.draw(matrixStack,
                    x + width * healthPercent,
                    y,
                    width * absorptionPercent,
                    barHeight,
                    barHeight * 0.5f,
                    new Color(255, 210, 80));
        }

        String text = String.format("%.0f / %.0f", health + absorption, maxHealth);
        float size = scaled(6f);

        getSmallFont().drawCenteredText(
                matrixStack,
                text,
                x + width / 2f,
                y + barHeight / 2f - size / 2f,
                size,
                Color.WHITE
        );
    }

    private void renderHungerBar(MatrixStack matrixStack, float x, float y, float width) {
        PlayerEntity player = mc.player;

        int hunger = player.getHungerManager().getFoodLevel();
        float percent = hunger / 20f;

        RenderUtil.RECT.draw(matrixStack, x, y, width, barHeight,
                barHeight * 0.5f, new Color(0,0,0,90));

        Color base = getHungerColor(hunger);
        Color darker = ColorUtil.darker(base, 0.25f);

        RenderUtil.RECT.draw(matrixStack, x, y, width * percent, barHeight,
                barHeight * 0.5f, darker);

        String text = hunger + " / 20";
        float size = scaled(6f);

        getSmallFont().drawCenteredText(
                matrixStack,
                text,
                x + width / 2f,
                y + barHeight / 2f - size / 2f,
                size,
                Color.WHITE
        );
    }

    private void renderExpBar(MatrixStack matrixStack, float x, float y, float width) {
        PlayerEntity player = mc.player;

        float progress = player.experienceProgress;
        int level = player.experienceLevel;

        RenderUtil.RECT.draw(matrixStack, x, y, width, barHeight,
                barHeight * 0.5f, new Color(0,0,0,90));

        RenderUtil.RECT.draw(matrixStack, x, y, width * progress, barHeight,
                barHeight * 0.5f, new Color(0,180,255));

        String text = "Lvl " + level;
        float size = scaled(6f);

        getSmallFont().drawCenteredText(
                matrixStack,
                text,
                x + width / 2f,
                y + barHeight / 2f - size / 2f,
                size,
                Color.WHITE
        );
    }

    private Color getHealthColor(float healthPercent) {
        if (healthPercent > 0.75f) {
            return new Color(0, 200, 0);   // Зеленый
        } else if (healthPercent > 0.5f) {
            return new Color(200, 200, 0); // Желтый
        } else if (healthPercent > 0.25f) {
            return new Color(255, 140, 0); // Оранжевый
        } else {
            return new Color(200, 0, 0);   // Красный
        }
    }

    private Color getHungerColor(int hunger) {
        if (hunger > 15) return new Color(0, 200, 0);   // Зеленый
        if (hunger > 10) return new Color(200, 200, 0); // Желтый
        if (hunger > 5) return new Color(255, 140, 0);  // Оранжевый
        return new Color(200, 0, 0);                    // Красный
    }

    private Font getSmallFont() {
        return getMediumFont();
    }

    @Override
    public void render(MatrixStack matrixStack) {}


}