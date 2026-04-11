package system.alpha.inject.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import system.alpha.client.features.modules.other.ShulkerViewModule;

import static system.alpha.api.system.interfaces.QuickImports.mc;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen {

    @Shadow
    protected ScreenHandler handler;

    @Shadow
    protected abstract boolean isPointOverSlot(Slot slot, double pointX, double pointY);

    @Unique
    private static final Identifier SHULKER_BACKGROUND = Identifier.ofVanilla("textures/gui/container/shulker_box.png");

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void cancelTooltipForShulker(DrawContext context, int x, int y, CallbackInfo ci) {
        ShulkerViewModule module = ShulkerViewModule.getInstance();
        if (!module.shouldShowContents()) return;

        Slot slot = getSlotAt(x, y);
        if (slot == null || !slot.hasStack()) return;

        ItemStack stack = slot.getStack();
        if (ShulkerViewModule.isShulkerBox(stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderShulkerContents(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ShulkerViewModule module = ShulkerViewModule.getInstance();
        if (!module.shouldShowContents()) return;

        Slot slot = getSlotAt(mouseX, mouseY);
        if (slot == null || !slot.hasStack()) return;

        ItemStack stack = slot.getStack();
        if (!ShulkerViewModule.isShulkerBox(stack)) return;

        ContainerComponent container = ShulkerViewModule.getContainer(stack);
        if (container == null) return;

        renderShulkerInventory(context, mouseX, mouseY, container, stack, module);
    }

    @Unique
    private void renderShulkerInventory(DrawContext context, int mouseX, int mouseY, ContainerComponent container, ItemStack shulkerStack, ShulkerViewModule module) {
        float scale = 1f;
        int rows = 3;
        int cols = 9;
        int slotSize = 18;

        int windowWidth = cols * slotSize + 16;
        int windowHeight = rows * slotSize + 27;

        int scaledWidth = (int) (windowWidth * scale);
        int scaledHeight = (int) (windowHeight * scale);

        int x = mouseX + 12;
        int y = mouseY - scaledHeight / 2;

        if (x + scaledWidth > mc.getWindow().getScaledWidth()) {
            x = mouseX - scaledWidth - 12;
        }
        if (x < 0) x = 4;
        if (y < 0) y = 4;
        if (y + scaledHeight > mc.getWindow().getScaledHeight()) {
            y = mc.getWindow().getScaledHeight() - scaledHeight - 4;
        }

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 400f);
        context.getMatrices().scale(scale, scale, 1f);

        if (module.shouldShowBackground()) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, SHULKER_BACKGROUND);
            context.drawTexture(RenderLayer::getGuiTextured, SHULKER_BACKGROUND, 0, 0, 0, 0, windowWidth, windowHeight, 256, 256);
        }

        int startX = 8;
        int startY = 18;

        java.util.List<ItemStack> items = container.stream().toList();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                if (index >= items.size()) break;

                ItemStack item = items.get(index);
                int slotX = startX + col * slotSize;
                int slotY = startY + row * slotSize;

                if (!item.isEmpty()) {
                    context.drawItem(item, slotX, slotY);
                    context.drawStackOverlay(mc.textRenderer, item, slotX, slotY);
                }
            }
        }

        String title = shulkerStack.getName().getString();
        int maxTitleWidth = windowWidth - 16;
        String displayTitle = title;
        if (mc.textRenderer.getWidth(title) > maxTitleWidth) {
            displayTitle = mc.textRenderer.trimToWidth(title, maxTitleWidth - 8) + "...";
        }
        int titleWidth = mc.textRenderer.getWidth(displayTitle);
        context.drawTextWithShadow(mc.textRenderer, displayTitle, windowWidth - titleWidth - 38, 6, 0xFFFFFF);

        long filledSlots = container.stream().filter(s -> !s.isEmpty()).count();
        String countText = filledSlots + "/27";
        int countWidth = mc.textRenderer.getWidth(countText);
        context.drawTextWithShadow(mc.textRenderer, countText, windowWidth - countWidth - 2, 6, 0xAAAAAA);

        context.getMatrices().pop();
    }

    @Unique
    private Slot getSlotAt(double mouseX, double mouseY) {
        for (Slot slot : this.handler.slots) {
            if (slot.isEnabled() && this.isPointOverSlot(slot, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }
}