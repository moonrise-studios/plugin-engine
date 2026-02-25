package games.negative.engine.paper.gui;

import org.bukkit.inventory.MenuType;

/**
 * Represents the ChestInterface interface.
 */

public interface ChestInterface extends UserInterface {

    /**
     * Minimum number of rows in a chest inventory.
     */
    int MIN_ROWS = 1;

    /**
     * Maximum number of rows in a chest inventory.
     */
    int MAX_ROWS = 6;

    default MenuType typeFromRows(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9X1;
            case 2 -> MenuType.GENERIC_9X2;
            case 3 -> MenuType.GENERIC_9X3;
            case 4 -> MenuType.GENERIC_9X4;
            case 5 -> MenuType.GENERIC_9X5;
            case 6 -> MenuType.GENERIC_9X6;
            default -> throw new IllegalArgumentException("Rows must be between 1 and 6.");
        };
    }

}
