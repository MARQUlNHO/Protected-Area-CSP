package me.marquinho.protectedarea.util;

import me.marquinho.protectedarea.network.ProtectedAreaPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class Messages {

    public static final String OPCODE = "MESSAGE";

    private static final byte KIND_CHAT    = 0;
    private static final byte KIND_ERROR   = 1;
    private static final byte KIND_OVERLAY = 2;

    public record Arg(String name, String value, boolean markup) {
        public static Arg of(String name, Object value) {
            return new Arg(name, String.valueOf(value), false);
        }

        public static Arg ofMarkup(String name, String value) {
            return new Arg(name, value, true);
        }
    }

    private Messages() {
    }

    public static Arg arg(String name, Object value) {
        return Arg.of(name, value);
    }

    public static Arg markup(String name, String miniMessage) {
        return Arg.ofMarkup(name, miniMessage);
    }

    public static void send(ServerCommandSource source, String key, Arg... args) {
        dispatch(source, key, KIND_CHAT, args);
    }

    public static void error(ServerCommandSource source, String key, Arg... args) {
        dispatch(source, key, KIND_ERROR, args);
    }

    public static void sendTo(ServerPlayerEntity player, String key, Arg... args) {
        if (!sendPacket(player, key, KIND_CHAT, args)) {
            player.sendMessage(render(key, args), false);
        }
    }

    public static void sendOverlay(ServerPlayerEntity player, String key, Arg... args) {
        if (!sendPacket(player, key, KIND_OVERLAY, args)) {
            player.sendMessage(render(key, args), true);
        }
    }

    private static void dispatch(ServerCommandSource source, String key, byte kind, Arg... args) {
        ServerPlayerEntity player = source.getPlayer();
        if (player != null && sendPacket(player, key, kind, args)) return;

        if (kind == KIND_ERROR) source.sendError(render(key, args));
        else source.sendFeedback(() -> render(key, args), false);
    }

    private static boolean sendPacket(ServerPlayerEntity player, String key, byte kind, Arg... args) {
        if (!ServerPlayNetworking.canSend(player, ProtectedAreaPayload.ID)) return false;

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF(OPCODE);
            out.writeUTF(key);
            out.writeByte(kind);
            out.writeInt(args.length);
            for (Arg arg : args) {
                out.writeUTF(arg.name());
                out.writeUTF(arg.value());
                out.writeBoolean(arg.markup());
            }
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Text render(String key, Arg... args) {
        ComponentLike[] components = new ComponentLike[args.length];
        for (int i = 0; i < args.length; i++) {
            components[i] = args[i].markup()
                    ? Argument.component(args[i].name(), MiniMessage.miniMessage().deserialize(args[i].value()))
                    : Argument.string(args[i].name(), args[i].value());
        }
        return TextUtil.translate(key, components);
    }
}
