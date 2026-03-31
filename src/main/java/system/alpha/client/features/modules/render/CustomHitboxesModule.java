package system.alpha.client.features.modules.render;

import lombok.Getter;
import net.minecraft.util.math.Box;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.player.other.UpdateEvent;
import system.alpha.api.event.events.render.Render3DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.render.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import system.alpha.api.utils.render.display.hitbox.HitboxQueue;

import java.awt.*;

@ModuleRegister(name = "Custom Hitboxes", category = Category.RENDER)
public class CustomHitboxesModule extends Module {
    @Getter
    private static final CustomHitboxesModule instance = new CustomHitboxesModule();

    private final BooleanSetting fill = new BooleanSetting("Заливка").value(true);
    private final SliderSetting lineWidth = new SliderSetting("Толщина линии")
            .value(1.5f)
            .range(0.5f, 6.0f)
            .step(0.1f);

    private final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки")
            .value(90f)
            .range(0f, 255f)
            .step(1f);

    private final SliderSetting outlineAlpha = new SliderSetting("Прозрачность контура")
            .value(255f)
            .range(0f, 255f)
            .step(1f);

    private final BooleanSetting gradientColors = new BooleanSetting("Градиентные цвета").value(true);

    private final SliderSetting gradientSpeed = new SliderSetting("Скорость градиента")
            .value(1.0f)
            .range(0.1f, 5.0f)
            .step(0.1f)
            .setVisible(() -> gradientColors.getValue());

    private final BooleanSetting showOnPlayers = new BooleanSetting("На игроках").value(true);
    private final BooleanSetting showOnMobs = new BooleanSetting("На мобах").value(true);
    private final BooleanSetting showOnItems = new BooleanSetting("На предметах").value(false);
    private final BooleanSetting showOnOther = new BooleanSetting("На других").value(false);

    private final AnimationUtil animationUtil = new AnimationUtil();
    private float gradientOffset = 0f;

    public CustomHitboxesModule() {
        addSettings(fill, lineWidth, fillAlpha, outlineAlpha,
                gradientColors, gradientSpeed,
                showOnPlayers, showOnMobs, showOnItems, showOnOther);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            animationUtil.update();
            double target = isEnabled() ? 1.0 : 0.0;
            animationUtil.run(target, 200, Easing.SINE_OUT);

            gradientOffset += gradientSpeed.getValue() * 0.5f;
            if (gradientOffset > 360f) {
                gradientOffset -= 360f;
            }
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;
            if (mc.currentScreen != null) return;

            MatrixStack matrixStack = event.matrixStack();
            RenderUtil.WORLD.startRender(matrixStack);

            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) {
                    Box box = entity.getBoundingBox()
                            .offset(-mc.getEntityRenderDispatcher().camera.getPos().getX(),
                                    -mc.getEntityRenderDispatcher().camera.getPos().getY(),
                                    -mc.getEntityRenderDispatcher().camera.getPos().getZ());

                    Color fillColor = getFillColorForRendering();
                    Color outlineColor = getOutlineColorForRendering();
                    float lineWidth = getLineWidth();

                    if (shouldFill()) {
                        HitboxQueue.addHitbox(box, fillColor, outlineColor, lineWidth);
                    } else {
                        HitboxQueue.addHitboxOutline(box, outlineColor, lineWidth);
                    }
                    continue;
                }

                if (shouldRenderEntity(entity)) {
                    Box box = entity.getBoundingBox()
                            .offset(-mc.getEntityRenderDispatcher().camera.getPos().getX(),
                                    -mc.getEntityRenderDispatcher().camera.getPos().getY(),
                                    -mc.getEntityRenderDispatcher().camera.getPos().getZ());

                    Color fillColor = getFillColorForRendering();
                    Color outlineColor = getOutlineColorForRendering();
                    float lineWidth = getLineWidth();

                    if (shouldFill()) {
                        HitboxQueue.addHitbox(box, fillColor, outlineColor, lineWidth);
                    } else {
                        HitboxQueue.addHitboxOutline(box, outlineColor, lineWidth);
                    }
                }
            }

            HitboxQueue.renderQueuedHitboxes(matrixStack);
            RenderUtil.WORLD.endRender(matrixStack);
        }));

        addEvents(updateEvent, renderEvent);
    }

    public boolean shouldRenderEntityInHitbox(Entity entity) {
        return shouldRenderEntity(entity);
    }

    public Color getFillColorForRendering() {
        float animationValue = (float) animationUtil.getValue();
        int alpha = (int)(fillAlpha.getValue() * animationValue);

        if (gradientColors.getValue()) {
            int gradientIndex = (int)(gradientOffset) % 360;
            return UIColors.gradient(gradientIndex, alpha);
        } else {
            return UIColors.primary(alpha);
        }
    }

    public Color getOutlineColorForRendering() {
        float animationValue = (float) animationUtil.getValue();
        int alpha = (int)(outlineAlpha.getValue() * animationValue);

        if (gradientColors.getValue()) {
            int gradientIndex = (int)(gradientOffset + 180) % 360;
            return UIColors.gradient(gradientIndex, alpha);
        } else {
            return UIColors.secondary(alpha);
        }
    }

    public boolean shouldFill() {
        return fill.getValue();
    }

    public float getLineWidth() {
        return lineWidth.getValue().floatValue() * (float) animationUtil.getValue();
    }

    private boolean shouldRenderEntity(Entity entity) {
        if (entity.isInvisible()) return false;

        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (livingEntity.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                return false;
            }
        }

        if (entity instanceof net.minecraft.entity.player.PlayerEntity) {
            return showOnPlayers.getValue();
        }
        else if (entity instanceof net.minecraft.entity.mob.MobEntity ||
                entity instanceof net.minecraft.entity.passive.PassiveEntity ||
                entity instanceof net.minecraft.entity.mob.HostileEntity) {
            return showOnMobs.getValue();
        }
        else if (entity instanceof net.minecraft.entity.ItemEntity ||
                entity instanceof net.minecraft.entity.decoration.ItemFrameEntity) {
            return showOnItems.getValue();
        }
        else {
            return showOnOther.getValue();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        animationUtil.setValue(0.0);
        gradientOffset = 0f;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}