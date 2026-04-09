package system.alpha.client.features.modules.player;

import lombok.Getter;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.ModeSetting;

@ModuleRegister(name = "Cape", category = Category.PLAYER, description = "Сексуальный, альфовский плащ")
public class CapeModule extends Module {

    @Getter
    private static final CapeModule instance = new CapeModule();

    private final ModeSetting showMode = new ModeSetting("Показывать")
            .value("Всем")
            .values("Всем", "Только себе", "Только друзьям");

    private final BooleanSetting showFriends = new BooleanSetting("Показывать на друзьях")
            .value(true);

    public CapeModule() {
        addSettings(showMode, showFriends);
    }

    public boolean shouldShowCape(String playerName, boolean isSelf) {
        if (!isEnabled()) return false;

        String mode = showMode.getValue();

        if (mode.equals("Только себе")) {
            return isSelf;
        }

        if (mode.equals("Только друзьям")) {
            if (isSelf) return true;
            return system.alpha.api.system.configs.FriendManager.getInstance().contains(playerName);
        }

        if (mode.equals("Всем")) {
            if (!showFriends.getValue()) {
                if (isSelf) return true;
                return system.alpha.api.system.configs.FriendManager.getInstance().contains(playerName);
            }
            return true;
        }

        return true;
    }

    @Override
    public void onEvent() {

    }
}