package system.alpha.client.services;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import system.alpha.api.system.interfaces.QuickImports;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.stream.Collectors;

public class ShaderService implements QuickImports {
    private static ShaderService instance;
    private int shaderProgram = -1;
    private float time = 0;
    private long lastTime = System.currentTimeMillis();
    private boolean loaded = false;

    public static ShaderService getInstance() {
        if (instance == null) {
            instance = new ShaderService();
        }
        return instance;
    }

    public void load() {
        try {
            String vertexSource = loadShaderSource("shaders/mainmenu/background.vert");
            String fragmentSource = loadShaderSource("shaders/mainmenu/background.frag");
            shaderProgram = createShaderProgram(vertexSource, fragmentSource);
            loaded = true;
            System.out.println("[MainMenuShaderService] Shader loaded successfully!");
        } catch (Exception e) {
            System.err.println("[MainMenuShaderService] Failed to load shader!");
            e.printStackTrace();
            loaded = false;
        }
    }

    private String loadShaderSource(String path) {
        try {
            Identifier id = Identifier.of("alphavisuals", path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(mc.getResourceManager().open(id)));
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

    private int compileShader(int type, String source) {
        int shader = GlStateManager.glCreateShader(type);
        GlStateManager.glShaderSource(shader, source);
        GlStateManager.glCompileShader(shader);

        if (GlStateManager.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GlStateManager.glGetShaderInfoLog(shader, 1024);
            throw new RuntimeException("Failed to compile shader: " + log);
        }
        return shader;
    }

    private int createShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

        int program = GlStateManager.glCreateProgram();
        GlStateManager.glAttachShader(program, vertexShader);
        GlStateManager.glAttachShader(program, fragmentShader);
        GlStateManager.glLinkProgram(program);

        if (GlStateManager.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            String log = GlStateManager.glGetProgramInfoLog(program, 1024);
            throw new RuntimeException("Failed to link shader: " + log);
        }

        GlStateManager.glDeleteShader(vertexShader);
        GlStateManager.glDeleteShader(fragmentShader);

        return program;
    }

    public void updateTime() {
        long currentTime = System.currentTimeMillis();
        time += (currentTime - lastTime) * 0.001f;
        lastTime = currentTime;
        if (time > 1000f) time -= 1000f;
    }

    public void renderFullscreenQuad(float fade) {
        if (!loaded || shaderProgram == -1) return;

        updateTime();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        buffer.vertex(0, 0, 0);
        buffer.vertex(screenWidth, 0, 0);
        buffer.vertex(screenWidth, screenHeight, 0);
        buffer.vertex(0, screenHeight, 0);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        useShader(() -> {
            setUniformFloat("Time", time);
            setUniformFloat("Fade", fade);
            setUniformVec2("Resolution", screenWidth, screenHeight);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        });

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void useShader(Runnable renderCode) {
        GlStateManager._glUseProgram(shaderProgram);
        renderCode.run();
        GlStateManager._glUseProgram(0);
    }

    private void setUniformFloat(String name, float value) {
        int location = GlStateManager._glGetUniformLocation(shaderProgram, name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(1);
                buffer.put(0, value);
                GlStateManager._glUniform1(location, buffer);
            }
        }
    }

    private void setUniformVec2(String name, float x, float y) {
        int location = GlStateManager._glGetUniformLocation(shaderProgram, name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(2);
                buffer.put(0, x);
                buffer.put(1, y);
                GlStateManager._glUniform2(location, buffer);
            }
        }
    }

    public void unload() {
        if (shaderProgram != -1) {
            GlStateManager.glDeleteProgram(shaderProgram);
            shaderProgram = -1;
        }
        loaded = false;
    }

    public boolean isLoaded() {
        return loaded;
    }
}