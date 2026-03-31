package system.alpha.client.features.modules.render;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;

@ModuleRegister(name = "ItemPhysic", category = Category.RENDER, description = "Изменяет физику падения предметов.")
public class ItemPhysicModule extends Module {
    @Getter
    private static final ItemPhysicModule instance = new ItemPhysicModule();

    public ItemPhysicModule() {

    }
    @Override
    public void onEvent() {

    }
}