package system.alpha.client.features.modules.other;

import lombok.Getter;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.ModeSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.module.setting.ColorSetting;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@ModuleRegister(name = "ChatUtils", category = Category.OTHER, description = "Улучшения чата")
public class ChatUtilsModule extends Module {

    @Getter
    private static final ChatUtilsModule instance = new ChatUtilsModule();

    private final BooleanSetting showTime = new BooleanSetting("Показывать время").value(true);
    private final ModeSetting timeFormat = new ModeSetting("Формат времени")
            .value("24h")
            .values("24h", "12h");
    private final BooleanSetting showSeconds = new BooleanSetting("Показывать секунды").value(true);
    private final BooleanSetting timeBrackets = new BooleanSetting("Скобки времени").value(true);
    private final ColorSetting timeColor = new ColorSetting("Цвет времени").value(new Color(170, 170, 170));

    public final BooleanSetting resizeChat = new BooleanSetting("Изменение размера чата").value(true);
    private final SliderSetting chatWidth = new SliderSetting("Ширина чата")
            .value(320f)
            .range(200f, 600f)
            .step(5f)
            .setVisible(resizeChat::getValue);
    private final SliderSetting chatHeight = new SliderSetting("Высота чата")
            .value(180f)
            .range(100f, 300f)
            .step(5f)
            .setVisible(resizeChat::getValue);

    private DateTimeFormatter formatter;

    public ChatUtilsModule() {
        addSettings(showTime, timeFormat, showSeconds, timeBrackets, timeColor, resizeChat, chatWidth, chatHeight);
        updateFormatter();
    }

    @Override
    public void onEvent() {
    }

    @Override
    public void onEnable() {
        updateFormatter();
        if (resizeChat.getValue() && mc.options != null) {
            applyChatSize();
        }
    }

    public void updateFormatter() {
        String pattern;
        if (timeFormat.is("24h")) {
            pattern = showSeconds.getValue() ? "HH:mm:ss" : "HH:mm";
        } else {
            pattern = showSeconds.getValue() ? "hh:mm:ss a" : "hh:mm a";
        }
        formatter = DateTimeFormatter.ofPattern(pattern);
    }

    public String getFormattedTime() {
        String time = LocalTime.now().format(formatter);
        if (timeBrackets.getValue()) {
            return "[" + time + "] ";
        }
        return time + " ";
    }

    public Color getTimeColor() {
        return timeColor.getValue();
    }

    public boolean shouldShowTime() {
        return isEnabled() && showTime.getValue();
    }

    public void applyChatSize() {
        if (mc.options != null && resizeChat.getValue()) {
            mc.options.getChatWidth().setValue(chatWidth.getValue().doubleValue());
            mc.options.getChatHeightFocused().setValue(chatHeight.getValue().doubleValue());
            mc.options.getChatHeightUnfocused().setValue(chatHeight.getValue().doubleValue());
            mc.options.write();
        }
    }

    public float getChatWidth() {
        return chatWidth.getValue();
    }

    public float getChatHeight() {
        return chatHeight.getValue();
    }
}