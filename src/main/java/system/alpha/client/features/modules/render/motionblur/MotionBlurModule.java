package system.alpha.client.features.modules.render.motionblur;

import lombok.Getter;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;

@ModuleRegister(name = "Motion Blur", category = Category.RENDER, description = "При движении блюрится.")
public class MotionBlurModule extends Module {
    @Getter private static final MotionBlurModule instance = new MotionBlurModule();
    public final ShaderMotionBlur shader;

    @Getter public final SliderSetting strength = new SliderSetting("Сила").value(-0.8f)
            .range(-2f, 2f).step(0.1f)
            .onAction(() -> setMotionBlurStrength(getStrength().getValue()));
    public final BooleanSetting useRRC = new BooleanSetting("Использовать RRC").value(true);


    public MotionBlurModule() {
        shader = new ShaderMotionBlur(this);
        shader.registerShaderCallbacks();
        addSettings(strength, useRRC);
    }

    @Override
    public void onEvent() {

    }

    private void setMotionBlurStrength(float strength) {
        shader.updateBlurStrength(strength);
    }

    public enum BlurAlgorithm {BACKWARDS, CENTERED}
    public static BlurAlgorithm blurAlgorithm = BlurAlgorithm.CENTERED;
}
