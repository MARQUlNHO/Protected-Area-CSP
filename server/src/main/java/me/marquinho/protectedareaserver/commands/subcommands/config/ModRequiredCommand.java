package me.marquinho.protectedareaserver.commands.subcommands.config;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ModRequiredCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("mod-required")
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> executeToggle(context, plugin))
                )
                .executes(context -> executeStatus(context, plugin));
    }

    private static int executeToggle(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        plugin.getConfigManager().setModRequired(enabled);

        if (enabled) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Sistema de mod obligatorio ACTIVADO!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Los jugadores sin el mod de cliente serán expulsados del servidor"), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("<red>¡Sistema de mod obligatorio DESACTIVADO!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Los jugadores pueden unirse sin el mod de cliente"), false);
        }

        return 1;
    }

    private static int executeStatus(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        boolean isRequired = plugin.getConfigManager().isModRequired();

        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        source.sendFeedback(() -> TextUtil.parse("<gold><bold>Estado del Sistema de Mod Obligatorio"), false);
        source.sendFeedback(() -> TextUtil.parse(""), false);

        if (isRequired) {
            source.sendFeedback(() -> TextUtil.parse("  <green>Estado: <dark_green><bold>ACTIVADO"), false);
            source.sendFeedback(() -> TextUtil.parse("  <gray>Los jugadores deben tener el mod instalado"), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("  <red>Estado: <dark_red><bold>DESACTIVADO"), false);
            source.sendFeedback(() -> TextUtil.parse("  <gray>Los jugadores pueden unirse sin el mod"), false);
        }

        source.sendFeedback(() -> TextUtil.parse(""), false);
        source.sendFeedback(() -> TextUtil.parse("  <yellow>Mensaje de expulsión:"), false);
        source.sendFeedback(() -> TextUtil.parse("  <gray>" + plugin.getConfigManager().getKickMessage()), false);
        source.sendFeedback(() -> TextUtil.parse(""), false);
        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);

        return 1;
    }
}
