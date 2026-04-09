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
import system.alpha.api.utils.math.MathUtil;

@ModuleRegister(name = "Zoom", category = Category.PLAYER, description = "Позволяет приближать, как было в 1.16.5")
public class ZoomModule extends Module {
    @Getter
    private static final ZoomModule instance = new ZoomModule();

    private final SliderSetting zoomStrength = new SliderSetting("Сила зума")
            .value(4.0f)
            .range(1.0f, 15.0f)
            .step(0.1f);

    private float originalFov = -1.0f;
    private boolean isZoomKeyPressed = false;
    private long lastScrollTime = 0;
    private static final int ZOOM_KEY = 67;

    private float currentZoomStrength;

    public ZoomModule() {
        addSettings(zoomStrength);
        currentZoomStrength = zoomStrength.getValue();
    }

    @Override
    public void onEvent() {
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!isEnabled()) return;

            if (event.key() == ZOOM_KEY) {
                if (event.action() == 1) {
                    if (!isZoomKeyPressed && !isAnyGuiOpen()) {
                        isZoomKeyPressed = true;
                    }
                } else if (event.action() == 0) {
                    if (isZoomKeyPressed) {
                        isZoomKeyPressed = false;
                    }
                }
            }

            if ((event.key() == 3 || event.key() == 4) && event.action() == 1) {
                handleMouseScroll(event.key());
            }
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (isAnyGuiOpen() && isZoomKeyPressed) {
                isZoomKeyPressed = false;
            }

            currentZoomStrength = zoomStrength.getValue();
        }));

        addEvents(keyEvent, tickEvent);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        originalFov = -1.0f;
        isZoomKeyPressed = false;
        currentZoomStrength = zoomStrength.getValue();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        originalFov = -1.0f;
        isZoomKeyPressed = false;
    }

    private boolean isAnyGuiOpen() {
        if (mc.currentScreen == null) {
            return false;
        }
        return true;
    }

    private void handleMouseScroll(int button) {
        if (isAnyGuiOpen() || !isZoomKeyPressed) {
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
        if (!isEnabled() || isAnyGuiOpen()) {
            if (originalFov != -1.0f) {
                originalFov = -1.0f;
            }
            return baseFov;
        }

        if (!isZoomKeyPressed) {
            if (originalFov != -1.0f) {
                originalFov = -1.0f;
            }
            return baseFov;
        }

        if (originalFov < 0.0f) {
            originalFov = baseFov;
        }

        return originalFov / Math.max(1.0f, currentZoomStrength);
    }
}