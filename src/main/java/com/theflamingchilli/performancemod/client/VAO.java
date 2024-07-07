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
    public int instanceTransformBufferID;
    public int uvCoordsID;
    public List<Float> instanceTransforms = new ArrayList<>();
    public AbstractTexture texture;
    private int texWidth;
    private int texHeight;

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
        LOGGER.info("Created new VAO for entity: " + entityName);
    }

    private List<Float> getVertices() {
        List<Float> vertices = new ArrayList<>();
        for (ModelPart part : modelParts) {
            Matrix4f rotationMatrix = new Matrix4f().rotationXYZ(part.pitch, part.yaw, part.roll);
            this.numCuboids += part.cuboids.size();
            for (ModelPart.Cuboid cuboid : part.cuboids) {
//                Vector3f min = new Vector3f(cuboid.minX, cuboid.minY, cuboid.minZ);
//                Vector3f max = new Vector3f(cuboid.maxX, cuboid.maxY, cuboid.maxZ);
//                Matrix4f rotationMatrix = new Matrix4f().rotationXYZ(part.pitch, part.yaw, part.roll);
//                min = rotationMatrix.transformPosition(min);
//                max = rotationMatrix.transformPosition(max);
//                min.add(part.pivotX, part.pivotY, part.pivotZ);
//                max.add(part.pivotX, part.pivotY, part.pivotZ);
//                float minx = Math.min(min.x, max.x) / 16.0f;
//                float maxx = Math.max(min.x, max.x) / 16.0f;
//                float miny = Math.min(min.y, max.y) / 16.0f;
//                float maxy = Math.max(min.y, max.y) / 16.0f;
//                float minz = Math.min(min.z, max.z) / 16.0f;
//                float maxz = Math.max(min.z, max.z) / 16.0f;
//                float[] temp = {
//                        minx, miny, minz, maxx, miny, minz,
//                        maxx, maxy, minz, minx, maxy, minz,
//                        minx, miny, maxz, maxx, miny, maxz,
//                        maxx, maxy, maxz, minx, maxy, maxz,
//                };
//                for (float v : temp) {
//                    vertices.add(v);
//                }
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
//                    List<String> verts = new ArrayList<>();
//                    for (Vector3f v : rotatedVertices) {
//                        verts.add("(" + v.x + " " + v.y + " " + v.z + ")");
//                    }
//                    LOGGER.info(String.join(", ", verts));
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
        LOGGER.info("Uploaded uvs to VAO (ID:" + ID + ", " + numBytes + " bytes)");
        return this;
    }

    public VAO createInstanceTransformBuffer() {
        instanceTransformBufferID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceTransformBufferID);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(2);
        glVertexAttribDivisor(2, 1);
        LOGGER.info("Prepared instance transforms for VAO (ID:" + ID + ")");
        return this;
    }

    public VAO updateInstanceTransforms() {
        float[] instanceTransformsArray =  new float[instanceTransforms.size()];
        for (int i = 0; i < instanceTransforms.size(); i++) instanceTransformsArray[i] = instanceTransforms.get(i);

        glBindBuffer(GL_ARRAY_BUFFER, instanceTransformBufferID);
        glBufferData(GL_ARRAY_BUFFER, instanceTransformsArray, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

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
        int[] textureDimensions = getTextureDimensions();
        texWidth = textureDimensions[0];
        texHeight = textureDimensions[1];
        return this;
    }

    public VAO build() {
        glBindVertexArray(0);
        LOGGER.info("Built VAO (ID:" + ID + ")");
        return this;
    }

    public void cleanup() {
        glDeleteBuffers(vertexBufferID);
        glDeleteBuffers(indexBufferID);
        glDeleteBuffers(instanceTransformBufferID);
        glDeleteVertexArrays(ID);
    }

    public int numElements() {
        return indices.length * numCuboids;
    }

    public int numInstances() {
        return instanceTransforms.size();
    }

    public void addInstanceTransform(float x, float y, float z, float yaw) {
        instanceTransforms.add(x);
        instanceTransforms.add(y);
        instanceTransforms.add(z);
        instanceTransforms.add(yaw);
    }
}
