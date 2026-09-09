package com.lulan.shincolle.tileentity;

import com.lulan.shincolle.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Block entity for the Waypoint block.
 * Stores owner UUID, linked waypoint chain (next/last), and paired chest
 * position.
 * Ported from 1.10.2 TileEntityWaypoint.
 */
public class TileEntityWaypoint extends BasicTileEntity implements ITileWaypoint {

    private UUID ownerUUID;
    private int playerUID = -1;
    private BlockPos nextPos = BlockPos.ZERO;
    private BlockPos lastPos = BlockPos.ZERO;
    private BlockPos chestPos = BlockPos.ZERO;
    private int wpstay = 0;

    public TileEntityWaypoint(BlockPos pos, BlockState state) {
        this(ModBlockEntities.WAYPOINT.get(), pos, state);
    }

    public TileEntityWaypoint(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ========== Owner ==========

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
        markChangedAndSync();
    }

    public int getPlayerUID() {
        return playerUID;
    }

    public void setPlayerUID(int uid) {
        this.playerUID = uid;
        markChangedAndSync();
    }

    @Override
    public Entity getHostEntity() {
        return null;
    }

    // ========== Waypoint Route ==========

    public BlockPos getNextWaypoint() {
        return nextPos;
    }

    public void setNextWaypoint(BlockPos pos) {
        this.nextPos = pos != null ? pos : BlockPos.ZERO;
        markChangedAndSync();
    }

    public BlockPos getLastWaypoint() {
        return lastPos;
    }

    public void setLastWaypoint(BlockPos pos) {
        this.lastPos = pos != null ? pos : BlockPos.ZERO;
        markChangedAndSync();
    }

    public boolean hasNextWaypoint() {
        return !nextPos.equals(BlockPos.ZERO);
    }

    public boolean hasLastWaypoint() {
        return !lastPos.equals(BlockPos.ZERO);
    }

    // ========== Chest Pairing ==========

    public BlockPos getPairedChest() {
        return chestPos;
    }

    public void setPairedChest(BlockPos pos) {
        this.chestPos = pos != null ? pos : BlockPos.ZERO;
        markChangedAndSync();
    }

    public boolean hasPairedChest() {
        return !chestPos.equals(BlockPos.ZERO);
    }

    // ========== Stay Time ==========

    public int getWpStayTime() {
        return wpstay;
    }

    public void setWpStayTime(int time) {
        this.wpstay = Math.max(0, Math.min(time, 16));
        markChangedAndSync();
    }

    private void markChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            load(packet.getTag());
        }
    }

    // ========== NBT ==========

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        tag.putInt("PlayerUID", playerUID);
        tag.putInt("NextX", nextPos.getX());
        tag.putInt("NextY", nextPos.getY());
        tag.putInt("NextZ", nextPos.getZ());
        tag.putInt("LastX", lastPos.getX());
        tag.putInt("LastY", lastPos.getY());
        tag.putInt("LastZ", lastPos.getZ());
        tag.putInt("ChestX", chestPos.getX());
        tag.putInt("ChestY", chestPos.getY());
        tag.putInt("ChestZ", chestPos.getZ());
        tag.putInt("WpStay", wpstay);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
        playerUID = tag.getInt("PlayerUID");
        nextPos = new BlockPos(tag.getInt("NextX"), tag.getInt("NextY"), tag.getInt("NextZ"));
        lastPos = new BlockPos(tag.getInt("LastX"), tag.getInt("LastY"), tag.getInt("LastZ"));
        chestPos = new BlockPos(tag.getInt("ChestX"), tag.getInt("ChestY"), tag.getInt("ChestZ"));
        wpstay = tag.getInt("WpStay");
    }
}
