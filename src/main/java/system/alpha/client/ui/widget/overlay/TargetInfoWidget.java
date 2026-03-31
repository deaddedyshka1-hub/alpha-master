package system.alpha.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.ColorUtil;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.math.MathUtil;
import system.alpha.api.utils.render.RenderUtil;
import system.alpha.api.system.configs.WidgetConfigManager;
import system.alpha.client.ui.widget.Widget;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.time.Duration;
import java.util.List;

public class TargetInfoWidget extends Widget {
    private boolean showAnimals = true;
    private boolean showMobs = true;
    private boolean showPlayers = true;
    private boolean showSelf = false;

    private float settingsAnim;
    private boolean settingsOpen;
    private float settingsX, settingsY;
    private boolean wasRightClick;
    private boolean wasLeftClick;
    private final AnimationUtil settingsScaleAnimation = new AnimationUtil();
    private boolean wasChatOpen = false;

    private final AnimationUtil showAnimation = new AnimationUtil();
    private float healthAnimation = 0f;
    private LivingEntity target = null;
    private double maxRange = 10.0;

    // Флаг для принудительного показа себя при открытом чате
    private boolean forceShowSelfInChat = false;

    @Override
    public String getName() {
        return "Target info";
    }

    public TargetInfoWidget() {
        super(30f, 30f);
        loadConfig();
    }

    // Геттеры и сеттеры для настроек
    public boolean isShowAnimals() {
        return showAnimals;
    }

    public void setShowAnimals(boolean showAnimals) {
        this.showAnimals = showAnimals;
        saveConfig();
    }

    public boolean isShowMobs() {
        return showMobs;
    }

    public void setShowMobs(boolean showMobs) {
        this.showMobs = showMobs;
        saveConfig();
    }

    public boolean isShowPlayers() {
        return showPlayers;
    }

    public void setShowPlayers(boolean showPlayers) {
        this.showPlayers = showPlayers;
        saveConfig();
    }

    public boolean isShowSelf() {
        return showSelf;
    }

    public void setShowSelf(boolean showSelf) {
        this.showSelf = showSelf;
        saveConfig();
    }

    // Загрузка конфигурации
    public void loadConfig() {
        WidgetConfigManager configManager = WidgetConfigManager.getInstance();

        showAnimals = configManager.getBoolean("TargetInfo", "showAnimals", true);
        showMobs = configManager.getBoolean("TargetInfo", "showMobs", true);
        showPlayers = configManager.getBoolean("TargetInfo", "showPlayers", true);
        showSelf = configManager.getBoolean("TargetInfo", "showSelf", false);
    }

    // Сохранение конфигурации
    private void saveConfig() {
        WidgetConfigManager configManager = WidgetConfigManager.getInstance();

        configManager.setValue("TargetInfo", "showAnimals", showAnimals);
        configManager.setValue("TargetInfo", "showMobs", showMobs);
        configManager.setValue("TargetInfo", "showPlayers", showPlayers);
        configManager.setValue("TargetInfo", "showSelf", showSelf);

        configManager.save();
    }

