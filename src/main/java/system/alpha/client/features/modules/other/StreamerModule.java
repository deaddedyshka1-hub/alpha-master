package system.alpha.client.features.modules.other;

import lombok.Getter;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.MultiBooleanSetting;
import system.alpha.api.system.configs.FriendManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ModuleRegister(name = "Streamer", category = Category.OTHER, description = "Помогает скрыть никнеймы.")
public class StreamerModule extends Module {
    @Getter private static final StreamerModule instance = new StreamerModule();

    @Getter private MultiBooleanSetting hide = new MultiBooleanSetting("Скрывать").value(
            new BooleanSetting("Никнейм").value(true),
            new BooleanSetting("Друзей").value(true).setVisible(() -> getHide().isEnabled("Никнейм")),
            new BooleanSetting("Фантайм").value(false)
    );

    public StreamerModule() {
        addSettings(hide);
    }

    private final ConcurrentHashMap<String, Integer> friendCounter = new ConcurrentHashMap<>();
    private final AtomicInteger globalCounter = new AtomicInteger(1);

    public String getProtectedName() {
        return this.isEnabled() ? "Шикарная" : mc.getSession().getUsername();
    }

    public String getProtectedFriendName(String name) {
        return this.isEnabled() && hide.isEnabled("Никнейм") && hide.isEnabled("Друзей") && FriendManager.getInstance().contains(name) ? generateProtectedFriendName(name) : name;
    }

    public String generateProtectedFriendName(String originalName) {
        int id = friendCounter.computeIfAbsent(originalName.toLowerCase(), key -> globalCounter.getAndIncrement());
        return "Подружка " + id;
    }

    @Override
    public void onEvent() {

    }
}
