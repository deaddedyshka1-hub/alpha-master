package system.alpha.client.features.modules.render.targetesp;

import lombok.Getter;
import system.alpha.api.event.Listener;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.events.player.other.UpdateEvent;
import system.alpha.api.event.events.render.Render3DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.ModeSetting;
import system.alpha.api.module.setting.SliderSetting;
import system.alpha.client.features.modules.render.targetesp.modes.TargetEspComets;
import system.alpha.client.features.modules.render.targetesp.modes.TargetEspCrystal;
import system.alpha.client.features.modules.render.targetesp.modes.TargetEspTexture;

@ModuleRegister(name = "Target Esp", category = Category.RENDER, description = "Рисует маркера при наводке на энтити.")
public class TargetEspModule extends Module {
    @Getter private static final TargetEspModule instance = new TargetEspModule();

    private final TargetEspComets espComets = new TargetEspComets();
    private final TargetEspTexture espTexture = new TargetEspTexture();
    private final TargetEspCrystal espCrystal = new TargetEspCrystal();

    private TargetEspMode currentMode = espTexture;

    @Getter public final ModeSetting mode = new ModeSetting("Мод").value("Маркер").values("Маркер", "Кометы", "Кристаллы").onAction(() -> {
        currentMode = switch (getMode().getValue()) {
            case "Кометы" -> espComets;
            case "Кристаллы" -> espCrystal;
            default -> espTexture;
        };
    });
    private final ModeSetting animation = new ModeSetting("Анимация").value("In").values("In", "Out", "None");
    private final SliderSetting duration = new SliderSetting("Время").value(3f).range(1f, 20f).step(1f);
    private final SliderSetting size = new SliderSetting("Размер").value(1f).range(0.1f, 2f).step(0.1f);
    private final SliderSetting inSize = new SliderSetting("По размеру").value(0f).range(0f, 1f).step(0.1f).setVisible(() -> animation.is("In"));
    private final SliderSetting outSize = new SliderSetting("Выходящий размер").value(2f).range(1f, 2f).step(0.1f).setVisible(() -> animation.is("Out"));
    public final BooleanSetting lastPosition = new BooleanSetting("Ласт позиция").value(true);
    public final BooleanSetting ignoreInvisible = new BooleanSetting("Игнорировать невидимых").value(true);
    public final SliderSetting crystalSpeed = new SliderSetting("Скорость").value(6f).range(1f, 20f).step(0.5f).setVisible(() -> mode.is("Кристаллы"));
    public final BooleanSetting crystalBloom = new BooleanSetting("Блум").value(true).setVisible(() -> mode.is("Кристаллы"));
    public final SliderSetting crystalBloomSize = new SliderSetting("Размер блума").value(1f).range(0.3f, 2f).step(0.1f).setVisible(() -> mode.is("Кристаллы") && crystalBloom.getValue());
    public final BooleanSetting crystalRedOnImpact = new BooleanSetting("Красный прои ударе").value(true).setVisible(() -> mode.is("Кристаллы"));
    public final SliderSetting crystalImpactFadeIn = new SliderSetting("Аним. затухания").value(0.3f).range(0.05f, 1f).step(0.01f).setVisible(() -> mode.is("Кристаллы") && crystalRedOnImpact.getValue());
    public final SliderSetting crystalImpactFadeOut = new SliderSetting("Аним. исчезноваения").value(0.08f).range(0.01f, 0.5f).step(0.01f).setVisible(() -> mode.is("Кристаллы") && crystalRedOnImpact.getValue());
    public final SliderSetting crystalImpactIntensity = new SliderSetting("Интенсивность").value(1f).range(0.1f, 1f).step(0.05f).setVisible(() -> mode.is("Кристаллы") && crystalRedOnImpact.getValue());

    public TargetEspModule() {
        addSettings(
                mode, animation, duration,
                size, inSize, outSize, lastPosition, ignoreInvisible,
                crystalSpeed, crystalBloom, crystalBloomSize,
                crystalRedOnImpact, crystalImpactFadeIn, crystalImpactFadeOut, crystalImpactIntensity);
    }

    public boolean shouldIgnoreInvisible() {
        return ignoreInvisible.getValue();
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            TargetEspMode.updatePositions();

            currentMode.onRender3D(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.updateAnimation(duration.getValue().longValue() * 50, animation.getValue(), size.getValue(), inSize.getValue(), outSize.getValue());
            currentMode.updateTarget();
            currentMode.onUpdate();
        }));

        addEvents(render3DEvent, updateEvent);
    }
}