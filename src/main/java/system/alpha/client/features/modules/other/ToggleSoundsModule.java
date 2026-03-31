package system.alpha.client.features.modules.other;

import lombok.Getter;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.ModeSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.utils.other.SoundUtil;

@ModuleRegister(name = "Sounds", category = Category.OTHER, description = "Меняет звуки.")
public class ToggleSoundsModule extends Module {
    @Getter private static final ToggleSoundsModule instance = new ToggleSoundsModule();

    private final ModeSetting sound = new ModeSetting("Звуки модулей").value("Blop").values("Smooth", "Celestial", "Nursultan", "Akrien", "Tech", "Blop");
    public final SliderSetting volume = new SliderSetting("Громкость").value(60f).range(1f, 100f).step(1f);
    public final ModeSetting totemSound = new ModeSetting("Звук тотема")
            .value("1")
            .values("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15");
    public final SliderSetting totemVolume =
            new SliderSetting("Громкость тотема")
                    .value(80f)
                    .range(1f, 100f)
                    .step(1f);
    public ToggleSoundsModule() {
        addSettings(sound, volume, totemSound, totemVolume);
    }

    public static void playToggle(boolean state) {
        if (!instance.isEnabled()) return;

        SoundUtil.playSound(switch (instance.sound.getValue()) {
            case "Nursultan" -> state ? SoundUtil.ENABLE_NU_EVENT : SoundUtil.DISABLE_NU_EVENT;
            case "Celestial" -> state ? SoundUtil.ENABLE_CEL_EVENT : SoundUtil.DISABLE_CEL_EVENT;
            case "Akrien" -> state ? SoundUtil.ENABLE_AK_EVENT : SoundUtil.DISABLE_AK_EVENT;
            case "Tech" -> state ? SoundUtil.ENABLE_TECH_EVENT : SoundUtil.DISABLE_TECH_EVENT;
            case "Blop" -> state ? SoundUtil.ENABLE_BLOP_EVENT : SoundUtil.DISABLE_BLOP_EVENT;
            default -> state ? SoundUtil.ENABLE_SMOOTH_EVENT : SoundUtil.DISABLE_SMOOTH_EVENT;
        });
    }
    @Override
    public void onEvent() {

    }
}
