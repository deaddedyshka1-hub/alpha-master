package system.alpha.client.ui.widget.overlay;

import net.minecraft.text.Text;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleManager;
import system.alpha.api.system.backend.KeyStorage;
import system.alpha.client.ui.widget.ContainerWidget;

import java.util.*;

public class KeybindsWidget extends ContainerWidget {
    public KeybindsWidget() {
        super(3f, 120f);
    }

    @Override
    public String getName() {
        return "Keybinds";
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> map = new HashMap<>();
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (m.isEnabled() && m.hasBind()) {
                map.put(m.getName(), new ContainerElement.ColoredString(KeyStorage.getBind(m.getBind())));
            }
        }
        return map;
    }
}