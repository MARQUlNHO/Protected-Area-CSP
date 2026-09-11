package me.marquinho.protectedarea.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.Locale;

public class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Text parse(String mm) {
        if (mm == null || mm.isEmpty()) return Text.empty();
        return toText(MM.deserialize(mm));
    }

    public static Text translate(String key, ComponentLike... args) {
        if (key == null || key.isEmpty()) return Text.empty();
        return toText(Component.translatable(key, args));
    }

    private static Text toText(Component component) {
        Component rendered = GlobalTranslator.render(component, Locale.US);
        String json = GsonComponentSerializer.gson().serialize(rendered);
        return TextCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                               .result()
                               .orElse(Text.empty());
    }
}
