package system.alpha.client.ui.theme.basic;

import system.alpha.client.ui.theme.Theme;

import java.awt.*;

public abstract class ADefaultTheme extends Theme {
    public ADefaultTheme(String name) {
        super(name);
    }

    public abstract Color setPrimary();
    public abstract Color setSecondary();
    public abstract Color setBlur();
    public abstract Color setWidgetBlur();
    public abstract Color setBackgroundBlur();
    public abstract Color setText();
    public abstract Color setInactiveText();
    public abstract Color setKnob();
    public abstract Color setInactiveKnob();

    public ADefaultTheme update() {
        for (ElementColor elementColor : getElementColors()) {
            switch (elementColor.getName()) {
                case "Первичный" -> elementColor.setColor(setPrimary());
                case "Вторичный" -> elementColor.setColor(setSecondary());
                case "Блюр" -> elementColor.setColor(setBlur());
                case "Виджет блюр" -> elementColor.setColor(setWidgetBlur());
                case "Бекраудн блюр" -> elementColor.setColor(setBackgroundBlur());
                case "Текст" -> elementColor.setColor(setText());
                case "Неактивный текст" -> elementColor.setColor(setInactiveText());
                case "Кнопка" -> elementColor.setColor(setKnob());
                case "Неактивная кнопка" -> elementColor.setColor(setInactiveKnob());
            }
        }
        return this;
    }
}
