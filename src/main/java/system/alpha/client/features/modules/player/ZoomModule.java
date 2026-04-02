package system.alpha.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.ingame.*;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.client.KeyEvent;
import system.alpha.api.event.events.client.TickEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.math.MathUtil;

@ModuleRegister(name = "Zoom", category = Category.PLAYER, description = "Позволяет приближать, как было в 1.16.5")
public class ZoomModule extends Module {
    @Getter
    private static final ZoomModule instance = new ZoomModule();

    private final SliderSetting zoomStrength = new SliderSetting("Сила зума")
            .value(4.0f)
            .range(1.0f, 15.0f)
            .step(0.1f);

    private final BooleanSetting smoothZoom = new BooleanSetting("Плавный зум")
            .value(true);

    private final SliderSetting smoothSpeed = new SliderSetting("Скорость плавности")
            .value(0.35f)
            .range(0.1f, 0.8f)
            .step(0.05f)
            .setVisible(smoothZoom::getValue);

    private final AnimationUtil zoomAnimation = new AnimationUtil();

    private float originalFov = -1.0f;
    private boolean isZoomKeyPressed = false;
    private boolean isGuiOpen = false;
    private long lastScrollTime = 0;
    private static final int ZOOM_KEY = 67; // Клавиша C

    private float targetZoomStrength;
    private float currentZoomStrength;
    private long lastZoomStrengthChange = 0;

    public ZoomModule() {
        addSettings(zoomStrength, smoothZoom, smoothSpeed);
        targetZoomStrength = zoomStrength.getValue();
        currentZoomStrength = zoomStrength.getValue();
    }

    @Override
    public void onEvent() {
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!isEnabled()) return;

            if (event.key() == ZOOM_KEY) {
                if (event.action() == 1) { // Нажатие
                    if (!isZoomKeyPressed) {
                        isZoomKeyPressed = true;
                        startZoomAnimation(true);
                    }
                } else if (event.action() == 0) { // Отпускание
                    if (isZoomKeyPressed) {
                        isZoomKeyPressed = false;
                        startZoomAnimation(false);
                    }
                }
            }

            if ((event.key() == 3 || event.key() == 4) && event.action() == 1) {
                handleMouseScroll(event.key());
            }
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            zoomAnimation.update();

            isGuiOpen = isAnyGuiOpen();

            if (isGuiOpen && isZoomKeyPressed) {
                isZoomKeyPressed = false;
                startZoomAnimation(false);
            }

            long currentTime = System.currentTimeMillis();
            if (Math.abs(targetZoomStrength - zoomStrength.getValue()) > 0.01f) {
                targetZoomStrength = zoomStrength.getValue();
                lastZoomStrengthChange = currentTime;
            }

            if (currentTime - lastZoomStrengthChange < 500) {
                float step = 0.1f;
                if (Math.abs(currentZoomStrength - targetZoomStrength) > step) {
                    currentZoomStrength = MathUtil.lerp(currentZoomStrength, targetZoomStrength, 0.15f);
                } else {
                    currentZoomStrength = targetZoomStrength;
                }
            } else {
                currentZoomStrength = targetZoomStrength;
            }
        }));

        addEvents(keyEvent, tickEvent);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        originalFov = -1.0f;
        isZoomKeyPressed = false;
        isGuiOpen = false;
        targetZoomStrength = zoomStrength.getValue();
        currentZoomStrength = zoomStrength.getValue();
        zoomAnimation.setValue(0.0);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        originalFov = -1.0f;
        isZoomKeyPressed = false;
        isGuiOpen = false;
        zoomAnimation.setValue(0.0);
    }

    private boolean isAnyGuiOpen() {
        if (mc.currentScreen == null) {
            return false;
        }

        return !(mc.currentScreen instanceof GameMenuScreen) &&
                !(mc.currentScreen instanceof ChatScreen);
    }

    private void startZoomAnimation(boolean zoomIn) {
        if (!smoothZoom.getValue() || !isEnabled()) {
            return;
        }

        if (zoomIn && isGuiOpen) {
            return;
        }

        long animationTime = zoomIn ? 400L : 350L;
        Easing easing = zoomIn ? Easing.CUBIC_OUT : Easing.QUAD_OUT;

        if (zoomIn) {
            zoomAnimation.run(1.0, animationTime, easing);
        } else {
            zoomAnimation.run(0.0, animationTime, easing);
        }
    }

    private void handleMouseScroll(int button) {
        if (isGuiOpen || !isZoomKeyPressed) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScrollTime < 50) return;
        lastScrollTime = currentTime;

        float newValue = zoomStrength.getValue();
        if (button == 3) {
            newValue = Math.max(1.0f, newValue - 0.5f);
        } else if (button == 4) {
            newValue = Math.min(15.0f, newValue + 0.5f);
        }

        zoomStrength.setValue(newValue);
    }

    public float applyZoom(float baseFov, float tickDelta) {
        if (!isEnabled() || isGuiOpen) {
            if (originalFov != -1.0f) {
                originalFov = -1.0f;
            }
            return baseFov;
        }

        if (!isZoomKeyPressed && zoomAnimation.getValue() <= 0.001) {
            if (originalFov != -1.0f) {
                originalFov = -1.0f;
            }
            return baseFov;
        }

        if (originalFov < 0.0f) {
            originalFov = baseFov;
        }

        float targetFov = originalFov / Math.max(1.0f, currentZoomStrength);

        if (!smoothZoom.getValue()) {
            return isZoomKeyPressed ? targetFov : originalFov;
        }

        double zoomProgress = zoomAnimation.getValue();

        if (isZoomKeyPressed && zoomProgress < 0.99) {
            float targetProgress = 1.0f;
            float currentProgress = (float) zoomProgress;
            float newProgress = MathUtil.lerp(currentProgress, targetProgress, 0.15f);
            zoomAnimation.setValue(Math.min(1.0, newProgress));
            zoomProgress = zoomAnimation.getValue();
        } else if (!isZoomKeyPressed && zoomProgress > 0.01) {
            float targetProgress = 0.0f;
            float currentProgress = (float) zoomProgress;
            float newProgress = MathUtil.lerp(currentProgress, targetProgress, 0.2f);
            zoomAnimation.setValue(Math.max(0.0, newProgress));
            zoomProgress = zoomAnimation.getValue();
        }

        float smoothProgress = (float) easeOutCubic(zoomProgress);
        return MathUtil.lerp(originalFov, targetFov, smoothProgress);
    }

    private double easeOutCubic(double x) {
        return 1 - Math.pow(1 - x, 3);
    }
}