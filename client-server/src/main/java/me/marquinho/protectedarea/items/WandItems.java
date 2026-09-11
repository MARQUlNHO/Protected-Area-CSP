package me.marquinho.protectedarea.items;

import me.marquinho.protectedarea.util.TextUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;

public final class WandItems {

    private static final String NAME_PREFIX = "Area Wand";

    private WandItems() {}

    public static ItemStack create(String type) {
        boolean flat = "flat".equals(type);
        String label = flat ? "Flat" : "Cube";

        ItemStack stack = new ItemStack(Items.GOLDEN_HOE);
        stack.set(DataComponentTypes.CUSTOM_NAME, TextUtil.parse("<gold><bold>" + NAME_PREFIX + " <gray>(" + label + ")"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                TextUtil.parse("<yellow>Left click: <white>Position 1"),
                TextUtil.parse("<yellow>Right click: <white>Position 2"),
                TextUtil.parse("<yellow>Sneak + right click: <white>Create area")
        )));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    public static boolean isWand(ItemStack stack) {
        return getWandType(stack) != null;
    }

    public static String getWandType(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (name == null) return null;

        String plain = name.getString();
        if (!plain.contains(NAME_PREFIX)) return null;
        if (plain.contains("(Flat)")) return "flat";
        if (plain.contains("(Cube)")) return "cube";
        return null;
    }
}
