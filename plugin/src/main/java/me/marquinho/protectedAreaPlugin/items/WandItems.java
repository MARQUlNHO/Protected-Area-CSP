package me.marquinho.protectedAreaPlugin.items;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class WandItems {

    private static final String KEY = "area_wand_type";

    private WandItems() {}

    public static ItemStack create(ProtectedAreaPlugin plugin, String type) {
        boolean flat = "flat".equals(type);
        String label = flat ? "Flat" : "Cube";

        ItemStack stack = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("§6§lArea Wand §7(" + label + ")");
        meta.setLore(List.of(
                "§eLeft click: §fPosition 1",
                "§eRight click: §fPosition 2",
                "§eSneak + right click: §fCreate area"
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY), PersistentDataType.STRING, type);
        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean isWand(ProtectedAreaPlugin plugin, ItemStack stack) {
        return getWandType(plugin, stack) != null;
    }

    public static String getWandType(ProtectedAreaPlugin plugin, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        return meta.getPersistentDataContainer().get(new NamespacedKey(plugin, KEY), PersistentDataType.STRING);
    }
}
