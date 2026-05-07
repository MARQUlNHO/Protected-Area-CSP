package me.marquinho.protectedareaserver.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ProtectedAreaPayload(byte[] data) implements CustomPayload {

    public static final Id<ProtectedAreaPayload> ID =
            new Id<>(Identifier.of("protectedarea", "main"));

    public static final PacketCodec<PacketByteBuf, ProtectedAreaPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBytes(value.data()),
            buf -> {
                int len = buf.readableBytes();
                byte[] bytes = new byte[len];
                buf.readBytes(bytes);
                return new ProtectedAreaPayload(bytes);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
