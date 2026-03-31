package system.alpha.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import system.alpha.client.ui.clickgui.module.settings.ColorComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Theme {
    private final String name;
    private final List<ElementColor> elementColors = new ArrayList<>();

    public Theme(String name) {
        this.name = name;

        elementColors.add(new ElementColor("Первичный", new Color(190, 141, 255)));
        elementColors.add(new ElementColor("Вторичный", new Color(168, 108, 255)));
        elementColors.add(new ElementColor("Блюр", new Color(37, 33, 46)));
        elementColors.add(new ElementColor("Виджет блюр", new Color(23,  23,  32, 255)));
        elementColors.add(new ElementColor("Бекраудн блюр", new Color(35,  29,  47, 255)));
        elementColors.add(new ElementColor("Текст", new Color(231, 217, 255, 255)));
        elementColors.add(new ElementColor("Неактивный текст", new Color(152, 152, 152)));
        elementColors.add(new ElementColor("Кнопка", new Color(181, 151, 252)));
        elementColors.add(new ElementColor("Неактивная кнопка", new Color(255, 255, 255)));
        elementColors.add(new ElementColor("Положительный", new Color(130, 255, 130)));
        elementColors.add(new ElementColor("Средний", new Color(255, 200, 95)));
        elementColors.add(new ElementColor("Отрицательный", new Color(255, 80, 80)));
    }

    public Color getPrimaryColor() { return getElementColor("Первичный"); }
    public Color getSecondaryColor() { return getElementColor("Вторичный"); }
    public Color getBlurColor() { return getElementColor("Блюр"); }
    public Color getWidgetBlurColor() { return getElementColor("Виджет блюр"); }
    public Color getBackgroundBlurColor() { return getElementColor("Бекраудн блюр"); }
    public Color getTextColor() { return getElementColor("Текст"); }
    public Color getInactiveTextColor() { return getElementColor("Неактивный текст"); }
    public Color getKnobColor() { return getElementColor("Кнопка"); }
    public Color getInactiveKnobColor() { return getElementColor("Неактивная кнопка"); }
    public Color getPositiveColor() { return getElementColor("Положительный"); }
    public Color getMiddleColor() { return getElementColor("Средний"); }
    public Color getNegativeColor() { return getElementColor("Отрицательный"); }

    public Color getElementColor(String elementName) {
        for (ElementColor element : elementColors) {
            if (element.getName().equalsIgnoreCase(elementName)) {
                return element.getColor();
            }
        }
        return new Color(-1);
    }

    @Getter
    public static class ElementColor {
        private final String name;
        @Setter private Color color;
        private final ColorComponent colorComponent;

        public ElementColor(String name, Color color) {
            this.name = name;
            this.color = color;
            this.colorComponent = new ColorComponent(this);
        }
    }
}
