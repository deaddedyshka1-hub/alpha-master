package system.alpha.client.features.modules.render;

import lombok.Getter;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.MultiBooleanSetting;

import java.util.Arrays;

@ModuleRegister(name = "Removals", category = Category.RENDER, description = "Удаляет не нужные вещи из майнкрафта.")
public class RemovalsModule extends Module {
    @Getter private static final RemovalsModule instance = new RemovalsModule();

    private final String[] elements = {
            "Экран огня", "Получение урона", "Экран воды",
            "Скорбоард", "Глоу эффект", "Плохие эффекты", "Боссбар", "Броню"
    };

    private final MultiBooleanSetting remove = new MultiBooleanSetting("Убрать").value(
            Arrays.stream(elements)
                    .map(name -> new BooleanSetting(name).value(false))
                    .toArray(BooleanSetting[]::new)
    );

    public RemovalsModule() {
        addSettings(remove);
    }

    public boolean isFireOverlay()   { return isEnabled() && remove.isEnabled("Экран огня"); }
    public boolean isHurtCamera()    { return isEnabled() && remove.isEnabled("Hurt camera"); }
    public boolean isWaterOverlay()  { return isEnabled() && remove.isEnabled("Экран воды"); }
    public boolean isScoreboard()    { return isEnabled() && remove.isEnabled("Скорбоард"); }
    public boolean isGlowEffect()    { return isEnabled() && remove.isEnabled("Глоу эффект"); }
    public boolean isBadEffects()    { return isEnabled() && remove.isEnabled("Плохие эффекты"); }
    public boolean isBossBar()       { return isEnabled() && remove.isEnabled("Боссбар"); }
    public boolean isArmor()       { return isEnabled() && remove.isEnabled("Броню"); }

    @Override
    public void onEvent() {

    }
}