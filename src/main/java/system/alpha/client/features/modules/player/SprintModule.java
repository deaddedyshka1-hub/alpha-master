package system.alpha.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
        client.options.sprintKey.setPressed(true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.sprintKey.setPressed(true);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.sprintKey.setPressed(false);
        }
    }

}