    @Override
    public void render(MatrixStack matrixStack) {
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        settingsScaleAnimation.update();
        showAnimation.update();

        // Обновляем анимацию меню настроек
        if (chatOpen && !wasChatOpen) {
            // Чат открылся - включаем принудительный показ себя
            forceShowSelfInChat = true;
        } else if (!chatOpen && wasChatOpen) {
            // Чат закрылся - закрываем меню и выключаем принудительный показ
            settingsOpen = false;
            settingsScaleAnimation.reset();
            forceShowSelfInChat = false;
        }
        wasChatOpen = chatOpen;

        updateTarget(chatOpen);

        // Если чат открыт и настройка "показывать себя" включена,
        // но цели нет - устанавливаем себя как цель
        if (chatOpen && showSelf && target == null && mc.player != null) {
            target = mc.player;
            showAnimation.run(1.0, 300, Easing.SINE_OUT);
            healthAnimation = MathHelper.clamp(mc.player.getHealth() / mc.player.getMaxHealth(), 0f, 1f);
        }

        if (showAnimation.getValue() <= 0.0 || target == null) {
            // Если нет цели, но чат открыт и навели на виджет, показываем меню
            if (chatOpen) {
                handleInteraction(matrixStack,
                        getDraggable().getX(),
                        getDraggable().getY(),
                        getDraggable().getWidth(),
                        getDraggable().getHeight());
            }
            return;
        }

        // Обновляем анимацию здоровья
        if (target.isAlive()) {
            healthAnimation = MathHelper.clamp(MathUtil.interpolate(
                    healthAnimation,
                    target.getHealth() / target.getMaxHealth(),
                    0.3f
            ), 0f, 1f);
        }

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float anim = (float) showAnimation.getValue();

        float[] headProperties = headProperties(x, y);
        float headX = headProperties[0];
        float headY = headProperties[1];
        float headSize = headProperties[2];

        float bigFontSize = headSize * 0.35f;
        float smallFontSize = (headSize * 0.4f) * 0.7f;

        String targetName = target.getName().getString();
        String healthText = String.format("%.1f", target.getHealth() + target.getAbsorptionAmount()) + "HP";
        float healthTextWidth = getMediumFont().getWidth(healthText, smallFontSize);

        float offset = getGap() * 3f;
        float margin = getGap() * 2f;
        float width = headSize * 3.7f + margin * 2f;
        float height = headSize + getGap() * 2f;
        float backgroundRound = offset * 0.7f;

        int fullAlpha = (int) (anim * 255f);

        float[] healthBarProperties = healthBarProperties();
        float healthBarHeight = healthBarProperties[0];
        float healthBarRound = healthBarProperties[1];
        float healthBarY = y + height - healthBarHeight - margin;
        float healthBarX = x + headSize + margin;
        float healthBarWidth = width - margin * 2.5f - headSize - healthTextWidth;

        float diffHealth = Math.abs(smallFontSize - healthBarHeight) / 2f;

        float nameDiffToHealthBar = Math.abs((y + margin) - (healthBarY - margin / 2f));
        float nameY = y + margin + nameDiffToHealthBar / 2f - bigFontSize / 2f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, backgroundRound, UIColors.widgetBlur(fullAlpha));

        Color textColor = UIColors.textColor(fullAlpha);
        getMediumFont().drawWrap(matrixStack, targetName, healthBarX, nameY, width - headSize - margin, bigFontSize, textColor, scaled(9f), Duration.ofMillis(2500), Duration.ofMillis(1700));
        getMediumFont().drawText(matrixStack, healthText, x + width - healthTextWidth - margin, healthBarY - diffHealth, smallFontSize, textColor);

        RenderUtil.RECT.draw(matrixStack, healthBarX, healthBarY, healthBarWidth, healthBarHeight, healthBarRound, UIColors.backgroundBlur(fullAlpha));

        Color color1 = UIColors.gradient(0, fullAlpha);
        Color color2 = UIColors.gradient(90, fullAlpha);
        RenderUtil.GRADIENT_RECT.draw(matrixStack, healthBarX, healthBarY, healthBarWidth * healthAnimation, healthBarHeight, healthBarRound, color1, color2, color1, color2);

