package system.alpha.api.utils.color;

public final class ColorHit {
    private ColorHit() {}
    public static final ThreadLocal<Boolean> SHOULD_TINT = ThreadLocal.withInitial(() -> false);
}


