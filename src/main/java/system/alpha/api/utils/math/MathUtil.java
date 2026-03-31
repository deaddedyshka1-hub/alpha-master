package system.alpha.api.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import system.alpha.api.system.interfaces.QuickImports;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class MathUtil implements QuickImports {
    private final RandomUtil randomUtil = new RandomUtil();

    public double getEntityBPS(Entity entity) {
        return Math.hypot(entity.prevX - entity.getX(), entity.prevZ - entity.getZ()) * 20;
    }

    /**
     * Линейная интерполяция (lerp) между двумя значениями
     * @param start начальное значение
     * @param end конечное значение
     * @param t коэффициент интерполяции (от 0.0 до 1.0)
     * @return интерполированное значение
     */
    public double lerp(double start, double end, double t) {
        t = clamp(t, 0.0, 1.0);
        return start + (end - start) * t;
    }

    /**
     * Линейная интерполяция (lerp) между двумя значениями
     * @param start начальное значение
     * @param end конечное значение
     * @param t коэффициент интерполяции (от 0.0f до 1.0f)
     * @return интерполированное значение
     */
    public float lerp(float start, float end, float t) {
        t = clamp(t, 0.0f, 1.0f);
        return start + (end - start) * t;
    }

    /**
     * Ограничение значения в заданных пределах
     * @param value значение для ограничения
     * @param min минимальное значение
     * @param max максимальное значение
     * @return ограниченное значение
     */
    public double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Ограничение значения в заданных пределах
     * @param value значение для ограничения
     * @param min минимальное значение
     * @param max максимальное значение
     * @return ограниченное значение
     */
    public float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * Ограничение значения в заданных пределах
     * @param value значение для ограничения
     * @param min минимальное значение
     * @param max максимальное значение
     * @return ограниченное значение
     */
    public int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public float interpolate(double oldValue, double newValue) {
        return (float) (oldValue + (newValue - oldValue) * mc.getRenderTickCounter().getTickDelta(false));
    }

    public double interpolate(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    public float interpolate(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    public int interpolate(int oldValue, int newValue, float interpolationValue) {
        return (int)(oldValue + (newValue - oldValue) * interpolationValue);
    }

    public double round(double value, double step) {
        double v = (double) Math.round(value / step) * step;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public float round(float value, float step) {
        double v = (double) Math.round(value / step) * step;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public int randomInRange(int min, int max) {
        return randomUtil.randomInRange(min, max);
    }

    public float randomInRange(float min, float max) {
        return randomUtil.randomInRange(min, max);
    }

    public double randomInRange(double min, double max) {
        return randomUtil.randomInRange(min, max);
    }
}