        if (target instanceof PlayerEntity player) {
            Color headColor = ColorUtil.setAlpha(Color.WHITE, fullAlpha);
            RenderUtil.TEXTURE_RECT.drawHead(matrixStack, player, headX, headY, headSize, headSize, getGap() / 2f, 0f, headColor);
        } else {
            float headFontSize = headSize * 0.8f;
            getSemiBoldFont().drawCenteredText(matrixStack, "?", headX + headSize / 2f, headY + headSize / 2f - headFontSize / 2f, headFontSize, UIColors.textColor(fullAlpha));
        }

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);

        // Обработка взаимодействия
        if (chatOpen) {
            handleInteraction(matrixStack, x, y, width, height);
        }
    }

    private void updateTarget(boolean chatOpen) {
        LivingEntity pretendTarget = getTargetDirectAim();

        if (pretendTarget != null) {
            target = pretendTarget;
        } else if (chatOpen && showSelf && mc.player != null) {
            // При открытом чате и включенной настройке показывать себя
            // устанавливаем себя как цель
            target = mc.player;
        } else {
            target = null;
        }

        // Обновляем анимацию показа/скрытия
        if (target != null) {
            showAnimation.run(1.0, 300, Easing.SINE_OUT);
        } else {
            showAnimation.run(0.0, 300, Easing.SINE_OUT);
        }
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

        String[] opts = {
                "Показывать животных",
                "Показывать мобов",
                "Показывать игроков",
                "Показывать себя"
        };

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
        boolean[] states = {showAnimals, showMobs, showPlayers, showSelf};

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
                    case 0 -> setShowAnimals(!showAnimals);
                    case 1 -> setShowMobs(!showMobs);
                    case 2 -> setShowPlayers(!showPlayers);
                    case 3 -> setShowSelf(!showSelf);
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

    private float[] healthBarProperties() {
        float height = scaled(5f);
        float round = height * 0.3f;
        return new float[]{height, round};
    }

    private float[] headProperties(float xPos, float yPos) {
        float x = xPos + getGap();
        float y = yPos + getGap();
        float size = scaled(25f);
        return new float[]{x, y, size};
    }

    private LivingEntity getTargetDirectAim() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) {
            return null;
        }

        // Проверяем прямое наведение прицела
        HitResult hitResult = mc.crosshairTarget;

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();

            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;

                // Проверяем условия с учетом фильтров
                if (isValidTarget(livingEntity, mc) && canSeeDirectly(livingEntity, mc)) {
                    return livingEntity;
                }
            }
        }

        // Дополнительная проверка через raycast
        return findTargetByRaycast(mc);
    }

    private LivingEntity findTargetByRaycast(MinecraftClient mc) {
        if (mc.player == null || mc.cameraEntity == null) return null;

        // Получаем позицию и направление камеры
        Vec3d cameraPos = mc.cameraEntity.getCameraPosVec(1.0f);
        Vec3d rotationVec = mc.cameraEntity.getRotationVec(1.0f);
        Vec3d endPos = cameraPos.add(rotationVec.multiply(maxRange));

        // Ищем все сущности на пути луча
        List<Entity> entities = mc.world.getOtherEntities(mc.player,
                new Box(cameraPos, endPos).expand(1.0));

        // Ищем первую живую сущность, которую можно увидеть напрямую
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                if (isValidTarget(livingEntity, mc) && canSeeDirectly(livingEntity, mc)) {
                    return livingEntity;
                }
            }
        }

        return null;
    }

    private boolean isValidTarget(LivingEntity entity, MinecraftClient mc) {
        if (entity == null || !entity.isAlive() || mc.player == null) return false;

        // Проверка на себя
        if (entity == mc.player) {
            return showSelf;
        }

        // Проверка типа сущности
        if (entity instanceof PlayerEntity) {
            return showPlayers;
        }

        // Определяем тип сущности (животное или моб)
        String entityType = getEntityType(entity);

        if ("animal".equals(entityType)) {
            return showAnimals;
        } else if ("mob".equals(entityType)) {
            return showMobs;
        }

        // По умолчанию показываем
        return true;
    }

    private String getEntityType(LivingEntity entity) {
        String name = entity.getType().toString().toLowerCase();

        if (name.contains("cow") || name.contains("pig") || name.contains("sheep") ||
                name.contains("chicken") || name.contains("horse") || name.contains("wolf") ||
                name.contains("cat") || name.contains("fox") || name.contains("rabbit") ||
                name.contains("llama") || name.contains("panda") || name.contains("bee") ||
                name.contains("dolphin") || name.contains("turtle") || name.contains("parrot") ||
                name.contains("fish") || name.contains("axolotl") || name.contains("frog") ||
                name.contains("goat") || name.contains("bat")) {
            return "animal";
        }

        if (name.contains("zombie") || name.contains("skeleton") || name.contains("creeper") ||
                name.contains("spider") || name.contains("enderman") || name.contains("witch") ||
                name.contains("slime") || name.contains("phantom") || name.contains("guardian") ||
                name.contains("shulker") || name.contains("wither") || name.contains("ender_dragon") ||
                name.contains("blaze") || name.contains("ghast") || name.contains("magma_cube") ||
                name.contains("pillager") || name.contains("vindicator") || name.contains("evoker") ||
                name.contains("ravager") || name.contains("vex") || name.contains("warden") ||
                name.contains("hoglin") || name.contains("piglin") || name.contains("strider")) {
            return "mob";
        }

        return "unknown";
    }

    private boolean canSeeDirectly(LivingEntity entity, MinecraftClient mc) {
        if (mc.player == null || mc.cameraEntity == null) return false;

        // Получаем позицию камеры и центр сущности
        Vec3d cameraPos = mc.cameraEntity.getCameraPosVec(1.0f);
        Vec3d entityPos = entity.getBoundingBox().getCenter();

        // Проверяем, нет ли препятствий между камерой и сущностью
        return mc.world.raycast(
                new RaycastContext(
                        cameraPos,
                        entityPos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        ).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }
}