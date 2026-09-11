package me.marquinho.protectedarea.client.debug;

import me.marquinho.protectedarea.client.network.ClientNetworkHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DebugKeyHandler {

    private static KeyBinding keyNext;
    private static KeyBinding keyPrev;
    private static KeyBinding keyToggleType;

    public static void register() {
        keyNext = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.protectedarea.debug_next",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                "category.protectedarea.debug"
        ));

        keyPrev = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.protectedarea.debug_prev",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                "category.protectedarea.debug"
        ));

        keyToggleType = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.protectedarea.debug_toggle_type",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                "category.protectedarea.debug"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            DebugState state = DebugStateHolder.get();
            if (!state.isActive()) return;

            while (keyNext.wasPressed()) {
                int page = state.nextPage();
                ClientNetworkHandler.requestDebugPage(page, state.getDebugType());
            }

            while (keyPrev.wasPressed()) {
                int page = state.prevPage();
                ClientNetworkHandler.requestDebugPage(page, state.getDebugType());
            }

            while (keyToggleType.wasPressed()) {
                state.toggleDebugType();
                if (!state.isFlatType()) {
                    ClientNetworkHandler.requestDebugPage(DebugState.PAGE_OVERVIEW, state.getDebugType());
                }
            }
        });
    }
}