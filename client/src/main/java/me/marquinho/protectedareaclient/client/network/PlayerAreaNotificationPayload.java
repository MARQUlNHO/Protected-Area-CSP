package me.marquinho.protectedareaclient.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayerAreaNotificationPayload(String action, String areaId) implements CustomPayload {

    public static final Id<PlayerAreaNotificationPayload> ID =
            new Id<>(Identifier.of("protectedarea", "main"));

    public static final PacketCodec<PacketByteBuf, PlayerAreaNotificationPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.action());
                        buf.writeString(value.areaId());
                    },
                    (buf) -> new PlayerAreaNotificationPayload(
                            buf.readString(),
                            buf.readString()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
