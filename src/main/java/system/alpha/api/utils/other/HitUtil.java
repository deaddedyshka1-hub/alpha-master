package system.alpha.api.utils.other;

public final class HitUtil {
    private HitUtil() {}

    public static final ThreadLocal<Boolean> SHOULD_TINT = ThreadLocal.withInitial(() -> false);
}