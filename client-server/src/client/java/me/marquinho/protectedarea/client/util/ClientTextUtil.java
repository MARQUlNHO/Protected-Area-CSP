package me.marquinho.protectedarea.client.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.List;

public final class ClientTextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public record Arg(String name, String value, boolean markup) {
    }

    private ClientTextUtil() {
    }

    public static Text translate(String key, List<Arg> args) {
        String raw = I18n.translate(key);
        if (raw == null || raw.equals(key)) return Text.literal(key);

        List<TagResolver> resolvers = new ArrayList<>();
        for (Arg arg : args) {
            resolvers.add(arg.markup()
                    ? Placeholder.parsed(arg.name(), arg.value())
                    : Placeholder.unparsed(arg.name(), arg.value()));
        }

        Component component = MM.deserialize(raw, TagResolver.resolver(resolvers));
        String json = GsonComponentSerializer.gson().serialize(component);
        return TextCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                               .result()
                               .orElse(Text.literal(key));
    }
}
