package com.theflamingchilli.performancemod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.SheepEntityModel;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class VAO {

    private static final Logger LOGGER = LoggerFactory.getLogger("PerformanceMod-VAO");

    public List<ModelPart> modelParts;
    public int numCuboids;
    public int ID;
    public int vertexBufferID;
    public int indexBufferID;
    public int instancePositionBufferID;
    public int instanceRotationBufferID;
    public int uvCoordsID;
    public int booleansBufferID;
    public List<Float> instancePositions = new ArrayList<>();
    public List<Float> instanceRotations = new ArrayList<>();
    public AbstractTexture texture;
    public Vector3f headOffset;
    public int totalBytes;

    private static int[] indices = {
            2, 3, 7, 7, 6, 2, // bottom
            5, 4, 0, 0, 1, 5,  // top
            1, 0, 3, 3, 2, 1, // front
            0, 4, 7, 7, 3, 0, // left
            4, 5, 6, 6, 7, 4, // back
            5, 1, 2, 2, 6, 5 // right
    };

    public VAO(List<ModelPart> modelParts, String entityName) {
        this.ID = glGenVertexArrays();
        glBindVertexArray(this.ID);
        this.modelParts = modelParts;
        totalBytes = 0;
        LOGGER.info("Created new VAO for entity: " + entityName);
    }

    private List<Float> getVertices() {
        List<Float> vertices = new ArrayList<>();
        for (ModelPart part : modelParts) {
            Matrix4f rotationMatrix = new Matrix4f().rotationXYZ(part.pitch, part.yaw, part.roll);
            this.numCuboids += part.cuboids.size();
            for (ModelPart.Cuboid cuboid : part.cuboids) {
                for (int i = 0; i < cuboid.sides.length; i++) {
                    ModelPart.Quad side = cuboid.sides[i];
                    List<Vector3f> rotatedVertices = new ArrayList<>();
                    for (ModelPart.Vertex vertex : side.vertices) {
                        Vector3f v = vertex.pos;
                        float x = v.x / 16.0f;
                        float y = v.y / 16.0f;
                        float z = v.z / 16.0f;
                        Vector3f scaled = new Vector3f(x, y, z);
                        scaled = rotationMatrix.transformPosition(scaled);
                        rotatedVertices.add(scaled);
                    }
                    int[] vertexOrder = new int[] {0, 1, 2, 2, 3, 0};
                    for (int i1 : vertexOrder) {
                        Vector3f v = rotatedVertices.get(i1);
                        vertices.add(v.x + (part.pivotX / 16.0f));
                        vertices.add(v.y + (part.pivotY / 16.0f));
                        vertices.add(v.z + (part.pivotZ / 16.0f));
                    }
                }
            }
        }
        return vertices;
    }

    public VAO createVerticesBuffer() {
        List<Float> vertices = getVertices();
        vertexBufferID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID);
        float[] verticesArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) verticesArray[i] = vertices.get(i);
        glBufferData(GL_ARRAY_BUFFER, verticesArray, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        int numBytes = verticesArray.length * Float.BYTES;
        totalBytes += numBytes;
        LOGGER.info("Uploaded vertices to VAO (ID:" + ID + ", " + numBytes + " bytes)");
        return this;
    }

    public VAO createIndicesBuffer() {
//        indices = new int[] {
//                1, 0, 3, 3, 2, 1, // front
//                0, 4, 7, 7, 3, 0, // left
//                4, 5, 6, 6, 7, 4, // back
//                5, 1, 2, 2, 6, 5, // right
//                2, 3, 7, 7, 6, 2, // bottom
//                5, 4, 0, 0, 1, 5  // top
//        };

        indexBufferID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
        List<Byte> indexes = new ArrayList<>();
        for (int i = 0, j = 0; i < numCuboids; i++, j+=8) {
            for (int index : indices) indexes.add((byte) (index + j));
        }
        byte[] indices = new byte[indexes.size()];
        for (int i = 0; i < indexes.size(); i++) indices[i] = indexes.get(i);
        ByteBuffer buffer = BufferUtils.createByteBuffer(indexes.size());
        buffer.put(indices);
        buffer.flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        int numBytes = indices.length * Byte.BYTES;
        totalBytes += numBytes;
        LOGGER.info("Uploaded indices to VAO (ID:" + ID + ", " + numBytes + " bytes)");
        return this;
    }

    private List<Float> getUVs() {
        List<Float> uvCoords = new ArrayList<>();
        for (ModelPart part : modelParts) {
            for (ModelPart.Cuboid cuboid : part.cuboids) {
                for (ModelPart.Quad side : cuboid.sides) {
                    uvCoords.add(side.vertices[0].u);
                    uvCoords.add(side.vertices[0].v);
                    uvCoords.add(side.vertices[1].u);
                    uvCoords.add(side.vertices[1].v);
                    uvCoords.add(side.vertices[2].u);
                    uvCoords.add(side.vertices[2].v);
                    uvCoords.add(side.vertices[2].u);
                    uvCoords.add(side.vertices[2].v);
                    uvCoords.add(side.vertices[3].u);
                    uvCoords.add(side.vertices[3].v);
                    uvCoords.add(side.vertices[0].u);
                    uvCoords.add(side.vertices[0].v);
                }
            }
        }
        return uvCoords;
    }

    public VAO createUVCoords() {
        uvCoordsID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, uvCoordsID);
        List<Float> uvs = getUVs();
        int numUVs = 36 * 2 * numCuboids; // 2 floats a uv, 8 vertices a cuboid, 16 uv floats a cuboid
        float[] uvCoordsArray = new float[numUVs];
        for (int i = 0; i < numUVs; i++) uvCoordsArray[i] = uvs.get(i);
        glBufferData(GL_ARRAY_BUFFER, uvCoordsArray, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        int numBytes = uvCoordsArray.length * Float.BYTES;
        totalBytes += numBytes;
        LOGGER.info("Uploaded uvs to VAO (ID:" + ID + ", " + numBytes + " bytes)");
        return this;
    }

    public VAO createInstanceTransformBuffer() {
        instancePositionBufferID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instancePositionBufferID);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(2);
        glVertexAttribDivisor(2, 1);
        instanceRotationBufferID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceRotationBufferID);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(3);
        glVertexAttribDivisor(3, 1);
        LOGGER.info("Prepared instance transforms for VAO (ID:" + ID + ")");
        return this;
    }

    public VAO updateInstanceTransforms() {
        float[] instancePositionsArray =  new float[instancePositions.size()];
        for (int i = 0; i < instancePositions.size(); i++) instancePositionsArray[i] = instancePositions.get(i);
        glBindBuffer(GL_ARRAY_BUFFER, instancePositionBufferID);
        glBufferData(GL_ARRAY_BUFFER, instancePositionsArray, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        float[] instanceRotationsArray =  new float[instanceRotations.size()];
        for (int i = 0; i < instanceRotations.size(); i++) instanceRotationsArray[i] = instanceRotations.get(i);
        glBindBuffer(GL_ARRAY_BUFFER, instanceRotationBufferID);
        glBufferData(GL_ARRAY_BUFFER, instanceRotationsArray, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        return this;
    }

    private List<Byte> getBooleans() {
        List<Byte> booleans = new ArrayList<>();
        ModelPart headpart = modelParts.get(0);
        headOffset = new Vector3f(headpart.pivotX / 16.0f, headpart.pivotY / 16.0f, headpart.pivotZ / 16.0f);
        for (int i = 0; i < headpart.cuboids.size(); i++) {
            for (int j = 0; j < 36; j++) {
                booleans.add((byte) 1);
            }
        }
        for (int i = 1; i < modelParts.size(); i++) {
            ModelPart part = modelParts.get(i);
            for (int j = 0; j < part.cuboids.size(); j++) {
                for (int k = 0; k < 36; k++) {
                    booleans.add((byte) 0);
                }
            }
        }
        return booleans;
    }

    public VAO createBooleansBuffer() {
        booleansBufferID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, booleansBufferID);
        List<Byte> booleans = getBooleans();
        byte[] booleansArray = new byte[booleans.size()];
        for (int i = 0; i < booleans.size(); i++) booleansArray[i] = booleans.get(i);
        ByteBuffer buffer = BufferUtils.createByteBuffer(booleans.size());
        buffer.put(booleansArray);
        buffer.flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glVertexAttribPointer(4, 1, GL_UNSIGNED_BYTE, false, Byte.BYTES, 0);
        glEnableVertexAttribArray(4);
        int numBytes = booleansArray.length * Byte.BYTES;
        totalBytes += numBytes;
        LOGGER.info("Uploaded booleans to VAO (ID:" + ID + ", " + numBytes + " bytes)");
        return this;
    }

    public static AbstractTexture get(Identifier identifierToTextureFile) {
        AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(identifierToTextureFile);
        LOGGER.info("Obtained texture (ID:" + texture.getGlId() + ")");
        return texture;
    }

    private int[] getTextureDimensions() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);

            glBindTexture(GL_TEXTURE_2D, texture.getGlId());
            glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width);
            glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, height);
            glBindTexture(GL_TEXTURE_2D, 0);

            return new int[]{width.get(0), height.get(0)};
        }
    }

    public VAO setTexture(Entity entity) {
        Map<String, String> texturePaths = Map.of(
                "Cow", "cow/cow.png",
                "Goat", "goat/goat.png",
                "Panda", "panda/panda.png",
                "Pig", "pig/pig.png",
                "Polar Bear", "bear/polarbear.png",
                "Sheep", "sheep/sheep.png",
                "Turtle", "turtle/big_sea_turtle.png"
        );
        String name = entity.getName().getString();
        texture = get(new Identifier("textures/entity/" + texturePaths.get(name)));
        return this;
    }

    public VAO build() {
        glBindVertexArray(0);
        LOGGER.info("Built VAO (ID:" + ID + "), Total bytes sent to GPU: " + totalBytes + " bytes");
        return this;
    }

    public void cleanup() {
        glDeleteBuffers(vertexBufferID);
        glDeleteBuffers(indexBufferID);
        glDeleteBuffers(instancePositionBufferID);
        glDeleteBuffers(instanceRotationBufferID);
        glDeleteBuffers(uvCoordsID);
        glDeleteBuffers(booleansBufferID);
        glDeleteVertexArrays(ID);
    }

    public int numElements() {
        return indices.length * numCuboids;
    }

    public int numInstances() {
        return instancePositions.size() / 3;
    }

    public void addInstanceTransform(float x, float y, float z, float entityYaw, float headPitch, float headYaw) {
        instancePositions.add(x);
        instancePositions.add(y);
        instancePositions.add(z);
        instanceRotations.add(entityYaw);
        instanceRotations.add(headPitch);
        instanceRotations.add(headYaw);
    }
}
