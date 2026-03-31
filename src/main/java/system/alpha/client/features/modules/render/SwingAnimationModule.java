package system.alpha.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.module.setting.ModeSetting;

@ModuleRegister(name = "Swing Animation", category = Category.RENDER, description = "Меняет анимацию удара и других действий.")
public class SwingAnimationModule extends Module {
    @Getter private static final SwingAnimationModule instance = new SwingAnimationModule();

    // Основные режимы для разных типов анимаций
    public final ModeSetting mode = new ModeSetting("Мод").value("Мод 1")
            .values("Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5", "Кастом 1", "Кастом 2", "Кастом 3");

    // Режимы для разных действий
    public final ModeSetting actionMode = new ModeSetting("Action Mode").value("Все действия")
            .values("Все действия", "Только меч", "Только инструменты", "Только лук", "Только еда", "Только арбалет");

    // Настройки для конкретных действий
    public final BooleanSetting customSwordAnim = new BooleanSetting("Меч кастом").value(true);
    public final BooleanSetting customToolAnim = new BooleanSetting("Инструменты кастом").value(true);
    public final BooleanSetting customBowAnim = new BooleanSetting("Лук кастом").value(true);
    public final BooleanSetting customEatAnim = new BooleanSetting("Еда кастом").value(true);
    public final BooleanSetting customCrossbowAnim = new BooleanSetting("Арбалет кастом").value(true);

    // Настройки анимации
    public final SliderSetting strength = new SliderSetting("Сила").value(20f).range(20f, 75f).step(0.1f);
    public final SliderSetting swingRange = new SliderSetting("Размах").value(1f).range(0.5f, 2f).step(0.1f);
    public final SliderSetting height = new SliderSetting("Высота").value(0f).range(-1f, 1f).step(0.1f);
    public final SliderSetting tilt = new SliderSetting("Наклон").value(0f).range(-90f, 90f).step(1f);

    public final BooleanSetting slow = new BooleanSetting("Медленно").value(false);
    public final SliderSetting speed = new SliderSetting("Скорость").value(12f).range(1f, 50f).step(1f).setVisible(slow::getValue);

    // Кастомные вращения
    public final SliderSetting customRotX = new SliderSetting("Кастом вращ X").value(0f).range(-360f, 360f).step(1f);
    public final SliderSetting customRotY = new SliderSetting("Кастом вращ Y").value(0f).range(-360f, 360f).step(1f);
    public final SliderSetting customRotZ = new SliderSetting("Кастом вращ Z").value(0f).range(-360f, 360f).step(1f);

    // Кастомные трансляции
    public final SliderSetting customTransX = new SliderSetting("Кастом смещ X").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting customTransY = new SliderSetting("Кастом смещ Y").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting customTransZ = new SliderSetting("Кастом смещ Z").value(0f).range(-2f, 2f).step(0.1f);

    public SwingAnimationModule() {
        addSettings(mode, actionMode, customSwordAnim, customToolAnim, customBowAnim, customEatAnim, customCrossbowAnim,
                strength, swingRange, height, tilt, slow, speed,
                customRotX, customRotY, customRotZ, customTransX, customTransY, customTransZ);
    }

    @Override
    public void onEvent() {}

    private boolean shouldApplyAnimation(ItemStack item) {
        String action = actionMode.getValue();

        switch (action) {
            case "Все действия":
                return true;
            case "Только меч":
                return isSword(item);
            case "Только инструменты":
                return isTool(item);
            case "Только лук":
                return item.isOf(Items.BOW);
            case "Только арбалет":
                return item.isOf(Items.CROSSBOW);
            default:
                return true;
        }
    }

    private boolean isSword(ItemStack item) {
        return item.getItem() instanceof SwordItem;
    }

    private boolean isTool(ItemStack item) {
        return item.getItem() instanceof PickaxeItem ||
                item.getItem() instanceof AxeItem ||
                item.getItem() instanceof ShovelItem ||
                item.getItem() instanceof HoeItem;
    }

    private boolean isMiningTool(ItemStack item) {
        return item.getItem() instanceof MiningToolItem;
    }

