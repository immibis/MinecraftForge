package net.minecraftforge.inventory;

import net.minecraft.item.ItemStack;

/**
 * An inventory that can be accessed as an array of slots, like a chest. This
 * doesn't make sense for all inventories, such as barrels.
 * 
 * This is similar to vanilla's IInventory but allows slots to be completely
 * independent of each other - for example, they can have different maximum
 * stack sizes.
 */
public interface ILinearInventory extends IForgeInventory {
    /**
     * @return The number of slots in this inventory.
     */
    int getNumSlots();

    /**
     * @param index
     *            The slot index. Valid slot indices are from 0 to
     *            getNumSlots()-1 inclusive.
     * @return The slot in this inventory with the given index.
     * @throws IllegalArgumentException
     *             If the slot index is out of range.
     */
    IInventorySlot getSlot(int index) throws IllegalArgumentException;
}
