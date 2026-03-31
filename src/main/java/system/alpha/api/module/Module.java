package system.alpha.api.module;

import lombok.Getter;
import lombok.Setter;
import system.alpha.api.system.backend.Configurable;
import system.alpha.api.system.interfaces.QuickImports;
import system.alpha.client.features.modules.other.ToggleSoundsModule;
import system.alpha.client.features.modules.render.ClickGUIModule;
import system.alpha.client.ui.widget.WidgetManager;
import system.alpha.client.ui.widget.overlay.notify.NotificationWidget;

@Getter
public abstract class Module extends Configurable implements QuickImports {
    private final String name;
    private final Category category;
    private final String description;
    @Setter private int bind;

    private boolean enabled;

    public Module() {
        ModuleRegister data = getClass().getAnnotation(ModuleRegister.class);

        if (data == null) try {
            throw new Exception("No data for " + getClass().getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.name = data.name();
        this.category = data.category();
        this.bind = data.bind();
        this.description = data.description();
    }

    public boolean hasDescription() {
        return description != null && !description.isEmpty();
    }

    public String getDescription() {
        return hasDescription() ? description : "Для данной функции ещё нет описания";
    }

    public boolean hasBind() { return bind != -999; }

    public void toggle() {
        setEnabled(!enabled, false, false);
    }

    public void toggle(boolean fromBind) {
        setEnabled(!enabled, false, fromBind);
    }

    public void setEnabled(boolean newState) {
        setEnabled(newState, false, false);
    }

    public void setEnabled(boolean newState, boolean config) {
        setEnabled(newState, config, false);
    }

    public void setEnabled(boolean newState, boolean config, boolean fromBind) {
        if (enabled == newState) return;

        enabled = newState;
        if (enabled) {
            onEnable();
            onEvent();
        } else {
            onDisable();
            removeAllEvents();
        }

        if (config || this instanceof ClickGUIModule) return;
        ToggleSoundsModule.playToggle(newState);

        if (fromBind) {
            NotificationWidget widget = (NotificationWidget) WidgetManager.getInstance().getWidgets().stream()
                    .filter(w -> w instanceof NotificationWidget)
                    .findFirst()
                    .orElse(null);

            if (widget != null && widget.isModuleState()) {
                widget.addNotif(name + (newState ? " §aвключен" : " §cвыключен"));
            }
        }
    }

    public abstract void onEvent();

    public void onEnable() {}
    public void onDisable() {}

    public void onUpdate() {}
}
