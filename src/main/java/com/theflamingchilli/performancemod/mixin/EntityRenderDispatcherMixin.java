package com.theflamingchilli.performancemod.mixin;

import com.theflamingchilli.performancemod.client.PerformanceModClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    private static final RenderLayer SHADOW_LAYER = RenderLayer.getEntityShadow(new Identifier("textures/misc/shadow.png"));

    @Inject(
            method = "renderShadow",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void renderShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float opacity, float tickDelta, WorldView world, float radius, CallbackInfo ci) {
        if (PerformanceModClient.fastRenderingToggle) {
            //ci.cancel();
            // TODO: get these stupid shadows rendering
            //renderShadow2(matrices, vertexConsumers, entity, opacity, tickDelta, world, radius);
        }
    }

    private static void renderShadow2(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float opacity, float tickDelta, WorldView world, float radius) {
        float shadowRadius = radius;
        if (entity instanceof MobEntity && ((MobEntity) entity).isBaby()) {
            shadowRadius *= 0.5f;
        }

        double entityX = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        double entityY = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        double entityZ = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

        // Prepare the rendering matrix
        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(SHADOW_LAYER);

        // Calculate the quad corners
        float minX = (float) (entityX - shadowRadius);
        float maxX = (float) (entityX + shadowRadius);
        float minZ = (float) (entityZ - shadowRadius);
        float maxZ = (float) (entityZ + shadowRadius);
        float y = (float) entityY + 0.001f; // Slightly above the ground to avoid z-fighting

        // Set the texture coordinates
        float minU = 0.0f;
        float maxU = 1.0f;
        float minV = 0.0f;
        float maxV = 1.0f;

        // Render the quad
        vertexConsumer.vertex(minX, y, minZ, 1.0f, 1.0f, 1.0f, opacity, minU, minV, OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, 0.0f, 1.0f, 0.0f);
        vertexConsumer.vertex(maxX, y, minZ, 1.0f, 1.0f, 1.0f, opacity, maxU, minV, OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, 0.0f, 1.0f, 0.0f);
        vertexConsumer.vertex(maxX, y, maxZ, 1.0f, 1.0f, 1.0f, opacity, maxU, maxV, OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, 0.0f, 1.0f, 0.0f);
        vertexConsumer.vertex(minX, y, maxZ, 1.0f, 1.0f, 1.0f, opacity, minU, maxV, OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, 0.0f, 1.0f, 0.0f);
        //vertexConsumer.vertex(entry.getPositionMatrix(), minX, y, minZ).color(1.0f, 1.0f, 1.0f, opacity).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
        //vertexConsumer.vertex(entry.getPositionMatrix(), maxX, y, minZ).color(1.0f, 1.0f, 1.0f, opacity).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
        //vertexConsumer.vertex(entry.getPositionMatrix(), maxX, y, maxZ).color(1.0f, 1.0f, 1.0f, opacity).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
        //vertexConsumer.vertex(entry.getPositionMatrix(), minX, y, maxZ).color(1.0f, 1.0f, 1.0f, opacity).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(entry.getNormalMatrix(), 0.0f, 1.0f, 0.0f).next();
    }

}
