package system.alpha.client.ui.theme.basic;

import java.awt.*;

public class MidnightForestTheme extends ADefaultTheme {
    public MidnightForestTheme() {
        super("Полночный лес");
    }

    @Override
    public Color setPrimary() {
        return new Color(123, 237, 159, 255); // Светящийся изумруд
    }

    @Override
    public Color setSecondary() {
        return new Color(72, 187, 120, 255); // Зеленый мох
    }

    @Override
    public Color setBlur() {
        return new Color(24, 61, 42, 255); // Глубокий лесной
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(40, 85, 60, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(32, 72, 50, 255);
    }

    @Override
    public Color setText() {
        return new Color(220, 245, 225, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(160, 190, 165, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(166, 255, 203, 255); // Светящийся зеленый
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(60, 100, 75, 255);
    }
}