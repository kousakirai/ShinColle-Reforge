package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.Values;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * ItemStack and inventory helper methods.
 * <p>
 * Ported from 1.10.2 InventoryHelper.
 * Key changes:
 * - IInventory → IItemHandler (capability-based)
 * - TileEntityChest adjacent chest handling removed (1.20.1 double chests
 * handled via capability)
 * - OreDictionary → Tags (simplified item matching)
 * - Baubles integration removed
 * - null stacks → ItemStack.EMPTY
 * - stackSize → getCount()/setCount()
 * - IFluidContainerItem → IFluidHandlerItem capability
 */
public class InventoryHelper {

    public InventoryHelper() {
    }

    /**
     * Check inventory has items matching template settings.
     * <p>
     * If excess TRUE (excess mode):
     * return TRUE if all target amounts >= template stack counts
     * <p>
     * If excess FALSE (remain mode):
     * return TRUE if all target amounts <= template stack counts
     *
     * @param handler    inventory to check
     * @param tempStacks template items (9 slots)
     * @param modeStacks NOT mode flags (true = skip this slot)
     * @param excess     true for excess mode, false for remain mode
     */
    public static boolean checkInventoryAmount(IItemHandler handler, ItemStack[] tempStacks,
                                               boolean[] modeStacks, boolean excess) {
        if (handler == null)
            return true;

        boolean noTempItem = true;
        int[] targetAmount = new int[9];

        if (tempStacks == null || tempStacks.length != 9) {
            return true;
        }

        // check itemstack temp setting
        for (int i = 0; i < 9; i++) {
            if (!tempStacks[i].isEmpty()) {
                noTempItem = false;

                // ignore NOT mode item
                if (!modeStacks[i]) {
                    targetAmount[i] = calcItemStackAmount(handler, tempStacks[i]);
                }
            }

            if (i == 8 && noTempItem) {
                return true;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (excess) {
                // EXCESS MODE: all item amount must >= temp setting
                if (!tempStacks[i].isEmpty() && !modeStacks[i]) {
                    if (targetAmount[i] < tempStacks[i].getCount())
                        return false;
                }
            } else {
                // REMAIN MODE: all item amount must <= temp setting
                if (!tempStacks[i].isEmpty() && !modeStacks[i]) {
                    if (targetAmount[i] > tempStacks[i].getCount())
                        return false;
                }
            }
        }

        return true;
    }

    /**
     * Check all fluid containers in inventory are full or empty.
     *
     * @param handler     inventory to check
     * @param targetFluid target fluid to check against (null = any fluid)
     * @param checkFull   true: check all containers are full; false: check all are
     *                    empty
     */
    public static boolean checkFluidContainer(IItemHandler handler, FluidStack targetFluid,
                                              boolean checkFull) {
        if (handler == null)
            return true;

        for (int i = 0; i < handler.getSlots(); i++) {
            if (!checkFluidContainer(handler.getStackInSlot(i), targetFluid, checkFull)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check fluid container in single item stack.
     * <p>
     * checkFull = TRUE: return FALSE if itemstack can accept target fluid
     * = FALSE: return FALSE if itemstack can drain target fluid
     */
    public static boolean checkFluidContainer(ItemStack stack, FluidStack targetFluid,
                                              boolean checkFull) {
        if (!stack.isEmpty()) {
            // check if item has fluid capability
            net.minecraftforge.common.util.LazyOptional<IFluidHandlerItem> opt = stack
                    .getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM);

            if (opt.isPresent()) {
                IFluidHandlerItem fh = opt.orElse(null);


                int tanks = fh.getTanks();
                for (int i = 0; i < tanks; i++) {
                    FluidStack fstack = fh.getFluidInTank(i);
                    int capacity = fh.getTankCapacity(i);

                    if (checkFull) {
                        // check container is full
                        if (fstack.isEmpty() || fstack.getAmount() < capacity) {
                            if (targetFluid == null || targetFluid.isEmpty()
                                    || fstack.isEmpty()
                                    || fstack.getFluid() == targetFluid.getFluid()) {
                                return false;
                            }
                        }
                    } else {
                        // check container is empty
                        if (!fstack.isEmpty() && fstack.getAmount() > 0) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Check if IItemHandler inventory is full (all slots non-empty and at max).
     */
    public static boolean checkInventoryFull(IItemHandler handler) {
        if (handler == null)
            return true;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check inventory is empty (no matching items).
     * <p>
     * If tempStacks is null: check all slots are empty
     * If tempStacks specified: check no matching item in inventory
     *
     * @param modeStacks TRUE = NOT mode (skip this slot check)
     */
    public static boolean checkInventoryEmpty(IItemHandler handler, ItemStack[] tempStacks,
                                              boolean[] modeStacks) {
        if (handler == null)
            return true;

        boolean noTempItem = true;

        if (tempStacks == null || tempStacks.length != 9 || modeStacks == null
                || modeStacks.length != 9) {
            return isAllSlotEmpty(handler);
        }

        // check specified itemstack
        for (int i = 0; i < 9; i++) {
            if (!tempStacks[i].isEmpty()) {
                noTempItem = false;

                // ignore NOT mode item
                if (!modeStacks[i]) {
                    if (matchTargetItem(handler, tempStacks[i])) {
                        return false;
                    }
                }
            }

            if (i == 8 && noTempItem) {
                return isAllSlotEmpty(handler);
            }
        }

        return true;
    }

    /**
     * Return TRUE if all slots are empty.
     */
    public static boolean isAllSlotEmpty(IItemHandler handler) {
        if (handler == null)
            return true;

        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculate the total STACK COUNT of target item matches (not item amount).
     */
    public static int calcItemStackAmount(IItemHandler handler, ItemStack temp) {
        int targetAmount = 0;

        for (int i = 0; i < handler.getSlots(); i++) {
            if (matchTargetItem(handler.getStackInSlot(i), temp)) {
                targetAmount++;
            }
        }

        return targetAmount;
    }

    /**
     * Check if handler has any matching item, return TRUE if found.
     */
    public static boolean matchTargetItem(IItemHandler handler, ItemStack temp) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (matchTargetItem(handler.getStackInSlot(i), temp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check target item in handler except specified slots.
     * Return slot id, -1 if not found.
     */
    public static int matchTargetItemExceptSlots(IItemHandler handler, ItemStack temp,
                                                 int[] exceptSlots) {
        if (temp.isEmpty())
            return -1;

        for (int i = 0; i < handler.getSlots(); i++) {
            if (CalcHelper.checkIntNotInArray(i, exceptSlots)) {
                if (matchTargetItem(handler.getStackInSlot(i), temp)) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Get and remove matching item from handler, return extracted items.
     *
     * @param handler     inventory to extract from
     * @param temp        template item to match
     * @param number      amount to extract
     * @param exceptSlots slots to exclude from search
     * @return extracted ItemStack or EMPTY if nothing found
     */
    public static ItemStack getAndRemoveItem(IItemHandler handler, ItemStack temp, int number,
                                             int[] exceptSlots) {
        if (temp.isEmpty() || number <= 0)
            return ItemStack.EMPTY;
        if (number > 64)
            number = 64;

        ItemStack getItem = ItemStack.EMPTY;
        int remaining = number;

        while (remaining > 0) {
            int slotid = matchTargetItemExceptSlots(handler, temp, exceptSlots);

            if (slotid < 0) {
                return getItem;
            }

            ItemStack slotStack = handler.getStackInSlot(slotid);
            int toExtract = Math.min(slotStack.getCount(), remaining);

            ItemStack extracted = handler.extractItem(slotid, toExtract, false);
            if (extracted.isEmpty())
                break;

            if (getItem.isEmpty()) {
                getItem = extracted;
                // refine search to exact match
                temp = getItem.copy();
            } else {
                getItem.grow(extracted.getCount());
            }

            remaining -= extracted.getCount();
        }

        return getItem;
    }

    /**
     * Check target stack matches template stack.
     * Uses ItemStack.isSameItemSameTags for 1.20.1 (replaces metadata/NBT/oredict
     * checks).
     */
    public static boolean matchTargetItem(ItemStack target, ItemStack temp) {
        if (target.isEmpty() && temp.isEmpty())
            return true;
        if (target.isEmpty() || temp.isEmpty())
            return false;

        return ItemStack.isSameItemSameTags(target, temp);
    }

    /**
     * Check target matches template with configurable matching.
     *
     * @param checkTags true to also check NBT tags
     */
    public static boolean matchTargetItem(ItemStack target, ItemStack temp, boolean checkTags) {
        if (target.isEmpty() && temp.isEmpty())
            return true;
        if (target.isEmpty() || temp.isEmpty())
            return false;

        if (checkTags) {
            return ItemStack.isSameItemSameTags(target, temp);
        } else {
            return ItemStack.isSameItem(target, temp);
        }
    }

    /**
     * Check slot is NOT mode.
     *
     * @param slotID    slot index (bit position)
     * @param stackMode bit flags where 1 = NOT MODE
     * @return true if slot is in NOT mode
     */
    public static boolean getItemMode(int slotID, int stackMode) {
        return ((stackMode >> slotID) & 1) == 1;
    }

    /**
     * Set item mode bit.
     *
     * @return new stackMode value
     */
    public static int setItemMode(int slotID, int stackMode, boolean notMode) {
        int slot = 1 << slotID;

        if (notMode) {
            stackMode = stackMode | slot;
        } else {
            stackMode = stackMode & (~slot);
        }

        return stackMode;
    }

    /**
     * Get available slots from sided inventory by side configuration.
     * <p>
     * side: TaskSide bit flags from ship:
     * 0~5 bit for input side: Down, Up, N, S, W, E
     * 6~11 bit for output side
     * 12~17 bit for fuel side
     * <p>
     * type: 0=input, 1=output, 2=fuel
     *
     * @param handler the item handler to check
     * @param stack   target item (EMPTY = ignore)
     * @param side    TaskSide bit flags
     * @param type    slot type (0=input, 1=output, 2=fuel)
     * @return array of available slot indices
     */
    public static int[] getSlotsFromSide(IItemHandler handler, ItemStack stack, int side, int type) {
        if (handler == null)
            return new int[0];

        // For non-sided inventories, return all slots
        Set<Integer> slots = new HashSet<>();
        int padbit = type * 6;

        // check which sides are enabled
        boolean anySideEnabled = false;
        for (int i = 0; i < 6; i++) {
            int tarbit = i + padbit;
            if ((side & Values.N.Pow2[tarbit]) == Values.N.Pow2[tarbit]) {
                anySideEnabled = true;
                break;
            }
        }

        if (!anySideEnabled)
            return new int[0];

        // For IItemHandler, we just return all valid slots
        // (sided access is handled by the capability system)
        for (int i = 0; i < handler.getSlots(); i++) {
            if (type != 1) {
                // input/fuel: check can insert
                if (stack.isEmpty() || handler.isItemValid(i, stack)) {
                    slots.add(i);
                }
            } else {
                // output: check can extract
                if (stack.isEmpty() || !handler.extractItem(i, 1, true).isEmpty()) {
                    slots.add(i);
                }
            }
        }

        if (!slots.isEmpty()) {
            return CalcHelper.intSetToArray(slots);
        }
        return new int[0];
    }

    /**
     * Move item stack to IItemHandler with slot specification.
     *
     * @param handler  target inventory
     * @param moveitem item to move (will be modified - count reduced)
     * @param toSlots  specific slots to use, null = all slots
     * @return true if any items were moved
     */
    public static boolean moveItemstackToHandler(IItemHandler handler, ItemStack moveitem,
                                                 int[] toSlots) {
        if (handler == null || moveitem.isEmpty())
            return false;

        return mergeItemStack(handler, moveitem, toSlots);
    }

    /**
     * Merge item stack into handler slots.
     *
     * @param handler target inventory
     * @param stack   item to merge (count will be reduced as items are placed)
     * @param slots   specific slots, null = all slots
     * @return true if any items were placed
     */
    public static boolean mergeItemStack(IItemHandler handler, ItemStack stack, int[] slots) {
        boolean movedItem = false;
        int startId = 0;
        int maxSlots = handler.getSlots();

        if (slots != null) {

            maxSlots = slots.length;
        }

        // try to merge with existing stacks first (if stackable)
        if (stack.isStackable()) {
            int k = startId;

            while (stack.getCount() > 0 && k < maxSlots) {
                int j = (slots != null) ? slots[k] : k;

                ItemStack slotstack = handler.getStackInSlot(j);

                if (!slotstack.isEmpty() && ItemStack.isSameItemSameTags(stack, slotstack)) {
                    int canInsert = Math.min(stack.getCount(),
                            slotstack.getMaxStackSize() - slotstack.getCount());
                    if (canInsert > 0) {
                        ItemStack toInsert = stack.copyWithCount(canInsert);
                        ItemStack remainder = handler.insertItem(j, toInsert, false);
                        int inserted = canInsert - remainder.getCount();
                        if (inserted > 0) {
                            stack.shrink(inserted);
                            movedItem = true;
                        }
                    }
                }

                k++;
            }
        }

        // find empty slot for remaining items
        if (stack.getCount() > 0) {
            int k = startId;

            while (k < maxSlots) {
                int j = (slots != null) ? slots[k] : k;

                ItemStack slotstack = handler.getStackInSlot(j);

                if (slotstack.isEmpty()) {
                    ItemStack remainder = handler.insertItem(j, stack.copy(), false);
                    int inserted = stack.getCount() - remainder.getCount();
                    if (inserted > 0) {
                        stack.shrink(inserted);
                        movedItem = true;
                        if (stack.isEmpty())
                            break;
                    }
                }

                k++;
            }
        }

        return movedItem;
    }

    /**
     * Check item exists in ship inventory (by item type, within slot range).
     */
    public static boolean checkItemInShipInventory(CapaShipInventory inv, Item item,
                                                   int minSlot, int maxSlot) {
        if (inv != null) {
            for (int i = minSlot; i <= maxSlot && i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == item) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Try to fill fluid containers in ship inventory.
     *
     * @param inv ship inventory
     * @param fs  fluid to fill (amount will be reduced)
     * @return true if any fluid was transferred
     */
    public static boolean tryFillContainer(CapaShipInventory inv, FluidStack fs) {
        if (fs == null || fs.isEmpty())
            return false;

        int totalFilled = 0;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);

            // only for single-item stacks that have fluid capability
            if (!stack.isEmpty() && stack.getCount() == 1) {
                net.minecraftforge.common.util.LazyOptional<IFluidHandlerItem> opt = stack
                        .getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM);

                if (opt.isPresent()) {
                    IFluidHandlerItem fluid = opt.orElse(null);

                    int filled = fluid.fill(fs, IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) {
                        totalFilled += filled;
                        fs.shrink(filled);

                        // update the container in inventory
                        inv.setStackInSlot(i, fluid.getContainer());

                        if (fs.isEmpty())
                            break;
                    }
                }
            }
        }


        return totalFilled > 0;
    }

    /**
     * Put itemstack into ship inventory or drop on ground.
     *
     * @return true if put into inventory, false if dropped
     */
    public static boolean moveItemstackToShipOrDrop(BasicEntityShip ship, ItemStack moveitem) {
        if (ship == null || !ship.isAlive() || moveitem.isEmpty())
            return false;

        boolean moved = ship.getCapaShipInventory().addItemStackToInventory(moveitem);

        if (!moved || moveitem.getCount() > 0) {
            dropItemOnGround(ship, moveitem);
            return false;
        }

        return true;
    }

    /**
     * Drop item on ground near entity.
     */
    public static void dropItemOnGround(Entity host, ItemStack stack) {
        if (stack.isEmpty() || host.level().isClientSide())
            return;

        ItemEntity entityitem = new ItemEntity(host.level(),
                host.getX(), host.getY(), host.getZ(), stack.copy());
        entityitem.setDeltaMovement(
                host.level().random.nextGaussian() * 0.08D,
                host.level().random.nextGaussian() * 0.05D + 0.2D,
                host.level().random.nextGaussian() * 0.08D);
        host.level().addFreshEntity(entityitem);
        stack.setCount(0);
    }
}
