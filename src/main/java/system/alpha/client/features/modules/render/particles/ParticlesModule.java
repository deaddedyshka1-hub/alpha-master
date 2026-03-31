package system.alpha.client.features.modules.render.particles;

import lombok.Getter;
import lombok.experimental.Accessors;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
//.api.module.setting.*;
import system.alpha.api.module.setting.*;
import system.alpha.api.system.backend.Configurable;

@ModuleRegister(name = "Particles", category = Category.RENDER, description = "Рисует красивые партиклы в небе.")
public class ParticlesModule extends Module {
    @Getter private static final ParticlesModule instance = new ParticlesModule();

    private final HitParticles hitParticles = new HitParticles();
    private final WorldParticles worldParticles = new WorldParticles();

    @Getter private final BooleanSetting worldBoolean = new BooleanSetting("Мир").value(true).onAction(() -> {
        if (!getWorldBoolean().getValue()) {
            worldParticles.toggle();
            worldParticles.removeAllEvents();
            removeEvents(worldParticles.getEventListeners());
        } else if (isEnabled()) {
            worldParticles.onEvent();
            addEvents(worldParticles.getEventListeners());
        }
    });

    @Getter private final BooleanSetting hitBoolean = new BooleanSetting("Удары").value(false).onAction(() -> {
        if (!getHitBoolean().getValue()) {
            hitParticles.toggle();
            hitParticles.removeAllEvents();
            removeEvents(hitParticles.getEventListeners());
        } else if (isEnabled()) {
            hitParticles.onEvent();
            addEvents(hitParticles.getEventListeners());
        }
    });

    private final MultiBooleanSetting spawn = new MultiBooleanSetting("Спавн").value(
            worldBoolean,
            hitBoolean
    );
    
    @Override
    public void toggle() {
        super.toggle();
        hitParticles.toggle();
        worldParticles.toggle();
    }

    public ParticlesModule() {
        addSettings(spawn);

        for (Setting<?> setting : worldParticles.getSettings()) {
            setting.setVisible(() -> spawn.isEnabled(worldBoolean.getName()));
        }
        addSettings(worldParticles.getSettings());


        for (Setting<?> setting : hitParticles.getSettings()) {
            setting.setVisible(() -> spawn.isEnabled(hitBoolean.getName()));
        }
        addSettings(hitParticles.getSettings());
    }

    @Override
    public void onEvent() {
        if (hitBoolean.getValue()) {
            hitParticles.onEvent();
            addEvents(hitParticles.getEventListeners());
        }

        if (worldBoolean.getValue()) {
            worldParticles.onEvent();
            addEvents(worldParticles.getEventListeners());
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class BaseSettings extends Configurable {
        private final ModeSetting textureMode;
        private final SliderSetting count;
        private final SliderSetting size;
        private final SliderSetting lifeTime;
        private final SliderSetting spawnDuration;
        private final SliderSetting dyingDuration;
        private final BooleanSetting rotate;
        private final BooleanSetting trail;
        private final SliderSetting trailLength;
        private final BooleanSetting dyingEffect;

        public final String prefix;

        public BaseSettings(String prefix) {
            this.prefix = prefix + ": ";

            textureMode = new ModeSetting(this.prefix + "Текстура").value("Искра").values(ParticleRender.textures);
            count = new SliderSetting(this.prefix + "Кол-во").value(25f).range(10f, 100f).step(1f);
            size = new SliderSetting(this.prefix + "Размер").value(0.2f).range(0.1f, 0.4f).step(0.05f);
            lifeTime = new SliderSetting(this.prefix + "Время жизни").value(10f).range(2f, 100f).step(1f);
            spawnDuration = new SliderSetting(this.prefix + "Время появления").value(15f).range(0f, 40f).step(1f);
            dyingDuration = new SliderSetting(this.prefix + "Время смерти").value(15f).range(0f, 40f).step(1f);
            rotate = new BooleanSetting(this.prefix + "Вращать").value(true);
            trail = new BooleanSetting(this.prefix + "След").value(false);
            trailLength = new SliderSetting(this.prefix + "Длина следа").value(5f).range(1f, 20f).step(1f);
            dyingEffect = new BooleanSetting(this.prefix + "Эффект смерти").value(false);


            addSettings(
                    textureMode,
                    count, size,
                    lifeTime, spawnDuration, dyingDuration,
                    rotate, trail, trailLength
            );
        }
    }
}
