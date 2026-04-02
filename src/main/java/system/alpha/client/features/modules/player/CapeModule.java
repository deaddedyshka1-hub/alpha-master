package system.alpha.client.features.modules.player;

import lombok.Getter;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;

@ModuleRegister(name = "Cape", category = Category.PLAYER, description = "Сексуальный, альфовский плащ")
public class CapeModule extends Module {

    @Getter
    private static final CapeModule instance = new CapeModule();

    @Override
    public void onEvent() {

    }
}