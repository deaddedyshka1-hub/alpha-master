package system.alpha.client.ui.theme.basic;

import java.awt.*;

public class OceanDepthsTheme extends ADefaultTheme {
    public OceanDepthsTheme() {
        super("Глубины океана");
    }

    @Override
    public Color setPrimary() {
        return new Color(64, 156, 255, 255); // Яркий аквамарин
    }

    @Override
    public Color setSecondary() {
        return new Color(0, 180, 216, 255); // Бирюзовый
    }

    @Override
    public Color setBlur() {
        return new Color(12, 53, 106, 255); // Темно-синяя глубина
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(30, 80, 140, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(20, 65, 120, 255);
    }

    @Override
    public Color setText() {
        return new Color(230, 240, 255, 255); // Светлый голубоватый
    }

    @Override
    public Color setInactiveText() {
        return new Color(170, 190, 210, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(0, 212, 255, 255); // Неоново-голубой
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(80, 120, 160, 255);
    }
}