package com.theflamingchilli.performancemod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class ShadowVAO extends VAO {
    private static final Logger LOGGER = LoggerFactory.getLogger("PerformanceMod-SHADOWS");

    public ShadowVAO() {
        super(null, null);
        this.ID = glGenVertexArrays();
        glBindVertexArray(this.ID);
        totalBytes = 0;
        LOGGER.info("Created Shadow VAO");
        setTexture();
    }

    public VAO setTexture() {
        texture = MinecraftClient.getInstance().getTextureManager().getTexture(new Identifier("textures/misc/shadow.png"));
        textureID = texture.getGlId();
        return this;
    }

    public VAO createVerticesBuffer() {
        vertexBufferID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferID);
        float[] verticesArray = {
                -0.7f, 0, 0.7f,
                0.7f, 0, 0.7f,
                0.7f, 0, -0.7f,
                0.7f, 0, -0.7f,
                -0.7f, 0, -0.7f,
                -0.7f, 0, 0.7f,
        };
        glBufferData(GL_ARRAY_BUFFER, verticesArray, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        int numBytes = verticesArray.length * Float.BYTES;
        totalBytes += numBytes;
        LOGGER.info("Uploaded vertices to VAO (ID:" + ID + ", " + numBytes + " bytes)");
        return this;
    }

    public VAO createUVCoords() {
        uvCoordsID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, uvCoordsID);
        float[] uvCoordsArray = {
                1, 0, 0, 0, 0, 1,
                0, 1, 1, 1, 1, 0
        };
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
        LOGGER.info("Prepared instance transforms for VAO (ID:" + ID + ")");
        return this;
    }

    public VAO updateInstanceTransforms() {
        float[] instancePositionsArray = new float[instancePositions.size()];
        for (int i = 0; i < instancePositions.size(); i++) instancePositionsArray[i] = instancePositions.get(i);
        glBindBuffer(GL_ARRAY_BUFFER, instancePositionBufferID);
        glBufferData(GL_ARRAY_BUFFER, instancePositionsArray, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return this;
    }

    public void addShadowPosition(float x, float y, float z) {
        instancePositions.add(x);
        instancePositions.add(y);
        instancePositions.add(z);
    }

    public void cleanup() {
        glDeleteBuffers(vertexBufferID);
        glDeleteBuffers(instancePositionBufferID);
        glDeleteBuffers(uvCoordsID);
        glDeleteVertexArrays(ID);
    }
}
