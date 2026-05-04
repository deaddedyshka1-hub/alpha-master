package system.alpha.client.ui.widget.overlay.notify;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import system.alpha.api.event.events.render.Render2DEvent;
import system.alpha.api.system.configs.WidgetConfigManager;
import system.alpha.api.system.draggable.Draggable;
import system.alpha.api.system.draggable.DraggableManager;
import system.alpha.api.system.files.FileUtil;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.render.RenderUtil;
import system.alpha.client.features.modules.render.InterfaceModule;
import system.alpha.client.services.CheckService;
import system.alpha.client.ui.widget.Widget;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class NotificationWidget extends Widget {
    private final Identifier star = FileUtil.getImage("particles/star1");
    private boolean wasChatOpen;
    private float fixedX, fixedY;
    private float hintAlpha;

    private boolean specRequest = false;
    private boolean moduleState = false;
    private boolean lowDurability = false;
    private boolean showCheckTimer = true;

    private float settingsAnim;
    private boolean settingsOpen;
    private float settingsX, settingsY;

    private boolean wasRightClick;
    private boolean wasLeftClick;
    private long lastDuraCheck;

    private final List<Notif> notifs = new CopyOnWriteArrayList<>();

    private final AnimationUtil chatOpenAnimation = new AnimationUtil();
    private final AnimationUtil scaleAnimation = new AnimationUtil();
    private final AnimationUtil slideAnimation = new AnimationUtil();
    private final AnimationUtil settingsScaleAnimation = new AnimationUtil();
    private final AnimationUtil timerAnimation = new AnimationUtil();

    private boolean hasPlayedOpenAnimation = false;
    private boolean hasPlayedCloseAnimation = true;

    private final CheckService checkService = CheckService.getInstance();
    private String lastCheckedPlayer = null;
    private float timerPulse = 0;
    private boolean timerPulseDirection = true;

    private Draggable timerDraggable;
    private float timerX = 0.5f;
    private float timerY = 0.15f;

    public NotificationWidget() {
        super(0, 0);
        updateFixedPosition();
        loadTimerPosition();
    }

    private void createTimerDraggable() {
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        float x = timerX * screenWidth;
        float y = timerY * screenHeight;

        timerDraggable = DraggableManager.getInstance().create(InterfaceModule.getInstance(), "TimerWidget_" + getName(), x, y);
        timerDraggable.setWidth(100);
        timerDraggable.setHeight(40);
    }

    private void saveTimerPosition() {
        if (timerDraggable != null && mc.getWindow() != null) {
            float screenWidth = mc.getWindow().getScaledWidth();
            float screenHeight = mc.getWindow().getScaledHeight();
            timerX = timerDraggable.getX() / screenWidth;
            timerY = timerDraggable.getY() / screenHeight;

            WidgetConfigManager configManager = WidgetConfigManager.getInstance();
            configManager.setValue("Notification", "timerX", timerX);
            configManager.setValue("Notification", "timerY", timerY);
            configManager.save();
        }
    }

    private void loadTimerPosition() {
        WidgetConfigManager configManager = WidgetConfigManager.getInstance();
        timerX = configManager.getFloat("Notification", "timerX", 0.5f);
        timerY = configManager.getFloat("Notification", "timerY", 0.15f);
    }


    public boolean isSpecRequest() {
        return specRequest;
    }

    public void setSpecRequest(boolean specRequest) {
        this.specRequest = specRequest;
        saveConfig();
    }

    public boolean isModuleState() {
        return moduleState;
    }

    public void setModuleState(boolean moduleState) {
        this.moduleState = moduleState;
        saveConfig();
    }

    public void setLowDurability(boolean lowDurability) {
        this.lowDurability = lowDurability;
        saveConfig();
    }

    public void setShowCheckTimer(boolean showCheckTimer) {
        this.showCheckTimer = showCheckTimer;
        saveConfig();
    }

    public void loadConfig() {
        WidgetConfigManager configManager = WidgetConfigManager.getInstance();

        specRequest = configManager.getBoolean("Notification", "specRequest", false);
        moduleState = configManager.getBoolean("Notification", "moduleState", false);
        lowDurability = configManager.getBoolean("Notification", "lowDurability", false);
        showCheckTimer = configManager.getBoolean("Notification", "showCheckTimer", true);
        loadTimerPosition();
    }

    private void saveConfig() {
        WidgetConfigManager configManager = WidgetConfigManager.getInstance();

        configManager.setValue("Notification", "specRequest", specRequest);
        configManager.setValue("Notification", "moduleState", moduleState);
        configManager.setValue("Notification", "lowDurability", lowDurability);
        configManager.setValue("Notification", "showCheckTimer", showCheckTimer);

        configManager.save();
        saveTimerPosition();
    }

    @Override
    public String getName() {
        return "Notification";
    }

    public void addNotif(String text) {
        notifs.add(new Notif(text));
    }

    public void onSendMessage(String message) {
        if (message.startsWith("/check ") && !message.startsWith("/check addtime") &&
                !message.startsWith("/check settime") && !message.startsWith("/check setdefaulttime") &&
                !message.startsWith("/check settimeadditions") && !message.startsWith("/check list")) {

            String[] parts = message.split(" ");
            if (parts.length >= 2) {
                String playerName = parts[1];

                if (parts.length == 2) {
                    if (checkService.isBeingChecked(playerName)) {
                        forceStopCheck(playerName);
                    } else {
                        checkService.startCheck(playerName);
                        addNotif("Начата проверка игрока " + playerName);
                        lastCheckedPlayer = playerName;
                        timerAnimation.reset();
                        timerAnimation.setValue(0.0);
                        timerAnimation.run(1.0, 400, Easing.BACK_OUT);
                        createTimerDraggable();
                    }
                } else if (parts.length >= 3 && parts[2].equalsIgnoreCase("start")) {
                    if (!checkService.isBeingChecked(playerName)) {
                        checkService.startCheck(playerName);
                        addNotif("Начата проверка игрока " + playerName);
                        lastCheckedPlayer = playerName;
                        timerAnimation.reset();
                        timerAnimation.setValue(0.0);
                        timerAnimation.run(1.0, 400, Easing.BACK_OUT);
                        createTimerDraggable();
                    }
                } else if (parts.length >= 3 && parts[2].equalsIgnoreCase("stop")) {
                    if (checkService.isBeingChecked(playerName)) {
                        forceStopCheck(playerName);
                    }
                }
            }
        }
    }

    public void onChatMessage(String message) {
        System.out.println("[DEBUG] Chat message: " + message);

        if (message.contains("Проверки » Игрок") && message.contains("вызван на проверку")) {
            String playerName = extractPlayerNameFromMessage(message);
            System.out.println("[DEBUG] Player called for check: " + playerName);
            if (playerName != null && !checkService.isBeingChecked(playerName)) {
                checkService.startCheck(playerName);
                addNotif("Начата проверка игрока " + playerName);
                lastCheckedPlayer = playerName;
                timerAnimation.reset();
                timerAnimation.setValue(0.0);
                timerAnimation.run(1.0, 400, Easing.BACK_OUT);
            }
        }

        if (message.contains("Вы отпустили игрока") && message.contains("с проверки")) {
            System.out.println("[DEBUG] Player released message received");
            if (lastCheckedPlayer != null) {
                System.out.println("[DEBUG] Stopping check for: " + lastCheckedPlayer);
                forceStopCheck(lastCheckedPlayer);
            } else if (hasActiveCheck()) {
                CheckService.CheckSession activeCheck = getActiveCheck();
                if (activeCheck != null) {
                    System.out.println("[DEBUG] Stopping check for active player: " + activeCheck.getPlayerName());
                    forceStopCheck(activeCheck.getPlayerName());
                }
            }
        }

        if ((message.contains("Проверка") && message.contains("закончена")) ||
                (message.contains("Проверка") && message.contains("остановлена"))) {
            String playerName = extractPlayerNameFromMessage(message);
            System.out.println("[DEBUG] Check finished: " + playerName);
            if (playerName != null && checkService.isBeingChecked(playerName)) {
                forceStopCheck(playerName);
            } else if (lastCheckedPlayer != null) {
                forceStopCheck(lastCheckedPlayer);
            }
        }
    }

    public void onPlayerLeave(String playerName) {
        System.out.println("[DEBUG] Player left: " + playerName);
        if (checkService.isBeingChecked(playerName)) {
            CheckService.CheckSession session = checkService.getCheck(playerName);
            if (session != null && !session.isExpired()) {
                addNotif("Игрок " + playerName + " вышел с сервера во время проверки");
                checkService.stopCheck(playerName);
                lastCheckedPlayer = null;
                timerAnimation.reset();
                timerAnimation.setValue(0.0);
                timerAnimation.run(0.0, 300, Easing.BACK_IN);
            }
        }
    }

    private String extractPlayerNameFromMessage(String message) {
        try {
            int startIndex = message.indexOf("Игрок ") + 6;
            int endIndex = message.indexOf(" вызван", startIndex);
            if (startIndex > 6 && endIndex > startIndex) {
                return message.substring(startIndex, endIndex);
            }

            startIndex = message.indexOf("игрока ") + 7;
            if (startIndex > 7) {
                endIndex = message.indexOf(" ", startIndex);
                if (endIndex > startIndex) {
                    return message.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void forceStopCheck(String playerName) {
        if (checkService.isBeingChecked(playerName)) {
            checkService.stopCheck(playerName);
            addNotif("Проверка игрока " + playerName + " завершена");
            lastCheckedPlayer = null;
            timerAnimation.reset();
            timerAnimation.setValue(0.0);
            timerAnimation.run(0.0, 300, Easing.BACK_IN);
            saveTimerPosition();
        }
    }

    public void updateChecks() {
        checkService.update();

        if (lastCheckedPlayer != null && !checkService.isBeingChecked(lastCheckedPlayer)) {
            System.out.println("[DEBUG] Player no longer being checked: " + lastCheckedPlayer);
            if (checkService.getCheck(lastCheckedPlayer) != null &&
                    checkService.getCheck(lastCheckedPlayer).isExpired()) {
                addNotif("Время проверки игрока " + lastCheckedPlayer + " истекло!");
            }
            lastCheckedPlayer = null;
        }

        if (lastCheckedPlayer == null) {
            for (CheckService.CheckSession session : checkService.getActiveChecks()) {
                if (!session.isExpired()) {
                    lastCheckedPlayer = session.getPlayerName();
                    System.out.println("[DEBUG] New active check: " + lastCheckedPlayer);
                    timerAnimation.reset();
                    timerAnimation.setValue(0.0);
                    timerAnimation.run(1.0, 400, Easing.BACK_OUT);
                    createTimerDraggable();
                    break;
                }
            }
        }

        if (hasActiveCheck() && getActiveCheck() != null) {
            long remaining = getActiveCheck().getRemainingTime();
            if (remaining < 30000 && remaining > 0) {
                if (timerPulseDirection) {
                    timerPulse += 0.05f;
                    if (timerPulse >= 1.0f) {
                        timerPulseDirection = false;
                    }
                } else {
                    timerPulse -= 0.05f;
                    if (timerPulse <= 0.3f) {
                        timerPulseDirection = true;
                    }
                }
            } else {
                timerPulse = 0;
            }
        }

        if (timerDraggable != null) {
            timerDraggable.onDraw();
            if (timerDraggable.isDragging()) {
                saveTimerPosition();
            }
        }
    }

    private boolean hasActiveCheck() {
        return checkService.getActiveChecks().stream().anyMatch(s -> !s.isExpired());
    }

    private CheckService.CheckSession getActiveCheck() {
        return checkService.getActiveChecks().stream().filter(s -> !s.isExpired()).findFirst().orElse(null);
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        MatrixStack ms = event.matrixStack();

        updateChecks();
        timerAnimation.update();

        notifs.removeIf(Notif::shouldRemove);
        renderNotifs(ms);

        renderCheckTimer(ms);

        if (lowDurability && System.currentTimeMillis() - lastDuraCheck > 5000) {
            checkDura();
            lastDuraCheck = System.currentTimeMillis();
        }

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        chatOpenAnimation.update();
        scaleAnimation.update();
        slideAnimation.update();
        settingsScaleAnimation.update();

        updateFixedPosition();

        if (chatOpen && !wasChatOpen) {
            resetAnimations();
            chatOpenAnimation.run(1.0, 400, Easing.BACK_OUT);
            scaleAnimation.run(1.0, 500, Easing.ELASTIC_OUT);
            slideAnimation.run(1.0, 300, Easing.CUBIC_OUT);
            hasPlayedOpenAnimation = true;
            hasPlayedCloseAnimation = false;
        } else if (!chatOpen && wasChatOpen) {
            chatOpenAnimation.run(0.0, 300, Easing.BACK_IN);
            scaleAnimation.run(0.0, 250, Easing.CUBIC_IN);
            slideAnimation.run(0.0, 200, Easing.CUBIC_IN);
            hasPlayedOpenAnimation = false;
            hasPlayedCloseAnimation = true;
            settingsOpen = false;
            resetSettingsAnimation();
        }

        boolean chatStateChanged = (chatOpen != wasChatOpen);
        wasChatOpen = chatOpen;

        if (chatOpen && !chatOpenAnimation.isActive() && !hasPlayedOpenAnimation) {
            chatOpenAnimation.setValue(1.0);
            scaleAnimation.setValue(1.0);
            slideAnimation.setValue(1.0);
            hasPlayedOpenAnimation = true;
        }

        if (!chatOpen && !chatOpenAnimation.isActive() && !hasPlayedCloseAnimation) {
            chatOpenAnimation.setValue(0.0);
            scaleAnimation.setValue(0.0);
            slideAnimation.setValue(0.0);
            hasPlayedCloseAnimation = true;
        }

        if (chatStateChanged && chatOpen) {
            updateFixedPosition();
        }

        if (!chatOpen) {
            settingsOpen = false;
            return;
        }

        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

        float pad = scaled(5);
        float icon = scaled(10);
        float gap = scaled(4);
        float font = scaled(7);

        String txt = "Это уведомление, кликни на меня для настройки";
        float txtW = getMediumFont().getWidth(txt, font);

        float w = pad + icon + gap + txtW + pad;
        float h = icon + pad * 2;
        float r = scaled(3);

        float slideOffset = (float) (slideAnimation.getValue() * scaled(30));
        float baseY = fixedY + scaled(15);
        float animatedY = baseY - slideOffset;

        float scaleValue = (float) scaleAnimation.getValue();
        scaleValue = Math.max(0, Math.min(1, scaleValue));

        float x = fixedX - w / 2;
        float y = animatedY;

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

        hintAlpha += (hover ? 0.15f : -0.15f);
        hintAlpha = Math.max(0, Math.min(1, hintAlpha));

        if (hintAlpha > 0) {
            String hint = "ПКМ - Дополнительные настройки";
            float hintW = getMediumFont().getWidth(hint, font);
            float chatAlphaValue = (float) Math.max(0, Math.min(1, chatOpenAnimation.getValue()));
            int alpha = (int) (255 * hintAlpha * chatAlphaValue);
            alpha = Math.max(0, Math.min(255, alpha));
            getMediumFont().drawText(ms, hint, fixedX - hintW / 2, y - font - scaled(3), font, new Color(255, 255, 255, alpha));
        }

        ms.push();
        float centerX = x + w / 2;
        float centerY = y + h / 2;
        ms.translate(centerX, centerY, 0);
        ms.scale(scaleValue, scaleValue, 1);
        ms.translate(-centerX, -centerY, 0);

        float chatAlphaValue = (float) Math.max(0, Math.min(1, chatOpenAnimation.getValue()));
        int alpha = (int) (255 * chatAlphaValue);
        alpha = Math.max(0, Math.min(255, alpha));

        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, r, UIColors.widgetBlur());

        float ix = x + pad;
        float iy = y + h / 2 - icon / 2;

        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int tex = mc.getTextureManager().getTexture(star).getGlId();
            RenderUtil.TEXTURE_RECT.draw(ms, ix, iy, icon, icon, scaled(1.5f), new Color(255, 80, 80, alpha), 0, 0, 1, 1, tex);
        } catch (Exception e) {
            RenderUtil.RECT.draw(ms, ix, iy, icon, icon, scaled(1.5f), new Color(255, 80, 80, alpha));
        }

        getMediumFont().drawText(ms, txt, ix + icon + gap, y + h / 2 - font / 2, font, new Color(255, 255, 255, alpha));
        ms.pop();

        renderSettings(ms, mx, my);
    }

    private void renderCheckTimer(MatrixStack ms) {
        if (!showCheckTimer) return;

        CheckService.CheckSession activeCheck = getActiveCheck();
        if (activeCheck == null) return;

        float timerAnimValue = (float) timerAnimation.getValue();

        boolean isExpired = activeCheck.isExpired();
        boolean hasPlayerLeft = false;

        if (lastCheckedPlayer != null && !checkService.isBeingChecked(lastCheckedPlayer) && isExpired == false) {
            hasPlayerLeft = true;
        }

        if (!isExpired && !hasPlayerLeft && timerAnimValue <= 0.01f) return;

        if (timerDraggable == null || mc.getWindow() == null) return;

        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        float x = timerDraggable.getX();
        float y = timerDraggable.getY();

        float font = scaled(7);
        float pad = scaled(6);
        float icon = scaled(10);
        float gap = scaled(4);
        float r = scaled(3);

        String playerText = "Проверка: " + activeCheck.getPlayerName();
        String timeText;
        String statusText = null;

        if (isExpired) {
            timeText = "Время истекло!";
        } else if (hasPlayerLeft) {
            timeText = "Игрок покинул сервер!";
        } else {
            timeText = "Осталось времени: " + activeCheck.getFormattedTime();
        }

        float timeW = getMediumFont().getWidth(timeText, font);
        float playerW = getMediumFont().getWidth(playerText, font);
        float maxW = Math.max(timeW, playerW);

        float w = pad + icon + gap + maxW + pad;
        float h = icon + pad * 2;

        if (x + w > screenWidth) x = screenWidth - w - 5;
        if (x < 0) x = 5;
        if (y + h > screenHeight) y = screenHeight - h - 5;
        if (y < 0) y = 5;

        timerDraggable.setX(x);
        timerDraggable.setY(y);
        timerDraggable.setWidth(w);
        timerDraggable.setHeight(h);

        ms.push();
        float centerX = x + w / 2;
        float centerY = y + h / 2;
        ms.translate(centerX, centerY, 0);

        float scale = Easing.BACK_OUT.apply(timerAnimValue);
        ms.scale(scale, scale, 1);
        ms.translate(-centerX, -centerY, 0);

        int alpha = (int) (255 * Math.min(1.0f, Math.max(0.0f, timerAnimValue)));
        alpha = Math.max(0, Math.min(255, alpha));

        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, r, UIColors.widgetBlur());

        float ix = x + pad;
        float iy = y + h / 2 - icon / 2;

        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int tex = mc.getTextureManager().getTexture(star).getGlId();
            RenderUtil.TEXTURE_RECT.draw(ms, ix, iy, icon, icon, scaled(1.5f),
                    new Color(255, 200, 100, alpha), 0, 0, 1, 1, tex);
        } catch (Exception e) {
            RenderUtil.RECT.draw(ms, ix, iy, icon, icon, scaled(1.5f), new Color(255, 200, 100, alpha));
        }

        Color timeColor;
        if (isExpired || hasPlayerLeft) {
            timeColor = new Color(255, 100, 100, alpha);
        } else if (activeCheck.getRemainingTime() < 30000) {
            int pulseAlpha = (int) ((150 + 105 * timerPulse) * timerAnimValue);
            pulseAlpha = Math.max(0, Math.min(255, pulseAlpha));
            timeColor = new Color(255, 100, 100, pulseAlpha);
        } else {
            timeColor = new Color(255, 255, 255, alpha);
        }

        getMediumFont().drawText(ms, playerText, ix + icon + gap, y + h / 2 - font - gap / 2, font,
                new Color(255, 200, 100, alpha));
        getMediumFont().drawText(ms, timeText, ix + icon + gap, y + h / 2 + gap / 2, font, timeColor);

        ms.pop();

        if (timerDraggable.isDragging()) {
            saveTimerPosition();
        }
    }

    private void renderSettings(MatrixStack ms, double mx, double my) {
        settingsScaleAnimation.update();

        settingsAnim += (settingsOpen ? 0.15f : -0.15f);
        settingsAnim = Math.max(0, Math.min(1, settingsAnim));

        if (settingsAnim <= 0.05f && !settingsOpen) return;

        float pad = scaled(6);
        float gap = scaled(5);
        float font = scaled(6);
        float toggle = scaled(7);

        String[] opts = {"Просьба о наблюдении", "Состояние модулей", "Низкая прочность предметов", "Отсчёт времени"};

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

        float scaleAnim = (float) settingsScaleAnimation.getValue();

        ms.translate(centX, centY, 0);
        ms.scale(scaleAnim, scaleAnim, 1);
        ms.translate(-centX, -centY, 0);

        int alpha = (int) (255 * settingsAnim * scaleAnim);
        alpha = Math.max(0, Math.min(255, alpha));

        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, scaled(3),
                new Color(UIColors.widgetBlur().getRed(),
                        UIColors.widgetBlur().getGreen(),
                        UIColors.widgetBlur().getBlue(),
                        alpha));

        float cy = y + pad;
        boolean[] states = {specRequest, moduleState, lowDurability, showCheckTimer};

        boolean leftClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        for (int i = 0; i < opts.length; i++) {
            float tx = x + pad + toggle + gap;
            int textAlpha = (int) (255 * settingsAnim * scaleAnim);
            textAlpha = Math.max(0, Math.min(255, textAlpha));

            getMediumFont().drawText(ms, opts[i], tx, cy, font, new Color(200, 200, 200, textAlpha));

            float toggleX = x + pad;
            float toggleY = cy;

            boolean hoverToggle = mx >= toggleX && mx <= toggleX + toggle && my >= toggleY && my <= toggleY + toggle;

            if (hoverToggle && leftClick && !wasLeftClick) {
                switch (i) {
                    case 0 -> setSpecRequest(!specRequest);
                    case 1 -> setModuleState(!moduleState);
                    case 2 -> setLowDurability(!lowDurability);
                    case 3 -> setShowCheckTimer(!showCheckTimer);
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

    private void renderNotifs(MatrixStack ms) {
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        float yOff = scaled(25);

        for (Notif n : notifs) {
            float v = n.getAlpha();
            if (v <= 0.05f) continue;

            String txt = n.text;
            float font = scaled(7);
            float pad = scaled(5);
            float icon = scaled(10);
            float gap = scaled(4);
            float r = scaled(3);

            float txtW = getMediumFont().getWidth(txt, font);
            float w = pad + icon + gap + txtW + pad;
            float h = icon + pad * 2;

            float x = cx - w / 2;
            float y = cy + yOff;

            ms.push();
            float centX = cx;
            float centY = y + h / 2;
            ms.translate(centX, centY, 0);

            float scale = (float) Easing.ELASTIC_OUT.apply(v);
            ms.scale(scale, scale, 1);
            ms.translate(-centX, -centY, 0);

            RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, r, UIColors.widgetBlur());

            float ix = x + pad;
            float iy = y + h / 2 - icon / 2;

            boolean isEnabled = n.text.contains("Начата") || n.text.contains("включен");
            Color iconColor = isEnabled ? new Color(100, 255, 100) : new Color(255, 100, 100);

            try {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                int tex = mc.getTextureManager().getTexture(star).getGlId();
                int iconAlpha = (int) (255 * v);
                iconAlpha = Math.max(0, Math.min(255, iconAlpha));
                RenderUtil.TEXTURE_RECT.draw(ms, ix, iy, icon, icon, scaled(1.5f),
                        new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), iconAlpha), 0, 0, 1, 1, tex);
            } catch (Exception e) {
                int iconAlpha = (int) (255 * v);
                iconAlpha = Math.max(0, Math.min(255, iconAlpha));
                RenderUtil.RECT.draw(ms, ix, iy, icon, icon, scaled(1.5f),
                        new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), iconAlpha));
            }

            int textAlpha = (int) (255 * v);
            textAlpha = Math.max(0, Math.min(255, textAlpha));

            Color textColor;
            if (n.text.contains("Начата")) {
                textColor = new Color(100, 255, 100, textAlpha);
            } else if (n.text.contains("завершена") || n.text.contains("истекло") || n.text.contains("вышел")) {
                textColor = new Color(255, 100, 100, textAlpha);
            } else {
                textColor = new Color(255, 255, 255, textAlpha);
            }

            getMediumFont().drawText(ms, txt, ix + icon + gap, y + h / 2 - font / 2, font, textColor);

            ms.pop();
            yOff += (h + scaled(3)) * v;
        }
    }

    private void checkDura() {
        if (mc.player == null) return;

        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.isEmpty() || !stack.isDamageable()) continue;

            int left = stack.getMaxDamage() - stack.getDamage();
            if (left > 0 && left < 100) {
                addNotif("Низкая прочность: " + stack.getName().getString());
                break;
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack) {}

    private void updateFixedPosition() {
        if (mc.getWindow() != null) {
            fixedX = mc.getWindow().getScaledWidth() / 2f;
            fixedY = mc.getWindow().getScaledHeight() / 2f;
        }
    }

    private void resetAnimations() {
        chatOpenAnimation.reset();
        scaleAnimation.reset();
        slideAnimation.reset();
    }

    private void resetSettingsAnimation() {
        settingsAnim = 0;
    }

    private static class Notif {
        String text;
        long start = System.currentTimeMillis();
        long dur = 4000;
        boolean expired;

        Notif(String t) {
            text = t;
        }

        float getAlpha() {
            long e = System.currentTimeMillis() - start;
            if (e < 300) return e / 300f;
            if (e < dur - 300) return 1f;
            if (e < dur) return 1f - (e - (dur - 300)) / 300f;
            return 0f;
        }

        boolean shouldRemove() {
            if (!expired && System.currentTimeMillis() - start > dur) {
                expired = true;
            }
            return expired && getAlpha() <= 0.05f;
        }
    }
}