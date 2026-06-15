package com.lulan.shincolle.client.gui.inventory;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModMenuTypes;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Container/Menu for Large Shipyard (Grudge Heavy multiblock).
 * 1 output slot + 9 material slots = 10 slots + player inventory.
 * <p>
 * ContainerData layout:
 * 0: fuelPercent (0-1000)
 * 1: buildPercent (0-1000)
 * 2: buildType (0-4)
 * 3: buildTimeSeconds
 * 4-7: matsStock[0-3] (grudge, abyssium, ammo, polymetal)
 * 8-11: matsBuild[0-3]
 * 12: invMode (0=recycle, 1=release)
 * 13: selectMat (0-3, currently selected material for increment UI)
 */
public class ContainerLargeShipyard extends AbstractContainerMenu {

    public static final int OUTPUT_SLOT = 0;
    public static final int MATERIAL_SLOT_START = 1;
    public static final int MATERIAL_SLOT_COUNT = 9;
    public static final int SHIPYARD_SLOT_COUNT = 10;

    public static final int DATA_FUEL_PERCENT = 0;
    public static final int DATA_BUILD_PERCENT = 1;
    public static final int DATA_BUILD_TYPE = 2;
    public static final int DATA_BUILD_TIME = 3;
    public static final int DATA_STOCK_BASE = 4;
    public static final int DATA_BUILD_BASE = 8;
    public static final int DATA_INV_MODE = 12;
    public static final int DATA_SELECT_MAT = 13;
    public static final int DATA_COUNT = 14;

    private final TileMultiGrudgeHeavy tile;
    private final ContainerData data;

    /**
     * Client-side constructor
     */
    public ContainerLargeShipyard(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, getTileFromBuf(playerInv.player, buf), new SimpleContainerData(DATA_COUNT));
    }

    /**
     * Server-side constructor
     */
    public ContainerLargeShipyard(int containerId, Inventory playerInv, TileMultiGrudgeHeavy tile) {
        this(containerId, playerInv, tile, createTileData(tile));
    }

    private ContainerLargeShipyard(int containerId, Inventory playerInv, TileMultiGrudgeHeavy tile,
                                   ContainerData data) {
        super(ModMenuTypes.LARGE_SHIPYARD.get(), containerId);
        this.tile = tile;
        this.data = data;

        IItemHandler handler = tile != null ? tile.getInventory() : new ItemStackHandler(SHIPYARD_SLOT_COUNT);

        // Output slot (0): right side, below build type buttons
        addSlot(new SlotLargeShipyard(handler, OUTPUT_SLOT, 168, 51, true));

        // Material/fuel slots (1-9): horizontal row (original: 7 + i*18 where i=1..9)
        for (int i = 0; i < MATERIAL_SLOT_COUNT; i++) {
            addSlot(new SlotLargeShipyard(handler, MATERIAL_SLOT_START + i,
                    25 + i * 18, 116, false));
        }

        // Player inventory: original uses (25, 141) for rows and (24, 199) for hotbar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + col, 25 + col * 18, 141 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 24 + col * 18, 199));
        }

        addDataSlots(data);
    }

    private static ContainerData createTileData(TileMultiGrudgeHeavy tile) {
        if (tile == null)
            return new SimpleContainerData(DATA_COUNT);
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_FUEL_PERCENT -> tile.getPowerMax() > 0
                            ? (int) ((long) tile.getPowerRemained() * 1000 / tile.getPowerMax())
                            : 0;
                    case DATA_BUILD_PERCENT -> tile.getPowerGoal() > 0
                            ? (int) ((long) tile.getPowerConsumed() * 1000 / tile.getPowerGoal())
                            : 0;
                    case DATA_BUILD_TYPE -> tile.getBuildType();
                    case DATA_BUILD_TIME -> {
                        int goal = tile.getPowerGoal();
                        int consumed = tile.getPowerConsumed();
                        if (goal <= 0)
                            yield 0;
                        int buildSpeed = (int) ConfigHandler.tileShipyardLarge[1];
                        if (buildSpeed <= 0)
                            yield 0;
                        yield Math.max(0, (goal - consumed) / buildSpeed / 20);
                    }
                    case DATA_STOCK_BASE, DATA_STOCK_BASE + 1, DATA_STOCK_BASE + 2, DATA_STOCK_BASE + 3 ->
                            Math.min(tile.getMatStock(index - DATA_STOCK_BASE), 32767);
                    case DATA_BUILD_BASE, DATA_BUILD_BASE + 1, DATA_BUILD_BASE + 2, DATA_BUILD_BASE + 3 ->
                            Math.min(tile.getMatBuild(index - DATA_BUILD_BASE), 32767);
                    case DATA_INV_MODE -> tile.getInvMode();
                    case DATA_SELECT_MAT -> tile.getSelectMat();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static TileMultiGrudgeHeavy getTileFromBuf(Player player, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof TileMultiGrudgeHeavy t ? t : null;
    }

    protected void addPlayerInventory(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, x + col * 18, y + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (tile == null)
            return false;
        BlockPos pos = tile.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < SHIPYARD_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, SHIPYARD_SLOT_COUNT, SHIPYARD_SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotStack, MATERIAL_SLOT_START, SHIPYARD_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    public TileMultiGrudgeHeavy getTile() {
        return tile;
    }

    public int getFuelPercent() {
        return data.get(DATA_FUEL_PERCENT);
    }

    public int getBuildPercent() {
        return data.get(DATA_BUILD_PERCENT);
    }

    public int getBuildType() {
        return data.get(DATA_BUILD_TYPE);
    }

    public int getBuildTimeSeconds() {
        return data.get(DATA_BUILD_TIME);
    }

    public int getMatStock(int i) {
        return data.get(DATA_STOCK_BASE + i);
    }

    public int getMatBuild(int i) {
        return data.get(DATA_BUILD_BASE + i);
    }

    public int getInvMode() {
        return data.get(DATA_INV_MODE);
    }

    public int getSelectMat() {
        return data.get(DATA_SELECT_MAT);
    }
}
