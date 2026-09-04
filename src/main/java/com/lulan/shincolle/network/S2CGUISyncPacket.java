package com.lulan.shincolle.network;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.team.TeamData;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.PacketHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Server-to-Client GUI synchronization packet.
 * <p>
 * Syncs GUI, tile entity, and player capability data from server to client.
 * <p>
 * Sub-type categories (from 1.10.2 S2CGUIPackets):
 * 0-5: Tile entity data (SmallShipyard, LargeShipyard, Desk, VolCore, Waypoint,
 * Crane)
 * 40-49: Player capability sync (misc, formation, teams, collections, unit
 * names)
 * 80-81: Flags (initSID, showPlayerSkill)
 * 100-102: GUI sync (ship inventory, ship list, entity item list)
 * <p>
 * Ported from 1.10.2 S2CGUIPackets.
 */
public class S2CGUISyncPacket {

    // ========== Packet IDs ==========

    // tile entity data
    public static final byte TileSmallSY = 0;
    public static final byte TileLargeSY = 1;
    public static final byte TileDesk = 2;
    public static final byte TileVolCore = 3;
    public static final byte TileWaypoint = 4;
    public static final byte TileCrane = 5;

    // player capability sync
    public static final byte SyncPlayerProp = 40;
    public static final byte SyncPlayerProp_TargetClass = 41;
    public static final byte SyncPlayerProp_TeamData = 42;
    public static final byte SyncPlayerProp_Formation = 43;
    public static final byte SyncPlayerProp_ShipsAll = 44;
    public static final byte SyncPlayerProp_ShipsInTeam = 45;
    public static final byte SyncPlayerProp_ColledShip = 46;
    public static final byte SyncPlayerProp_ColledEquip = 47;
    public static final byte SyncPlayerProp_Misc = 48;
    public static final byte SyncPlayerProp_UnitName = 49;

    // flags
    public static final byte FlagInitSID = 80;
    public static final byte FlagShowPlayerSkill = 81;

    // GUI syncs
    public static final byte SyncGUI_ShipInv = 100;
    public static final byte SyncGUI_ShipList = 101;
    public static final byte SyncGUI_EntityItemList = 102;

    // ========== Fields ==========

    private final byte type;
    private final byte[] payload;

    // ========== Constructors ==========

    public S2CGUISyncPacket(byte type, byte[] payload) {
        this.type = type;
        this.payload = payload != null ? payload : new byte[0];
    }

    /**
     * Decoder constructor
     */
    public S2CGUISyncPacket(FriendlyByteBuf buf) {
        this.type = buf.readByte();
        int len = buf.readVarInt();
        this.payload = buf.readByteArray(len);
    }

    private static byte[] toBytes(java.util.function.Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writer.accept(buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }

    // ========== Payload Helpers ==========

    /**
     * Sync player capability misc data
     */
    public static S2CGUISyncPacket syncPlayerMisc(CapaTeitoku capa) {
        byte[] data = toBytes(buf -> {
            buf.writeInt(capa.isRingActive() ? 1 : 0);
            buf.writeInt(capa.getPlayerUID() > 0 ? 1 : 0); // hasTeam
            buf.writeInt(capa.getSelectTeam());
            buf.writeInt(capa.getMarriageNum());
            buf.writeInt(capa.getPlayerUID());
            buf.writeInt(capa.getTeamCooldown());
        });
        return new S2CGUISyncPacket(SyncPlayerProp_Misc, data);
    }

    // ========== Factory Methods ==========

    /**
     * Sync full player capability (misc + formation + current team)
     */
    public static S2CGUISyncPacket syncPlayerFull(CapaTeitoku capa) {
        byte[] data = toBytes(buf -> {
            // misc data (6 ints)
            buf.writeInt(capa.isRingActive() ? 1 : 0);
            buf.writeInt(capa.getPlayerUID() > 0 ? 1 : 0); // hasTeam
            buf.writeInt(capa.getSelectTeam());
            buf.writeInt(capa.getMarriageNum());
            buf.writeInt(capa.getPlayerUID());
            buf.writeInt(capa.getTeamCooldown());

            // formation IDs (9)
            for (int i = 0; i < CapaTeitoku.TEAM_NUM; i++) {
                buf.writeInt(capa.getFormatID(i));
            }

            // current team ship list + select state
            int teamId = capa.getSelectTeam();
            for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
                buf.writeInt(capa.getTeamMember(teamId, i));
                buf.writeInt(capa.getTeamSID(teamId, i));
                buf.writeBoolean(capa.getSelectStateCurrentTeam(i));
            }
        });
        return new S2CGUISyncPacket(SyncPlayerProp, data);
    }

