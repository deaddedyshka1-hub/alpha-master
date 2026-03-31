package system.alpha.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import system.alpha.api.event.EventListener;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.client.PacketEvent;
import system.alpha.api.event.events.render.Render3DEvent;
import system.alpha.api.module.Category;
import system.alpha.api.module.Module;
import system.alpha.api.module.ModuleRegister;
import system.alpha.api.module.setting.BooleanSetting;
import system.alpha.api.module.setting.ColorSetting;
import system.alpha.api.module.setting.ModeSetting;
import system.alpha.api.module.setting.SliderSetting;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

@ModuleRegister(name = "TotemPops", category = Category.RENDER, description = "При потери тотема вверх взлетает ваш силуэт.")
public class TotemPopsModule extends Module {
    @Getter
    private static final TotemPopsModule instance = new TotemPopsModule();

    private final ModeSetting mode = new ModeSetting("Режим")
            .value("Angel")
            .values("Shatter", "Angel");

    private final BooleanSetting outline = new BooleanSetting("Обводка")
            .value(false);

    private final SliderSetting riseHeight = new SliderSetting("Высота подъема")
            .value(4.0f)
            .range(0.2f, 5.0f)
            .step(0.1f);

    private final SliderSetting duration = new SliderSetting("Время жизни")
            .value(3.0f)
            .range(0.2f, 6.0f)
            .step(0.1f);

    private final BooleanSetting showSelf = new BooleanSetting("Показывать на себе")
            .value(false);

    private final SliderSetting brightness = new SliderSetting("Яркость")
            .value(1.0f)
            .range(0.2f, 3.0f)
            .step(0.1f);

    private final ColorSetting color = new ColorSetting("Цвет")
            .value(new Color(94, 255, 69, 255));

    private final Deque<Ghost> ghosts = new ArrayDeque<>();
    private final Deque<Shatter> shatters = new ArrayDeque<>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public TotemPopsModule() {
        addSettings(mode, outline, riseHeight, duration, showSelf, brightness, color);
    }

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            Object p = packetFrom(event);
            if (!(p instanceof EntityStatusS2CPacket pkt)) return;
            if (!isTotemStatus(pkt)) return;

            if (mc.world == null) return;
            Entity ent = pkt.getEntity(mc.world);
            if (!(ent instanceof LivingEntity le)) return;

            if (!showSelf.getValue() && mc.player != null && le.getId() == mc.player.getId()) return;

            if (mode.getValue().equals("Shatter")) addShatter(le);
            else addGhost(le);
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;

            boolean sh = mode.getValue().equals("Shatter");
            boolean ol = outline.getValue();

            Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();

            if (!sh) {
                if (ghosts.isEmpty()) return;

                long now = System.currentTimeMillis();
                float lifeMs = duration.getValue().floatValue() * 1000.0f;
                float rise = riseHeight.getValue().floatValue();
                float bright = MathHelper.clamp(brightness.getValue().floatValue(), 0.2f, 3.0f);

                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.disableCull();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

                Iterator<Ghost> it = ghosts.iterator();
                while (it.hasNext()) {
                    Ghost g1 = it.next();
                    float t = (now - g1.startTime()) / Math.max(1.0f, lifeMs);
                    if (t >= 1.0f) {
                        it.remove();
                        continue;
                    }

                    float up = rise * (float) ease(MathHelper.clamp(t, 0.0f, 0.75f));
                    float a = (float) easeOutAlpha(MathHelper.clamp(t, 0.0f, 1.0f));
                    float alphaMul = MathHelper.clamp(a * bright, 0.0f, 0.95f);

                    Color baseColor = color.getValue();
                    int r = baseColor.getRed();
                    int g = baseColor.getGreen();
                    int b = baseColor.getBlue();
                    int aValue = MathHelper.clamp((int)(255 * alphaMul), 0, 255);
                    int argb = (aValue << 24) | (r << 16) | (g << 8) | b;

                    event.matrixStack().push();
                    // Переводим в координаты относительно камеры
                    event.matrixStack().translate(
                            g1.pos().x - cameraPos.x,
                            g1.pos().y + up - cameraPos.y,
                            g1.pos().z - cameraPos.z
                    );
                    event.matrixStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-g1.bodyYaw()));

                    renderBodyBoxes(event.matrixStack(), argb, ol);

