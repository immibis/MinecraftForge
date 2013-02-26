package net.minecraftforge.inventory;

import net.minecraftforge.common.ForgeDirection;

/**
 * This interface is implemented by tile entities that wish to use the new Forge
 * inventory system.
 */
public interface IForgeInventoryTile {
    IForgeInventory getInventory(ForgeDirection side);
}
