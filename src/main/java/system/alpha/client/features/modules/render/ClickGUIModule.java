package system.alpha.client.features.modules.render;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.client.ui.clickgui.ScreenClickGUI;

@ModuleRegister(name = "Click GUI", category = Category.RENDER, bind = GLFW.GLFW_KEY_GRAVE_ACCENT, description = "Данное меню.")
public class ClickGUIModule extends Module {
    @Getter private static final ClickGUIModule instance = new ClickGUIModule();

    public ClickGUIModule() {

    }

    @Override
    public void onEnable() {
        if (mc.currentScreen != null) return;

        mc.setScreen(ScreenClickGUI.getInstance());
    }

    @Override
    public void onEvent() {
        toggle();
    }
}
