package com.lulan.shincolle.network;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.client.gui.inventory.ContainerFormation;
import com.lulan.shincolle.client.gui.inventory.ContainerShipInventory;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.server.ServerDataManager;
import com.lulan.shincolle.team.TeamData;
import com.lulan.shincolle.tileentity.TileEntityCrane;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.tileentity.TileEntityVolCore;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;
import com.lulan.shincolle.utility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Client-to-Server GUI input packet.
 * <p>
 * Sent when the player clicks buttons or submits text in ShinColle GUIs.
 * <p>
 * Ported from 1.10.2 C2SGUIPackets.
 */
public class C2SGUIInputPacket {
    // simple GUI button clicks
    public static final byte ShipBtn = 0;

    // ========== Packet IDs ==========
    public static final byte TileBtn = 1;
    // pointer GUI commands
    public static final byte AddTeam = 20;
    public static final byte AttackTarget = 21;
    public static final byte OpenShipGUI = 22;
    public static final byte SetSitting = 23;
    public static final byte SyncPlayerItem = 24;
    public static final byte GuardEntity = 25;
    public static final byte ClearTeam = 26;
    public static final byte SetShipTeamID = 27;
    public static final byte SetMove = 28;
    public static final byte SetSelect = 29;
    public static final byte SetTarClass = 30;
    public static final byte SetFormation = 31;
    public static final byte OpenItemGUI = 32;
    public static final byte SwapShip = 33;
    public static final byte HitHeight = 35;
    public static final byte SetUnitName = 36;
    // op tool
    public static final byte SetUnatkClass = 50;
    public static final byte ShowUnatkClass = 51;
    // desk GUI
    public static final byte Desk_Create = 70;
    public static final byte Desk_Rename = 71;
    public static final byte Desk_Ally = 72;
    public static final byte Desk_Break = 73;
    public static final byte Desk_Ban = 74;
    public static final byte Desk_Unban = 75;
    public static final byte Desk_Disband = 76;
    public static final byte Desk_FuncSync = 77;
    private static final int LARGE_SHIPYARD_MAT_BUILD_MAX = 1000;

    // ========== Fields ==========
    private final byte type;
    private final int[] values;
    private final String stringData;

    // ========== Constructors ==========

    public C2SGUIInputPacket(byte type, int[] values) {
        this(type, values, null);
    }

    public C2SGUIInputPacket(byte type, int[] values, String stringData) {
        this.type = type;
        this.values = values != null ? values : new int[0];
        this.stringData = stringData;
    }

    /**
     * Decoder constructor
     */
    public C2SGUIInputPacket(FriendlyByteBuf buf) {
        this.type = buf.readByte();
        this.values = PacketHelper.readIntArray(buf);
        this.stringData = PacketHelper.readNullableString(buf);
    }

    private static void handleSmallShipyardBtn(TileEntitySmallShipyard tile, int buttonId, int value) {
        if (buttonId == ID.B.Shipyard_Type) {
            tile.setBuildType(value);
        }
    }

    // ========== Handler ==========

    private static void handleLargeShipyardBtn(TileMultiGrudgeHeavy tile, int buttonId, int value) {
        switch (buttonId) {
            case ID.B.Shipyard_Type:
                tile.setBuildType(Math.max(0, Math.min(value, 4)));
                break;
            case ID.B.Shipyard_InvMode:
                tile.setInvMode(value != 0 ? 1 : 0);
                break;
            case ID.B.Shipyard_SelectMat:
                tile.setSelectMat(Math.max(0, Math.min(value, 3)));
                break;
            case ID.B.Shipyard_INCDEC:
                int matIndex = Math.max(0, Math.min(tile.getSelectMat(), 3));
                applyLargeShipyardMaterialDelta(tile, matIndex, value);
                break;
        }
    }

    // ========== Handler Methods ==========

    private static void applyLargeShipyardMaterialDelta(TileMultiGrudgeHeavy tile, int matIndex, int delta) {
        if (delta == 0)
            return;

        int currentBuild = Math.max(0, tile.getMatBuild(matIndex));
        int stock = Math.max(0, tile.getMatStock(matIndex));
        int maxBuild = Math.min(LARGE_SHIPYARD_MAT_BUILD_MAX, stock);
        int target = currentBuild + delta;

        target = Math.max(0, Math.min(target, maxBuild));
        tile.setMatBuild(matIndex, target);
    }

