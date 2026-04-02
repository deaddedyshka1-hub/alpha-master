package system.alpha.client.features.modules.player;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.client.PacketEvent;
import system.alpha.api.event.events.render.Render3DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.SliderSetting;

import java.util.UUID;

@ModuleRegister(name = "FakePlayer", category = Category.PLAYER, description = "Создаёт фейкового игрока для тренировки")
public class FakePlayerModule extends Module {

    @Getter
    private static final FakePlayerModule instance = new FakePlayerModule();

    // Настройки
    private final BooleanSetting showHealth = new BooleanSetting("Показывать здоровье").value(true);
    private final BooleanSetting autoRespawn = new BooleanSetting("Автовозрождение").value(true);
    private final SliderSetting respawnTime = new SliderSetting("Время возрождения")
            .value(3.0f)
            .range(1.0f, 10.0f)
            .step(0.5f)
            .setVisible(() -> autoRespawn.getValue());

    private AbstractClientPlayerEntity fakePlayer = null;
    private final UUID fakeUUID = UUID.fromString("70ee432d-0a96-4137-a2c0-37cc9df67f03");
    private long lastDeathTime = 0;

    public FakePlayerModule() {
        addSettings(showHealth, autoRespawn, respawnTime);
    }

    @Override
    public void onEvent() {
        // Обработка рендера для синхронизации
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null || fakePlayer == null) return;

            // Синхронизируем повороты и анимации
            fakePlayer.setYaw(mc.player.getYaw());
            fakePlayer.setHeadYaw(mc.player.getHeadYaw());
            fakePlayer.setPitch(mc.player.getPitch());
            fakePlayer.setMainArm(mc.player.getMainArm());

            // Синхронизируем использование предметов
            if (mc.player.getActiveHand() != null && mc.player.isUsingItem()) {
                fakePlayer.setCurrentHand(mc.player.getActiveHand());
            } else {
                fakePlayer.clearActiveItem();
            }

            // Показываем здоровье над головой
            if (showHealth.getValue()) {
                renderHealthTag(event, fakePlayer);
            }

