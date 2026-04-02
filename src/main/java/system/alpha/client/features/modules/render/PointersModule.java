package system.alpha.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.render.Render2DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.*;
import system.alpha.api.system.configs.FriendManager;
import system.alpha.api.system.files.FileUtil;
import system.alpha.api.utils.animation.AnimationUtil;
import system.alpha.api.utils.animation.Easing;
import system.alpha.api.utils.color.ColorUtil;
import system.alpha.api.utils.color.UIColors;
import system.alpha.api.utils.math.MathUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

@ModuleRegister(name = "Pointers", category = Category.RENDER, description = "Указывает где стрелочкой, что и где.")
public class PointersModule extends Module {
    @Getter private static final PointersModule instance = new PointersModule();

    protected final MultiBooleanSetting targets = new MultiBooleanSetting("Цели").value(
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Предметы").value(false)
    );

    private final ModeSetting playerModeC = new ModeSetting("Цвет игроков").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Игроки"));
    private final ModeSetting animalsModeC = new ModeSetting("Цвет животных").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Животные"));
    private final ModeSetting mobModeC = new ModeSetting("Цвет мобов").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Мобы"));
    private final ModeSetting friendModeC = new ModeSetting("Цвет друзей").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Игроки"));
    private final ModeSetting itemModeC = new ModeSetting("Цвет предметов").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Предметы"));

    private final ColorSetting playerColor = new ColorSetting("Цвет игроков").value(new Color(-1)).setVisible(() -> playerModeC.is("Свой") && targets.isEnabled("Игроки"));
    private final ColorSetting animalsColor = new ColorSetting("Цвет животных").value(new Color(-1)).setVisible(() -> animalsModeC.is("Свой") && targets.isEnabled("Животные"));
    private final ColorSetting mobColor = new ColorSetting("Цвет мобов").value(new Color(-1)).setVisible(() -> mobModeC.is("Свой") && targets.isEnabled("Мобы"));
    private final ColorSetting friendColor = new ColorSetting("Цвет друзей").value(new Color(94, 255, 69)).setVisible(() -> friendModeC.is("Свой") && targets.isEnabled("Игроки"));
    private final ColorSetting itemColor = new ColorSetting("Цвет предметов").value(new Color(255, 72, 69)).setVisible(() -> itemModeC.is("Свой") && targets.isEnabled("Предметы"));

    private final SliderSetting pointerSize = new SliderSetting("Размер").value(1f).range(0.5f, 2.5f).step(0.1f);
    private final SliderSetting pointerRadius = new SliderSetting("Радиус").value(40f).range(20f, 100f).step(1f);

    private final ModeSetting animation = new ModeSetting("Анимация").value("Появление").values("Исчезновение", "Появление", "Нет");
    private final SliderSetting duration = new SliderSetting("Длительность").value(4f).range(1f, 20f).step(1f);

    private final HashSet<Entity> alive = new HashSet<>();
    private final HashMap<Entity, AnimationUtil> animations = new HashMap<>();
    private final AnimationUtil yawAnimation = new AnimationUtil();
    private final AnimationUtil radiusAnimation = new AnimationUtil();

    public PointersModule() {
        addSettings(targets,
                playerModeC, animalsModeC, mobModeC, friendModeC, itemModeC,
                playerColor, animalsColor, mobColor, friendColor, itemColor,
                pointerSize, pointerRadius, animation, duration
        );
    }

