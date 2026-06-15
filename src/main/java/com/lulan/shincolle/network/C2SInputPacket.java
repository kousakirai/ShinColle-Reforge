package com.lulan.shincolle.network;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.BasicEntitySummon;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.PacketHelper;
import com.lulan.shincolle.utility.TargetHelper;
import com.lulan.shincolle.utility.TileEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-Server input packet.
 * <p>
 * Sent when the player uses keybinds, issues commands, or makes client-side
 * requests that need server processing (outside of GUI context).
 * <p>
 * Ported from 1.10.2 C2SInputPackets.
 */
public class C2SInputPacket {

    // ========== Packet IDs ==========

    public static final byte MountMove = 0;
    public static final byte MountGUI = 1;
    public static final byte SyncHandheld = 2;
    public static final byte CmdChOwner = 3;
    public static final byte CmdShipAttr = 4;
    public static final byte Request_SyncModel = 5;
    public static final byte Request_WpSet = 6;
    public static final byte Request_Riding = 7;
    public static final byte Request_PlaceFluid = 8;
    public static final byte Request_ChestSet = 9;
    public static final byte Request_UnitName = 10;
    public static final byte Request_Buffmap = 11;
    public static final byte Request_EntityItemList = 14;

    // ========== Fields ==========

    private final byte type;
    private final int[] values;

    // ========== Constructors ==========

    public C2SInputPacket(byte type, int... values) {
        this.type = type;
        this.values = values != null ? values : new int[0];
    }

    /**
     * Decoder constructor
     */
    public C2SInputPacket(FriendlyByteBuf buf) {
        this.type = buf.readByte();
        this.values = PacketHelper.readIntArray(buf);
    }

    /**
     * Encode
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(type);
        PacketHelper.writeIntArray(buf, values);
    }

    // ========== Handler ==========

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null)
                return;

            try {
                switch (type) {
                    case MountMove:
                        handleMountMove(sender);
                        break;
                    case MountGUI:
                        handleMountGUI(sender);
                        break;
                    case SyncHandheld:
                        handleSyncHandheld(sender);
                        break;
                    case CmdChOwner:
                        handleCmdChOwner(sender);
                        break;
                    case CmdShipAttr:
                        handleCmdShipAttr(sender);
                        break;
                    case Request_SyncModel:
                        handleRequestSyncModel(sender);
                        break;
                    case Request_Riding:
                        handleRequestRiding(sender);
                        break;
                    case Request_UnitName:
                        handleRequestUnitName(sender);
                        break;
                    case Request_Buffmap:
                        handleRequestBuffmap(sender);
                        break;
                    case Request_WpSet:
                        handleRequestWpSet(sender);
                        break;
                    case Request_ChestSet:
                        handleRequestChestSet(sender);
                        break;
                    case Request_PlaceFluid:
                        handleRequestPlaceFluid(sender);
                        break;
                    case Request_EntityItemList:
                        handleRequestEntityItemList(sender);
                        break;
                    default:
                        LogHelper.debug("C2SInputPacket: unknown type=" + type);
                        break;
                }
            } catch (Exception e) {
                LogHelper.debug("C2SInputPacket: handler error type=" + type
                        + " err=" + e.getMessage());
            }
        });
        ctx.setPacketHandled(true);
    }

    // ========== Handler Methods ==========

    /**
     * Mount movement key input
     */
    private void handleMountMove(ServerPlayer player) {
        if (values.length < 1)
            return;

        if (player.isPassenger() && player.getVehicle() instanceof BasicEntityMount mount) {
            BasicEntityShip ship = mount.getHost();

            if (ship != null && TargetHelper.checkSameOwner(player, ship)) {
                mount.setMountKeyInput(values[0]);
            }
        }
    }

    /**
     * Mount open GUI key
     */
    private void handleMountGUI(ServerPlayer player) {
        // open host ship's GUI when on mount
        if (player.isPassenger() && player.getVehicle() instanceof BasicEntityMount mount) {
            BasicEntityShip ship = mount.getHost();

            if (ship != null && TargetHelper.checkSameOwner(player, ship)) {
                ship.openGUI(player);
            }
        }
        // ship riding on player
        else if (!player.getPassengers().isEmpty()
                && player.getPassengers().get(0) instanceof BasicEntityShip ship) {
            if (TargetHelper.checkSameOwner(player, ship)) {
                ship.openGUI(player);
            }
        }
    }

    /**
     * Sync current handheld item slot
     */
    private void handleSyncHandheld(ServerPlayer player) {
        if (values.length < 1)
            return;
        int slot = values[0];
        if (slot >= 0 && slot < 9) {
            player.getInventory().selected = slot;
        }
    }

    /**
     * Command: change owner
     */
    private void handleCmdChOwner(ServerPlayer player) {
        // Require OP level 2
        if (!player.hasPermissions(2)) {
            LogHelper.info("C2SInputPacket: Player " + player.getName().getString()
                    + " tried to use CmdChOwner without permission");
            return;
        }

        // values: 0:owner eid, 1:ship eid
        if (values.length < 2)
            return;

        ServerLevel level = player.serverLevel();
        Entity ownerEntity = level.getEntity(values[0]);
        Entity shipEntity = level.getEntity(values[1]);

        if (ownerEntity instanceof ServerPlayer newOwner && shipEntity instanceof BasicEntityShip ship) {
            // change ship ownership
            ship.tame(newOwner);
            ship.setStateMinor(ID.M.PlayerUID, -1); // will be reassigned
            ship.sendSyncPacketAll();
            LogHelper.debug("C2SInputPacket: CmdChOwner - changed owner of " + ship + " to " + newOwner);
        }
    }

