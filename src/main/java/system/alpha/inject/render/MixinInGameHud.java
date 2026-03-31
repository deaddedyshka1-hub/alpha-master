package system.alpha.inject.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.api.event.events.render.Render2DEvent;
import system.alpha.api.utils.render.KawaseBlurProgram;
import system.alpha.client.features.modules.render.CrosshairModule;
import system.alpha.client.features.modules.render.InterfaceModule;
import system.alpha.client.features.modules.render.RemovalsModule;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Inject(method = "render", at = @At("HEAD"))
    public void renderHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        KawaseBlurProgram.render(context.getMatrices());

        Render2DEvent.getInstance().call(new Render2DEvent.Render2DEventData(context, context.getMatrices(), tickCounter.getTickDelta(false)));
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Potions")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    public void renderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrosshairModule module = CrosshairModule.getInstance();
        if (module.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void disableVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void disableVanillaStatusBars(DrawContext context, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void disableVanillaExpBar(DrawContext context, int x, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMountHealth", at = @At("HEAD"), cancellable = true)
    private void disableVanillaMountHealth(DrawContext context, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
    private void disableVanillaHealthBar(DrawContext context, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void disableVanillaFoodBar(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void disableVanillaExpLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void disableVanillaItemTooltip(DrawContext context, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaSidebar(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isScoreboard()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void disableSpectatorHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Hotbar")) {
            ci.cancel();
        }
    }
}