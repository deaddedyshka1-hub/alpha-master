package system.alpha.client.features.modules.other;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;

@ModuleRegister(
        name = "ShulkerView",
        category = Category.OTHER,
        description = "Показывает содержимое шалкера при наведении"
)
public class ShulkerViewModule extends Module {

    @Getter
    private static final ShulkerViewModule instance = new ShulkerViewModule();

    private final BooleanSetting showContents = new BooleanSetting("Показывать содержимое").value(true);
    private final BooleanSetting showBackground = new BooleanSetting("Показывать фон").value(true);

    @Override
    public void onEvent() {
    }

    public ShulkerViewModule() {
        addSettings(showContents, showBackground);
    }

    public boolean shouldShowContents() {
        return isEnabled() && showContents.getValue();
    }

    public boolean shouldShowBackground() {
        return showBackground.getValue();
    }


    public static boolean isShulkerBox(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        return block instanceof ShulkerBoxBlock || block == Blocks.SHULKER_BOX;
    }

    public static ContainerComponent getContainer(ItemStack stack) {
        if (!isShulkerBox(stack)) return null;
        return stack.get(DataComponentTypes.CONTAINER);
    }

    public static int getItemCount(ItemStack shulkerStack) {
        ContainerComponent container = getContainer(shulkerStack);
        if (container == null) return 0;
        return (int) container.stream().filter(s -> !s.isEmpty()).count();
    }
}