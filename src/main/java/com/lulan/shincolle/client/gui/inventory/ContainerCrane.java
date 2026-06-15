package com.lulan.shincolle.client.gui.inventory;

import com.lulan.shincolle.init.ModMenuTypes;
import com.lulan.shincolle.tileentity.TileEntityCrane;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Container/Menu for the Crane block.
 * 18 ghost filter slots (9 loading filters + 9 unloading filters) + player
 * inventory.
 * Ghost slots use custom click handling: clicking with an item sets the filter,
 * clicking with empty hand clears it.
 * <p>
 * ContainerData layout:
 * 0: craneMode (0-24)
 * 1: isActive (0/1)
 * 2: isPaired (0/1)
 * 3: enabLoad (0/1)
 * 4: enabUnload (0/1)
 * 5: checkMetadata (0/1)
 * 6: checkNbt (0/1)
 * 7: checkDict (0/1)
 * 8: redSignalMode (0-2)
 * 9: liquidMode (0-2)
 * 10: energyMode (0-2)
 */
public class ContainerCrane extends AbstractContainerMenu {

    public static final int LOADING_FILTER_START = 0;
    public static final int LOADING_FILTER_COUNT = 9;
    public static final int UNLOADING_FILTER_START = 9;
    public static final int UNLOADING_FILTER_COUNT = 9;
    public static final int GHOST_SLOT_COUNT = 18;

    public static final int DATA_CRANE_MODE = 0;
    public static final int DATA_IS_ACTIVE = 1;
    public static final int DATA_IS_PAIRED = 2;
    public static final int DATA_ENAB_LOAD = 3;
    public static final int DATA_ENAB_UNLOAD = 4;
    public static final int DATA_CHECK_META = 5;
    public static final int DATA_CHECK_NBT = 6;
    public static final int DATA_CHECK_DICT = 7;
    public static final int DATA_RED_MODE = 8;
    public static final int DATA_LIQUID_MODE = 9;
    public static final int DATA_ENERGY_MODE = 10;
    public static final int DATA_COUNT = 11;

    private final TileEntityCrane tile;
    private final ContainerData data;

    /**
     * Client-side constructor
     */
    public ContainerCrane(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, getTileFromBuf(playerInv.player, buf), new SimpleContainerData(DATA_COUNT));
    }

    /**
     * Server-side constructor
     */
    public ContainerCrane(int containerId, Inventory playerInv, TileEntityCrane tile) {
        this(containerId, playerInv, tile, createTileData(tile));
    }

    private ContainerCrane(int containerId, Inventory playerInv, TileEntityCrane tile, ContainerData data) {
        super(ModMenuTypes.CRANE.get(), containerId);
        this.tile = tile;
        this.data = data;

        IItemHandler handler = tile != null ? tile.getInventory() : new ItemStackHandler(GHOST_SLOT_COUNT);

        // Loading filter slots (0-8): row at y=65 (original layout)
        for (int i = 0; i < LOADING_FILTER_COUNT; i++) {
            addSlot(new SlotCrane(handler, LOADING_FILTER_START + i, 8 + i * 18, 65));
        }

        // Unloading filter slots (9-17): row at y=96 (original layout)
        for (int i = 0; i < UNLOADING_FILTER_COUNT; i++) {
            addSlot(new SlotCrane(handler, UNLOADING_FILTER_START + i, 8 + i * 18, 96));
        }

        // Player inventory
        addPlayerInventory(playerInv, 8, 119);

        addDataSlots(data);
    }

    private static ContainerData createTileData(TileEntityCrane tile) {
        if (tile == null)
            return new SimpleContainerData(DATA_COUNT);
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_CRANE_MODE -> tile.getCraneMode();
                    case DATA_IS_ACTIVE -> tile.isActive() ? 1 : 0;
                    case DATA_IS_PAIRED -> tile.isPaired() ? 1 : 0;
                    case DATA_ENAB_LOAD -> tile.isEnabLoad() ? 1 : 0;
                    case DATA_ENAB_UNLOAD -> tile.isEnabUnload() ? 1 : 0;
                    case DATA_CHECK_META -> tile.isCheckMetadata() ? 1 : 0;
                    case DATA_CHECK_NBT -> tile.isCheckNbt() ? 1 : 0;
                    case DATA_CHECK_DICT -> tile.isCheckDict() ? 1 : 0;
                    case DATA_RED_MODE -> tile.getRedSignalMode();
                    case DATA_LIQUID_MODE -> tile.getLiquidMode();
                    case DATA_ENERGY_MODE -> tile.getEnergyMode();
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

    private static TileEntityCrane getTileFromBuf(Player player, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof TileEntityCrane t ? t : null;
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

    /**
     * Custom click handling for ghost filter slots.
     * Ghost slots: set filter to a copy of the carried item, or clear if empty
     * hand.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < GHOST_SLOT_COUNT) {
            Slot slot = this.slots.get(slotId);
            ItemStack carried = getCarried();

            if (carried.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                ItemStack filterItem = carried.copy();
                filterItem.setCount(1);
                slot.set(filterItem);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
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
        return ItemStack.EMPTY;
    }

    public TileEntityCrane getTile() {
        return tile;
    }

    public int getCraneMode() {
        return data.get(DATA_CRANE_MODE);
    }

    public boolean isActive() {
        return data.get(DATA_IS_ACTIVE) != 0;
    }

    public boolean isPaired() {
        return data.get(DATA_IS_PAIRED) != 0;
    }

    public boolean isEnabLoad() {
        return data.get(DATA_ENAB_LOAD) != 0;
    }

    public boolean isEnabUnload() {
        return data.get(DATA_ENAB_UNLOAD) != 0;
    }

    public boolean isCheckMetadata() {
        return data.get(DATA_CHECK_META) != 0;
    }

    public boolean isCheckNbt() {
        return data.get(DATA_CHECK_NBT) != 0;
    }

    public boolean isCheckDict() {
        return data.get(DATA_CHECK_DICT) != 0;
    }

    public int getRedSignalMode() {
        return data.get(DATA_RED_MODE);
    }

    public int getLiquidMode() {
        return data.get(DATA_LIQUID_MODE);
    }

    public int getEnergyMode() {
        return data.get(DATA_ENERGY_MODE);
    }
}
