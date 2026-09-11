package me.marquinho.protectedarea.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.List;

public final class CommandSuggestions {

    private CommandSuggestions() {
    }

    public static SuggestionProvider<ServerCommandSource> nestedCommand() {
        return (context, builder) -> {
            ServerCommandSource source = context.getSource();
            CommandDispatcher<ServerCommandSource> dispatcher =
                    source.getServer().getCommandManager().getDispatcher();

            String typed = builder.getRemaining();

            int slash = typed.startsWith("/") ? 1 : 0;
            String command = typed.substring(slash);
            int offset = builder.getStart() + slash;

            ParseResults<ServerCommandSource> parse = dispatcher.parse(new StringReader(command), source);

            return dispatcher.getCompletionSuggestions(parse, command.length())
                    .thenApply(suggestions -> remap(suggestions, command, offset));
        };
    }

    private static Suggestions remap(Suggestions suggestions, String command, int offset) {
        StringRange range = suggestions.getRange();
        int end = Math.min(range.getEnd(), command.length());

        if (range.getStart() < end && containsWhitespace(command, range.getStart(), end)) {
            return Suggestions.empty().join();
        }

        return shift(suggestions, offset);
    }

    private static boolean containsWhitespace(String text, int from, int to) {
        for (int i = from; i < to; i++) {
            if (Character.isWhitespace(text.charAt(i))) return true;
        }
        return false;
    }

    private static Suggestions shift(Suggestions suggestions, int offset) {
        StringRange range = suggestions.getRange();
        StringRange shifted = StringRange.between(range.getStart() + offset, range.getEnd() + offset);

        List<Suggestion> moved = new ArrayList<>(suggestions.getList().size());
        for (Suggestion suggestion : suggestions.getList()) {
            moved.add(new Suggestion(shifted, suggestion.getText(), suggestion.getTooltip()));
        }

        return new Suggestions(shifted, moved);
    }
}