    private static void handleCraneBtn(TileEntityCrane tile, int buttonId, int value) {
        switch (buttonId) {
            case ID.B.Crane_Power:
                tile.setActive(value != 0);
                break;
            case ID.B.Crane_Mode:
                tile.setCraneMode(value);
                break;
            case ID.B.Crane_Meta:
                tile.setCheckMetadata(value != 0);
                break;
            case ID.B.Crane_Dict:
                tile.setCheckDict(value != 0);
                break;
            case ID.B.Crane_Load:
                tile.setEnabLoad(value != 0);
                break;
            case ID.B.Crane_Unload:
                tile.setEnabUnload(value != 0);
                break;
            case ID.B.Crane_Nbt:
                tile.setCheckNbt(value != 0);
                break;
            case ID.B.Crane_Red:
                // [PORT] 1.10.2 -> 1.20.1: preserve explicit tri-state values from GUI packet.
                tile.setRedSignalMode(value);
                break;
            case ID.B.Crane_Liquid:
                tile.setLiquidMode(value);
                break;
            case ID.B.Crane_Energy:
                tile.setEnergyMode(value);
                break;
        }
    }

    private static void handleVolCoreBtn(TileEntityVolCore tile, int buttonId, int value) {
        if (buttonId == ID.B.VolCore_Power) {
            tile.setBtnActive(!tile.isBtnActive());
        }
    }

    /**
     * Apply a GUI button action to a ship entity.
     * Ported from PacketHelper.setEntityByGUI().
     *
     * @param ship   the target ship
     * @param button the button ID (from ID.B)
     * @param value  the new value
     */
    public static void applyShipGUIButton(BasicEntityShip ship, int button, int value) {
        boolean boolVal = (value != 0);

        switch (button) {
            case ID.B.ShipInv_Melee:
                ship.setStateFlag(ID.F.UseMelee, boolVal);
                break;
            case ID.B.ShipInv_AmmoLight:
                ship.setStateFlag(ID.F.UseAmmoLight, boolVal);
                break;
            case ID.B.ShipInv_AmmoHeavy:
                ship.setStateFlag(ID.F.UseAmmoHeavy, boolVal);
                break;
            case ID.B.ShipInv_AirLight:
                ship.setStateFlag(ID.F.UseAirLight, boolVal);
                break;
            case ID.B.ShipInv_AirHeavy:
                ship.setStateFlag(ID.F.UseAirHeavy, boolVal);
                break;
            case ID.B.ShipInv_FollowMin:
                ship.setStateMinor(ID.M.FollowMin, value);
                if (ship.getStateMinor(ID.M.FollowMin) >= ship.getStateMinor(ID.M.FollowMax)) {
                    ship.setStateMinor(ID.M.FollowMax, value + 1);
                }
                break;
            case ID.B.ShipInv_FollowMax:
                ship.setStateMinor(ID.M.FollowMax, value);
                if (ship.getStateMinor(ID.M.FollowMax) <= ship.getStateMinor(ID.M.FollowMin)) {
                    ship.setStateMinor(ID.M.FollowMin, value - 1);
                }
                break;
            case ID.B.ShipInv_FleeHP:
                ship.setStateMinor(ID.M.FleeHP, value);
                break;
            case ID.B.ShipInv_TarAI:
                ship.setStateFlag(ID.F.PassiveAI, boolVal);
                break;
            case ID.B.ShipInv_AuraEffect:
                ship.setStateFlag(ID.F.UseRingEffect, boolVal);
                break;
            case ID.B.ShipInv_OnSightAI:
                ship.setStateFlag(ID.F.OnSightChase, boolVal);
                break;
            case ID.B.ShipInv_PVPAI:
                ship.setStateFlag(ID.F.PVPFirst, boolVal);
                break;
            case ID.B.ShipInv_AAAI:
                ship.setStateFlag(ID.F.AntiAir, boolVal);
                break;
            case ID.B.ShipInv_ASMAI:
                ship.setStateFlag(ID.F.AntiSS, boolVal);
                break;
            case ID.B.ShipInv_TIMEKEEPAI:
                ship.setStateFlag(ID.F.TimeKeeper, boolVal);
                break;
            case ID.B.ShipInv_InvPage:
                ship.getCapaShipInventory().setInventoryPage(value);
                break;
            case ID.B.ShipInv_PickitemAI:
                ship.setStateFlag(ID.F.PickItem, boolVal);
                break;
            case ID.B.ShipInv_WpStay:
                ship.setStateMinor(ID.M.WpStay, value);
                break;
            case ID.B.ShipInv_ShowHeld:
                ship.setStateFlag(ID.F.ShowHeldItem, boolVal);
                break;
            case ID.B.ShipInv_AutoCR:
                ship.setStateMinor(ID.M.UseCombatRation, value);
                break;
            case ID.B.ShipInv_AutoPump:
                ship.setStateFlag(ID.F.AutoPump, boolVal);
                break;
            case ID.B.ShipInv_Task:
                ship.setStateMinor(ID.M.Task, value);
                break;
            case ID.B.ShipInv_TaskSide:
                ship.setStateMinor(ID.M.TaskSide, value);
                break;
            case ID.B.ShipInv_NoFuel:
                ship.setStateFlag(ID.F.NoFuel, boolVal);
                break;
            default:
                // model state toggles and other buttons
                if (button >= ID.B.ShipInv_ModelState01 && button <= ID.B.ShipInv_ModelState01 + 15) {
                    int bit = button - ID.B.ShipInv_ModelState01;
                    int state = ship.getStateEmotion(ID.S.State);
                    ship.setStateEmotion(ID.S.State, state ^ (1 << bit), false);
                } else {
                    LogHelper.debug("C2SGUIInputPacket: unknown ship button=" + button + " value=" + value);
                }
                break;
        }
    }

