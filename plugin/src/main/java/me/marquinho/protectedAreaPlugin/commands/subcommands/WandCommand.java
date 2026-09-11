package me.marquinho.protectedAreaPlugin.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.items.WandItems;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WandCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("wand")
                .then(Commands.literal("cube").executes(context -> giveWand(context, plugin, "cube")))
                .then(Commands.literal("flat").executes(context -> giveWand(context, plugin, "flat")));
    }

    private static int giveWand(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin, String type) {
        CommandSender sender = context.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player can receive the wand");
            return 0;
        }

        if (player.getInventory().firstEmpty() == -1) {
            sender.sendMessage("§cYou don't have space in your inventory");
            return 0;
        }

        ItemStack wand = WandItems.create(plugin, type);
        player.getInventory().addItem(wand);

        sender.sendMessage("§aArea wand (" + type + ") given!");
        sender.sendMessage("§eLeft click: §fPos1 §7| §eRight click: §fPos2 §7| §eSneak + right click: §fCreate");
        return 1;
    }
}
