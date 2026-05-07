package me.marquinho.protectedareaclient.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AreaDataPayload(
        String action,
        String id,
        String dimension,
        int x1, int y1, int z1,
        int x2, int y2, int z2
) implements CustomPayload {

    public static final CustomPayload.Id<AreaDataPayload> ID =
            new CustomPayload.Id<>(Identifier.of("protectedarea", "main"));

    public static final PacketCodec<PacketByteBuf, AreaDataPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeString(value.action());
                        buf.writeString(value.id());
                        buf.writeString(value.dimension());
                        buf.writeInt(value.x1());
                        buf.writeInt(value.y1());
                        buf.writeInt(value.z1());
                        buf.writeInt(value.x2());
                        buf.writeInt(value.y2());
                        buf.writeInt(value.z2());
                    },
                    (buf) -> new AreaDataPayload(
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
