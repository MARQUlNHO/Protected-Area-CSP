package me.marquinho.protectedareaserver.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Text parse(String mm) {
        if (mm == null || mm.isEmpty()) return Text.empty();
        Component comp = MM.deserialize(mm);
        String json = GsonComponentSerializer.gson().serialize(comp);
        return TextCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                               .result()
                               .orElse(Text.empty());
    }
}
