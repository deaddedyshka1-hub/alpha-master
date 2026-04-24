package system.alpha.inject.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.api.command.CommandManager;
import system.alpha.api.event.Flora;
import system.alpha.api.event.events.player.other.PlayerLeaveEvent;
import system.alpha.api.utils.other.SoundUtil;
import system.alpha.client.features.modules.other.ToggleSoundsModule;
import system.alpha.client.ui.widget.WidgetManager;
import system.alpha.client.ui.widget.overlay.notify.NotificationWidget;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void sendChatMessage(String content, CallbackInfo ci) {
        CommandManager.getInstance().executeCommands(content, ci);

        NotificationWidget widget = (NotificationWidget) WidgetManager.getInstance().getWidgets().stream()
                .filter(w -> w instanceof NotificationWidget)
                .findFirst()
                .orElse(null);

        if (widget != null) {
            widget.onSendMessage(content);
        }
    }

    @Inject(method = "onPlayerRemove", at = @At("HEAD"))
    private void onPlayerRemove(PlayerRemoveS2CPacket packet, CallbackInfo ci) {
        ClientPlayNetworkHandler handler = (ClientPlayNetworkHandler) (Object) this;

        packet.profileIds().forEach(uuid -> {
            PlayerListEntry entry = handler.getPlayerListEntry(uuid);
            if (entry != null) {
                String playerName = entry.getProfile().getName();
                NotificationWidget widget = (NotificationWidget) WidgetManager.getInstance().getWidgets().stream()
                        .filter(w -> w instanceof NotificationWidget)
                        .findFirst()
                        .orElse(null);
                if (widget != null) {
                    widget.onPlayerLeave(playerName);
                }
            }
        });
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"), cancellable = true)
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() == 35) {
            if (!ToggleSoundsModule.getInstance().isEnabled()) return;
            float vol = ToggleSoundsModule.getInstance().totemVolume.getValue() / 100f;
            int index = Integer.parseInt(ToggleSoundsModule.getInstance()
                    .totemSound.getValue()) - 1;

            SoundUtil.playSound(SoundUtil.TOTEM_EVENTS[index], vol);

            ci.cancel();
        }
    }
}