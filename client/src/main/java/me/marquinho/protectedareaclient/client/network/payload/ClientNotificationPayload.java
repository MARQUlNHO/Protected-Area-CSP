package me.marquinho.protectedareaclient.client.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClientNotificationPayload(byte[] data) implements CustomPayload {

    public static final Id<ClientNotificationPayload> ID = new Id<>(Identifier.of("protectedarea", "main"));

    public static final PacketCodec<PacketByteBuf, ClientNotificationPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeBytes(value.data()),
                    (buf) -> {
                        int len = buf.readableBytes();
                        byte[] bytes = new byte[len];
                        buf.readBytes(bytes);
                        return new ClientNotificationPayload(bytes);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
