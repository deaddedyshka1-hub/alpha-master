package system.alpha.inject.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import system.alpha.api.event.events.player.world.ClickSlotEvent;
import system.alpha.api.event.events.world.BlockBreakEvent;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void onClickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null) return;
        ClickSlotEvent.ClickSlotEventData event = new ClickSlotEvent.ClickSlotEventData(actionType, slotId, button, syncId);
        if (ClickSlotEvent.getInstance().call(event)) {
            ci.cancel();
        }
    }
    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    public void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {

        if (BlockBreakEvent.getInstance().call()) {

            cir.setReturnValue(false);
            cir.cancel();

        }
    }
}
