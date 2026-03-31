package system.alpha.client.features.modules.render;

import lombok.Getter;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.module.setting.MultiBooleanSetting;
import system.alpha.api.module.setting.RunSetting;
import system.alpha.api.utils.render.KawaseBlurProgram;
import system.alpha.client.services.RenderService;
import system.alpha.client.ui.theme.ThemeEditor;
import system.alpha.client.ui.widget.WidgetManager;

@ModuleRegister(name = "Interface", category = Category.RENDER, description = "Виджеты, а так же темы.")
public class InterfaceModule extends Module {
    @Getter private static final InterfaceModule instance = new InterfaceModule();

    public final MultiBooleanSetting widgets = new MultiBooleanSetting("Виджеты");
    private final RunSetting themes = new RunSetting("Редактор тем").value(() -> {
        ThemeEditor.getInstance().setOpen(!ThemeEditor.getInstance().isOpen());
    });
    public final SliderSetting scale = new SliderSetting("Масштаб").value(0.9f).range(0.6f, 1.5f).step(0.05f).onAction(() -> RenderService.getInstance().updateScale());
    public final SliderSetting glassy = new SliderSetting("Жидкость").value(0.4f).range(0.0f, 1f).step(0.1f);
    public final SliderSetting passes = new SliderSetting("Переход").value(3f).range(1f, 5f).step(1f).onAction(KawaseBlurProgram::recreate);
    public final SliderSetting offset = new SliderSetting("Сдвиг").value(12f).range(5f, 25f).step(1f);

    public static float getScale() { return getInstance().scale.getValue(); }
    public static float getGlassy() { return 1f - getInstance().glassy.getValue(); }
    public static int getPasses() { return getInstance().passes.getValue().intValue(); }
    public static float getOffset() { return getInstance().offset.getValue(); }

    public void init() {
        widgets.value(WidgetManager.getInstance().getWidgets().stream()
                .map(widget -> {
                    BooleanSetting setting = new BooleanSetting(widget.getName()).value(widget.isEnabled());
                    setting.onAction(() -> widget.setEnabled(setting.getValue()));
                    return setting;
                })
                .toList());

        addSettings(widgets, themes,
                scale, glassy, passes, offset);
    }

    @Override
    public void onEvent() {

    }
}
