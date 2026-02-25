package games.negative.engine.paper.item;

import games.negative.engine.message.util.MiniMessageUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;
import java.util.function.Consumer;

/**
 * A builder class for creating and modifying ItemStacks with various properties.
 */
public final class ItemBuilder {

    private static final MiniMessage MINIMESSAGE = MiniMessage.builder()
            .postProcessor(component -> component.decoration(TextDecoration.ITALIC, false))
            .build();

    private final ItemStack stack;
    private final ItemMeta meta;
    private Player viewer;

    /**
     * Creates a new ItemBuilder for the given ItemStack.
     * @param stack the ItemStack to build upon
     */
    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
        this.meta = stack.getItemMeta();
    }

    /**
     * Creates a new ItemBuilder for the given ItemStack.
     * @param stack the ItemStack to build upon
     * @return the ItemBuilder instance
     */
    public static ItemBuilder of(ItemStack stack) {
        return new ItemBuilder(stack);
    }

    /**
     * Creates a new ItemBuilder for the given Material.
     * @param material the Material to create the ItemStack from
     * @return the ItemBuilder instance
     */
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    /**
     * Creates a new ItemBuilder for the given ItemType.
     * @param type the ItemType to create the ItemStack from
     * @return the ItemBuilder instance
     */
    public static ItemBuilder of(ItemType type) {
        return new ItemBuilder(type.createItemStack());
    }

    /**
     * Creates a new ItemBuilder by copying an existing one.
     * @param builder the ItemBuilder to copy
     * @return the new ItemBuilder instance
     */
    public static ItemBuilder of(ItemBuilder builder) {
        ItemStack copiedStack = builder.stack.clone();
        ItemBuilder newBuilder = new ItemBuilder(copiedStack);
        newBuilder.viewer = builder.viewer;
        return newBuilder;
    }

    /**
     * Sets the viewer for the ItemBuilder. This is used for relational placeholders using PlaceholderAPI
     * @param player the Player who will view the item
     * @return the ItemBuilder instance
     */
    public ItemBuilder viewer(Player player) {
        this.viewer = player;
        return this;
    }

    /**
     * Sets the amount of the ItemStack.
     * @param amount the amount to set
     * @return the ItemBuilder instance
     */
    public ItemBuilder amount(int amount) {
        return applyItemStack(itemStack -> itemStack.setAmount(amount));
    }

    /**
     * Sets the name of the ItemStack using MiniMessage formatting.
     * @param name the name to set
     * @param placeholders placeholders to replace in the name
     * @return the ItemBuilder instance
     */
    public ItemBuilder name(String name, TagResolver.Single... placeholders) {
        return applyMeta(itemMeta -> {
            Component component = MiniMessageUtil.fromText(MINIMESSAGE, viewer, name, placeholders);
            itemMeta.displayName(component);
        });
    }

    /**
     * Sets the lore of the ItemStack using MiniMessage formatting.
     * @param lore the lore to set
     * @param placeholders placeholders to replace in the lore
     * @return the ItemBuilder instance
     */
    public ItemBuilder lore(List<String> lore, TagResolver.Single... placeholders) {
        return applyMeta(itemMeta -> {
            List<Component> components = lore.stream()
                    .map(line -> MiniMessageUtil.fromText(MINIMESSAGE, viewer, line, placeholders))
                    .toList();

            itemMeta.lore(components);
        });
    }

    /**
     * Adds an enchantment to the ItemStack.
     * @param enchantment the Enchantment to add
     * @param level the level of the enchantment
     * @return the ItemBuilder instance
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        return applyMeta(itemMeta -> itemMeta.addEnchant(enchantment, level, true));
    }

    /**
     * Sets the unbreakable status of the ItemStack.
     * @param value true to make the item unbreakable, false otherwise
     * @return the ItemBuilder instance
     */
    public ItemBuilder unbreakable(boolean value) {
        return applyItemStack(itemStack -> {
            if (!value && itemStack.hasData(DataComponentTypes.UNBREAKABLE)) {
                itemStack.unsetData(DataComponentTypes.UNBREAKABLE);
                return;
            }

            itemStack.setData(DataComponentTypes.UNBREAKABLE);
        });
    }

    /**
     * Sets the custom model data of the ItemStack.
     * @param modelData the custom model data to set
     * @return the ItemBuilder instance
     */
    public ItemBuilder customModelData(int modelData) {
        return applyMeta(itemMeta -> itemMeta.setCustomModelData(modelData));
    }

    /**
     * Applies a Consumer to the PersistentDataContainer of the ItemStack.
     * @param consumer the Consumer to apply
     * @return the ItemBuilder instance
     */
    public ItemBuilder applyPersistentData(Consumer<PersistentDataContainer> consumer) {
        return applyMeta(itemMeta -> {
            PersistentDataContainer container = itemMeta.getPersistentDataContainer();
            consumer.accept(container);
        });
    }

    /**
     * Sets whether the ItemStack should have a glowing effect.
     * @param value true to make the item glow, false otherwise
     * @return the ItemBuilder instance
     */
    public ItemBuilder glowing(boolean value) {
        return applyItemStack(itemStack -> {
            if (!value && itemStack.hasData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
                itemStack.unsetData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
                return;
            }

            itemStack.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, value);
        });
    }

    /**
     * Sets whether the ItemStack's tooltip should be hidden.
     * @param value true to hide the tooltip, false otherwise
     * @return the ItemBuilder instance
     */
    public ItemBuilder hideToolTip(boolean value) {
        return applyItemStack(itemStack -> {
            if (!value && itemStack.hasData(DataComponentTypes.TOOLTIP_DISPLAY)) {
                itemStack.unsetData(DataComponentTypes.TOOLTIP_DISPLAY);
                return;
            }

            itemStack.setData(
                    DataComponentTypes.TOOLTIP_DISPLAY,
                    TooltipDisplay.tooltipDisplay()
                            .hideTooltip(value)
                            .build()
            );
        });
    }

    /**
     * Applies a Consumer to the ItemMeta of the ItemStack.
     * @param consumer the Consumer to apply
     * @return the ItemBuilder instance
     */
    public ItemBuilder applyMeta(Consumer<ItemMeta> consumer) {
        if (this.meta == null) return this;

        consumer.accept(this.meta);
        this.stack.setItemMeta(this.meta);
        return this;
    }

    /**
     * Applies a Consumer to a specific subclass of ItemMeta.
     * @param metaClass the class of the ItemMeta subclass
     * @param consumer the Consumer to apply
     * @return the ItemBuilder instance
     * @param <T> the type of the ItemMeta subclass
     */
    public <T extends ItemMeta> ItemBuilder applyMeta(Class<T> metaClass, Consumer<T> consumer) {
        if (this.meta == null) return this;

        if (metaClass.isInstance(this.meta)) {
            consumer.accept(metaClass.cast(this.meta));
            this.stack.setItemMeta(this.meta);
        }
        return this;
    }

    /**
     * Applies a Consumer to the ItemStack.
     * @param consumer the Consumer to apply
     * @return the ItemBuilder instance
     */
    public ItemBuilder applyItemStack(Consumer<ItemStack> consumer) {
        consumer.accept(this.stack);
        return this;
    }

    /**
     * Builds and returns the final ItemStack.
     * @return the built ItemStack
     */
    public ItemStack build() {
        return this.stack.clone();
    }

}
