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
            .value(3.0f)
            .range(1.0f, 15.0f)
            .step(0.1f);

    private final BooleanSetting smoothZoom = new BooleanSetting("Плавный зум")
            .value(true);

    private final SliderSetting smoothSpeed = new SliderSetting("Скорость плавности")
            .value(0.2f)
            .range(0.05f, 1.0f)
            .step(0.05f)
            .setVisible(smoothZoom::getValue);

    private final AnimationUtil zoomAnimation = new AnimationUtil();
    private final AnimationUtil fovAnimation = new AnimationUtil();
    private final AnimationUtil zoomStrengthAnimation = new AnimationUtil();

    // Состояние зума
    private float originalFov = -1.0f;
    private boolean isZoomKeyPressed = false;
    private boolean isGuiOpen = false;
    private long lastScrollTime = 0;
    private static final int ZOOM_KEY = 67; // Клавиша C

    // Текущие значения
    private float currentFov = -1.0f;
    private float currentZoomStrength = 3.0f;
    private boolean wasZooming = false;

    public ZoomModule() {
        addSettings(zoomStrength, smoothZoom, smoothSpeed);
        currentZoomStrength = zoomStrength.getValue();
        zoomStrengthAnimation.setValue(zoomStrength.getValue());
    }

    @Override
    public void onEvent() {
        // Обработка событий клавиатуры
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!isEnabled()) return;

            // Обработка клавиши C
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

            // Колесико мыши (кнопки 3 и 4)
            if ((event.key() == 3 || event.key() == 4) && event.action() == 1) {
                handleMouseScroll(event.key());
            }
        }));

        // Обновление состояния
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            // Обновляем анимации
            zoomAnimation.update();
            fovAnimation.update();
            zoomStrengthAnimation.update();

            // Проверяем, открыт ли GUI
            isGuiOpen = isAnyGuiOpen();

            // Если открыт GUI и зум активен, отключаем его
            if (isGuiOpen && (isZoomKeyPressed || zoomAnimation.getValue() > 0.001)) {
                isZoomKeyPressed = false;
                startZoomAnimation(false);
            }

            // Синхронизируем силу зума с настройкой
            if (Math.abs(currentZoomStrength - zoomStrength.getValue()) > 0.01f) {
                zoomStrengthAnimation.run(zoomStrength.getValue(), 200L, Easing.CUBIC_BOTH);
            }

            // Получаем актуальное значение силы зума из анимации
            currentZoomStrength = (float) zoomStrengthAnimation.getValue();
        }));

        addEvents(keyEvent, tickEvent);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        originalFov = -1.0f;
        currentFov = -1.0f;
        currentZoomStrength = zoomStrength.getValue();
        isZoomKeyPressed = false;
        isGuiOpen = false;
        wasZooming = false;

        // Сбрасываем анимации
        zoomAnimation.setValue(0.0);
        fovAnimation.setValue(0.0);
        zoomStrengthAnimation.setValue(zoomStrength.getValue());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        originalFov = -1.0f;
        currentFov = -1.0f;
        isZoomKeyPressed = false;
        isGuiOpen = false;
        wasZooming = false;
    }

    private boolean isAnyGuiOpen() {
        if (mc.currentScreen == null) {
            return false;
        }

        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen) {
            return false;
        }

        return true;
    }

    private boolean shouldDisableZoomForCurrentScreen() {
        if (mc.currentScreen == null) {
            return false;
        }

        if (mc.currentScreen instanceof ChatScreen ||
                mc.currentScreen instanceof InventoryScreen ||
                mc.currentScreen instanceof GenericContainerScreen ||
                mc.currentScreen instanceof ShulkerBoxScreen ||
                mc.currentScreen instanceof HopperScreen ||
                mc.currentScreen instanceof FurnaceScreen ||
                mc.currentScreen instanceof BlastFurnaceScreen ||
                mc.currentScreen instanceof SmokerScreen ||
                mc.currentScreen instanceof BrewingStandScreen ||
                mc.currentScreen instanceof BeaconScreen ||
                mc.currentScreen instanceof AnvilScreen ||
                mc.currentScreen instanceof EnchantmentScreen ||
                mc.currentScreen instanceof GrindstoneScreen ||
                mc.currentScreen instanceof LoomScreen ||
                mc.currentScreen instanceof CartographyTableScreen ||
                mc.currentScreen instanceof StonecutterScreen ||
                mc.currentScreen instanceof SmithingScreen ||
                mc.currentScreen instanceof MerchantScreen ||
                mc.currentScreen instanceof CraftingScreen ||
                mc.currentScreen instanceof HorseScreen ||
                mc.currentScreen instanceof CreativeInventoryScreen) {
            return true;
        }

        return false;
    }

    private void startZoomAnimation(boolean zoomIn) {
        if (!smoothZoom.getValue() || !isEnabled()) {
            return;
        }

        if (zoomIn && isGuiOpen) {
            return;
        }

        long animationTime = smoothZoom.getValue() ? 300L : 100L;

        if (zoomIn) {
            zoomAnimation.run(1.0, animationTime, Easing.BACK_OUT);
        } else {
            zoomAnimation.run(0.0, animationTime, Easing.CUBIC_OUT);
        }
    }

    private void handleMouseScroll(int button) {
        if (isGuiOpen) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScrollTime < 50) return;
        lastScrollTime = currentTime;

        if (button == 3) {
            zoomStrength.setValue(Math.max(1.0f, zoomStrength.getValue() - 1f));
        } else if (button == 4) {
            zoomStrength.setValue(Math.min(15.0f, zoomStrength.getValue() + 1f));
        }
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
            currentFov = baseFov;
        }

        float targetFov = originalFov / currentZoomStrength;

        if (!smoothZoom.getValue()) {
            if (isZoomKeyPressed) {
                return targetFov;
            } else {
                return originalFov;
            }
        }

        double zoomProgress = zoomAnimation.getValue();

        if (zoomProgress <= 0.001) {
            currentFov = originalFov;
        } else if (zoomProgress >= 0.999) {
            currentFov = targetFov;
        } else {
            float interpolationSpeed = smoothSpeed.getValue() * 10f;
            float lerpAmount = Math.min(1.0f, tickDelta * interpolationSpeed);
            float targetProgress = isZoomKeyPressed ? 1.0f : 0.0f;
            zoomProgress = MathUtil.lerp((float) zoomProgress, targetProgress, lerpAmount);

            currentFov = MathUtil.lerp(originalFov, targetFov, (float) zoomProgress);
        }

        return currentFov;
    }
}