package system.alpha.client.services;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.client.KeyEvent;
import system.alpha.api.event.events.other.ScreenEvent;
import system.alpha.api.event.events.client.TickEvent;
import system.alpha.api.event.events.render.Render2DEvent;
import system.alpha.api.module.ModuleManager;
import system.alpha.api.system.client.GpsManager;
import system.alpha.api.system.configs.ConfigSkin;
import system.alpha.api.system.configs.MacroManager;
import system.alpha.api.system.draggable.DraggableManager;
import system.alpha.api.system.interfaces.QuickImports;
import system.alpha.api.utils.other.SlownessManager;

public class HeartbeatService implements QuickImports {
    @Getter private static final HeartbeatService instance = new HeartbeatService();

    public void load() {
        keyEvent();
        render2dEvent();
        tickEvent();
    }

    private void tickEvent() {
        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            SlownessManager.tick();

            ConfigSkin.getInstance().fetchSkin();
        }));
    }

    private void render2dEvent() {
        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.currentScreen instanceof ChatScreen) {
                DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
                    if (draggable.getModule().isEnabled()) {
                        draggable.onDraw();
                    }
                });
            }

            GpsManager.getInstance().update(event.context());
        }));
    }

    private void keyEvent() {
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != 1 || event.key() == -999 || event.key() == -1) return;

            int action = event.action();
            int key = event.key() + (event.mouse() ? -100 : 0);

            if (mc.currentScreen == null) {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    int bind = module.getBind();
                    if (bind == key && module.hasBind()) {
                        module.toggle(true);
                    }
                });

                MacroManager.getInstance().onKeyPressed(key);
            }
        }));
    }
}

