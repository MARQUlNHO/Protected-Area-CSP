package me.marquinho.protectedarea.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public final class LangManager {

    private static final Key STORE_KEY = Key.key("protectedarea", "lang");
    private static final String RESOURCE = "/assets/protectedarea/lang/en_us.json";

    private static MiniMessageTranslationStore store;

    private LangManager() {
    }

    public static void register() {
        if (store != null) {
            GlobalTranslator.translator().removeSource(store);
            store = null;
        }

        MiniMessageTranslationStore created = MiniMessageTranslationStore.create(STORE_KEY);
        created.defaultLocale(Locale.US);
        created.registerAll(Locale.US, load());

        GlobalTranslator.translator().addSource(created);
        store = created;
    }

    private static Map<String, String> load() {
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();

        try (InputStream in = LangManager.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return Map.of();
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, String> parsed = new Gson().fromJson(reader, type);
                return parsed == null ? Map.of() : parsed;
            }
        } catch (IOException e) {
            return Map.of();
        }
    }
}
