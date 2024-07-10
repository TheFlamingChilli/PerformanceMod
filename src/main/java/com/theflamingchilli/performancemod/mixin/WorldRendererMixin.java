package com.theflamingchilli.performancemod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.theflamingchilli.performancemod.client.PerformanceModClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V",
                    ordinal = 3,
                    shift = At.Shift.AFTER
            )
    )
    private void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (PerformanceModClient.fastRenderingToggle) {
            RenderSystem.enableDepthTest();
            PerformanceModClient.updateInstances(tickDelta);
            PerformanceModClient.renderEntitiesInstanced(matrices);
        }
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (PerformanceModClient.fastRenderingToggle) {
            if (PerformanceModClient.namesOfEntitiesToInstance.contains(entity.getName().getString())) {
                ci.cancel();
            }
        }
        /*
        Problems with cancelling renderEntity:
        - F3 + B hitbox rendering no longer works
        - Entities on fire no longer render the fire
        - Circular entity shadows no longer render
         */
    }
}
