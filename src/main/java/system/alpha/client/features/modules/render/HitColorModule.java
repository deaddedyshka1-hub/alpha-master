package system.alpha.client.features.modules.render;

import lombok.Getter;
import net.minecraft.block.*;
import net.minecraft.client.render.*;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.utils.color.UIColors;
import java.awt.*;

@ModuleRegister(name = "HitColor", category = Category.RENDER, description = "При ударе создаёт пузырь под углом.")
public class HitColorModule extends Module {

    @Getter
    private static final HitColorModule instance = new HitColorModule();

    public final SliderSetting alpha = new SliderSetting("Прозрачность")
            .value(0.8f)
            .range(0.0f, 1.0f)
            .step(0.01f);

    public HitColorModule() {
        addSettings(alpha);
    }

    @Override
    public void onEvent() {
    }
}