    private static void syncDeskTeamData(ServerPlayer player, CapaTeitoku capa) {
        if (player == null || capa == null) {
            return;
        }

        int teamId = capa.getPlayerUID();
        TeamData myTeamData = teamId > 0 ? ServerDataManager.getTeamData(teamId) : null;
        ModNetworking.sendToPlayer(
                S2CGUISyncPacket.syncTeamData(capa, myTeamData, ServerDataManager.getAllTeamWorldData()),
                player);
    }

    /**
     * Find a team ID by the team leader's player name.
     *
     * @param name the leader name to search for
     * @return the team ID, or -1 if not found
     */
    private static int findTeamByLeaderName(String name) {
        if (name == null || name.isEmpty())
            return -1;
        HashMap<Integer, TeamData> allTeams = ServerDataManager.getAllTeamWorldData();
        if (allTeams == null)
            return -1;
        for (var entry : allTeams.entrySet()) {
            if (name.equals(entry.getValue().getTeamLeaderName())) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private static int findTeamSlotByUID(CapaTeitoku capa, int teamId, int shipUid) {
        if (shipUid <= 0) {
            return -1;
        }
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            if (capa.getTeamMember(teamId, i) == shipUid) {
                return i;
            }
        }
        return -1;
    }

    private static int findFirstEmptySlot(CapaTeitoku capa, int teamId) {
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            if (capa.getTeamMember(teamId, i) <= 0 || capa.getTeamSID(teamId, i) <= 0) {
                return i;
            }
        }
        return -1;
    }

    private static BasicEntityShip resolveTeamShip(ServerLevel level, CapaTeitoku capa, int teamId, int slot) {
        int entityId = capa.getTeamSID(teamId, slot);
        if (entityId <= 0) {
            return null;
        }
        Entity shipEnt = level.getEntity(entityId);
        if (shipEnt instanceof BasicEntityShip ship) {
            return ship;
        }
        return null;
    }