    /**
     * Command: set ship attributes (admin/debug)
     */
    private void handleCmdShipAttr(ServerPlayer player) {
        // Require OP level 2
        if (!player.hasPermissions(2)) {
            LogHelper.info("C2SInputPacket: Player " + player.getName().getString()
                    + " tried to use CmdShipAttr without permission");
            return;
        }

        // values: 0:ship eid, 1:(unused world id), 2:level, 3-8:bonus values
        if (values.length < 3)
            return;

        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[0]);

        if (entity instanceof BasicEntityShip ship) {
            if (values.length >= 9) {
                Attrs shipAttrs = ship.getAttrs();
                shipAttrs.setAttrsBonus(ID.AttrsBase.HP, values[3]);
                shipAttrs.setAttrsBonus(ID.AttrsBase.ATK, values[4]);
                shipAttrs.setAttrsBonus(ID.AttrsBase.DEF, values[5]);
                shipAttrs.setAttrsBonus(ID.AttrsBase.SPD, values[6]);
                shipAttrs.setAttrsBonus(ID.AttrsBase.MOV, values[7]);
                shipAttrs.setAttrsBonus(ID.AttrsBase.HIT, values[8]);
                ship.setShipLevel(values[2], true);
            } else {
                ship.setShipLevel(values[2], true);
            }
        }
    }

    /**
     * Request server to sync model/emotion display
     */
    private void handleRequestSyncModel(ServerPlayer player) {
        // values: 0:entity id, 1:(unused world id)
        if (values.length < 1)
            return;

        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[0]);

        if (entity instanceof BasicEntityShip ship) {
            ship.sendSyncPacketEmotion();
        } else if (entity instanceof BasicEntityShipHostile hostile) {
            hostile.sendSyncPacket(0);
        } else if (entity instanceof BasicEntitySummon summon) {
            summon.sendSyncPacket(0);
        }
    }

    /**
     * Request riding: make ship ride player
     */
    private void handleRequestRiding(ServerPlayer player) {
        // values: 0:entity id, 1:(unused world id)
        if (values.length < 1)
            return;

        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[0]);

        if (entity instanceof BasicEntityShip ship) {
            if (TargetHelper.checkSameOwner(player, ship)) {
                ship.setEntitySit(false);
                ship.startRiding(player, true);
                ship.getNavigation().stop();
                ship.sendSyncPacketRiders();
            }
        }
    }

    /**
     * Request unit name sync
     */
    private void handleRequestUnitName(ServerPlayer player) {
        // values: 0:entity id, 1:(unused world id)
        if (values.length < 1)
            return;

        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[0]);

        if (entity instanceof BasicEntityShip ship) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncUnitName(ship), ship);
        }
    }

    /**
     * Request buff map sync
     */
    private void handleRequestBuffmap(ServerPlayer player) {
        // values: 0:entity id, 1:(unused world id)
        if (values.length < 1)
            return;

        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[0]);

        if (entity instanceof BasicEntityShip ship) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncBuffMap(ship), ship);
        }
    }

    /**
     * Waypoint pairing
     */
    private void handleRequestWpSet(ServerPlayer player) {
        // values: 0:playerUID, 1-3:from xyz, 4-6:to xyz
        if (values.length < 7)
            return;
        BlockPos posFrom = new BlockPos(values[1], values[2], values[3]);
        BlockPos posTo = new BlockPos(values[4], values[5], values[6]);
        TileEntityHelper.pairingWaypoints(player, values[0], player.serverLevel(), posFrom, posTo);
    }

    /**
     * Chest and waypoint pairing
     */
    private void handleRequestChestSet(ServerPlayer player) {
        // values: 0:playerUID, 1-3:waypoint xyz, 4-6:chest xyz
        if (values.length < 7)
            return;
        BlockPos posWp = new BlockPos(values[1], values[2], values[3]);
        BlockPos posChest = new BlockPos(values[4], values[5], values[6]);
        TileEntityHelper.pairingWaypointAndChest(player, values[0], player.serverLevel(), posWp, posChest);
    }

    /**
     * Ship tank place fluid
     */
    private void handleRequestPlaceFluid(ServerPlayer player) {
        // values: 0:x, 1:y, 2:z
        if (values.length < 3)
            return;
        BlockPos pos = new BlockPos(values[0], values[1], values[2]);

        // Distance check: prevent placing fluid at remote positions
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64)
            return;

        // Place water source at position if ship has fluid tank capability
        ServerLevel level = player.serverLevel();
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
        }
    }

    /**
     * Entity item list for radar
     */
    private void handleRequestEntityItemList(ServerPlayer player) {
        // Gather nearby BasicEntityShip positions for radar display
        ServerLevel level = player.serverLevel();
        java.util.List<BasicEntityShip> ships = level.getEntitiesOfClass(
                BasicEntityShip.class, player.getBoundingBox().inflate(256));
        float[] items = new float[ships.size() * 3]; // x, y, z per ship
        for (int i = 0; i < ships.size(); i++) {
            BasicEntityShip s = ships.get(i);
            items[i * 3] = (float) s.getX();
            items[i * 3 + 1] = (float) s.getY();
            items[i * 3 + 2] = (float) s.getZ();
        }
        ModNetworking.sendToPlayer(S2CGUISyncPacket.syncEntityItemList(items), player);
    }

    // ========== Getters ==========

    public byte getType() {
        return type;
    }

    public int[] getValues() {
        return values;
    }
}
