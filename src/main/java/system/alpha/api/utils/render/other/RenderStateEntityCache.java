package system.alpha.api.utils.render.other;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;

public final class RenderStateEntityCache {
    private static final Map<LivingEntityRenderState, LivingEntity> STATE_ENTITY = Collections.synchronizedMap(new WeakHashMap());

    private RenderStateEntityCache() {
    }

    public static void put(LivingEntityRenderState state, LivingEntity entity) {
        if (state == null || entity == null) {
            return;
        }
        STATE_ENTITY.put(state, entity);
    }

    public static LivingEntity get(LivingEntityRenderState state) {
        if (state == null) {
            return null;
        }
        return STATE_ENTITY.get(state);
    }
}