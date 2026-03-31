package system.alpha.client.ui.clickgui.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import system.alpha.api.module.setting.Setting;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.client.ui.UIComponent;

@Getter
@RequiredArgsConstructor
public abstract class SettingComponent extends UIComponent {
    private final Setting<?> setting;
    private final AnimationUtil visibleAnimation = new AnimationUtil();

    public void updateHeight(float value) {
        setHeight(scaled(value));
    }
}
