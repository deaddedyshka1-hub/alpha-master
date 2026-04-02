package system.alpha.client.features.modules.other;

import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.ModeSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@ModuleRegister(name = "TimeStamps", category = Category.OTHER, description = "Добавляет время к сообщениям в чате")
public class ChatUtilsModule extends Module {

    private static ChatUtilsModule instance;

    private final BooleanSetting enabled = new BooleanSetting("Показывать время").value(true);
    private final ModeSetting format = new ModeSetting("Формат")
            .value("24h")
            .values("24h", "12h");
    private final BooleanSetting showSeconds = new BooleanSetting("Показывать секунды").value(true);
    private final BooleanSetting brackets = new BooleanSetting("Скобки").value(true);

    private DateTimeFormatter formatter;

    public ChatUtilsModule() {
        instance = this;
        addSettings(enabled, format, showSeconds, brackets);
        updateFormatter();
    }

    @Override
    public void onEvent() {
    }

    @Override
    public void onEnable() {
        updateFormatter();
    }

    public void updateFormatter() {
        String pattern;
        if (format.is("24h")) {
            pattern = showSeconds.getValue() ? "HH:mm:ss" : "HH:mm";
        } else {
            pattern = showSeconds.getValue() ? "hh:mm:ss a" : "hh:mm a";
        }
        formatter = DateTimeFormatter.ofPattern(pattern);
    }

    public String getFormattedTime() {
        return brackets.getValue() ? "[" + LocalTime.now().format(formatter) + "] " : LocalTime.now().format(formatter) + " ";
    }

    public boolean shouldShowTime() {
        return isEnabled() && enabled.getValue();
    }

    public static ChatUtilsModule getInstance() {
        return instance;
    }
}