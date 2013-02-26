package net.minecraftforge.inventory;

import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISidedInventory;

/**
 * Contains methods for converting between different types of inventories.
 */
public class InventoryAdapters {

    /**
     * Creates an ILinearInventory view of a tile entity side. Returns null if
     * the adapter could not be created. The side must not be null or UNKNOWN.
     */
    public static ILinearInventory asLinearInventory(TileEntity te, ForgeDirection side) throws NullPointerException, IllegalArgumentException
    {
        if (side == null) throw new NullPointerException("side");
        if (side == ForgeDirection.UNKNOWN) throw new IllegalArgumentException("side");

        if (te instanceof IForgeInventoryTile)
        {
            IForgeInventory t = ((IForgeInventoryTile) te).getInventory(side);
            if (t instanceof ILinearInventory) return ((ILinearInventory) t);
        }

        if (te instanceof IInventory)
        {
            ILinearInventory rv = asLinearInventory((IInventory) te, side);
            if (rv != null) return rv;
        }

        return null;
    }

    private static IInventory mergeChest(IInventory inv, TileEntityChest chest, int dx, int dz)
    {
        if (!chest.worldObj.blockExists(chest.xCoord + dx, chest.yCoord, chest.zCoord + dz)) return inv;

        TileEntity adjacent = chest.worldObj.getBlockTileEntity(chest.xCoord + dx, chest.yCoord, chest.zCoord + dz);
        if (adjacent instanceof TileEntityChest)
        {
            if (dx < 0 || dz < 0)
                return new InventoryLargeChest("container.chestDouble", (TileEntityChest) adjacent, inv);
            else
                return new InventoryLargeChest("container.chestDouble", inv, (TileEntityChest) adjacent);
        }
        return inv;
    }

    /**
     * Creates an ILinearInventory view of an IInventory. If the IInventory is
     * also an ISidedInventory, the side must be specified. Otherwise, the side
     * may be null. Returns null if the adapter could not be created.
     */
    public static ILinearInventory asLinearInventory(IInventory inv, ForgeDirection side)
    {
        if (inv instanceof TileEntityChest)
        {
            TileEntityChest chest = (TileEntityChest) inv;
            inv = mergeChest(inv, chest, -1, 0);
            inv = mergeChest(inv, chest, 1, 0);
            inv = mergeChest(inv, chest, 0, -1);
            inv = mergeChest(inv, chest, 0, 1);
        }

        int start = 0, end = inv.getSizeInventory();

        if (inv instanceof ISidedInventory)
        {
            if (side == null || side == ForgeDirection.UNKNOWN) return null;

            start = ((ISidedInventory) inv).getStartInventorySide(side);
            end = start + ((ISidedInventory) inv).getSizeInventorySide(side);
        }

        if (inv instanceof TileEntityFurnace && side != ForgeDirection.UP && side != ForgeDirection.DOWN)
            return new FurnaceOutputToLinearAdapter(inv, start);
        else
            return new InventoryToLinearAdapter(inv, start, end);
    }

    /**
     * Creates an ICustomInventory view of a (possibly sided) IInventory.
     * If the inventory is sided and the side is null or UNKNOWN, the adapter cannot be created.
     * Returns null if the adapter could not be created.
     */
    public static ICustomInventory asCustomInventory(IInventory inv, ForgeDirection side) throws NullPointerException, IllegalArgumentException
    {
        ILinearInventory li = asLinearInventory(inv, side);
        if (li != null) return new LinearToCustomAdapter(li);
        return null;
    }
    
    /**
     * Creates an ICustomInventory view of a tile entity side. Returns null if
     * the adapter could not be created. The side must not be null or UNKNOWN.
     */
    public static ICustomInventory asCustomInventory(TileEntity te, ForgeDirection side) throws NullPointerException, IllegalArgumentException
    {
        if (side == null) throw new NullPointerException("side");
        if (side == ForgeDirection.UNKNOWN) throw new IllegalArgumentException("side");

        if (te instanceof IForgeInventoryTile)
        {
            IForgeInventory t = ((IForgeInventoryTile) te).getInventory(side);
            if (t instanceof ICustomInventory) return ((ICustomInventory) t);
            if (t instanceof ILinearInventory) return new LinearToCustomAdapter((ILinearInventory) t);
        }
        if (te instanceof IInventory)
        {
            ILinearInventory li = asLinearInventory(te, side);
            if (li != null) return new LinearToCustomAdapter(li);
        }
        return null;
    }

    private static class InventorySlotAdapter implements IInventorySlot {

        private final IInventory inv;
        private final int index;

        public InventorySlotAdapter(IInventory inv, int index)
        {
            this.inv = inv;
            this.index = index;
        }

        @Override
        public ItemStack getStack()
        {
            return inv.getStackInSlot(index);
        }

        @Override
        public boolean setStack(ItemStack is, boolean simulate)
        {
            if (is != null && is.stackSize > inv.getInventoryStackLimit()) return false;

            if (!simulate) inv.setInventorySlotContents(index, is);

            return true;
        }

        @Override
        public int getMaximumStackSize()
        {
            return inv.getInventoryStackLimit();
        }
    }

