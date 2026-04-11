package system.alpha.client.features.modules.player;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.player.other.UpdateEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.ModeSetting;

import java.util.HashMap;
import java.util.Map;

@ModuleRegister(
        name = "CustomModels",
        category = Category.PLAYER,
        description = "Заменяет модели игроков на кастомные 3D."
)
public class CustomModelsModule extends Module {

    @Getter
    private static final CustomModelsModule instance = new CustomModelsModule();

    private final ModeSetting type = new ModeSetting("Модель")
            .values(CustomModelType.names())
            .value(CustomModelType.CRAZY_RABBIT.getDisplayName());

    private final Map<PlayerEntity, CustomModelEntity> customEntities = new HashMap<>();

    public CustomModelsModule() {
        addSettings(type);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            CustomModelType selectedType = getSelectedType();
            if (selectedType == null || !isEnabled()) {
                customEntities.clear();
                return;
            }

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!shouldApplyTo(player)) continue;

                if (!customEntities.containsKey(player)) {
                    CustomModelEntity customEntity = new CustomModelEntity(selectedType, player);
                    customEntities.put(player, customEntity);
                }
            }

            customEntities.keySet().removeIf(player -> !mc.world.getPlayers().contains(player));
        }));

        addEvents(updateEvent);
    }

    @Override
    public void onEnable() {
        customEntities.clear();
    }

    @Override
    public void onDisable() {
        customEntities.clear();
    }

    public CustomModelType getSelectedType() {
        return CustomModelType.fromDisplay(type.getValue());
    }

    public boolean shouldApplyTo(LivingEntity player) {
        if (!isEnabled() || player == null) {
            return false;
        }
        return player == mc.player;
    }

    public static class CustomModelEntity {
        private final CustomModelType type;
        private final PlayerEntity owner;
        private float animationProgress = 0f;
        private boolean walkingUp = true;

        public CustomModelEntity(CustomModelType type, PlayerEntity owner) {
            this.type = type;
            this.owner = owner;
        }

        public CustomModelType getType() {
            return type;
        }

        public PlayerEntity getOwner() {
            return owner;
        }

        public void updateAnimation() {
            if (owner.isOnGround() && owner.getVelocity().horizontalLength() > 0.01f) {
                if (walkingUp) {
                    animationProgress += 0.1f;
                    if (animationProgress >= 1f) {
                        walkingUp = false;
                    }
                } else {
                    animationProgress -= 0.1f;
                    if (animationProgress <= 0f) {
                        walkingUp = true;
                    }
                }
            } else {
                animationProgress = Math.max(0, animationProgress - 0.05f);
            }
        }

        public float getAnimationProgress() {
            return animationProgress;
        }
    }
}