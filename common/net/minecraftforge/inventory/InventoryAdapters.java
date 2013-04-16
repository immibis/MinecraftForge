package net.minecraftforge.inventory;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
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
    public static IForgeLinearInventory asLinearInventory(TileEntity te, ForgeDirection side) throws NullPointerException, IllegalArgumentException
    {
        if (side == null) throw new NullPointerException("side");
        if (side == ForgeDirection.UNKNOWN) throw new IllegalArgumentException("side");

        if (te instanceof IForgeInventoryTile)
        {
            IForgeInventory t = ((IForgeInventoryTile) te).getSideInventory(side);
            if (t instanceof IForgeLinearInventory) return ((IForgeLinearInventory) t);
        }

        if (te instanceof IInventory)
        {
            IForgeLinearInventory rv = asLinearInventory((IInventory) te, side);
            if (rv != null) return rv;
        }

        return null;
    }

    /**
     * Creates an ILinearInventory view of an IInventory. If the IInventory is
     * also an ISidedInventory, the side must be specified. Otherwise, the side
     * may be null. Returns null if the adapter could not be created.
     */
    public static IForgeLinearInventory asLinearInventory(IInventory inv, ForgeDirection side)
    {
        if (inv instanceof TileEntityChest)
        {
            TileEntityChest chest = (TileEntityChest) inv;
            return asLinearInventory(Block.chest.func_94442_h_(chest.worldObj, chest.xCoord, chest.yCoord, chest.zCoord), side);
        }

        if (inv instanceof net.minecraft.inventory.ISidedInventory)
        {
            if (side == null || side == ForgeDirection.UNKNOWN) return null;

            return new VanillaSidedToLinearAdapter((net.minecraft.inventory.ISidedInventory) inv, side);
        }

        int start = 0, end = inv.getSizeInventory();

        if (inv instanceof ISidedInventory)
        {
            if (side == null || side == ForgeDirection.UNKNOWN) return null;

            start = ((ISidedInventory) inv).getStartInventorySide(side);
            end = start + ((ISidedInventory) inv).getSizeInventorySide(side);
        }

        return new InventoryToLinearAdapter(inv, start, end);
    }

    /**
     * Creates an ICustomInventory view of a (possibly sided) IInventory. If the
     * inventory is sided and the side is null or UNKNOWN, the adapter cannot be
     * created. Returns null if the adapter could not be created.
     */
    public static IForgeCustomInventory asCustomInventory(IInventory inv, ForgeDirection side)
    {
        IForgeLinearInventory li = asLinearInventory(inv, side);
        if (li != null) return new LinearToCustomAdapter(li);
        return null;
    }

    /**
     * Creates an IForgeCustomInventory view of an IForgeLinearInventory.
     */
    public static IForgeCustomInventory asCustomInventory(IForgeLinearInventory inv)
    {
        return new LinearToCustomAdapter(inv);
    }

    /**
     * Creates an ICustomInventory view of a tile entity side. Returns null if
     * the adapter could not be created. The side must not be null or UNKNOWN.
     */
    public static IForgeCustomInventory asCustomInventory(TileEntity te, ForgeDirection side) throws NullPointerException, IllegalArgumentException
    {
        if (side == null) throw new NullPointerException("side");
        if (side == ForgeDirection.UNKNOWN) throw new IllegalArgumentException("side");

        if (te instanceof IForgeInventoryTile)
        {
            IForgeInventory t = ((IForgeInventoryTile) te).getSideInventory(side);
            if (t instanceof IForgeCustomInventory) return ((IForgeCustomInventory) t);
            if (t instanceof IForgeLinearInventory) return new LinearToCustomAdapter((IForgeLinearInventory) t);
        }
        if (te instanceof IInventory)
        {
            IForgeLinearInventory li = asLinearInventory(te, side);
            if (li != null) return new LinearToCustomAdapter(li);
        }
        return null;
    }

    /**
     * Creates an ICustomInventory view of a side of a thing at the given
     * coordinates. Returns null if the adapter could not be created. The side
     * must not be null or UNKNOWN.
     * 
     * Supports both entities and tile entities.
     * 
     * When calling this from a tile entity, you most likely want to pass
     * integer coordinates.
     */
    public static IForgeCustomInventory getCustomInventoryAt(World world, double x, double y, double z, ForgeDirection side) throws NullPointerException,
            IllegalArgumentException
    {
        if (side == null) throw new NullPointerException("side");
        if (side == ForgeDirection.UNKNOWN) throw new IllegalArgumentException("side");

        IForgeCustomInventory inv = null;

        TileEntity te = world.getBlockTileEntity(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
        if (te != null) inv = asCustomInventory(te, side);

        if (inv == null)
        {
            List list = world.func_94576_a((Entity) null, AxisAlignedBB.getAABBPool().getAABB(x, y, z, x + 1, y + 1, z + 1), IEntitySelector.field_96566_b);

            if (list != null && list.size() > 0)
            {
                inv = asCustomInventory((IInventory) list.get(world.rand.nextInt(list.size())), side);
            }
        }

        return inv;
    }

    private static class InventorySlotAdapter implements ILinearInventorySlot {

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
            return ItemStack.copyItemStack(inv.getStackInSlot(index));
        }

        @Override
        public boolean setStack(ItemStack is, boolean simulate)
        {
            if (is != null && is.stackSize > inv.getInventoryStackLimit()) return false;

            if (is != null && !inv.isStackValidForSlot(index, is)) return false;

            if (!simulate) inv.setInventorySlotContents(index, is);

            return true;
        }

        @Override
        public int getMaximumStackSize()
        {
            return inv.getInventoryStackLimit();
        }

        @Override
        public boolean shouldExtractItems()
        {
            return inv.getStackInSlot(index) != null;
        }

        @Override
        public boolean shouldInsertItem(ItemStack is)
        {
            return true;
        }
    }

    private static class InventoryToLinearAdapter implements IForgeLinearInventory {

        private final int start, end;
        private final IInventory inv;

        public InventoryToLinearAdapter(IInventory inv, int start, int end)
        {
            this.inv = inv;
            this.start = start;
            this.end = end;
        }

        @Override
        public int getNumInventorySlots()
        {
            return end - start;
        }

        @Override
        public ILinearInventorySlot getInventorySlot(int index) throws IndexOutOfBoundsException
        {
            if (index < 0 || index >= end - start)
                throw new IndexOutOfBoundsException("Slot index " + index + " out of range (max is " + (end - start + 1) + ")");
            return new InventorySlotAdapter(inv, start + index);
        }
    }

    private static class LinearToCustomAdapter implements IForgeCustomInventory {
        private final IForgeLinearInventory inv;

        public LinearToCustomAdapter(IForgeLinearInventory inv)
        {
            this.inv = inv;
        }

        @Override
        public int insertInventoryItems(ItemStack item, int amount, boolean simulate) throws NullPointerException, IllegalArgumentException
        {
            if (item == null) throw new NullPointerException("item");
            if (amount <= 0) throw new IllegalArgumentException("amount <= 0");

            int size = inv.getNumInventorySlots();

            for (int k = 0; k < size; k++)
            {
                ILinearInventorySlot slot = inv.getInventorySlot(k);
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
                ILinearInventorySlot slot = inv.getInventorySlot(k);
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
        public ItemStack extractInventoryItems(IStackFilter filter, int amount, boolean simulate) throws NullPointerException, IllegalArgumentException
        {
            if (filter == null) throw new NullPointerException("filter");
            if (amount <= 0) throw new IllegalArgumentException("amount <= 0");

            int size = inv.getNumInventorySlots();

            ItemStack rv = null;

            for (int k = 0; k < size; k++)
            {
                ILinearInventorySlot slot = inv.getInventorySlot(k);
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

        @Override
        public Iterable<ItemStack> listContents(final IStackFilter filter, final boolean extractableOnly) throws NullPointerException
        {
            if (filter == null) throw new NullPointerException("filter");

            return new Iterable<ItemStack>() {
                @Override
                public Iterator<ItemStack> iterator()
                {
                    return new Iterator<ItemStack>() {
                        int curSlot = 0;
                        int numSlots = inv.getNumInventorySlots();
                        ItemStack nextStack;

                        private void advance()
                        {
                            nextStack = null;

                            while (curSlot < numSlots)
                            {
                                ILinearInventorySlot slot = inv.getInventorySlot(curSlot++);
                                if (!extractableOnly || slot.shouldExtractItems())
                                {
                                    nextStack = slot.getStack();
                                    if (!filter.matchesItem(nextStack))
                                        nextStack = null;
                                    else if (nextStack != null) break;
                                }
                            }
                        }

                        {
                            advance();
                        }

                        @Override
                        public boolean hasNext()
                        {
                            return nextStack != null;
                        }

                        @Override
                        public ItemStack next()
                        {
                            if (nextStack == null) throw new NoSuchElementException();

                            ItemStack rv = nextStack;
                            advance();
                            return rv;
                        }

                        @Override
                        public void remove()
                        {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        }
    }

    private static class VanillaSidedSlotAdapter implements ILinearInventorySlot {
        private final net.minecraft.inventory.ISidedInventory inv;
        private final int slotIndex;
        private final int side;

        public VanillaSidedSlotAdapter(net.minecraft.inventory.ISidedInventory inv, int slot, int side)
        {
            this.inv = inv;
            this.slotIndex = slot;
            this.side = side;
        }

        @Override
        public int getMaximumStackSize()
        {
            return inv.getInventoryStackLimit();
        }

        @Override
        public ItemStack getStack()
        {
            return ItemStack.copyItemStack(inv.getStackInSlot(slotIndex));
        }

        @Override
        public boolean setStack(ItemStack is, boolean simulate)
        {
            if (is != null && is.stackSize > getMaximumStackSize()) return false;

            ItemStack old = inv.getStackInSlot(slotIndex);
            int newCount = (is == null ? 0 : is.stackSize);
            int oldCount = (old == null ? 0 : old.stackSize);

            if (old != null && is != null
                    && (old.itemID != is.itemID || old.getItemDamage() != is.getItemDamage() || !ItemStack.areItemStackTagsEqual(old, is)))
            {
                // If changing the item type, check that we can extract all the
                // old items and insert all the new ones

                if (is != null && !inv.isStackValidForSlot(slotIndex, is)) return false;
                if (is != null && !inv.func_102007_a(slotIndex, is, side)) return false;
            }
            else if (newCount > oldCount && is != null)
            {
                // If we're only inserting items, check we can do that.
                ItemStack newItems = is.copy();
                newItems.stackSize = newCount - oldCount;

                if (!inv.isStackValidForSlot(slotIndex, newItems)) return false;
                if (!inv.func_102007_a(slotIndex, newItems, side)) return false;
            }

            if (!simulate) inv.setInventorySlotContents(slotIndex, is);

            return true;

        }

        @Override
        public boolean shouldExtractItems()
        {
            ItemStack stack = inv.getStackInSlot(slotIndex);
            return stack != null && inv.func_102008_b(slotIndex, stack, side);
        }

        @Override
        public boolean shouldInsertItem(ItemStack is)
        {
            return is != null && inv.isStackValidForSlot(slotIndex, is) && inv.func_102007_a(slotIndex, is, side);
        }
    }

    private static class VanillaSidedToLinearAdapter implements IForgeLinearInventory {
        private final int side;
        private final net.minecraft.inventory.ISidedInventory inv;
        private final int[] accessibleSlotIndices;

        public VanillaSidedToLinearAdapter(net.minecraft.inventory.ISidedInventory inv, ForgeDirection side)
        {
            this.inv = inv;
            this.side = side.ordinal();
            this.accessibleSlotIndices = inv.getSizeInventorySide(this.side);
        }

        @Override
        public ILinearInventorySlot getInventorySlot(int index) throws IndexOutOfBoundsException
        {
            if (index < 0 || index >= accessibleSlotIndices.length)
                throw new IndexOutOfBoundsException("Slot index " + index + " out of range (max is " + (accessibleSlotIndices.length - 1) + ")");

            return new VanillaSidedSlotAdapter(inv, accessibleSlotIndices[index], side);
        }

        @Override
        public int getNumInventorySlots()
        {
            return accessibleSlotIndices.length;
        }
    }
}
