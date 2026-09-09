package com.lulan.shincolle.capability;

import com.lulan.shincolle.entity.BasicEntityShip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Ship inventory capability.
 * Manages ship inventory slots and NBT serialization.
 * Slots 0-5: equipment slots, 6+: general inventory (3 pages x 18 slots)
 */
public class CapaShipInventory {

    public static final int EquipSlots = 6;
    public static final int SlotMax = 60; // 6 equip + 18*3 inventory
    public static final String InvName = "ShipInventory";

    private final ItemStack[] stacks;
    private final Entity owner;
    private int inventoryPage;

    public CapaShipInventory(int size, Entity owner) {
        this.stacks = new ItemStack[size];
        this.owner = owner;
        this.inventoryPage = 0;
        for (int i = 0; i < size; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < stacks.length) {
            return stacks[slot];
        }
        return ItemStack.EMPTY;
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < stacks.length) {
            stacks[slot] = stack != null ? stack : ItemStack.EMPTY;
            // trigger equipment stat recalculation when equip slot changes
            if (slot < EquipSlots && owner instanceof BasicEntityShip ship) {
                ship.calcShipAttributes(2, true);
            }
        }
    }

    public int getSlots() {
        return stacks.length;
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < stacks.length; i++) {
            if (!stacks[i].isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stacks[i].save(itemTag);
                list.add(itemTag);
            }
        }
        nbt.put("Items", list);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        // Clear all slots before loading to avoid stale data
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = ItemStack.EMPTY;
        }
        ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < stacks.length) {
                stacks[slot] = ItemStack.of(itemTag);
            }
        }
    }

    /**
     * Find first empty slot for item storage. Returns slot index or -1 if full.
     */
    public int getFirstSlotForItem() {
        for (int i = EquipSlots; i < stacks.length; i++) {
            if (stacks[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Try to add an item stack to the inventory. Returns true if fully added.
     */
    public boolean addItemStackToInventory(ItemStack stack) {
        if (stack.isEmpty())
            return false;

        // try to merge with existing stacks first
        for (int i = EquipSlots; i < stacks.length; i++) {
            ItemStack itemStack = stacks[i];
            if (!itemStack.isEmpty() && ItemStack.isSameItemSameTags(itemStack, stack)) {
                int maxSize = itemStack.getMaxStackSize();
                int canAdd = maxSize - itemStack.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, stack.getCount());
                    itemStack.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty())
                        return true;
                }
            }
        }

        // try empty slots
        int emptySlot = getFirstSlotForItem();
        if (emptySlot >= 0) {
            stacks[emptySlot] = stack.copy();
            stack.setCount(0);
            return true;
        }

        return false;
    }

    public int getInventoryPage() {
        return this.inventoryPage;
    }

    public void setInventoryPage(int page) {
        this.inventoryPage = page;
    }
}