    /**
     * Encode
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(type);
        PacketHelper.writeIntArray(buf, values);
        PacketHelper.writeNullableString(buf, stringData);
    }

    // ========== Ship GUI Button Handler ==========

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null)
                return;

            try {
                switch (type) {
                    case ShipBtn:
                        handleShipBtn(sender);
                        break;
                    case TileBtn:
                        handleTileBtn(sender);
                        break;
                    case OpenShipGUI:
                        handleOpenShipGUI(sender);
                        break;
                    case SetSitting:
                        handleSetSitting(sender);
                        break;
                    case HitHeight:
                        handleHitHeight(sender);
                        break;
                    case AddTeam:
                        handleAddTeam(sender);
                        break;
                    case AttackTarget:
                        handleAttackTarget(sender);
                        break;
                    case SyncPlayerItem:
                        handleSyncPlayerItem(sender);
                        break;
                    case GuardEntity:
                        handleGuardEntity(sender);
                        break;
                    case ClearTeam:
                        handleClearTeam(sender);
                        break;
                    case SetShipTeamID:
                        handleSetShipTeamID(sender);
                        break;
                    case SetMove:
                        handleSetMove(sender);
                        break;
                    case SetSelect:
                        handleSetSelect(sender);
                        break;
                    case SetTarClass:
                        handleSetTarClass(sender);
                        break;
                    case SetFormation:
                        handleSetFormation(sender);
                        break;
                    case OpenItemGUI:
                        handleOpenItemGUI(sender);
                        break;
                    case SwapShip:
                        handleSwapShip(sender);
                        break;
                    case SetUnitName:
                        handleSetUnitName(sender);
                        break;
                    case SetUnatkClass:
                        handleSetUnatkClass(sender);
                        break;
                    case ShowUnatkClass:
                        handleShowUnatkClass(sender);
                        break;
                    case Desk_Create:
                        handleDeskCreate(sender);
                        break;
                    case Desk_Rename:
                        handleDeskRename(sender);
                        break;
                    case Desk_Ally:
                        handleDeskAlly(sender);
                        break;
                    case Desk_Break:
                        handleDeskBreak(sender);
                        break;
                    case Desk_Ban:
                        handleDeskBan(sender);
                        break;
                    case Desk_Unban:
                        handleDeskUnban(sender);
                        break;
                    case Desk_Disband:
                        handleDeskDisband(sender);
                        break;
                    case Desk_FuncSync:
                        handleDeskFuncSync(sender);
                        break;
                    default:
                        LogHelper.debug("C2SGUIInputPacket: unknown type=" + type);
                        break;
                }
            } catch (Exception e) {
                LogHelper.debug("C2SGUIInputPacket: handler error type=" + type
                        + " err=" + e.getMessage());
            }
        });
        ctx.setPacketHandled(true);
    }

    // ========== Pointer GUI Command Handlers ==========

    /**
     * Ship entity GUI button click.
     * values: 0:entity id, 1:(unused dim), 2:button id, 3:value
     */
    private void handleShipBtn(ServerPlayer player) {
        if (values.length < 4)
            return;

        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[0]);

