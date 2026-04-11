package system.alpha.api.utils.entity;

import net.minecraft.entity.LivingEntity;

public final class ArmorTintContext {
    private static final ThreadLocal<LivingEntity> CURRENT = new ThreadLocal();

    private ArmorTintContext() {
    }

    public static void set(LivingEntity entity) {
        CURRENT.set(entity);
    }

    public static LivingEntity get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}