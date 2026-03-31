package system.alpha.api.module;

import lombok.Getter;
import system.alpha.client.features.modules.player.*;
import system.alpha.client.features.modules.render.*;
import system.alpha.client.features.modules.other.*;
import system.alpha.client.features.modules.render.motionblur.MotionBlurModule;
import system.alpha.client.features.modules.render.particles.ParticlesModule;
import system.alpha.client.features.modules.render.targetesp.TargetEspModule;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleManager {
    @Getter private final static ModuleManager instance = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    public void load() {
        register(
                SprintModule.getInstance(),
                ClickGUIModule.getInstance(),
                AmbienceModule.getInstance(),
                InterfaceModule.getInstance(),
                TPAcceptModule.getInstance(),
                RemovalsModule.getInstance(),
                SwingAnimationModule.getInstance(),
                ViewModelModule.getInstance(),
                PointersModule.getInstance(),
                MouseTweaksModule.getInstance(),
                ParticlesModule.getInstance(),
                TapeMouseModule.getInstance(),
                HealthResolverModule.getInstance(),
                StreamerModule.getInstance(),
                JumpCircleModule.getInstance(),
                CrosshairModule.getInstance(),
                CustomHitboxesModule.getInstance(),
                CustomHatModule.getInstance(),
                BlockOverlayModule.getInstance(),
                TotemPopsModule.getInstance(),
                HitBubblesModule.getInstance(),
                HitColorModule.getInstance(),
                ItemPhysicModule.getInstance(),
                ZoomModule.getInstance(),
                ToggleSoundsModule.getInstance(),
                CrashNotificationModule.getInstance(),
                MotionBlurModule.getInstance(),
                NightVisionModule.getInstance(),TrailsModule.getInstance(),
                TargetEspModule.getInstance()
        );

        modules.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public void register(Module... modules) {
        this.modules.addAll(List.of(modules));
    }
}