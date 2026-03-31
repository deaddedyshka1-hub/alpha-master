package system.alpha.client.ui.theme.basic;

import java.awt.*;

public class ArcticAuroraTheme extends ADefaultTheme {
    public ArcticAuroraTheme() {
        super("Полярное сияние");
    }

    @Override
    public Color setPrimary() {
        return new Color(0, 255, 255, 255); // Яркий циан
    }

    @Override
    public Color setSecondary() {
        return new Color(180, 70, 255, 255); // Фиолетовый
    }

    @Override
    public Color setBlur() {
        return new Color(10, 30, 70, 255); // Глубокий ночной
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(30, 50, 100, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(20, 40, 85, 255);
    }

    @Override
    public Color setText() {
        return new Color(230, 250, 255, 255); // Ледяной белый
    }

    @Override
    public Color setInactiveText() {
        return new Color(170, 200, 220, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(120, 255, 255, 255); // Светящийся лед
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(60, 120, 180, 255);
    }
}
