package system.alpha;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import system.alpha.api.command.CommandManager;
import system.alpha.api.module.ModuleManager;
import system.alpha.api.system.DiscordHook;
import system.alpha.api.system.configs.*;
import system.alpha.api.system.draggable.DraggableManager;
import system.alpha.api.system.files.FileManager;
import system.alpha.api.utils.other.SoundUtil;
import system.alpha.api.utils.render.KawaseBlurProgram;
import system.alpha.api.utils.render.fonts.Fonts;
import system.alpha.api.utils.rotation.manager.RotationManager;
import system.alpha.client.services.HeartbeatService;
import system.alpha.client.services.RenderService;
import system.alpha.client.ui.theme.ThemeEditor;
import system.alpha.client.ui.widget.Widget;
import system.alpha.client.ui.widget.WidgetManager;
import system.alpha.client.ui.widget.overlay.WatermarkWidget;
import system.alpha.client.ui.widget.overlay.notify.NotificationWidget;

public class AlphaLoader implements ClientModInitializer {
	@Getter private static AlphaLoader instance = new AlphaLoader();

    @Override
	public void onInitializeClient() {
        instance = this;

        SoundUtil.load();

        loadManagers();
        loadServices();
        loadFiles();
    }

    public void postLoad() {
        ModuleManager.getInstance().getModules().sort((a, b) -> Float.compare(
                Fonts.PS_MEDIUM.getWidth(b.getName(), 7f),
                Fonts.PS_MEDIUM.getWidth(a.getName(), 7f)
        ));

        KawaseBlurProgram.load();

        loadWidgetsConfiguration();

    }

    private void loadWidgetsConfiguration() {
        // Получите экземпляр WidgetManager и загрузите конфигурацию для каждого виджета
        WidgetManager widgetManager = WidgetManager.getInstance();

        for (Widget widget : widgetManager.getWidgets()) {
            if (widget instanceof WatermarkWidget) {
                ((WatermarkWidget) widget).loadConfig();
            } else if (widget instanceof NotificationWidget) {
                ((NotificationWidget) widget).loadConfig();
            }
        }
    }

    private void loadFiles() {
        ConfigManager.getInstance().load("autoConfig");
        DraggableManager.getInstance().load();
        FriendManager.getInstance().load();
        MacroManager.getInstance().load();
        WidgetConfigManager.getInstance().load();
    }

    private void loadManagers() {
        WidgetManager.getInstance().load();
        RotationManager.getInstance().load();

        ModuleManager.getInstance().load();
        CommandManager.getInstance().load();

        ThemeEditor.getInstance().load();
    }

    private void loadServices() {
        HeartbeatService.getInstance().load();
        RenderService.getInstance().load();
        ConfigSkin.getInstance().load();

        DiscordHook.startRPC();
    }

    public void onClose() {
        ConfigManager.getInstance().save("autoConfig");
        FileManager.getInstance().save();
        WidgetConfigManager.getInstance().save();
        ThemeEditor.getInstance().save(true);
        DraggableManager.getInstance().save();
        MacroManager.getInstance().save();

        DiscordHook.stopRPC();
    }
}