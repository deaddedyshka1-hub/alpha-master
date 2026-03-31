package system.alpha.client.features.modules.player;

import lombok.Getter;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.player.move.SprintEvent;
import system.alpha.api.event.events.player.move.UpdateWalkingPlayerEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import net.minecraft.entity.effect.StatusEffects;

@ModuleRegister(name = "Sprint", category = Category.PLAYER, description = "Позволяет бежать, без использования CTRL.")
public class SprintModule extends Module {
    @Getter
    private static final SprintModule instance = new SprintModule();

    private boolean wasSprinting;
    private int sprintTicks;

    @Override
    public void onEvent() {
        EventListener sprintListener = SprintEvent.getInstance().subscribe(new Listener<>(event -> {
            if (shouldSprint()) {
                event.setSprint(true);
            }
        }));

        EventListener updateListener = UpdateWalkingPlayerEvent.getInstance().subscribe(new Listener<>(event -> {
            handleSprint();
        }));

        addEvents(sprintListener, updateListener);
    }

    private void handleSprint() {
        if (mc.player == null) return;

        boolean shouldSprint = shouldSprint();

        if (shouldSprint && !mc.player.isSprinting()) {
            if (canStartSprint()) {
                mc.player.setSprinting(true);
                wasSprinting = true;
                sprintTicks = 0;
            }
        } else if (!shouldSprint && mc.player.isSprinting() && wasSprinting) {
            if (shouldStopSprint()) {
                mc.player.setSprinting(false);
                wasSprinting = false;
            }
        }

        if (mc.player.isSprinting()) {
            sprintTicks++;
        } else {
            sprintTicks = 0;
        }
    }

    private boolean shouldSprint() {
        if (mc.player == null || mc.options == null) return false;

        return mc.player.input.movementForward > 0 &&
                !mc.player.horizontalCollision &&
                !mc.player.isSneaking() &&
                mc.player.getHungerManager().getFoodLevel() > 6 &&
                mc.player.getVelocity().y >= -0.5;
    }

    private boolean canStartSprint() {
        return sprintTicks <= 0 &&
                mc.player.getHungerManager().getFoodLevel() > 6 &&
                !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) &&
                mc.player.getVelocity().y >= -0.3;
    }

    private boolean shouldStopSprint() {
        return mc.player.input.movementForward <= 0 ||
                mc.player.horizontalCollision ||
                mc.player.isSneaking() ||
                mc.player.getHungerManager().getFoodLevel() <= 6 ||
                mc.player.getVelocity().y < -0.5 ||
                sprintTicks > 200;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null && mc.player.isSprinting() && wasSprinting) {
            mc.player.setSprinting(false);
            wasSprinting = false;
            sprintTicks = 0;
        }
    }
}