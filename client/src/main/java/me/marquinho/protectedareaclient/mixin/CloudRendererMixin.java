package me.marquinho.protectedareaclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.marquinho.protectedareaclient.client.managers.SkyboxManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class CloudRendererMixin {

    @Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void beforeRenderClouds(MatrixStack matrices, Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
                                    float tickDelta, double cameraX, double cameraY, double cameraZ,
                                    CallbackInfo ci) {
        float alpha = SkyboxManager.getTransitionAlpha();
        if (alpha <= 0f) return;

        if (alpha >= 1f) {
            ci.cancel();
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f - alpha);
    }

    @Inject(method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("RETURN"))
    private void afterRenderClouds(MatrixStack matrices, Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
                                   float tickDelta, double cameraX, double cameraY, double cameraZ,
                                   CallbackInfo ci) {
        float alpha = SkyboxManager.getTransitionAlpha();
        if (alpha > 0f && alpha < 1f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }
}
