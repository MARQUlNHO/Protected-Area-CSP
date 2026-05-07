package me.marquinho.protectedareaclient.mixin;

import me.marquinho.protectedareaclient.client.managers.SkyboxManager;
import me.marquinho.protectedareaclient.client.render.SkyboxRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class SkyRendererMixin {

    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
            at = @At("RETURN"))
    private void onRenderSky(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta,
                              Camera camera, boolean isFoggy, Runnable skyDarkener, CallbackInfo ci) {
        if (SkyboxManager.isRendering()) {
            SkyboxRenderer.render(
                    SkyboxManager.getRenderingSkybox(),
                    modelViewMatrix,
                    projectionMatrix,
                    SkyboxManager.getTransitionAlpha()
            );
        }
    }
}
