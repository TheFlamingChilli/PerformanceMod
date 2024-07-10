package com.theflamingchilli.performancemod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.model.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_TEXTURE0;
import static org.lwjgl.opengl.GL15.glActiveTexture;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class PerformanceModClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("PerformanceMod");
    private static final Logger LOGGER_KEYBIND = LoggerFactory.getLogger("PerformanceMod-KEYBIND");
    public static final Logger LOGGER_MIXIN = LoggerFactory.getLogger("PerformanceMod-MIXIN");
    public static final MinecraftClient client = MinecraftClient.getInstance();

    private static KeyBinding fastRenderingKeybinding;
    private static KeyBinding reloadShadersKeybinding;
    private static boolean entityRenderingIsPressed = false;
    private static boolean entityRenderingWasPressed = false;
    public static boolean fastRenderingToggle = false;
    private static boolean reloadShadersIsPressed = false;
    private static boolean reloadShadersWasPressed = false;
    private static boolean readyToRender = false;

    public static int shaderProgram;
    public static List<String> namesOfEntitiesToInstance;
    public static Map<String, VAO> vaos = new HashMap<>();

    @Override
    public void onInitializeClient() {
        fastRenderingKeybinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.performancemod.fastRenderingToggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_SCROLL_LOCK,
                "category.performancemod"
        ));
        LOGGER_KEYBIND.info("Created fastRenderingToggle keybind");

        reloadShadersKeybinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.performancemod.reloadFastRenderingShaders",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PAUSE,
                "category.performancemod"
        ));
        LOGGER_KEYBIND.info("Created reloadFastRenderingShaders keybind");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            entityRenderingIsPressed = fastRenderingKeybinding.isPressed();
            if (entityRenderingIsPressed && !entityRenderingWasPressed) {
                try {
                    keyPressEvent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            entityRenderingWasPressed = entityRenderingIsPressed;
        });
        LOGGER_KEYBIND.info("Registered entityRenderingToggle keybind");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            reloadShadersIsPressed = reloadShadersKeybinding.isPressed();
            if (reloadShadersIsPressed && !reloadShadersWasPressed) {
                reloadShaders();
            }
            reloadShadersWasPressed = reloadShadersIsPressed;
        });
        LOGGER_KEYBIND.info("Registered reloadFastRenderingShaders keybind");
    }

    private static List<ModelPart> obtainModelParts(Entity entity) throws InvocationTargetException, IllegalAccessException {
        LivingEntityRenderer entityRenderer = (LivingEntityRenderer) client.getEntityRenderDispatcher().getRenderer(entity);
        QuadrupedEntityModel model = (QuadrupedEntityModel) entityRenderer.getModel();
        Method getTexturedModelData;
        TexturedModelData data = null;

        /*
        When Sinytra Connector translates the mod to forge, the getTexturedModelData method is translated to m_170800_,
        so we must account for both method names when obtaining the model parts.
         */
        String[] methodNames = {
                "getTexturedModelData", // fabric mapping
                "m_170800_"             // forge mapping
        };
        for (String methodName : methodNames) {
            try {
                getTexturedModelData = model.getClass().getDeclaredMethod(methodName);
                data = (TexturedModelData) getTexturedModelData.invoke(null);
                break;
            } catch (NoSuchMethodException e) {
                try {
                    getTexturedModelData = model.getClass().getDeclaredMethod(methodName, Dilation.class);
                    data = (TexturedModelData) getTexturedModelData.invoke(null, Dilation.NONE);
                    break;
                } catch (Exception e1) {
                    continue;
                }
            }
        }

        assert data != null : "Failed to obtain model data";
        ModelData modelData = data.data;
        ModelPartData root = modelData.getRoot();
        ModelPart head = root.getChild(EntityModelPartNames.HEAD).createPart(64, 32);
        ModelPart body = root.getChild(EntityModelPartNames.BODY).createPart(64, 32);
        ModelPart leg1 = root.getChild(EntityModelPartNames.RIGHT_HIND_LEG).createPart(64, 32);
        ModelPart leg2 = root.getChild(EntityModelPartNames.LEFT_HIND_LEG).createPart(64, 32);
        ModelPart leg3 = root.getChild(EntityModelPartNames.RIGHT_FRONT_LEG).createPart(64, 32);
        ModelPart leg4 = root.getChild(EntityModelPartNames.LEFT_FRONT_LEG).createPart(64, 32);
        return List.of(head, body, leg1, leg2, leg3, leg4);
    }

    private static void keyPressEvent() {
        long start = PerfTimer.start();
        namesOfEntitiesToInstance = List.of(
                "Cow", "Goat", "Panda", "Pig", "Polar Bear", "Sheep", "Turtle"
        );
        fastRenderingToggle = !fastRenderingToggle;
        String booleanText = fastRenderingToggle ? "True" : "False";
        Formatting textFormatting = fastRenderingToggle ? Formatting.GREEN : Formatting.RED;
        client.player.sendMessage(Text.literal("Fast rendering: ").append(Text.literal(booleanText).formatted(textFormatting)));
        if (!fastRenderingToggle) {
            for (VAO vao : vaos.values()) vao.cleanup();
            vaos.clear();
            readyToRender = false;
            return;
        }
        try {
            for (Entity entity : client.world.getEntities()) {
                String entityName = entity.getName().getString();
                if (namesOfEntitiesToInstance.contains(entityName)) {
                    if (vaos.containsKey(entityName)) continue;
                    List<ModelPart> parts = obtainModelParts(entity);
                    VAO vao = new VAO(parts, entityName)
                            .createVerticesBuffer()
                            .setTexture(entity)
                            .createUVCoords()
                            .createInstanceTransformBuffer()
                            .createBooleansBuffer()
                            .build();
                    vaos.put(entityName, vao);
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.info("EXCEPTION:\n" + sw);
        }
        LOGGER.info("VAOs: " + vaos.size());
        PerfTimer.end(start, "Keypress event");
        readyToRender = true;
    }

    private static void reloadShaders() {
        if (glIsShader(shaderProgram)) glDeleteShader(shaderProgram);
        createShaderProgram("vertex.vsh", "fragment.fsh");
        LOGGER.info("Reloaded fast rendering shaders");
        client.player.sendMessage(Text.literal("Reloaded fast rendering shaders"));
    }

    public static void updateInstances(float tickDelta) {
        if (!readyToRender) return;
        for (VAO vao : vaos.values()) {
            vao.instancePositions.clear();
            vao.instanceRotations.clear();
        }
        for (Entity entity : client.world.getEntities()) {
            String entityName = entity.getName().getString();
            if (namesOfEntitiesToInstance.contains(entityName)) {
                double d = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
                double e = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
                double f = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
                VAO vao = vaos.get(entityName);
                if (vao == null) continue;
                vao.addInstanceTransform(
                        (float) d,
                        (float) e,
                        (float) f,
                        (float) Math.toRadians(entity.getBodyYaw() + 180),
                        (float) Math.toRadians(entity.getPitch()),
                        (float) Math.toRadians(entity.getHeadYaw() - entity.getBodyYaw())
                );
            }
        }
    }

    public static void renderEntitiesInstanced(MatrixStack matrixStack) {
        if (!readyToRender) return;
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        matrixStack.push();
        Vec3d campos = client.getBlockEntityRenderDispatcher().camera.getPos();
        matrixStack.translate(-campos.x, -campos.y, -campos.z);
        Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
        Matrix4f projectionMatrix = client.gameRenderer.getBasicProjectionMatrix(client.options.getFov().getValue() * client.player.getFovMultiplier());

        glUseProgram(shaderProgram);
        setShaderUniforms(viewMatrix, projectionMatrix, 0);

        for (VAO vao : vaos.values()) {
            int textureSamplerLocation = glGetUniformLocation(shaderProgram, "headOffset");
            glUniform3f(textureSamplerLocation, vao.headOffset.x, vao.headOffset.y, vao.headOffset.z);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, vao.texture.getGlId());
            vao.updateInstanceTransforms();
            glBindVertexArray(vao.ID);
            glDrawArraysInstanced(GL_TRIANGLES, 0, 36 * vao.numCuboids, vao.numInstances());
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        glUseProgram(0);
        matrixStack.pop();
        //glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    private static int createShaderFromFile(String filePath, int type) {
        try {
            InputStream inputStream = PerformanceModClient.class.getResourceAsStream("/shaders/" + filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String shaderSource = bufferedReader.lines().collect(Collectors.joining("\n"));
            int shader = glCreateShader(type);
            glShaderSource(shader, shaderSource);
            glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new RuntimeException("Failed to compile shader: " + glGetShaderInfoLog(shader));
            }
            LOGGER.info("Compiled shader from (/shaders/" + filePath + ")");
            return shader;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void createShaderProgram(String vertexFilePath, String fragmentFilePath) {
        int vertexShader = createShaderFromFile(vertexFilePath, GL_VERTEX_SHADER);
        int fragmentShader = createShaderFromFile(fragmentFilePath, GL_FRAGMENT_SHADER);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Failed to link shader program: " + glGetProgramInfoLog(shaderProgram));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private static void setShaderUniforms(Matrix4f viewMatrix, Matrix4f projectionMatrix, int textureUnitID) {
        int viewLocation = glGetUniformLocation(shaderProgram, "viewMatrix");
        FloatBuffer viewBuffer = BufferUtils.createFloatBuffer(16);
        viewMatrix.get(viewBuffer);
        glUniformMatrix4fv(viewLocation, false, viewBuffer);

        int projectionLocation = glGetUniformLocation(shaderProgram, "projectionMatrix");
        FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
        projectionMatrix.get(projectionBuffer);
        glUniformMatrix4fv(projectionLocation, false, projectionBuffer);

        int textureSamplerLocation = glGetUniformLocation(shaderProgram, "textureSampler");
        glUniform1i(textureSamplerLocation, textureUnitID);
    }
}