        if (entity instanceof BasicEntityShip ship) {
            applyShipGUIButton(ship, values[2], values[3]);
            if (values[2] == ID.B.ShipInv_InvPage
                    && player.containerMenu instanceof ContainerShipInventory menu
                    && menu.getShip() == ship) {
                menu.setInventoryPage(values[3]);
            }
        }
    }

    /**
     * Tile entity GUI button click.
     * values: 0:(unused dim), 1:x, 2:y, 3:z, 4:button id, 5:value
     */
    private void handleTileBtn(ServerPlayer player) {
        if (values.length < 6)
            return;
        BlockPos pos = new BlockPos(values[1], values[2], values[3]);

        // Verify player is near the tile entity
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0)
            return;

        net.minecraft.world.level.block.entity.BlockEntity be = player.serverLevel().getBlockEntity(pos);
        if (be == null)
            return;

        int buttonId = values[4];
        int buttonValue = values[5];

        if (be instanceof TileEntitySmallShipyard tile) {
            handleSmallShipyardBtn(tile, buttonId, buttonValue);
        } else if (be instanceof TileMultiGrudgeHeavy tile) {
            handleLargeShipyardBtn(tile, buttonId, buttonValue);
        } else if (be instanceof TileEntityCrane tile) {
            handleCraneBtn(tile, buttonId, buttonValue);
        } else if (be instanceof TileEntityVolCore tile) {
            handleVolCoreBtn(tile, buttonId, buttonValue);
        }
    }

    /**
     * Open ship GUI.
     * values: 0:player eid, 1:(unused dim), 2:entity id
     */
    private void handleOpenShipGUI(ServerPlayer player) {
        if (values.length < 3)
            return;

        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[2]);

        if (entity instanceof BasicEntityShip ship) {
            ship.openGUI(player);
        }
    }

    /**
     * Set ship sitting state (team-aware).
     * values: 0:player eid, 1:(unused dim), 2:mode, 3:entity id
     * <p>
     * In single mode: toggles sit for the clicked ship only.
     * In group/formation mode: toggles sit for all ships in the current team.
     * If the clicked ship is not in any team, toggles sit for that ship only.
     */
    private void handleSetSitting(ServerPlayer player) {
        if (values.length < 4)
            return;

        ServerLevel level = player.serverLevel();
        int mode = values[2];
        Entity entity = level.getEntity(values[3]);

        if (!(entity instanceof BasicEntityShip clickedShip))
            return;
        if (!TeamHelper.checkSameOwner(player, clickedShip) && !clickedShip.isOwnedBy(player))
            return;

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);
        if (capa == null) {
            boolean newSit = !clickedShip.isOrderedToSit();
            clickedShip.setEntitySit(newSit);
            clickedShip.setRiderAndMountSit();
            return;
        }

        int teamId = capa.getSelectTeam();
        int clickedUid = clickedShip.getStateMinor(ID.M.ShipUID);
        boolean inTeam = findTeamSlotByUID(capa, teamId, clickedUid) >= 0;

        if (!inTeam || mode == 0) {
            // Not in team or single mode: toggle only the clicked ship
            boolean newSit = !clickedShip.isOrderedToSit();
            clickedShip.setEntitySit(newSit);
            clickedShip.setRiderAndMountSit();
        } else {
            // Group/formation mode: toggle all ships in the team
            // Use the first team ship's state to decide toggle direction
            boolean newSit = !clickedShip.isOrderedToSit();
            for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
                BasicEntityShip ship = resolveTeamShip(level, capa, teamId, i);
                if (ship != null) {
                    ship.setEntitySit(newSit);
                    ship.setRiderAndMountSit();
                }
            }
        }
    }

    /**
     * Set hit height for targeting.
     * values: 0:player eid, 1:(unused dim), 2:entity id, 3:height, 4:angle
     */
    private void handleHitHeight(ServerPlayer player) {
        if (values.length < 5)
            return;

        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[2]);

        if (entity instanceof BasicEntityShip ship) {
            ship.setStateMinor(ID.M.HitHeight, values[3]);
            ship.setStateMinor(ID.M.HitAngle, values[4]);
        }
    }

    /**
     * Add a ship entity to the player's currently selected team.
     * values: 0:player eid, 1:(unused dim), 2:entity id
     */
    private void handleAddTeam(ServerPlayer player) {
        if (values.length < 3)
            return;
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[2]);
        if (!(entity instanceof BasicEntityShip ship) || !TeamHelper.checkSameOwner(player, ship)) {
            return;
        }

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);


        int teamId = capa.getSelectTeam();
        int shipUid = ship.getStateMinor(ID.M.ShipUID);
        int existingSlot = findTeamSlotByUID(capa, teamId, shipUid);

        if (existingSlot >= 0) {
            // [PORT] 1.10.2 -> 1.20.1: AddTeam acts as toggle; existing member is removed.
            capa.setTeamMember(teamId, existingSlot, 0);
            capa.setTeamSID(teamId, existingSlot, 0);
            ship.setStateMinor(ID.M.FormatType, 0);
            ship.setStateMinor(ID.M.FormatPos, 0);
        } else {
            int insertSlot = findFirstEmptySlot(capa, teamId);
            if (insertSlot < 0) {
                insertSlot = 0;
            }

            capa.setTeamMember(teamId, insertSlot, shipUid);
            capa.setTeamSID(teamId, insertSlot, ship.getId());
            ship.setStateMinor(ID.M.FormatType, capa.getFormatID(teamId));
            ship.setStateMinor(ID.M.FormatPos, insertSlot);
        }

        // [PORT] 1.10.2 -> 1.20.1: changing team members clears formation selection.
        capa.setFormatID(teamId, 0);
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            BasicEntityShip teamShip = resolveTeamShip(level, capa, teamId, i);
            if (teamShip != null) {
                teamShip.setStateMinor(ID.M.FormatType, 0);
                teamShip.setStateMinor(ID.M.FormatPos, i);
            }
        }

        ModNetworking.sendToPlayer(S2CGUISyncPacket.syncShipsInTeam(capa, teamId), player);
    }

    /**
     * Order all ships in the selected team to attack a target entity.
     * values: 0:player eid, 1:(unused dim), 2:target entity id
     */
    private void handleAttackTarget(ServerPlayer player) {
        if (values.length < 3)
            return;
        ServerLevel level = player.serverLevel();
        Entity target = level.getEntity(values[2]);
        if (target == null)
            return;
        if (TargetHelper.isEntityInvulnerable(target)
                || TeamHelper.checkSameOwner(player, target)
                || TeamHelper.checkIsAlly(player, target)) {
            return;
        }

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int teamId = capa.getSelectTeam();
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            BasicEntityShip ship = resolveTeamShip(level, capa, teamId, i);
            if (ship != null) {
                if (ship.getStateFlag(ID.F.NoFuel))
                    continue;
                ship.setEntitySit(false);
                ship.setTarget(target instanceof LivingEntity le ? le : null);
                ship.setEntityTarget(target);
                ship.applyEmotesReaction(5);
            }
        }
    }

    /**
     * Order all ships in the selected team to guard a target entity.
     * values: 0:player eid, 1:(unused dim), 2:target entity id
     */
    private void handleGuardEntity(ServerPlayer player) {
        if (values.length < 3)
            return;
        ServerLevel level = player.serverLevel();
        Entity target = level.getEntity(values[2]);
        if (target == null)
            return;
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int teamId = capa.getSelectTeam();
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            BasicEntityShip ship = resolveTeamShip(level, capa, teamId, i);
            if (ship != null) {
                if (ship.getStateFlag(ID.F.NoFuel))
                    continue;
                FormationHelper.applyShipGuardEntity(ship, target);
                ship.sendSyncPacketGuard();
            }
        }
    }

    /**
     * Clear all ships from the selected team.
     */
    private void handleClearTeam(ServerPlayer player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int teamId = capa.getSelectTeam();
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            int sid = capa.getTeamSID(teamId, i);
            if (sid > 0) {
                Entity shipEnt = player.serverLevel().getEntity(sid);
                if (shipEnt instanceof BasicEntityShip ship) {
                    ship.setStateMinor(ID.M.FormatType, 0);
                    ship.setStateMinor(ID.M.FormatPos, 0);
                }
            }
            capa.setTeamMember(teamId, i, 0);
            capa.setTeamSID(teamId, i, 0);
        }
        ModNetworking.sendToPlayer(S2CGUISyncPacket.syncShipsInTeam(capa, teamId), player);
    }

    /**
     * Set a ship's team ID.
     * values: 0:player eid, 1:(unused dim), 2:entity id, 3:team id
     */
    private void handleSetShipTeamID(ServerPlayer player) {
        if (values.length < 4)
            return;
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(values[2]);
        if (entity instanceof BasicEntityShip ship && TeamHelper.checkSameOwner(player, ship)) {
            ship.setStateMinor(ID.M.FormatType, values[3]);
        }
    }

    /**
     * Order all ships in the selected team to start moving (unsit) and set guard
     * position.
     * values: 0:player eid, 1:(unused dim), 2:mode, 3:guardType, 4:x, 5:y, 6:z
     */
    private void handleSetMove(ServerPlayer player) {
        if (values.length < 7)
            return;
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        ServerLevel level = player.serverLevel();
        int teamId = capa.getSelectTeam();
        int gx = values[4];
        int gy = values[5];
        int gz = values[6];
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            BasicEntityShip ship = resolveTeamShip(level, capa, teamId, i);
            if (ship != null) {
                if (ship.getStateFlag(ID.F.NoFuel))
                    continue;
                FormationHelper.applyShipGuard(ship, gx, gy, gz, false);
                ship.sendSyncPacketGuard();
            }
        }
    }

    /**
     * Select a team by index.
     * values: 0:player eid, 1:(unused dim), 2:team index
     */
    private void handleSetSelect(ServerPlayer player) {
        if (values.length < 3)
            return;
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        capa.setSelectTeam(values[2]);
        ModNetworking.sendToPlayer(S2CGUISyncPacket.syncShipsInTeam(capa, values[2]), player);
    }

    /**
     * Set the formation type for the selected team.
     * values: 0:player eid, 1:team index, 2:formation id
     */
    private void handleSetFormation(ServerPlayer player) {
        if (values.length < 3)
            return;
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int teamId = values[1];
        capa.setFormatID(teamId, values[2]);
        // Update all ships in team
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            int sid = capa.getTeamSID(teamId, i);
            if (sid > 0) {
                Entity shipEnt = player.serverLevel().getEntity(sid);
                if (shipEnt instanceof BasicEntityShip ship) {
                    ship.setStateMinor(ID.M.FormatType, values[2]);
                }
            }
        }
        ModNetworking.sendToPlayer(S2CGUISyncPacket.syncFormation(capa), player);
    }

    // ========== OP Tool Command Handlers ==========

    /**
     * Toggle a custom target class for the player.
     * Uses stringData for the class name.
     */
    private void handleSetTarClass(ServerPlayer player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int pid = capa.getPlayerUID();
        if (stringData != null && !stringData.isEmpty()) {
            ServerDataManager.setPlayerTargetClass(pid, stringData);
        }
    }

    /**
     * Swap two ships in the selected team's ship slots.
     * values: 0:player eid, 1:(unused dim), 2:slot1, 3:slot2
     */
    private void handleSwapShip(ServerPlayer player) {
        if (values.length < 4)
            return;
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int teamId = capa.getSelectTeam();
        int slot1 = values[2];
        int slot2 = values[3];
        if (slot1 >= 0 && slot1 < CapaTeitoku.SLOT_NUM && slot2 >= 0 && slot2 < CapaTeitoku.SLOT_NUM) {
            int m1 = capa.getTeamMember(teamId, slot1);
            int s1 = capa.getTeamSID(teamId, slot1);
            capa.setTeamMember(teamId, slot1, capa.getTeamMember(teamId, slot2));
            capa.setTeamSID(teamId, slot1, capa.getTeamSID(teamId, slot2));
            capa.setTeamMember(teamId, slot2, m1);
            capa.setTeamSID(teamId, slot2, s1);
            ModNetworking.sendToPlayer(S2CGUISyncPacket.syncShipsInTeam(capa, teamId), player);
        }
    }

    // ========== Desk GUI Command Handlers ==========

    /**
     * Set a team's display name.
     * values: 0:player eid, 1:(unused dim), 2:team index
     * stringData: the new name
     */
    private void handleSetUnitName(ServerPlayer player) {
        if (values.length < 3 || stringData == null)
            return;
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int teamId = values[2];
        if (teamId >= 0 && teamId < CapaTeitoku.TEAM_NUM) {
            capa.setUnitName(teamId, stringData);
            ModNetworking.sendToPlayer(S2CGUISyncPacket.syncUnitNames(capa), player);
        }
    }

    /**
     * Open pointer item GUI.
     * values: 0:player eid, 1:(unused dim), 2:gui type (0=formation)
     */
    private void handleOpenItemGUI(ServerPlayer player) {
        if (values.length < 3)
            return;

        if (values[2] == 0) {// [PORT] 1.10.2 -> 1.20.1: OpenItemGUI is the pointer formation GUI entry
            // point.
            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (containerId, playerInv, p) -> new ContainerFormation(containerId, playerInv),
                    Component.translatable("gui.shincolle.formation.formation")));
        } else {
            LogHelper.debug("C2SGUIInputPacket: unknown OpenItemGUI type=" + values[2]);
        }
    }

    /**
     * Sync selected player item between client and server.
     * Vanilla handles item sync, so this is a no-op.
     */
    private void handleSyncPlayerItem(ServerPlayer player) {
        // Item sync is handled by vanilla; no server action needed
    }

    /**
     * Toggle an unattackable target class (OP tool).
     * Uses stringData for the class name.
     */
    private void handleSetUnatkClass(ServerPlayer player) {
        if (!ServerDataManager.checkOP(player))
            return;
        if (stringData != null && !stringData.isEmpty()) {
            boolean added = ServerDataManager.addUnattackableTargetClass(stringData);
            player.sendSystemMessage(Component.literal(
                    "[ShinColle] Unattackable class " + (added ? "added" : "removed") + ": " + stringData));
        }
    }

    /**
     * Show the unattackable class list to the player (OP tool).
     */
    private void handleShowUnatkClass(ServerPlayer player) {
        if (!ServerDataManager.checkOP(player))
            return;
        HashMap<Integer, String> map = ServerDataManager.getUnattackableTargetClass();
        String classList = (map != null) ? map.values().toString() : "[]";
        player.sendSystemMessage(Component.literal("[ShinColle] Unattackable classes: " + classList));
    }

    /**
     * Create a new team for the player.
     * stringData: the team name
     */
    private void handleDeskCreate(ServerPlayer player) {
        if (stringData == null || stringData.isEmpty())
            return;
        ServerDataManager.teamCreate(player, stringData);
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        ModNetworking.sendToPlayer(S2CGUISyncPacket.syncPlayerMisc(capa), player);
        syncDeskTeamData(player, capa);
    }


    /**
     * Rename the player's own team.
     * stringData: the new team name
     */
    private void handleDeskRename(ServerPlayer player) {
        if (stringData == null || stringData.isEmpty())
            return;
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        ServerDataManager.teamRename(capa.getPlayerUID(), stringData);
        syncDeskTeamData(player, capa);
    }

    /**
     * Add another team as an ally (by leader name).
     * stringData: the target team leader's name
     */
    private void handleDeskAlly(ServerPlayer player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int myTid = capa.getPlayerUID();
        int otherTid = resolveDeskTargetTeamId();
        if (otherTid > 0) {
            ServerDataManager.teamAddAlly(myTid, otherTid);
            syncDeskTeamData(player, capa);
        }
    }

    /**
     * Break alliance with another team (by leader name).
     * stringData: the target team leader's name
     */
    private void handleDeskBreak(ServerPlayer player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int myTid = capa.getPlayerUID();
        int otherTid = resolveDeskTargetTeamId();
        if (otherTid > 0) {
            ServerDataManager.teamRemoveAlly(myTid, otherTid);
            syncDeskTeamData(player, capa);
        }
    }

    /**
     * Ban another team (by leader name, bilateral).
     * stringData: the target team leader's name
     */
    private void handleDeskBan(ServerPlayer player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int myTid = capa.getPlayerUID();
        int otherTid = resolveDeskTargetTeamId();
        if (otherTid > 0) {
            ServerDataManager.teamAddBan(myTid, otherTid);
            syncDeskTeamData(player, capa);
        }
    }

    // ========== Utility ==========

    /**
     * Unban another team (by leader name, unilateral).
     * stringData: the target team leader's name
     */
    private void handleDeskUnban(ServerPlayer player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        int myTid = capa.getPlayerUID();
        int otherTid = resolveDeskTargetTeamId();
        if (otherTid > 0) {
            ServerDataManager.teamRemoveBan(myTid, otherTid);
            syncDeskTeamData(player, capa);
        }
    }

    private int resolveDeskTargetTeamId() {
        if (values.length > 0 && values[0] > 0) {
            return values[0];
        }
        if (stringData != null && !stringData.isEmpty()) {
            return findTeamByLeaderName(stringData);
        }
        return 0;
    }

    /**
     * Disband the player's own team.
     */
    private void handleDeskDisband(ServerPlayer player) {
        ServerDataManager.teamDisband(player);
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        ModNetworking.sendToPlayer(S2CGUISyncPacket.syncPlayerMisc(capa), player);
        syncDeskTeamData(player, capa);
    }


    /**
     * Full player data sync requested from desk GUI.
     */
    private void handleDeskFuncSync(ServerPlayer player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        ModNetworking.sendToPlayer(S2CGUISyncPacket.syncPlayerFull(capa), player);
        syncDeskTeamData(player, capa);
    }


    // ========== Getters ==========

    public byte getType() {
        return type;
    }

    public int[] getValues() {
        return values;
    }

    public String getStringData() {
        return stringData;
    }
}
