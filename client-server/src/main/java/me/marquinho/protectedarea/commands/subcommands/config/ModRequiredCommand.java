package me.marquinho.protectedarea.commands.subcommands.config;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
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
            Messages.send(source, "protectedarea.command.modrequired.enabled");
            Messages.send(source, "protectedarea.command.modrequired.enabled_desc");
        } else {
            Messages.send(source, "protectedarea.command.modrequired.disabled");
            Messages.send(source, "protectedarea.command.modrequired.disabled_desc");
        }

        return 1;
    }

    private static int executeStatus(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        boolean isRequired = plugin.getConfigManager().isModRequired();

        Messages.send(source, "protectedarea.command.exception.list.divider");
        Messages.send(source, "protectedarea.command.modrequired.status.header");
        Messages.send(source, "protectedarea.command.common.blank_line");

        if (isRequired) {
            Messages.send(source, "protectedarea.command.modrequired.status.enabled");
            Messages.send(source, "protectedarea.command.modrequired.status.enabled_desc");
        } else {
            Messages.send(source, "protectedarea.command.modrequired.status.disabled");
            Messages.send(source, "protectedarea.command.modrequired.status.disabled_desc");
        }

        Messages.send(source, "protectedarea.command.common.blank_line");
        Messages.send(source, "protectedarea.command.modrequired.status.kick_message_label");
        Messages.send(source, "protectedarea.command.modrequired.status.kick_message_value", Messages.arg("message", plugin.getConfigManager().getKickMessage()));
        Messages.send(source, "protectedarea.command.common.blank_line");
        Messages.send(source, "protectedarea.command.exception.list.divider");

        return 1;
    }
}
