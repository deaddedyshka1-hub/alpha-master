package system.alpha.api.utils.render.display.hitbox;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import system.alpha.api.system.interfaces.QuickImports;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HitboxQueue implements QuickImports {

    private static final List<BoxTask> boxTasks = new ArrayList<>();
    private static final List<LineTask> lineTasks = new ArrayList<>();

    public static void addHitbox(Box box, Color fillColor, Color outlineColor, float lineWidth) {
        boxTasks.add(new BoxTask(box, fillColor, outlineColor, lineWidth));
    }

    public static void addHitboxFill(Box box, Color color, float lineWidth) {
        boxTasks.add(new BoxTask(box, color, null, lineWidth));
    }

    public static void addHitboxOutline(Box box, Color color, float lineWidth) {
        boxTasks.add(new BoxTask(box, null, color, lineWidth));
    }

    public static void addHitboxLine(Vec3d start, Vec3d end, Color color, float lineWidth) {
        lineTasks.add(new LineTask(start, end, color, lineWidth));
    }

    public static void renderQueuedHitboxes(MatrixStack matrices) {
        if (boxTasks.isEmpty() && lineTasks.isEmpty()) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        boolean wasDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        renderAllFills(matrix, camera);
        renderAllOutlines(matrix, camera);
        renderAllLines(matrix, camera);

        boxTasks.clear();
        lineTasks.clear();

        if (wasCull) RenderSystem.enableCull();
        if (!wasBlend) RenderSystem.disableBlend();
        if (!wasDepthTest) RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void renderAllFills(Matrix4f matrix, Vec3d camera) {
        boolean hasFills = false;
        for (BoxTask task : boxTasks) {
            if (task.fillColor != null && task.fillColor.getAlpha() > 0) {
                hasFills = true;
                break;
            }
        }

        if (!hasFills) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (BoxTask task : boxTasks) {
            if (task.fillColor != null && task.fillColor.getAlpha() > 0) {
                addFillVertices(buffer, matrix, camera, task.box, task.fillColor);
            }
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void renderAllOutlines(Matrix4f matrix, Vec3d camera) {
        boolean hasOutlines = false;
        for (BoxTask task : boxTasks) {
            if (task.outlineColor != null && task.outlineColor.getAlpha() > 0) {
                hasOutlines = true;
                break;
            }
        }

        if (!hasOutlines) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (BoxTask task : boxTasks) {
            if (task.outlineColor != null && task.outlineColor.getAlpha() > 0) {
                addOutlineVertices(buffer, matrix, camera, task.box, task.outlineColor);
            }
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float lineWidth = getMaxOutlineWidth();
        GL11.glLineWidth(lineWidth);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private static float getMaxOutlineWidth() {
        float maxWidth = 1.5f;
        for (BoxTask task : boxTasks) {
            if (task.outlineColor != null && task.outlineColor.getAlpha() > 0) {
                if (task.lineWidth > maxWidth) {
                    maxWidth = task.lineWidth;
                }
            }
        }
        return maxWidth;
    }

    private static void renderAllLines(Matrix4f matrix, Vec3d camera) {
        if (lineTasks.isEmpty()) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (LineTask task : lineTasks) {
            addLineVertices(buffer, matrix, camera, task.start, task.end, task.color);
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float maxLineWidth = getMaxLineWidth();
        GL11.glLineWidth(maxLineWidth);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private static float getMaxLineWidth() {
        float maxWidth = 1.5f;
        for (LineTask task : lineTasks) {
            if (task.lineWidth > maxWidth) {
                maxWidth = task.lineWidth;
            }
        }
        return maxWidth;
    }

    private static void addFillVertices(BufferBuilder buffer, Matrix4f matrix, Vec3d camera, Box box, Color color) {
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
    }

    private static void addOutlineVertices(BufferBuilder buffer, Matrix4f matrix, Vec3d camera, Box box, Color color) {
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
    }

    private static void addLineVertices(BufferBuilder buffer, Matrix4f matrix, Vec3d camera, Vec3d start, Vec3d end, Color color) {
        Vec3d startLocal = start.subtract(camera);
        Vec3d endLocal = end.subtract(camera);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        buffer.vertex(matrix, (float) startLocal.x, (float) startLocal.y, (float) startLocal.z)
                .color(r, g, b, a);
        buffer.vertex(matrix, (float) endLocal.x, (float) endLocal.y, (float) endLocal.z)
                .color(r, g, b, a);
    }

    private static class BoxTask {
        final Box box;
        final Color fillColor;
        final Color outlineColor;
        final float lineWidth;

        BoxTask(Box box, Color fillColor, Color outlineColor, float lineWidth) {
            this.box = box;
            this.fillColor = fillColor;
            this.outlineColor = outlineColor;
            this.lineWidth = lineWidth;
        }
    }

    private static class LineTask {
        final Vec3d start;
        final Vec3d end;
        final Color color;
        final float lineWidth;

        LineTask(Vec3d start, Vec3d end, Color color, float lineWidth) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.lineWidth = lineWidth;
        }
    }
}