package system.alpha.inject.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.client.features.modules.render.InterfaceModule;
import system.alpha.client.features.modules.render.RemovalsModule;

@Mixin(BossBarHud.class)
public class MixinBossBarHud {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        if (InterfaceModule.getInstance().widgets.isEnabled("BossBar")) {
            ci.cancel();
        }

        if (RemovalsModule.getInstance().isBossBar() && InterfaceModule.getInstance().widgets.isEnabled("BossBar")) {
            ci.cancel();
        }
    }
}
