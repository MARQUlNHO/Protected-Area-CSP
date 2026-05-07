package me.marquinho.protectedAreaPlugin.commands.subcommands.config;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import org.bukkit.command.CommandSender;

public class ModRequiredCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("mod-required")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> executeToggle(context, plugin))
                )
                .executes(context -> executeStatus(context, plugin));
    }

    private static int executeToggle(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        plugin.getConfigManager().setModRequired(enabled);

        if (enabled) {
            sender.sendMessage("§a¡Sistema de mod obligatorio ACTIVADO!");
            sender.sendMessage("§eLos jugadores sin el mod de cliente serán expulsados del servidor");
        } else {
            sender.sendMessage("§c¡Sistema de mod obligatorio DESACTIVADO!");
            sender.sendMessage("§eLos jugadores pueden unirse sin el mod de cliente");
        }

        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        boolean isRequired = plugin.getConfigManager().isModRequired();

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lEstado del Sistema de Mod Obligatorio");
        sender.sendMessage("");

        if (isRequired) {
            sender.sendMessage("  §aEstado: §2§lACTIVADO");
            sender.sendMessage("  §7Los jugadores deben tener el mod instalado");
        } else {
            sender.sendMessage("  §cEstado: §4§lDESACTIVADO");
            sender.sendMessage("  §7Los jugadores pueden unirse sin el mod");
        }

        sender.sendMessage("");
        sender.sendMessage("  §eMensaje de expulsión:");
        sender.sendMessage("  §7" + plugin.getConfigManager().getKickMessage());
        sender.sendMessage("");
        sender.sendMessage("§e§m                                          ");

        return 1;
    }
}
