package system.alpha.client.ui.theme.basic;

import java.awt.*;


public class SolarFlareTheme extends ADefaultTheme {
    public SolarFlareTheme() {
        super("Солнечная вспышка");
    }

    @Override
    public Color setPrimary() {
        return new Color(255, 109, 0, 255); // Раскаленный оранжевый
    }

    @Override
    public Color setSecondary() {
        return new Color(255, 185, 0, 255); // Золотой
    }

    @Override
    public Color setBlur() {
        return new Color(80, 30, 15, 255); // Темный вулканический
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(120, 50, 25, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(100, 40, 20, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 235, 200, 255); // Теплый светлый
    }

    @Override
    public Color setInactiveText() {
        return new Color(210, 180, 150, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(255, 140, 0, 255); // Яркий янтарь
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(150, 90, 40, 255);
    }
}