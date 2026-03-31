package system.alpha.client.features.modules.other;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.screen.slot.SlotActionType;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.world.BlockBreakEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;


@ModuleRegister(name = "CrashPicxake", category = Category.OTHER, description = "Меняет кирку на другой слот, если мало прочности.")
public class CrashNotificationModule extends Module {
    @Getter
    private static final CrashNotificationModule instance = new CrashNotificationModule();

    @Override
    public void onEvent() {

        EventListener blockBreakEvent = BlockBreakEvent.getInstance().subscribe(
                new Listener<>(1, event -> {

                    ItemStack item = mc.player.getMainHandStack();

                    if (!(item.getItem() instanceof PickaxeItem))
                        return;

                    int remaining = item.getMaxDamage() - item.getDamage();

                    if (remaining < 50) {
                        mc.player.getInventory().selectedSlot = 1;
                        mc.interactionManager.clickSlot(
                                mc.player.currentScreenHandler.syncId,
                                mc.player.getInventory().selectedSlot,
                                0,
                                SlotActionType.SWAP,
                                mc.player
                        );
                        print("Ломать нельзя кирка щас сломается черт побери");
                    }
                })
        );
        addEvents(blockBreakEvent);
    }

}