            // Автовозрождение
            if (autoRespawn.getValue() && fakePlayer.getHealth() <= 0.0f && lastDeathTime > 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDeathTime >= respawnTime.getValue() * 1000) {
                    respawnFakePlayer();
                }
            }
        }));

        // Обработка пакетов для взаимодействия с фейк-игроком
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || fakePlayer == null) return;

            Object packet = getPacketFromEvent(event);

            // Обработка ударов по фейк-игроку
            if (packet instanceof PlayerInteractEntityC2SPacket interactPacket) {
                handleAttack(interactPacket);
            }

            // Обработка взрывов
            if (packet instanceof ExplosionS2CPacket explosionPacket) {
                handleExplosion(explosionPacket);
            }
        }));

        addEvents(renderEvent, packetEvent);
    }

    private void handleAttack(PlayerInteractEntityC2SPacket packet) {
        if (fakePlayer == null || fakePlayer.distanceTo(mc.player) > 6) return;

        // Проверяем, атакуем ли мы фейк-игрока
        try {
            int entityId = getEntityIdFromPacket(packet);
            if (entityId != fakePlayer.getId()) return;
        } catch (Exception e) {
            return;
        }

        if (fakePlayer.hurtTime > 0) {
            mc.player.getWorld().playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, mc.player.getSoundCategory(), 1.0f, 1.0f);
            return;
        }

        fakePlayer.hurtTime = 9;
        boolean cooled = mc.player.getAttackCooldownProgress(1.0f) >= 0.9;

        boolean movePressed = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() ||
                mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();

        boolean canCrit = cooled && (!mc.player.isSprinting() || !movePressed && !mc.player.isOnGround()) &&
                (mc.player.fallDistance != 0.0f && !mc.player.isOnGround());
        boolean canKnock = cooled && !canCrit && mc.player.isSprinting();
        boolean canSweep = cooled && !mc.player.isSprinting() && mc.player.isOnGround() &&
                mc.player.getMainHandStack().getItem() instanceof SwordItem;

        float attackCooldown = mc.player.getAttackCooldownProgress(0.5f);
        float damage = (0.2f + attackCooldown * attackCooldown * 0.8f) * (attackCooldown / 2.0f + 1.6f);

        // Воспроизводим звуки атаки
        if (canCrit || canKnock || canSweep) {
            mc.player.getWorld().playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_HURT, mc.player.getSoundCategory(), 0.5f, 1.0f);
        }

        if (canCrit) {
            damage *= 1.5f;
            mc.player.getWorld().playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, mc.player.getSoundCategory(), 1.0f, 1.0f);
        } else if (canKnock) {
            mc.player.getWorld().playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, mc.player.getSoundCategory(), 1.0f, 1.0f);
        } else if (canSweep) {
            mc.player.getWorld().playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, mc.player.getSoundCategory(), 1.0f, 1.0f);
        }

        // Наносим урон
        applyDamage(damage);

        fakePlayer.limbAnimator.setSpeed(fakePlayer.hurtTime / 10.0f);

        // Проверяем смерть
        if (fakePlayer.getHealth() <= 0.0f && fakePlayer.getAbsorptionAmount() <= 0.0f) {
            handleDeath();
        }
    }

    private void handleExplosion(ExplosionS2CPacket packet) {
        if (fakePlayer == null || fakePlayer.hurtTime > 1) return;

        Vec3d explosionPos = new Vec3d(0, 0, 0);
        double distance = fakePlayer.getPos().distanceTo(explosionPos);

        if (distance < 11.0) {
            float damage = (float) ((9.0 - distance) / 9.0) * 10.0f;

            if (fakePlayer.getAbsorptionAmount() > 0) {
                float newAbsorption = Math.max(0, fakePlayer.getAbsorptionAmount() - damage);
                fakePlayer.setAbsorptionAmount(newAbsorption);
                damage -= (fakePlayer.getAbsorptionAmount() - newAbsorption);
            }

            if (damage > 0) {
                fakePlayer.setHealth(Math.max(0.001f, fakePlayer.getHealth() - damage));
            }

            mc.player.getWorld().playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_HURT, mc.player.getSoundCategory(), 0.5f, 1.0f);

            // Проверяем тотем
            if (fakePlayer.getHealth() <= 0.0f && fakePlayer.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                useTotem();
            } else if (fakePlayer.getHealth() <= 0.0f) {
                handleDeath();
            }
        }
    }

    private void applyDamage(float damage) {
        if (fakePlayer.getAbsorptionAmount() > 0) {
            float newAbsorption = Math.max(0, fakePlayer.getAbsorptionAmount() - damage);
            fakePlayer.setAbsorptionAmount(newAbsorption);
            damage -= (fakePlayer.getAbsorptionAmount() - newAbsorption);
        }

        if (damage > 0) {
            fakePlayer.setHealth(Math.max(0.001f, fakePlayer.getHealth() - damage));
        }
    }

    private void useTotem() {
        fakePlayer.clearStatusEffects();
        mc.particleManager.addEmitter(fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
        mc.player.getWorld().playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ITEM_TOTEM_USE, mc.player.getSoundCategory(), 1.0f, 1.0f);

        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 880, 0));
        fakePlayer.setHealth(1.0f);

        // Убираем тотем из руки
        fakePlayer.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
    }

    private void handleDeath() {
        fakePlayer.clearStatusEffects();
        mc.particleManager.addEmitter(fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
        mc.player.getWorld().playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ENTITY_PLAYER_DEATH, mc.player.getSoundCategory(), 1.0f, 1.0f);

        lastDeathTime = System.currentTimeMillis();

        if (!autoRespawn.getValue()) {
            fakePlayer.setHealth(0.0f);
        }
    }

    private void respawnFakePlayer() {
        if (fakePlayer == null || mc.world == null) return;

        fakePlayer.setHealth(20.0f);
        fakePlayer.setAbsorptionAmount(0.0f);
        fakePlayer.clearStatusEffects();

        // Возвращаем тотем в оффхенд
        fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING, 64));
        fakePlayer.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));

        // Телепортируем обратно к игроку
        fakePlayer.setPosition(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        lastDeathTime = 0;
    }

    private void renderHealthTag(Render3DEvent.Render3DEventData event, AbstractClientPlayerEntity player) {
        if (player.getHealth() <= 0) return;

        // Здесь можно добавить отрисовку здоровья над головой
        // Для простоты оставляем пустым, но можно реализовать позже
    }

    @Override
    public void onEnable() {
        if (mc.world != null && mc.player != null) {
            fakePlayer = new AbstractClientPlayerEntity(mc.world,
                    new GameProfile(fakeUUID, "FakePlayer")) {};

            fakePlayer.copyPositionAndRotation(mc.player);
            fakePlayer.setPosition(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            fakePlayer.setHealth(20.0f);
            fakePlayer.setAbsorptionAmount(0.0f);

            // Даём предметы
            fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING, 64));
            fakePlayer.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));

            // Копируем инвентарь? (опционально)
            // fakePlayer.getInventory().clone(mc.player.getInventory());

            mc.world.addEntity(fakePlayer);
            print("§a[FakePlayer] §fФейк-игрок создан!");
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.world != null && fakePlayer != null) {
            fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
            print("§c[FakePlayer] §fФейк-игрок удалён!");
        }
        super.onDisable();
    }

    // Вспомогательные методы для получения данных из пакетов
    private Object getPacketFromEvent(PacketEvent.PacketEventData event) {
        try {
            java.lang.reflect.Method method = event.getClass().getMethod("getPacket");
            return method.invoke(event);
        } catch (Exception e) {
            return null;
        }
    }

    private int getEntityIdFromPacket(PlayerInteractEntityC2SPacket packet) {
        try {
            java.lang.reflect.Method method = packet.getClass().getMethod("getEntityId");
            return (int) method.invoke(packet);
        } catch (Exception e) {
            return -1;
        }
    }
}