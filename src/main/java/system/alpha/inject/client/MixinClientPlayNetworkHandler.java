package system.alpha.inject.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.api.command.CommandManager;
import system.alpha.api.utils.other.SoundUtil;
import system.alpha.client.features.modules.other.ToggleSoundsModule;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void sendChatMessage(String content, CallbackInfo ci) {
        CommandManager.getInstance().executeCommands(content, ci);
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