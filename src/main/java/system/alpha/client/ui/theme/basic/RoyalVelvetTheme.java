package system.alpha.client.ui.theme.basic;

import java.awt.*;

public class RoyalVelvetTheme extends ADefaultTheme {
    public RoyalVelvetTheme() {
        super("Королевский бархат");
    }

    @Override
    public Color setPrimary() {
        return new Color(198, 159, 255, 255); // Лавандовый
    }

    @Override
    public Color setSecondary() {
        return new Color(255, 159, 243, 255); // Розово-лиловый
    }

    @Override
    public Color setBlur() {
        return new Color(50, 30, 70, 255); // Темный фиолетовый
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(70, 45, 95, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(60, 38, 82, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 240, 255, 255); // Светлый лавандовый
    }

    @Override
    public Color setInactiveText() {
        return new Color(200, 180, 210, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(230, 180, 255, 255); // Светящийся фиолетовый
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(100, 70, 120, 255);
    }
}