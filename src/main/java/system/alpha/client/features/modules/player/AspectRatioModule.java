package system.alpha.client.features.modules.player;

import lombok.Getter;
import net.minecraft.block.*;
import net.minecraft.client.render.*;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.utils.color.UIColors;
import java.awt.*;

@ModuleRegister(name = "AspectRatio", category = Category.PLAYER, description = "Расширяет ваш экран.")
public class AspectRatioModule extends Module {

    @Getter
    private static final AspectRatioModule instance = new AspectRatioModule();

    public final SliderSetting ratio = new SliderSetting("Растяг")
            .value(1.777f)
            .range(0.5f, 3.0f)
            .step(0.01f);

    public AspectRatioModule() {
        addSettings(ratio);
    }

    @Override
    public void onEvent() {
    }
}

