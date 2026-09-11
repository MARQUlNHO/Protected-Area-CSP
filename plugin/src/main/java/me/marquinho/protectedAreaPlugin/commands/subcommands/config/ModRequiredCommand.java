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
            sender.sendMessage("§aMandatory mod system ENABLED!");
            sender.sendMessage("§ePlayers without the client mod will be kicked from the server");
        } else {
            sender.sendMessage("§cMandatory mod system DISABLED!");
            sender.sendMessage("§ePlayers can join without the client mod");
        }

        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        boolean isRequired = plugin.getConfigManager().isModRequired();

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lMandatory Mod System Status");
        sender.sendMessage("");

        if (isRequired) {
            sender.sendMessage("  §aStatus: §2§lENABLED");
            sender.sendMessage("  §7Players must have the mod installed");
        } else {
            sender.sendMessage("  §cStatus: §4§lDISABLED");
            sender.sendMessage("  §7Players can join without the mod");
        }

        sender.sendMessage("");
        sender.sendMessage("  §eKick message:");
        sender.sendMessage("  §7" + plugin.getConfigManager().getKickMessage());
        sender.sendMessage("");
        sender.sendMessage("§e§m                                          ");

        return 1;
    }
}
