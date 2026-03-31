package system.alpha.client.features.modules.other;

import lombok.Getter;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.client.TickEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.MultiBooleanSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.utils.math.TimerUtil;

import java.util.function.Supplier;

@ModuleRegister(name = "Tape Mouse", category = Category.OTHER, description = "Автоматически использует клавиши мыши.")
public class TapeMouseModule extends Module {
    @Getter private static final TapeMouseModule instance = new TapeMouseModule();

    private final MultiBooleanSetting actions = new MultiBooleanSetting("Действие").value(
            new BooleanSetting("Бить").value(true),
            new BooleanSetting("Юзать").value(false)
    );
    private final Supplier<Boolean> isAttack = () -> actions.isEnabled("Бить");
    private final Supplier<Boolean> isUse = () -> actions.isEnabled("Юзать");

    private final SliderSetting attackDelay = new SliderSetting("Задержка").value(10f).range(1f, 20f).step(1f).setVisible(isAttack);
    private final SliderSetting useDelay = new SliderSetting("Задержка").value(10f).range(1f, 20f).step(1f).setVisible(isUse);

    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil useTimer = new TimerUtil();

    public TapeMouseModule() {
        addSettings(actions, attackDelay, useDelay);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (isAttack.get()) handleAction(attackDelay.getValue(), attackTimer, () -> mc.doAttack());
            if (isUse.get()) handleAction(useDelay.getValue(), useTimer, () -> mc.doItemUse());
        }));

        addEvents(tickEvent);
    }

    private void handleAction(float delay, TimerUtil timer, Runnable run) {
        if (timer.finished(delay * 50)) {
            run.run();
            timer.reset();
        }
    }
}