    private void handleCustomAnim(MatrixStack matrices, float swingProgress, Arm arm, ItemStack item) {
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        float anim = (float) Math.sin(swingProgress * (Math.PI / 2) * 2);
        float isLeft = arm == Arm.LEFT ? -1f : 1f;

        String currentMode = mode.getValue();

        // Базовое применение настроек
        matrices.translate(customTransX.getValue() * swingProgress,
                customTransY.getValue() * swingProgress + height.getValue() * g,
                customTransZ.getValue() * swingProgress);

        switch (currentMode) {
            case "Мод 1" -> {
                applyEquipOffset(matrices, arm, 0);
                applySwingOffset(matrices, arm, swingProgress);
            }
            case "Мод 2" -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f + tilt.getValue()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * -60f + customRotY.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * (110f + strength.getValue() * g) + customRotZ.getValue() * g));
            }
            case "Мод 3" -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f + tilt.getValue()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * (-30f * (1f - g) - 30f + (strength.getValue() - 20f) * g) + customRotY.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * 110f + customRotZ.getValue() * g));
            }
            case "Мод 4" -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * 90f + customRotY.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * -30f + customRotZ.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f - strength.getValue() * anim + 10f + tilt.getValue()));
            }
            case "Мод 5" -> {
                float rotation = swingProgress * -360f * swingRange.getValue();
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation + customRotX.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(customRotY.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(customRotZ.getValue() * g));
            }
            case "Кастом 1" -> { // Вращение как пропеллер
                float rotation = swingProgress * 720f * swingRange.getValue();
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation + customRotY.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45f + tilt.getValue()));
            }
            case "Кастом 2" -> { // Вертикальный удар
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0, -0.5f * swingProgress, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f * swingProgress * swingRange.getValue() + tilt.getValue()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * 90f * g + customRotY.getValue()));
            }
            case "Кастом 3" -> { // Спираль
                float spiral = swingProgress * 360f * 3;
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spiral + customRotX.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spiral * 0.5f + customRotY.getValue() * g));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spiral * 0.3f + customRotZ.getValue() * g));
            }
        }
    }

    private void handleSwordAnim(MatrixStack matrices, float swingProgress, Arm arm, ItemStack item) {
        if (!customSwordAnim.getValue() || !isSword(item)) {
            applyDefaultSwing(matrices, swingProgress, arm);
            return;
        }
        handleCustomAnim(matrices, swingProgress, arm, item);
    }

    private void handleToolAnim(MatrixStack matrices, float swingProgress, Arm arm, ItemStack item) {
        if (!customToolAnim.getValue() || !isTool(item)) {
            applyDefaultSwing(matrices, swingProgress, arm);
            return;
        }
        handleCustomAnim(matrices, swingProgress, arm, item);
    }

    private void handleBowAnim(MatrixStack matrices, float pullProgress, Arm arm, ItemStack item) {
        if (!customBowAnim.getValue() || !item.isOf(Items.BOW)) {
            return;
        }

        float isLeft = arm == Arm.LEFT ? -1f : 1f;
        float pull = Math.min(pullProgress, 1.0f);

        matrices.translate(0, height.getValue() * pull, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20f + tilt.getValue() * pull));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * 45f + customRotY.getValue() * pull));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * -10f + customRotZ.getValue() * pull));

        // Добавляем тряску при натягивании
        if (pull > 0.1f && pull < 0.9f) {
            float shake = MathHelper.sin(pull * 20f) * 0.02f;
            matrices.translate(shake, shake, 0);
        }
    }

    private void handleCrossbowAnim(MatrixStack matrices, float pullProgress, Arm arm, ItemStack item) {
        if (!customCrossbowAnim.getValue() || !item.isOf(Items.CROSSBOW)) {
            return;
        }

        float isLeft = arm == Arm.LEFT ? -1f : 1f;
        float pull = Math.min(pullProgress, 1.0f);

        matrices.translate(0, height.getValue() * pull, isLeft * 0.1f * pull);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10f + tilt.getValue() * pull));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * 30f + customRotY.getValue() * pull));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * -5f + customRotZ.getValue() * pull));
    }


    private void applyDefaultSwing(MatrixStack matrices, float swingProgress, Arm arm) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    public void handleRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!player.isUsingSpyglass() && shouldApplyAnimation(item)) {
            boolean bl = hand == Hand.MAIN_HAND;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            matrices.push();

            if (item.isOf(Items.CROSSBOW)) {
                handleCrossbowRendering(player, tickDelta, hand, swingProgress, item, equipProgress, matrices, arm, vertexConsumers, light);
            } else {
                handleItemRendering(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light, arm, bl);
            }

            matrices.pop();
        }
    }

    private void handleCrossbowRendering(AbstractClientPlayerEntity player, float tickDelta, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, Arm arm, VertexConsumerProvider vertexConsumers, int light) {
        boolean bl3 = arm == Arm.RIGHT;
        int i = bl3 ? 1 : -1;

        if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
            this.applyEquipOffset(matrices, arm, equipProgress);
            matrices.translate((float) i * -0.4785682F, -0.094387F, 0.05731531F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 65.3F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * -9.785F));

            float f = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float g = f / (float) CrossbowItem.getPullTime(item, mc.player);
            if (g > 1.0F) g = 1.0F;

            handleCrossbowAnim(matrices, g, arm, item);

            if (g > 0.1F) {
                float h = MathHelper.sin((f - 0.1F) * 1.3F);
                float j = g - 0.1F;
                float k = h * j;
                matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
            }

            matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
            matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) i * 45.0F));
        } else {
            float fx = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float gx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
            float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
            matrices.translate((float) i * fx, gx, h);
            this.applyEquipOffset(matrices, arm, equipProgress);
            this.applySwingOffset(matrices, arm, swingProgress);

            boolean bl2 = CrossbowItem.isCharged(item);
            if (bl2 && swingProgress < 0.001F && hand == Hand.MAIN_HAND) {
                matrices.translate((float) i * -0.641864F, 0.0F, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 10.0F));
            }
        }
        this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
    }

    private void handleItemRendering(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm arm, boolean isMainHand) {
        boolean bl2 = arm == Arm.RIGHT;

        ViewModelModule viewModel = ViewModelModule.getInstance();
        if (viewModel.isEnabled()) {
            if (bl2) {
                matrices.translate(viewModel.rightX.getValue().doubleValue(), viewModel.rightY.getValue().doubleValue(), viewModel.rightZ.getValue().doubleValue());
            } else {
                matrices.translate(-viewModel.leftX.getValue().doubleValue(), viewModel.leftY.getValue().doubleValue(), viewModel.leftZ.getValue().doubleValue());
            }
        }

        if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
            int l = bl2 ? 1 : -1;
            switch (item.getUseAction()) {
                case NONE, BLOCK:
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    break;
                case EAT:
                case DRINK:
                    float useProgress = 1.0F - ((float) player.getItemUseTimeLeft() - tickDelta) / (float) item.getMaxUseTime(player);
                    this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item);
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    break;
                case BOW:
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float) l * -0.2785682F, 0.18344387F, 0.15731531F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));

                    float mx = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float pullProgress = mx / 20.0F;
                    pullProgress = (pullProgress * pullProgress + pullProgress * 2.0F) / 3.0F;
                    if (pullProgress > 1.0F) pullProgress = 1.0F;

                    handleBowAnim(matrices, pullProgress, arm, item);

                    if (pullProgress > 0.1F) {
                        float gx = MathHelper.sin((mx - 0.1F) * 1.3F);
                        float h = pullProgress - 0.1F;
                        float j = gx * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                    }

                    matrices.translate(pullProgress * 0.0F, pullProgress * 0.0F, pullProgress * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F + pullProgress * 0.2F);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                    break;
                case SPEAR:
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float) l * -0.5F, 0.7F, 0.1F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));

                    float m = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float fx = m / 10.0F;
                    if (fx > 1.0F) fx = 1.0F;

                    if (fx > 0.1F) {
                        float gx = MathHelper.sin((m - 0.1F) * 1.3F);
                        float h = fx - 0.1F;
                        float j = gx * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                    }

                    matrices.translate(0.0F, 0.0F, fx * 0.2F);
                    matrices.scale(1.0F, 1.0F, 1.0F + fx * 0.2F);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                    break;
                case BRUSH:
                    this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
            }
        } else if (player.isUsingRiptide()) {
            this.applyEquipOffset(matrices, arm, equipProgress);
            int l = bl2 ? 1 : -1;
            matrices.translate((float) l * -0.4F, 0.8F, 0.3F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 65.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -85.0F));
        } else {
            if (arm == mc.options.getMainArm().getValue() && isEnabled() && shouldApplyAnimation(item)) {
                if (isSword(item)) {
                    handleSwordAnim(matrices, swingProgress, arm, item);
                } else if (isTool(item)) {
                    handleToolAnim(matrices, swingProgress, arm, item);
                } else {
                    float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI) * swingRange.getValue();
                    float mxx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2)) * swingRange.getValue();
                    float fxxx = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI) * swingRange.getValue();
                    int o = bl2 ? 1 : -1;
                    matrices.translate((float) o * n, mxx + height.getValue(), fxxx);
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    this.applySwingOffset(matrices, arm, swingProgress);
                }
            } else {
                float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                float mxx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                float fxxx = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                int o = bl2 ? 1 : -1;
                matrices.translate((float) o * n, mxx, fxxx);
                this.applyEquipOffset(matrices, arm, equipProgress);
                this.applySwingOffset(matrices, arm, swingProgress);
            }
        }
        this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
    }

    private void applyBrushTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, float equipProgress) {
        this.applyEquipOffset(matrices, arm, equipProgress);
        float f = (float) (mc.player.getItemUseTimeLeft() % 10);
        float g = f - tickDelta + 1.0F;
        float h = 1.0F - g / 10.0F;
        float n = -15.0F + 75.0F * MathHelper.cos(h * 2.0F * (float) Math.PI);
        if (arm != Arm.RIGHT) {
            matrices.translate(0.1, 0.83, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
            matrices.translate(-0.3, 0.22, 0.35);
        } else {
            matrices.translate(-0.25, 0.22, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
        }
    }

    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack) {
        float f = (float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F;
        float g = f / (float) stack.getMaxUseTime(mc.player);
        if (g < 0.8F) {
            float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            matrices.translate(0.0F, h, 0.0F);
        }

        float h = 1.0F - (float) Math.pow(g, 27.0);
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6F * (float) i, h * -0.5F, h * 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * h * 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * h * 30.0F));
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    public void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!stack.isEmpty()) {
            mc.getItemRenderer().renderItem(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
        }
    }
}