    /**
     * Sync formation IDs
     */
    public static S2CGUISyncPacket syncFormation(CapaTeitoku capa) {
        byte[] data = toBytes(buf -> {
            for (int i = 0; i < CapaTeitoku.TEAM_NUM; i++) {
                buf.writeByte(capa.getFormatID(i));
            }
        });
        return new S2CGUISyncPacket(SyncPlayerProp_Formation, data);
    }

    /**
     * Sync unit names
     */
    public static S2CGUISyncPacket syncUnitNames(CapaTeitoku capa) {
        byte[] data = toBytes(buf -> {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < CapaTeitoku.TEAM_NUM; i++) {
                String name = capa.getUnitName(i);
                names.add(name != null && !name.isEmpty() ? name : " ");
            }
            PacketHelper.writeStringList(buf, names);
        });
        return new S2CGUISyncPacket(SyncPlayerProp_UnitName, data);
    }

    /**
     * Sync ships in a specific team
     */
    public static S2CGUISyncPacket syncShipsInTeam(CapaTeitoku capa, int teamId) {
        byte[] data = toBytes(buf -> {
            buf.writeInt(teamId);
            buf.writeByte(capa.getFormatID(teamId));
            buf.writeByte(1); // has data flag

            for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
                buf.writeInt(capa.getTeamMember(teamId, i));
                buf.writeInt(capa.getTeamSID(teamId, i));
                buf.writeBoolean(capa.getSelectStateCurrentTeam(i));
            }
        });
        return new S2CGUISyncPacket(SyncPlayerProp_ShipsInTeam, data);
    }

    /**
     * Sync all ships in all teams
     */
    public static S2CGUISyncPacket syncShipsAll(CapaTeitoku capa) {
        byte[] data = toBytes(buf -> {
            buf.writeInt(capa.getSelectTeam());

            // formation IDs
            for (int i = 0; i < CapaTeitoku.TEAM_NUM; i++) {
                buf.writeByte(capa.getFormatID(i));
            }

            buf.writeByte(1); // has data flag

            for (int t = 0; t < CapaTeitoku.TEAM_NUM; t++) {
                for (int s = 0; s < CapaTeitoku.SLOT_NUM; s++) {
                    buf.writeInt(capa.getTeamMember(t, s));
                    buf.writeInt(capa.getTeamSID(t, s));
                    buf.writeBoolean(capa.getSelectState(t, s));
                }
            }
        });
        return new S2CGUISyncPacket(SyncPlayerProp_ShipsAll, data);
    }

    /**
     * Sync ship inventory GUI data
     */
    public static S2CGUISyncPacket syncShipInvGUI(BasicEntityShip ship) {
        byte[] data = toBytes(buf -> {
            buf.writeInt(ship.getId());
            buf.writeInt(ship.getStateMinor(ID.M.Kills));
            buf.writeInt(ship.getStateMinor(ID.M.NumGrudge));
            buf.writeInt(ship.getStateMinor(ID.M.NumAmmoLight));
            buf.writeInt(ship.getStateMinor(ID.M.NumAmmoHeavy));
            buf.writeInt(0); // inventory page
        });
        return new S2CGUISyncPacket(SyncGUI_ShipInv, data);
    }

    /**
     * Sync desk team data and known team IDs for team relation GUI.
     */
    public static S2CGUISyncPacket syncTeamData(CapaTeitoku capa, TeamData myTeamData,
                                                Map<Integer, TeamData> allTeams) {
        byte[] data = toBytes(buf -> {
            String teamName = myTeamData != null ? myTeamData.getTeamName() : "";
            List<Integer> allyList = myTeamData != null
                    ? new ArrayList<>(myTeamData.getTeamAllyList())
                    : new ArrayList<>();
            List<Integer> banList = myTeamData != null
                    ? new ArrayList<>(myTeamData.getTeamBannedList())
                    : new ArrayList<>();

            List<Integer> knownTeams = new ArrayList<>();
            if (allTeams != null && !allTeams.isEmpty()) {
                for (Integer tid : allTeams.keySet()) {
                    if (tid != null && tid > 0) {
                        knownTeams.add(tid);
                    }
                }
            }

            if (knownTeams.isEmpty() && capa != null && capa.getPlayerUID() > 0) {
                knownTeams.add(capa.getPlayerUID());
            }

            Collections.sort(knownTeams);

            PacketHelper.writeNullableString(buf, teamName);
            PacketHelper.writeIntList(buf, allyList);
            PacketHelper.writeIntList(buf, banList);
            PacketHelper.writeIntList(buf, knownTeams);
        });
        return new S2CGUISyncPacket(SyncPlayerProp_TeamData, data);
    }

    /**
     * Sync an int list (ship list, collection lists)
     */
    public static S2CGUISyncPacket syncIntList(byte listType, List<Integer> list) {
        byte[] data = toBytes(buf -> PacketHelper.writeIntList(buf, list));
        return new S2CGUISyncPacket(listType, data);
    }

    /**
     * Sync a boolean flag
     */
    public static S2CGUISyncPacket syncFlag(byte flagType, boolean value) {
        byte[] data = toBytes(buf -> buf.writeBoolean(value));
        return new S2CGUISyncPacket(flagType, data);
    }

    /**
     * Sync entity item list (float array)
     */
    public static S2CGUISyncPacket syncEntityItemList(float[] items) {
        byte[] data = toBytes(buf -> PacketHelper.writeFloatArray(buf, items));
        return new S2CGUISyncPacket(SyncGUI_EntityItemList, data);
    }

    @OnlyIn(Dist.CLIENT)
    private static List<Integer> buildFallbackKnownTeamIds(CapaTeitoku capa,
                                                           List<Integer> allyList, List<Integer> banList) {
        List<Integer> known = new ArrayList<>();

        if (capa != null && capa.getPlayerUID() > 0) {
            known.add(capa.getPlayerUID());
        }

        if (allyList != null) {
            for (Integer tid : allyList) {
                if (tid != null && tid > 0 && !known.contains(tid)) {
                    known.add(tid);
                }
            }
        }

        if (banList != null) {
            for (Integer tid : banList) {
                if (tid != null && tid > 0 && !known.contains(tid)) {
                    known.add(tid);
                }
            }
        }

        Collections.sort(known);
        return known;
    }

    // ========== Handler ==========

    @OnlyIn(Dist.CLIENT)
    private static CapaTeitoku getClientCapa() {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return null;
        return player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);
    }

    /**
     * Encode
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(type);
        buf.writeVarInt(payload.length);
        buf.writeByteArray(payload);
    }

    // ========== Handler Methods ==========

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // [PORT] 1.10.2 -> 1.20.1: enforce client-only execution at reception side.
            if (ctx.getDirection().getReceptionSide().isClient()) {
                handleClient();
            }
        });
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        if (payload.length == 0)
            return;

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
        try {
            switch (type) {
                // tile entity syncs
                case TileSmallSY:
                case TileLargeSY:
                case TileDesk:
                case TileVolCore:
                case TileWaypoint:
                case TileCrane:
                    handleTileSync(buf);
                    break;

                // player capability syncs
                case SyncPlayerProp:
                    handleSyncPlayerProp(buf);
                    break;
                case SyncPlayerProp_Misc:
                    handleSyncPlayerMisc(buf);
                    break;
                case SyncPlayerProp_Formation:
                    handleSyncFormation(buf);
                    break;
                case SyncPlayerProp_UnitName:
                    handleSyncUnitName(buf);
                    break;
                case SyncPlayerProp_ShipsAll:
                    handleSyncShipsAll(buf);
                    break;
                case SyncPlayerProp_ShipsInTeam:
                    handleSyncShipsInTeam(buf);
                    break;
                case SyncPlayerProp_ColledShip:
                case SyncPlayerProp_ColledEquip:
                case SyncGUI_ShipList:
                    handleSyncIntList(buf);
                    break;
                case SyncPlayerProp_TargetClass:
                    handleSyncTargetClass(buf);
                    break;
                case SyncPlayerProp_TeamData:
                    handleSyncTeamData(buf);
                    break;

                // flags
                case FlagInitSID:
                case FlagShowPlayerSkill:
                    handleSyncFlag(buf);
                    break;

                // GUI syncs
                case SyncGUI_ShipInv:
                    handleSyncShipInv(buf);
                    break;
                case SyncGUI_EntityItemList:
                    handleSyncEntityItemList(buf);
                    break;

                default:
                    LogHelper.debug("S2CGUISyncPacket: unknown type=" + type);
                    break;
            }
        } catch (Exception e) {
            LogHelper.debug("S2CGUISyncPacket: handler error type=" + type
                    + " err=" + e.getMessage());
        } finally {
            buf.release();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleTileSync(FriendlyByteBuf buf) {
        // Tile entity GUI data sync - log the received payload
        // Full tile entity field sync will be implemented when tile entities
        // have per-type power/progress fields (e.g., build progress, fuel remaining)
        LogHelper.debug("S2CGUISyncPacket: tile sync type=" + type + " payload=" + buf.readableBytes() + " bytes");
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncPlayerProp(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;

        // misc data (6 ints)
        capa.setRingActive(buf.readInt() != 0);
        buf.readInt(); // hasTeam (unused, derived from playerUID)
        capa.setSelectTeam(buf.readInt());
        capa.setMarriageNum(buf.readInt());
        capa.setPlayerUID(buf.readInt());
        capa.setTeamCooldown(buf.readInt());

        // formation IDs (9)
        for (int i = 0; i < CapaTeitoku.TEAM_NUM; i++) {
            capa.setFormatID(i, buf.readInt());
        }

        // current team ship list
        int teamId = capa.getSelectTeam();
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            capa.setTeamMember(teamId, i, buf.readInt());
            capa.setTeamSID(teamId, i, buf.readInt());
            if (buf.readableBytes() > 0) {
                capa.setSelectState(teamId, i, buf.readBoolean());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncPlayerMisc(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;

        capa.setRingActive(buf.readInt() != 0);
        buf.readInt(); // hasTeam
        capa.setSelectTeam(buf.readInt());
        capa.setMarriageNum(buf.readInt());
        capa.setPlayerUID(buf.readInt());
        capa.setTeamCooldown(buf.readInt());
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncFormation(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;

        for (int i = 0; i < CapaTeitoku.TEAM_NUM; i++) {
            capa.setFormatID(i, buf.readByte());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncUnitName(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;

        List<String> names = PacketHelper.readStringList(buf);
        for (int i = 0; i < Math.min(names.size(), CapaTeitoku.TEAM_NUM); i++) {
            String name = names.get(i);
            capa.setUnitName(i, (name != null && !name.trim().isEmpty()) ? name : "");
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncShipsAll(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;

        capa.setSelectTeam(buf.readInt());

        // formation IDs
        for (int i = 0; i < CapaTeitoku.TEAM_NUM; i++) {
            capa.setFormatID(i, buf.readByte());
        }

        int hasData = buf.readByte();
        if (hasData <= 0)
            return;

        for (int t = 0; t < CapaTeitoku.TEAM_NUM; t++) {
            for (int s = 0; s < CapaTeitoku.SLOT_NUM; s++) {
                capa.setTeamMember(t, s, buf.readInt());
                capa.setTeamSID(t, s, buf.readInt());
                if (buf.readableBytes() > 0) {
                    capa.setSelectState(t, s, buf.readBoolean());
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncShipsInTeam(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;

        int teamId = buf.readInt();
        capa.setFormatID(teamId, buf.readByte());

        int hasData = buf.readByte();
        if (hasData <= 0)
            return;

        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            capa.setTeamMember(teamId, i, buf.readInt());
            capa.setTeamSID(teamId, i, buf.readInt());
            if (buf.readableBytes() > 0) {
                capa.setSelectState(teamId, i, buf.readBoolean());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncTargetClass(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;
        List<Integer> classes = PacketHelper.readIntList(buf);
        capa.setTargetClassList(classes);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncTeamData(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;
        // TeamData format: team name (string), ally list (int list), ban list (int
        // list), known team id list (int list)
        String teamName = PacketHelper.readNullableString(buf);
        List<Integer> allyList = PacketHelper.readIntList(buf);
        List<Integer> banList = PacketHelper.readIntList(buf);
        List<Integer> knownTeamIds = buf.readableBytes() > 0
                ? PacketHelper.readIntList(buf)
                : buildFallbackKnownTeamIds(capa, allyList, banList);
        capa.setTeamName(teamName != null ? teamName : "");
        capa.setAllyList(allyList);
        capa.setBanList(banList);
        capa.setKnownTeamIds(knownTeamIds);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncIntList(FriendlyByteBuf buf) {
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;
        List<Integer> list = PacketHelper.readIntList(buf);
        switch (type) {
            case SyncPlayerProp_ColledShip:
                capa.setColledShipList(list);
                break;
            case SyncPlayerProp_ColledEquip:
                capa.setColledEquipList(list);
                break;
            case SyncGUI_ShipList:
                capa.setShipList(list);
                break;
            default:
                LogHelper.debug("S2CGUISyncPacket: unhandled int list type=" + type);
                break;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncFlag(FriendlyByteBuf buf) {
        boolean value = buf.readBoolean();
        CapaTeitoku capa = getClientCapa();
        if (capa == null)
            return;
        switch (type) {
            case FlagInitSID:
                capa.setInitSID(value);
                break;
            case FlagShowPlayerSkill:
                capa.setShowPlayerSkill(value);
                break;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSyncShipInv(FriendlyByteBuf buf) {
        net.minecraft.world.level.Level level = Minecraft.getInstance().level;
        if (level == null)
            return;

        int entityId = buf.readInt();
        int kills = buf.readInt();
        int grudge = buf.readInt();
        int ammoLight = buf.readInt();
        int ammoHeavy = buf.readInt();
        int invPage = buf.readInt();

        Entity entity = level.getEntity(entityId);
        if (entity instanceof BasicEntityShip ship) {
            ship.setStateMinor(ID.M.Kills, kills);
            ship.setStateMinor(ID.M.NumGrudge, grudge);
            ship.setStateMinor(ID.M.NumAmmoLight, ammoLight);
            ship.setStateMinor(ID.M.NumAmmoHeavy, ammoHeavy);
            ship.getCapaShipInventory().setInventoryPage(invPage);
        }
    }

    // ========== Client Utility ==========

    @OnlyIn(Dist.CLIENT)
    private void handleSyncEntityItemList(FriendlyByteBuf buf) {
        float[] items = PacketHelper.readFloatArray(buf);
        CapaTeitoku capa = getClientCapa();
        if (capa != null) {
            capa.setEntityItemList(items);
        }
    }

    // ========== Getters ==========

    public byte getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }
}