    @Override
    public void onDisable() {
        alive.clear();
        animations.clear();
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(2, event -> {
            alive.clear();

            yawAnimation.update();
            radiusAnimation.update();

            yawAnimation.run(mc.player.getYaw(), 200, Easing.EXPO_OUT);
            radiusAnimation.run(getContainerSize(), 300, Easing.EXPO_OUT);

            for (Entity entity : mc.world.getEntities()) {
                if (mc.player == entity) continue;

                // Пропускаем невидимых существ
                if (isInvisible(entity)) continue;

                boolean isValid = false;

                if (entity instanceof ItemEntity && targets.isEnabled("Предметы")) {
                    isValid = true;
                }
                else if (entity instanceof PlayerEntity && targets.isEnabled("Игроки")) {
                    isValid = true;
                }
                else if (entity instanceof AnimalEntity && targets.isEnabled("Животные")) {
                    isValid = true;
                }
                else if (entity instanceof MobEntity && targets.isEnabled("Мобы")) {
                    isValid = true;
                }

                if (isValid && isVisible(entity)) {
                    alive.add(entity);
                }
            }

            for (Entity entity : alive) {
                if (!animations.containsKey(entity)) {
                    animations.put(entity, new AnimationUtil());
                }
            }

            Iterator<Map.Entry<Entity, AnimationUtil>> iterator = animations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Entity, AnimationUtil> entry = iterator.next();
                Entity entity = entry.getKey();
                AnimationUtil anim = entry.getValue();

                boolean isAlive = alive.contains(entity);
                anim.update();
                anim.run(isAlive ? 1.0 : 0.0, duration.getValue().longValue() * 50, Easing.SINE_OUT);

                if (anim.getValue() > 0.0) {
                    drawPointerToEntity(event, entity, anim);
                }

                if (!isAlive && anim.getValue() <= 0.0) {
                    iterator.remove();
                }
            }
        }));

        addEvents(renderEvent);
    }

    // Метод проверки на невидимость
    private boolean isInvisible(Entity entity) {
        // Предметы не могут быть невидимыми
        if (entity instanceof ItemEntity) return false;

        // Для живых существ проверяем эффект невидимости
        if (entity instanceof LivingEntity living) {
            return living.hasStatusEffect(StatusEffects.INVISIBILITY);
        }

        return false;
    }

    private void drawPointerToEntity(Render2DEvent.Render2DEventData event, Entity entity, AnimationUtil spawn){
        DrawContext context = event.context();
        MatrixStack matrixStack = context.getMatrices();
        float centerX = getCenterX();
        float centerY = getCenterY();

        float animFactor = 1f;
        float spawnAnim = (float) spawn.getValue();

        if (animation.is("Появление")) {
            animFactor = (2f - spawnAnim);
        } else if (animation.is("Исчезновение")) {
            animFactor = 0.3f + 0.7f * spawnAnim;
        }

        if (spawnAnim <= 0.0) return;

        float animatedRadius = pointerRadius.getValue() * animFactor;
        float yaw = (float) (getEntityYaw(entity) - yawAnimation.getValue());

        matrixStack.push();
        matrixStack.translate(centerX, centerY, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? -yaw : yaw));
        matrixStack.translate(-centerX, -centerY, 0.0F);

        Color color = getEntityColor(entity);
        drawPointer(context, centerX, (float) (centerY - animatedRadius - radiusAnimation.getValue()), pointerSize.getValue() * 20F, ColorUtil.setAlpha(color, (int) (spawnAnim * 255)), false);

        matrixStack.pop();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private boolean isVisible(Entity entity) {
        if (mc.player == null || mc.world == null) return false;

        Vec3d start = mc.player.getCameraPosVec(1.0F);
        Vec3d end = entity.getBoundingBox().getCenter();

        HitResult hitResult = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return hitResult.getType() == HitResult.Type.MISS;
    }

    public void drawPointer(DrawContext context, float x, float y, float size, Color color, boolean gps) {
        RenderSystem.setShaderTexture(0, FileUtil.getImage("pointers/" + (gps ? "arrow_gps" : "triangle")));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        RenderSystem.disableDepthTest();
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        float scaledSize = size + 8;
        buffer.vertex(matrix, x - (scaledSize / 2f), y + scaledSize, 0).texture(0f, 1f);
        buffer.vertex(matrix, x + scaledSize / 2f, y + scaledSize, 0).texture(1f, 1f);
        buffer.vertex(matrix, x + scaledSize / 2f, y, 0).texture(1f, 0);
        buffer.vertex(matrix, x - (scaledSize / 2f), y, 0).texture(0, 0);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float getCenterX() {
        return mc.getWindow().getScaledWidth() / 2f;
    }

    private float getCenterY() {
        return mc.getWindow().getScaledHeight() / 2f;
    }

    private float getContainerSize() {
        if (mc.currentScreen instanceof HandledScreen<?> containerScreen) {
            return Math.max(containerScreen.width, containerScreen.height) * 0.05f + (pointerRadius.getMax() - pointerRadius.getValue());
        }

        return 0f;
    }

    private float getEntityYaw(Entity entity) {
        if (mc.player == null) return 0;
        double xA = (MathUtil.interpolate(mc.player.prevX, mc.player.getPos().x));
        double zA = (MathUtil.interpolate(mc.player.prevZ, mc.player.getPos().z));
        double x = MathUtil.interpolate(entity.prevX, entity.getPos().x) - xA;
        double z = MathUtil.interpolate(entity.prevZ, entity.getPos().z) - zA;
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }

    private Color getEntityColor(Entity entity) {
        int seed = entity.getId() * 13;
        Color gradient = UIColors.gradient(seed);
        return switch (entity) {
            case ItemEntity itemEntity -> itemModeC.is("Свой") ? itemColor.getValue() : gradient;
            case PlayerEntity player -> FriendManager.getInstance().contains(player.getName().getString()) ? friendModeC.is("Свой") ? friendColor.getValue() : gradient : playerModeC.is("Свой") ? playerColor.getValue() : gradient;
            case AnimalEntity animalEntity -> animalsModeC.is("Свой") ? animalsColor.getValue() : gradient;
            case MobEntity mobEntity -> mobModeC.is("Свой") ? mobColor.getValue() : gradient;
            default -> new Color(-1);
        };
    }
}