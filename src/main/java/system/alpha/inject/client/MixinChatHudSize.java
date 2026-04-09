package system.alpha.inject.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import system.alpha.client.features.modules.other.ChatUtilsModule;

@Mixin(ChatHud.class)
public class MixinChatHudSize {

    private GameOptions options;

    @Inject(method = "getWidth", at = @At("RETURN"), cancellable = true)
    private void getWidth(CallbackInfoReturnable<Integer> cir) {
        ChatUtilsModule module = ChatUtilsModule.getInstance();
        if (module != null && module.isEnabled() && module.resizeChat.getValue()) {
            cir.setReturnValue((int) module.getChatWidth());
        }
    }

    @Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
    private void getHeight(CallbackInfoReturnable<Integer> cir) {
        ChatUtilsModule module = ChatUtilsModule.getInstance();
        if (module != null && module.isEnabled() && module.resizeChat.getValue()) {
            cir.setReturnValue((int) module.getChatHeight());
        }
    }
}