                    event.matrixStack().pop();
                }

                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                return;
            }

            if (shatters.isEmpty()) return;

            long now = System.currentTimeMillis();
            float lifeMs = duration.getValue().floatValue() * 1000.0f;
            float power = MathHelper.clamp(riseHeight.getValue().floatValue(), 0.2f, 5.0f);
            float bright = MathHelper.clamp(brightness.getValue().floatValue(), 0.2f, 3.0f);

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            Iterator<Shatter> it = shatters.iterator();
            while (it.hasNext()) {
                Shatter s = it.next();
                float t = (now - s.startTime) / Math.max(1.0f, lifeMs);
                if (t >= 1.0f) {
                    it.remove();
                    continue;
                }

                float a = (float) alphaProfile(t);
                float alphaMul = MathHelper.clamp(a * bright, 0.0f, 0.95f);

                Color baseColor = color.getValue();
                int r = baseColor.getRed();
                int g = baseColor.getGreen();
                int b = baseColor.getBlue();
                int aValue = MathHelper.clamp((int)(255 * alphaMul), 0, 255);
                int argb = (aValue << 24) | (r << 16) | (g << 8) | b;

                float td = t * (lifeMs / 1000.0f);
                float gravity = 6.2f;

                event.matrixStack().push();
                // Переводим в координаты относительно камеры
                event.matrixStack().translate(
                        s.pos.x - cameraPos.x,
                        s.pos.y - cameraPos.y,
                        s.pos.z - cameraPos.z
                );
                event.matrixStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-s.bodyYaw));

                renderPart(event.matrixStack(), s, PartId.HEAD, td, power, gravity, argb, ol);
                renderPart(event.matrixStack(), s, PartId.BODY, td, power, gravity, argb, ol);
                renderPart(event.matrixStack(), s, PartId.ARM_L, td, power, gravity, argb, ol);
                renderPart(event.matrixStack(), s, PartId.ARM_R, td, power, gravity, argb, ol);
                renderPart(event.matrixStack(), s, PartId.LEG_L, td, power, gravity, argb, ol);
                renderPart(event.matrixStack(), s, PartId.LEG_R, td, power, gravity, argb, ol);

                event.matrixStack().pop();
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }));

        addEvents(packetEvent, renderEvent);
    }

    private void renderPart(MatrixStack matrices, Shatter s, PartId id, float td, float power, float gravity, int argb, boolean outlineOnly) {
        PartMotion m = s.parts[id.ordinal()];

        double ox = m.vx * td * power;
        double oy = m.vy * td * power - 0.5 * gravity * td * td;
        double oz = m.vz * td * power;

        float spin = m.spinDeg * td * 120.0f;
        float spin2 = m.spinDeg2 * td * 95.0f;

        matrices.push();
        matrices.translate(ox, oy, oz);

        if (id != PartId.BODY) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spin2));
        }

        renderPartBox(matrices, id, argb, outlineOnly);
        matrices.pop();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ghosts.clear();
        shatters.clear();
    }

    private void addGhost(LivingEntity player) {
        float td = mc.getRenderTickCounter().getTickDelta(false);

        double x = MathHelper.lerp(td, player.prevX, player.getX());
        double y = MathHelper.lerp(td, player.prevY, player.getY());
        double z = MathHelper.lerp(td, player.prevZ, player.getZ());

        float bodyYaw = MathHelper.lerpAngleDegrees(td, player.prevBodyYaw, player.bodyYaw);

        ghosts.addLast(new Ghost(new Vec3d(x, y, z), bodyYaw, System.currentTimeMillis()));
        while (ghosts.size() > 64) ghosts.pollFirst();
    }

    private void addShatter(LivingEntity player) {
        float td = mc.getRenderTickCounter().getTickDelta(false);

        double x = MathHelper.lerp(td, player.prevX, player.getX());
        double y = MathHelper.lerp(td, player.prevY, player.getY());
        double z = MathHelper.lerp(td, player.prevZ, player.getZ());

        float bodyYaw = MathHelper.lerpAngleDegrees(td, player.prevBodyYaw, player.bodyYaw);

        long st = System.currentTimeMillis();
        int seed = mixSeed(player.getId(), st);

        PartMotion[] parts = new PartMotion[6];

        float yawRad = (float) Math.toRadians(bodyYaw);
        float fx = -MathHelper.sin(yawRad);
        float fz = MathHelper.cos(yawRad);
        float rx = fz;
        float rz = -fx;

        parts[PartId.HEAD.ordinal()] = motion(seed, 0, fx * 0.25f, 1.35f, fz * 0.25f, rx * 0.06f, rz * 0.06f);
        parts[PartId.BODY.ordinal()] = motion(seed, 1, fx * 0.10f, 0.55f, fz * 0.10f, rx * 0.03f, rz * 0.03f);

        parts[PartId.ARM_L.ordinal()] = motion(seed, 2, -rx * 0.95f + fx * 0.10f, 0.75f, -rz * 0.95f + fz * 0.10f, -rx * 0.12f, -rz * 0.12f);
        parts[PartId.ARM_R.ordinal()] = motion(seed, 3, rx * 0.95f + fx * 0.10f, 0.75f, rz * 0.95f + fz * 0.10f, rx * 0.12f, rz * 0.12f);

        parts[PartId.LEG_L.ordinal()] = motion(seed, 4, -rx * 0.45f + fx * 0.35f, 0.60f, -rz * 0.45f + fz * 0.35f, -rx * 0.06f, -rz * 0.06f);
        parts[PartId.LEG_R.ordinal()] = motion(seed, 5, rx * 0.45f + fx * 0.35f, 0.60f, rz * 0.45f + fz * 0.35f, rx * 0.06f, rz * 0.06f);

        shatters.addLast(new Shatter(new Vec3d(x, y, z), bodyYaw, st, parts));
        while (shatters.size() > 64) shatters.pollFirst();
    }

    private void renderBodyBoxes(MatrixStack ms, int argb, boolean outlineOnly) {
        Matrix4f m = ms.peek().getPositionMatrix();

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        if (!outlineOnly) {
            drawBoxFill(bb, m, -0.25f, 1.45f, -0.25f, 0.25f, 1.95f, 0.25f, argb);
            drawBoxFill(bb, m, -0.30f, 0.90f, -0.15f, 0.30f, 1.45f, 0.15f, argb);

            drawBoxFill(bb, m, -0.55f, 0.95f, -0.12f, -0.30f, 1.45f, 0.12f, argb);
            drawBoxFill(bb, m, 0.30f, 0.95f, -0.12f, 0.55f, 1.45f, 0.12f, argb);

            drawBoxFill(bb, m, -0.25f, 0.00f, -0.12f, 0.00f, 0.90f, 0.12f, argb);
            drawBoxFill(bb, m, 0.00f, 0.00f, -0.12f, 0.25f, 0.90f, 0.12f, argb);

            BufferRenderer.drawWithGlobalProgram(bb.end());
            return;
        }

        float t = 0.02f;

        drawBoxOutlineQuads(bb, m, -0.25f, 1.45f, -0.25f, 0.25f, 1.95f, 0.25f, argb, t);
        drawBoxOutlineQuads(bb, m, -0.30f, 0.90f, -0.15f, 0.30f, 1.45f, 0.15f, argb, t);

        drawBoxOutlineQuads(bb, m, -0.55f, 0.95f, -0.12f, -0.30f, 1.45f, 0.12f, argb, t);
        drawBoxOutlineQuads(bb, m, 0.30f, 0.95f, -0.12f, 0.55f, 1.45f, 0.12f, argb, t);

        drawBoxOutlineQuads(bb, m, -0.25f, 0.00f, -0.12f, 0.00f, 0.90f, 0.12f, argb, t);
        drawBoxOutlineQuads(bb, m, 0.00f, 0.00f, -0.12f, 0.25f, 0.90f, 0.12f, argb, t);

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private void renderPartBox(MatrixStack ms, PartId id, int argb, boolean outlineOnly) {
        Matrix4f m = ms.peek().getPositionMatrix();

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        if (!outlineOnly) {
            switch (id) {
                case HEAD:
                    drawBoxFill(bb, m, -0.25f, 1.45f, -0.25f, 0.25f, 1.95f, 0.25f, argb);
                    break;
                case BODY:
                    drawBoxFill(bb, m, -0.30f, 0.90f, -0.15f, 0.30f, 1.45f, 0.15f, argb);
                    break;
                case ARM_L:
                    drawBoxFill(bb, m, -0.55f, 0.95f, -0.12f, -0.30f, 1.45f, 0.12f, argb);
                    break;
                case ARM_R:
                    drawBoxFill(bb, m, 0.30f, 0.95f, -0.12f, 0.55f, 1.45f, 0.12f, argb);
                    break;
                case LEG_L:
                    drawBoxFill(bb, m, -0.25f, 0.00f, -0.12f, 0.00f, 0.90f, 0.12f, argb);
                    break;
                case LEG_R:
                    drawBoxFill(bb, m, 0.00f, 0.00f, -0.12f, 0.25f, 0.90f, 0.12f, argb);
                    break;
            }

            BufferRenderer.drawWithGlobalProgram(bb.end());
            return;
        }

        float t = 0.02f;

        switch (id) {
            case HEAD:
                drawBoxOutlineQuads(bb, m, -0.25f, 1.45f, -0.25f, 0.25f, 1.95f, 0.25f, argb, t);
                break;
            case BODY:
                drawBoxOutlineQuads(bb, m, -0.30f, 0.90f, -0.15f, 0.30f, 1.45f, 0.15f, argb, t);
                break;
            case ARM_L:
                drawBoxOutlineQuads(bb, m, -0.55f, 0.95f, -0.12f, -0.30f, 1.45f, 0.12f, argb, t);
                break;
            case ARM_R:
                drawBoxOutlineQuads(bb, m, 0.30f, 0.95f, -0.12f, 0.55f, 1.45f, 0.12f, argb, t);
                break;
            case LEG_L:
                drawBoxOutlineQuads(bb, m, -0.25f, 0.00f, -0.12f, 0.00f, 0.90f, 0.12f, argb, t);
                break;
            case LEG_R:
                drawBoxOutlineQuads(bb, m, 0.00f, 0.00f, -0.12f, 0.25f, 0.90f, 0.12f, argb, t);
                break;
        }

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    // Статические вспомогательные методы
    private static void drawBoxFill(BufferBuilder bb, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        bb.vertex(m, x1, y1, z2).color(c);
        bb.vertex(m, x2, y1, z2).color(c);
        bb.vertex(m, x2, y2, z2).color(c);
        bb.vertex(m, x1, y2, z2).color(c);

        bb.vertex(m, x2, y1, z1).color(c);
        bb.vertex(m, x1, y1, z1).color(c);
        bb.vertex(m, x1, y2, z1).color(c);
        bb.vertex(m, x2, y2, z1).color(c);

        bb.vertex(m, x1, y1, z1).color(c);
        bb.vertex(m, x1, y1, z2).color(c);
        bb.vertex(m, x1, y2, z2).color(c);
        bb.vertex(m, x1, y2, z1).color(c);

        bb.vertex(m, x2, y1, z2).color(c);
        bb.vertex(m, x2, y1, z1).color(c);
        bb.vertex(m, x2, y2, z1).color(c);
        bb.vertex(m, x2, y2, z2).color(c);

        bb.vertex(m, x1, y2, z2).color(c);
        bb.vertex(m, x2, y2, z2).color(c);
        bb.vertex(m, x2, y2, z1).color(c);
        bb.vertex(m, x1, y2, z1).color(c);

        bb.vertex(m, x1, y1, z1).color(c);
        bb.vertex(m, x2, y1, z1).color(c);
        bb.vertex(m, x2, y1, z2).color(c);
        bb.vertex(m, x1, y1, z2).color(c);
    }

    private static void drawBoxOutlineQuads(BufferBuilder bb, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int c, float t) {
        float xm = (x1 + x2) * 0.5f;
        float ym = (y1 + y2) * 0.5f;
        float zm = (z1 + z2) * 0.5f;

        float ex1 = x1, ex2 = x2, ey1 = y1, ey2 = y2, ez1 = z1, ez2 = z2;

        edgeX(bb, m, ex1, ex2, ey1, ez1, c, t);
        edgeX(bb, m, ex1, ex2, ey1, ez2, c, t);
        edgeX(bb, m, ex1, ex2, ey2, ez1, c, t);
        edgeX(bb, m, ex1, ex2, ey2, ez2, c, t);

        edgeY(bb, m, ey1, ey2, ex1, ez1, c, t);
        edgeY(bb, m, ey1, ey2, ex2, ez1, c, t);
        edgeY(bb, m, ey1, ey2, ex1, ez2, c, t);
        edgeY(bb, m, ey1, ey2, ex2, ez2, c, t);

        edgeZ(bb, m, ez1, ez2, ex1, ey1, c, t);
        edgeZ(bb, m, ez1, ez2, ex2, ey1, c, t);
        edgeZ(bb, m, ez1, ez2, ex1, ey2, c, t);
        edgeZ(bb, m, ez1, ez2, ex2, ey2, c, t);
    }

    private static void edgeX(BufferBuilder bb, Matrix4f m, float x1, float x2, float y, float z, int c, float t) {
        drawBoxFill(bb, m, x1, y - t, z - t, x2, y + t, z + t, c);
    }

    private static void edgeY(BufferBuilder bb, Matrix4f m, float y1, float y2, float x, float z, int c, float t) {
        drawBoxFill(bb, m, x - t, y1, z - t, x + t, y2, z + t, c);
    }

    private static void edgeZ(BufferBuilder bb, Matrix4f m, float z1, float z2, float x, float y, int c, float t) {
        drawBoxFill(bb, m, x - t, y - t, z1, x + t, y + t, z2, c);
    }

    private static PartMotion motion(int seed, int i, float bx, float by, float bz, float jx, float jz) {
        float r1 = rand(seed, i * 11 + 3) * 2.0f - 1.0f;
        float r2 = rand(seed, i * 11 + 7) * 2.0f - 1.0f;
        float r3 = rand(seed, i * 11 + 9) * 2.0f - 1.0f;

        float vx = bx + jx + r1 * 0.12f;
        float vy = by + rand(seed, i * 11 + 5) * 0.35f;
        float vz = bz + jz + r2 * 0.12f;

        float s1 = r3;
        float s2 = rand(seed, i * 13 + 1) * 2.0f - 1.0f;

        float len = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (len < 0.001f) len = 1.0f;
        vx /= len;
        vy /= len;
        vz /= len;

        return new PartMotion(vx, vy, vz, s1, s2);
    }

    private static double ease(double t) {
        double inv = 1.0D - t;
        return 1.0D - inv * inv * inv;
    }

    private static double easeOutAlpha(double t) {
        double inv = 1.0D - t;
        return 0.75D * (1.0D - inv * inv * inv);
    }

    private static double alphaProfile(double t) {
        double in = MathHelper.clamp(t / 0.10, 0.0, 1.0);
        double out = MathHelper.clamp((t - 0.65) / 0.35, 0.0, 1.0);
        double aIn = 1.0 - Math.pow(1.0 - in, 3.0);
        double aOut = 1.0 - out;
        return 0.85 * aIn * aOut;
    }

    private static int mixSeed(int id, long st) {
        long x = (st ^ (st >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        return (int) (x ^ (x >>> 32) ^ id * 0x9E3779B9);
    }

    private static float rand(int seed, int k) {
        int x = seed ^ (k * 0x27d4eb2d);
        x ^= (x >>> 15);
        x *= 0x85ebca6b;
        x ^= (x >>> 13);
        x *= 0xc2b2ae35;
        x ^= (x >>> 16);
        return (x & 0x00FFFFFF) / 16777215.0f;
    }

    // Вспомогательные методы для рефлексии (совместимость)
    private static boolean isTotemStatus(EntityStatusS2CPacket pkt) {
        try {
            Method m = pkt.getClass().getMethod("getStatus");
            Object v = m.invoke(pkt);
            if (v instanceof Byte b) return (b & 0xFF) == 35;
            if (v instanceof Number n) return (n.intValue() & 0xFF) == 35;
        } catch (Throwable ignored) {
        }
        try {
            Method m = pkt.getClass().getMethod("getStatusByte");
            Object v = m.invoke(pkt);
            if (v instanceof Byte b) return (b & 0xFF) == 35;
            if (v instanceof Number n) return (n.intValue() & 0xFF) == 35;
        } catch (Throwable ignored) {
        }
        try {
            Method m = pkt.getClass().getMethod("status");
            Object v = m.invoke(pkt);
            if (v instanceof Byte b) return (b & 0xFF) == 35;
            if (v instanceof Number n) return (n.intValue() & 0xFF) == 35;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Object packetFrom(PacketEvent.PacketEventData event) {
        try {
            Method m = event.getClass().getMethod("getPacket");
            return m.invoke(event);
        } catch (Throwable ignored) {
        }
        try {
            Method m = event.getClass().getMethod("packet");
            return m.invoke(event);
        } catch (Throwable ignored) {
        }
        try {
            Method m = event.getClass().getMethod("get");
            return m.invoke(event);
        } catch (Throwable ignored) {
        }
        return null;
    }

    // Внутренние классы
    private static class Ghost {
        private final Vec3d pos;
        private final float bodyYaw;
        private final long startTime;

        public Ghost(Vec3d pos, float bodyYaw, long startTime) {
            this.pos = pos;
            this.bodyYaw = bodyYaw;
            this.startTime = startTime;
        }

        public Vec3d pos() { return pos; }
        public float bodyYaw() { return bodyYaw; }
        public long startTime() { return startTime; }
    }

    private enum PartId {
        HEAD, BODY, ARM_L, ARM_R, LEG_L, LEG_R
    }

    private record PartMotion(float vx, float vy, float vz, float spinDeg, float spinDeg2) {}

    private static final class Shatter {
        final Vec3d pos;
        final float bodyYaw;
        final long startTime;
        final PartMotion[] parts;

        Shatter(Vec3d pos, float bodyYaw, long startTime, PartMotion[] parts) {
            this.pos = pos;
            this.bodyYaw = bodyYaw;
            this.startTime = startTime;
            this.parts = parts;
        }
    }
}