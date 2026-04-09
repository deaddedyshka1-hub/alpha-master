package system.alpha.client.features.modules.player;

import lombok.Getter;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.player.move.SprintEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;

@ModuleRegister(name = "Sprint", category = Category.PLAYER, description = "Автоматический спринт")
public class SprintModule extends Module {
    @Getter
    private static final SprintModule instance = new SprintModule();

    @Override
    public void onEvent() {
        EventListener sprintEvent = SprintEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            boolean canSprint = mc.player.getHungerManager().getFoodLevel() > 6.0F
                    && !mc.player.isSneaking()
                    && mc.player.forwardSpeed > 0;

            if (canSprint) {
                event.setSprint(true);
            }
        }));

        addEvents(sprintEvent);
    }
}