    private static class FurnaceOutputSlotAdapter extends InventorySlotAdapter {
        public FurnaceOutputSlotAdapter(IInventory inv, int slot)
        {
            super(inv, slot);
        }

        public boolean setStack(ItemStack is, boolean simulate)
        {
            if (is == null) return super.setStack(is, simulate);

            ItemStack old = getStack();

            // can't put in more items than were in there already
            if (old == null) return false;
            if (is.stackSize > old.stackSize) return false;

            // can't change the item either
            if (old.itemID != is.itemID || old.getItemDamage() != is.getItemDamage() || !ItemStack.areItemStackTagsEqual(old, is)) return false;

            return super.setStack(is, simulate);
        }
    }

    private static class InventoryToLinearAdapter implements ILinearInventory {

        private final int start, end;
        private final IInventory inv;

        public InventoryToLinearAdapter(IInventory inv, int start, int end)
        {
            this.inv = inv;
            this.start = start;
            this.end = end;
        }

        @Override
        public int getNumSlots()
        {
            return end - start;
        }

        @Override
        public IInventorySlot getSlot(int index) throws IllegalArgumentException
        {
            if (index < 0 || index >= end - start) throw new IllegalArgumentException("Slot index out of range");
            return new InventorySlotAdapter(inv, start + index);
        }
    }

    private static class FurnaceOutputToLinearAdapter implements ILinearInventory {
        private final int slot;
        private final IInventory inv;

        public FurnaceOutputToLinearAdapter(IInventory inv, int slot)
        {
            this.inv = inv;
            this.slot = slot;
        }

        @Override
        public int getNumSlots()
        {
            return 1;
        }

        @Override
        public IInventorySlot getSlot(int index) throws IllegalArgumentException
        {
            if (index != 0) throw new IllegalArgumentException("Slot index out of range");
            return new FurnaceOutputSlotAdapter(inv, slot);
        }
    }

    private static class LinearToCustomAdapter implements ICustomInventory {
        private final ILinearInventory inv;

        public LinearToCustomAdapter(ILinearInventory inv)
        {
            this.inv = inv;
        }

        @Override
        public int insert(ItemStack item, int amount, boolean simulate) throws NullPointerException, IllegalArgumentException
        {
            if (item == null) throw new NullPointerException("item");
            if (amount <= 0) throw new IllegalArgumentException("amount <= 0");

            int size = inv.getNumSlots();

            for (int k = 0; k < size; k++)
            {
                IInventorySlot slot = inv.getSlot(k);
                ItemStack oldStack = slot.getStack();

                if (oldStack != null && oldStack.itemID == item.itemID && oldStack.getItemDamage() == item.getItemDamage()
                        && ItemStack.areItemStackTagsEqual(oldStack, item))
                {
                    int adding = Math.min(amount, slot.getMaximumStackSize() - oldStack.stackSize);
                    if (adding > 0)
                    {
                        ItemStack newStack = oldStack.copy();
                        newStack.stackSize += adding;
                        if (slot.setStack(newStack, simulate))
                        {
                            amount -= adding;
                            if (amount == 0) return 0;
                        }
                    }
                }
            }

            for (int k = 0; k < size; k++)
            {
                IInventorySlot slot = inv.getSlot(k);
                ItemStack oldStack = slot.getStack();

                if (oldStack == null)
                {
                    ItemStack newStack = item.copy();
                    newStack.stackSize = Math.min(amount, slot.getMaximumStackSize());
                    if (slot.setStack(newStack, simulate))
                    {
                        amount -= newStack.stackSize;
                        if (amount == 0) return 0;
                    }
                }
            }

            return amount;
        }

        @Override
        public ItemStack extract(IStackFilter filter, int amount, boolean simulate) throws NullPointerException, IllegalArgumentException
        {
            if (filter == null) throw new NullPointerException("filter");
            if (amount <= 0) throw new IllegalArgumentException("amount <= 0");

            int size = inv.getNumSlots();

            ItemStack rv = null;

            for (int k = 0; k < size; k++)
            {
                IInventorySlot slot = inv.getSlot(k);
                ItemStack oldStack = slot.getStack();

                if (oldStack == null) continue;

                if (rv != null
                        && (rv.itemID != oldStack.itemID || rv.getItemDamage() != oldStack.getItemDamage() || !ItemStack.areItemStackTagsEqual(rv, oldStack)))
                    continue;

                if (oldStack != null && filter.matchesItem(oldStack))
                {
                    int removing = Math.min(amount, oldStack.stackSize);

                    ItemStack newStack;
                    if (removing == oldStack.stackSize)
                    {
                        newStack = null;
                    }
                    else
                    {
                        newStack = oldStack.copy();
                        newStack.stackSize -= removing;
                    }

                    if (slot.setStack(newStack, simulate))
                    {
                        amount -= removing;

                        if (rv == null)
                        {
                            rv = oldStack.copy();
                            rv.stackSize = removing;
                        }
                        else
                            rv.stackSize += removing;

                        if (amount == 0) return rv;
                    }
                }
            }

            return rv;
        }
    }
}
