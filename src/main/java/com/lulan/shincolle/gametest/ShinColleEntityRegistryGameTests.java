package com.lulan.shincolle.gametest;

import com.lulan.shincolle.ai.*;
import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.client.gui.inventory.ContainerFormation;
import com.lulan.shincolle.client.gui.inventory.ContainerShipInventory;
import com.lulan.shincolle.crafting.ShipCalc;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.other.EntityFloatingFort;
import com.lulan.shincolle.entity.other.EntityProjectileStatic;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModEntities;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.BasicEquip;
import com.lulan.shincolle.item.PointerItem;
import com.lulan.shincolle.item.ShipSpawnEgg;
import com.lulan.shincolle.network.C2SGUIInputPacket;
import com.lulan.shincolle.network.S2CGUISyncPacket;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.server.ServerDataManager;
import com.lulan.shincolle.team.TeamData;
import com.lulan.shincolle.tileentity.BasicTileMulti;
import com.lulan.shincolle.tileentity.TileEntityCrane;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;
import com.lulan.shincolle.utility.ClientRuntimeHelper;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.MulitBlockHelper;
import com.lulan.shincolle.utility.PacketHelper;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShinColleEntityRegistryGameTests {

    private ShinColleEntityRegistryGameTests() {
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void criticalEntityTypesCreate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        assertCanCreate(level, ModEntities.BB_KIRISHIMA_MOB.get(), "bb_kirishima_mob");
        assertCanCreate(level, ModEntities.SS_KA.get(), "ss_ka");
        assertCanCreate(level, ModEntities.ABYSS_MISSILE.get(), "abyss_missile");
        assertCanCreate(level, ModEntities.BASIC_ENTITY_ITEM.get(), "basic_entity_item");

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void criticalItemsServerSafe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TooltipFlag tooltipFlag = TooltipFlag.Default.NORMAL;

        assertItemTooltipSafe(level, ModItems.MARRIAGE_RING.get(), "marriage_ring", tooltipFlag);
        assertItemTooltipSafe(level, ModItems.POINTER.get(), "pointer", tooltipFlag);
        assertItemTooltipSafe(level, ModItems.EQUIP_CANNON.get(), "equip_cannon", tooltipFlag);

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void allRegisteredItemsTooltipServerSafe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TooltipFlag tooltipFlag = TooltipFlag.Default.NORMAL;

        for (RegistryObject<Item> itemObject : ModItems.ITEMS.getEntries()) {
            Item item = itemObject.get();
            assert itemObject.getId() != null;
            assertItemTooltipSafe(level, item, itemObject.getId().toString(), tooltipFlag);
        }

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void allRegisteredEntityTypesCreate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        for (RegistryObject<EntityType<?>> typeObject : ModEntities.ENTITIES.getEntries()) {
            EntityType<?> type = typeObject.get();
            assert typeObject.getId() != null;
            assertCanCreate(level, type, typeObject.getId().toString());
        }

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void pointerAndRingServerInteractionSafe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer fakePlayer = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000001"), "shincolle_gametest"));

        ItemStack pointer = new ItemStack(ModItems.POINTER.get());
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, pointer);
        InteractionResultHolder<ItemStack> pointerUse = ModItems.POINTER.get().use(level, fakePlayer,
                InteractionHand.MAIN_HAND);
        if (pointerUse.getResult() != InteractionResult.PASS) {
            throw new AssertionError("Pointer use result is not PASS on server: " + pointerUse.getResult());
        }
        ModItems.POINTER.get().inventoryTick(pointer, level, fakePlayer, 0, true);

        ItemStack ring = new ItemStack(ModItems.MARRIAGE_RING.get());
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ring);
        ModItems.MARRIAGE_RING.get().use(level, fakePlayer, InteractionHand.MAIN_HAND);
        if (!ring.getOrCreateTag().getBoolean("isActive")) {
            throw new AssertionError("Marriage ring should be active after first use.");
        }
        ModItems.MARRIAGE_RING.get().use(level, fakePlayer, InteractionHand.MAIN_HAND);
        if (ring.getOrCreateTag().getBoolean("isActive")) {
            throw new AssertionError("Marriage ring should be inactive after second use.");
        }
        for (int i = 0; i < 5; i++) {
            ModItems.MARRIAGE_RING.get().inventoryTick(ring, level, fakePlayer, 0, true);
        }

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void pointerModeNbtAndInvalidModeGuard(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer fakePlayer = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000002"), "shincolle_gametest_mode"));

        ItemStack pointer = new ItemStack(ModItems.POINTER.get());
        PointerItem.setMode(pointer, PointerItem.MODE_FORMATION);
        if (PointerItem.getMode(pointer) != PointerItem.MODE_FORMATION) {
            throw new AssertionError("Pointer mode NBT round-trip failed.");
        }

        PointerItem.setMode(pointer, 3);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, pointer);
        InteractionResultHolder<ItemStack> useResult = ModItems.POINTER.get().use(level, fakePlayer,
                InteractionHand.MAIN_HAND);
        if (useResult.getResult() != InteractionResult.SUCCESS) {
            throw new AssertionError("Pointer invalid mode should return SUCCESS guard path on server.");
        }

        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void pointerOpenItemGuiOpensFormationMenu(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createFollowTestOwner(helper, level,
                UUID.fromString("00000000-0000-0000-0000-000000000004"),
                "shincolle_pointer_open_item_gui");

        C2SGUIInputPacket packet = new C2SGUIInputPacket(
                C2SGUIInputPacket.OpenItemGUI,
                new int[]{player.getId(), 0, 0});
        invokePacketHandler(packet, "handleOpenItemGUI", player);

        if (!(player.containerMenu instanceof ContainerFormation)) {
            throw new AssertionError("Pointer OpenItemGUI should open ContainerFormation menu on server.");
        }

        player.closeContainer();
        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void formationSetUnitNamePacketUpdatesTeamName(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createFollowTestOwner(helper, level,
                UUID.fromString("00000000-0000-0000-0000-000000000005"),
                "shincolle_set_unit_name");

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);


        int teamId = 2;
        String expectedName = "Unit-Alpha";
        C2SGUIInputPacket packet = new C2SGUIInputPacket(
                C2SGUIInputPacket.SetUnitName,
                new int[]{player.getId(), 0, teamId},
                expectedName);
        invokePacketHandler(packet, "handleSetUnitName", player);

        String actualName = capa.getUnitName(teamId);
        if (!expectedName.equals(actualName)) {
            throw new AssertionError("SetUnitName packet should update team name. expected="
                    + expectedName + " actual=" + actualName);
        }

        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void formationSwapShipPacketSwapsSelectedTeamSlots(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createFollowTestOwner(helper, level,
                UUID.fromString("00000000-0000-0000-0000-000000000006"),
                "shincolle_swap_ship");

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);


        int teamId = 3;
        capa.setSelectTeam(teamId);

        int slotA = 1;
        int slotB = 4;
        int memberA = 1111;
        int sidA = 2111;
        int memberB = 4444;
        int sidB = 2444;
        capa.setTeamMember(teamId, slotA, memberA);
        capa.setTeamSID(teamId, slotA, sidA);
        capa.setTeamMember(teamId, slotB, memberB);
        capa.setTeamSID(teamId, slotB, sidB);

        C2SGUIInputPacket packet = new C2SGUIInputPacket(
                C2SGUIInputPacket.SwapShip,
                new int[]{player.getId(), 0, slotA, slotB});
        invokePacketHandler(packet, "handleSwapShip", player);

        if (capa.getTeamMember(teamId, slotA) != memberB || capa.getTeamSID(teamId, slotA) != sidB) {
            throw new AssertionError("SwapShip should move slotB values into slotA for selected team.");
        }
        if (capa.getTeamMember(teamId, slotB) != memberA || capa.getTeamSID(teamId, slotB) != sidA) {
            throw new AssertionError("SwapShip should move slotA values into slotB for selected team.");
        }

        helper.succeed();
    }

    // 2026/04/12：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void deskBreakPacketSupportsTeamIdPayload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createFollowTestOwner(helper, level,
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                "shincolle_desk_break_by_id");

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);


        int myTid = 8001;
        int allyTid = 8002;
        capa.setPlayerUID(myTid);

        ServerDataManager.setTeamData(new TeamData(myTid, "DeskBreakOwner", "desk_break_owner"));
        ServerDataManager.setTeamData(new TeamData(allyTid, "DeskBreakAlly", "desk_break_ally"));
        ServerDataManager.teamAddAlly(myTid, allyTid);

        C2SGUIInputPacket packet = new C2SGUIInputPacket(
                C2SGUIInputPacket.Desk_Break,
                new int[]{allyTid});
        invokePacketHandler(packet, "handleDeskBreak", player);

        TeamData myTeam = ServerDataManager.getTeamData(myTid);
        TeamData allyTeam = ServerDataManager.getTeamData(allyTid);
        if (myTeam == null || allyTeam == null) {
            throw new AssertionError("TeamData missing after desk break packet test setup.");
        }
        if (myTeam.isTeamAlly(allyTid) || allyTeam.isTeamAlly(myTid)) {
            throw new AssertionError("Desk_Break packet with team ID should remove alliance on both teams.");
        }

        helper.succeed();
    }

    // 2026/04/12：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void deskUnbanPacketSupportsTeamIdPayload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createFollowTestOwner(helper, level,
                UUID.fromString("00000000-0000-0000-0000-000000000008"),
                "shincolle_desk_unban_by_id");

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);


        int myTid = 8101;
        int bannedTid = 8102;
        capa.setPlayerUID(myTid);

        ServerDataManager.setTeamData(new TeamData(myTid, "DeskUnbanOwner", "desk_unban_owner"));
        ServerDataManager.setTeamData(new TeamData(bannedTid, "DeskUnbanTarget", "desk_unban_target"));
        ServerDataManager.teamAddBan(myTid, bannedTid);

        C2SGUIInputPacket packet = new C2SGUIInputPacket(
                C2SGUIInputPacket.Desk_Unban,
                new int[]{bannedTid});
        invokePacketHandler(packet, "handleDeskUnban", player);

        TeamData myTeam = ServerDataManager.getTeamData(myTid);
        TeamData bannedTeam = ServerDataManager.getTeamData(bannedTid);
        if (myTeam == null || bannedTeam == null) {
            throw new AssertionError("TeamData missing after desk unban packet test setup.");
        }
        if (myTeam.isTeamBanned(bannedTid)) {
            throw new AssertionError("Desk_Unban packet with team ID should clear ban from player team.");
        }
        if (!bannedTeam.isTeamBanned(myTid)) {
            throw new AssertionError("Desk_Unban should remain unilateral; target team keeps hostile relation.");
        }

        helper.succeed();
    }

    // 2026/04/12：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void syncTeamDataPacketIncludesKnownTeamIds(GameTestHelper helper) {
        CapaTeitoku capa = new CapaTeitoku();
        capa.setPlayerUID(8201);

        TeamData myTeam = new TeamData(8201, "DeskSyncOwner", "desk_sync_owner");
        myTeam.addTeamAlly(8203);
        myTeam.addTeamBanned(8202);

        HashMap<Integer, TeamData> allTeams = new HashMap<>();
        allTeams.put(8205, new TeamData(8205, "DeskTeamC", "desk_c"));
        allTeams.put(8201, myTeam);
        allTeams.put(8202, new TeamData(8202, "DeskTeamB", "desk_b"));

        S2CGUISyncPacket packet = S2CGUISyncPacket.syncTeamData(capa, myTeam, allTeams);
        if (packet.getType() != S2CGUISyncPacket.SyncPlayerProp_TeamData) {
            throw new AssertionError("syncTeamData should create SyncPlayerProp_TeamData packet.");
        }

        FriendlyByteBuf payload = new FriendlyByteBuf(Unpooled.wrappedBuffer(packet.getPayload()));
        String teamName = PacketHelper.readNullableString(payload);
        List<Integer> allyList = PacketHelper.readIntList(payload);
        List<Integer> banList = PacketHelper.readIntList(payload);
        List<Integer> knownTeamIds = PacketHelper.readIntList(payload);

        if (!"DeskSyncOwner".equals(teamName)) {
            throw new AssertionError("syncTeamData should include current team name in payload.");
        }
        if (!allyList.equals(List.of(8203))) {
            throw new AssertionError("syncTeamData should include ally list in payload.");
        }
        if (!banList.equals(List.of(8202))) {
            throw new AssertionError("syncTeamData should include ban list in payload.");
        }
        if (!knownTeamIds.equals(List.of(8201, 8202, 8205))) {
            throw new AssertionError("syncTeamData should include sorted known team IDs.");
        }

        helper.succeed();
    }

    // 2026/04/12：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void craneTileBtnAppliesExplicitValues(GameTestHelper helper) {
        TileEntityCrane tile = new TileEntityCrane(BlockPos.ZERO, Blocks.AIR.defaultBlockState());

        try {
            Method method = C2SGUIInputPacket.class
                    .getDeclaredMethod("handleCraneBtn", TileEntityCrane.class, int.class, int.class);
            method.setAccessible(true);

            // [PORT] 1.10.2 -> 1.20.1: packet payload must apply explicit values, not blind
            // toggles.
            method.invoke(null, tile, (int) ID.B.Crane_Power, 1);
            if (!tile.isActive()) {
                throw new AssertionError("Crane_Power value=1 should enable crane.");
            }
            method.invoke(null, tile, (int) ID.B.Crane_Power, 0);
            if (tile.isActive()) {
                throw new AssertionError("Crane_Power value=0 should disable crane.");
            }

            method.invoke(null, tile, (int) ID.B.Crane_Mode, -3);
            if (tile.getCraneMode() != 0) {
                throw new AssertionError("Crane_Mode should clamp low values to 0.");
            }
            method.invoke(null, tile, (int) ID.B.Crane_Mode, 999);
            if (tile.getCraneMode() != TileEntityCrane.MODE_NAMES.length - 1) {
                throw new AssertionError("Crane_Mode should clamp high values to max mode index.");
            }

            method.invoke(null, tile, (int) ID.B.Crane_Dict, 1);
            if (!tile.isCheckDict()) {
                throw new AssertionError("Crane_Dict value=1 should enable dict check.");
            }
            method.invoke(null, tile, (int) ID.B.Crane_Dict, 0);
            if (tile.isCheckDict()) {
                throw new AssertionError("Crane_Dict value=0 should disable dict check.");
            }

            method.invoke(null, tile, (int) ID.B.Crane_Red, 2);
            if (tile.getRedSignalMode() != 2) {
                throw new AssertionError("Crane_Red should keep explicit mode 2.");
            }
            method.invoke(null, tile, (int) ID.B.Crane_Red, 3);
            if (tile.getRedSignalMode() != 0) {
                throw new AssertionError("Crane_Red values above 2 should wrap to 0.");
            }

            method.invoke(null, tile, (int) ID.B.Crane_Liquid, 1);
            if (tile.getLiquidMode() != 1) {
                throw new AssertionError("Crane_Liquid should apply explicit mode value.");
            }
            method.invoke(null, tile, (int) ID.B.Crane_Energy, 2);
            if (tile.getEnergyMode() != 2) {
                throw new AssertionError("Crane_Energy should apply explicit mode value.");
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Reflection call to handleCraneBtn failed.", e);
        }

        helper.succeed();
    }

    // 2026/04/12：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipInvPagePacketUpdatesOpenedContainerPage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = createFollowTestOwner(helper, level,
                UUID.fromString("00000000-0000-0000-0000-000000000009"),
                "shincolle_ship_inv_page");

        Entity entity = ModEntities.BB_KONGOU.get().create(level);
        if (!(entity instanceof BasicEntityShip ship)) {
            throw new AssertionError("BB_KONGOU is not BasicEntityShip in inventory-page packet test.");
        }
        ship.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 2.5D, 0F, 0F);
        if (!level.addFreshEntity(ship)) {
            throw new AssertionError("Failed to add ship for inventory-page packet test.");
        }

        ship.openGUI(player);
        if (!(player.containerMenu instanceof ContainerShipInventory menu)) {
            throw new AssertionError("Ship GUI should open ContainerShipInventory in inventory-page packet test.");
        }

        C2SGUIInputPacket packet = new C2SGUIInputPacket(
                C2SGUIInputPacket.ShipBtn,
                new int[]{ship.getId(), 0, ID.B.ShipInv_InvPage, 2});
        invokePacketHandler(packet, "handleShipBtn", player);

        int invPageCap = ship.getCapaShipInventory().getInventoryPage();
        if (invPageCap != 2) {
            throw new AssertionError(
                    "ShipInv_InvPage should update capability inventory page to 2, actual=" + invPageCap);
        }
        if (menu.getInventoryPage() != 2) {
            throw new AssertionError("ShipInv_InvPage should update opened ContainerShipInventory page to 2.");
        }

        player.closeContainer();
        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void clientRuntimeHelperServerGuardSafe(GameTestHelper helper) {
        if (ClientRuntimeHelper.getClientPlayer() != null) {
            throw new AssertionError("ClientRuntimeHelper#getClientPlayer must be null on dedicated server.");
        }
        if (ClientRuntimeHelper.isControlDown()) {
            throw new AssertionError("ClientRuntimeHelper#isControlDown must be false on dedicated server.");
        }

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void tooltipFallbackAndHideFlagsServerBehavior(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        ItemStack ring = new ItemStack(ModItems.MARRIAGE_RING.get());
        List<Component> ringTooltip = new ArrayList<>();
        ModItems.MARRIAGE_RING.get().appendHoverText(ring, level, ringTooltip, TooltipFlag.Default.NORMAL);
        if (ringTooltip.isEmpty()) {
            throw new AssertionError("MarriageRing tooltip fallback should provide at least one line on server.");
        }

        Item basicEquipItem = null;
        Item baseTooltipEquipItem = null;
        for (RegistryObject<Item> itemObject : ModItems.ITEMS.getEntries()) {
            Item candidate = itemObject.get();
            if (candidate instanceof BasicEquip) {
                basicEquipItem = candidate;
                try {
                    Class<?> declaringClass = candidate.getClass()
                            .getMethod("appendHoverText", ItemStack.class, Level.class, List.class, TooltipFlag.class)
                            .getDeclaringClass();
                    if (declaringClass == BasicEquip.class) {
                        baseTooltipEquipItem = candidate;
                        break;
                    }
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("Failed to inspect BasicEquip tooltip method.", e);
                }
            }
        }
        if (basicEquipItem == null) {
            throw new AssertionError("No BasicEquip item found in registry.");
        }

        if (baseTooltipEquipItem != null) {
            ItemStack equip = new ItemStack(baseTooltipEquipItem);
            equip.getOrCreateTag();
            List<Component> equipTooltip = new ArrayList<>();
            baseTooltipEquipItem.appendHoverText(equip, level, equipTooltip, TooltipFlag.Default.NORMAL);
            int hideFlags = equip.getOrCreateTag().getInt("HideFlags");
            if (hideFlags != 0 && hideFlags != 1) {
                throw new AssertionError("BasicEquip HideFlags should stay within expected range [0,1], got: "
                        + hideFlags);
            }
        }

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void criticalEntitySpawnAndTickServerSafe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        assertCanSpawnAndTick(level, ModEntities.BB_KIRISHIMA_MOB.get(), "bb_kirishima_mob");
        assertCanSpawnAndTick(level, ModEntities.SS_KA.get(), "ss_ka");
        assertCanSpawnAndTick(level, ModEntities.ABYSS_MISSILE.get(), "abyss_missile");
        assertCanSpawnAndTick(level, ModEntities.BASIC_ENTITY_ITEM.get(), "basic_entity_item");

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void blackHoleSpecialEffectSpawnsStaticProjectile(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity hostEntity = ModEntities.BB_KONGOU.get().create(level);
        if (!(hostEntity instanceof IShipAttackBase host)) {
            throw new AssertionError("BB_KONGOU does not implement IShipAttackBase in test context.");
        }

        hostEntity.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(hostEntity)) {
            throw new AssertionError("Failed to add host entity for black hole special effect test.");
        }

        double x = hostEntity.getX();
        double y = hostEntity.getY();
        double z = hostEntity.getZ();
        CombatHelper.specialAttackEffect(host, 5, new float[]{(float) x, (float) y, (float) z});

        AABB search = new AABB(x - 2D, y - 2D, z - 2D, x + 2D, y + 2D, z + 2D);
        List<EntityProjectileStatic> effects = level.getEntitiesOfClass(EntityProjectileStatic.class, search);
        if (effects.isEmpty()) {
            throw new AssertionError("Black hole special effect did not spawn EntityProjectileStatic.");
        }

        hostEntity.discard();
        for (EntityProjectileStatic effect : effects) {
            effect.discard();
        }

        helper.succeed();
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void floatingFortHeavyAttackDetonatesAndDespawns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity hostEntity = ModEntities.BB_KONGOU.get().create(level);
        Entity targetEntity = ModEntities.SS_KA.get().create(level);
        Entity floatingEntity = ModEntities.FLOATING_FORT.get().create(level);

        if (!(hostEntity instanceof IShipAttackBase host)) {
            throw new AssertionError("BB_KONGOU is not IShipAttackBase in floating fort test.");
        }
        if (!(floatingEntity instanceof EntityFloatingFort floatingFort)) {
            throw new AssertionError("FLOATING_FORT entity type did not create EntityFloatingFort.");
        }
        if (targetEntity == null) {
            throw new AssertionError("Failed to create SS_KA target entity.");
        }

        hostEntity.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        targetEntity.moveTo(3.0D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(hostEntity) || !level.addFreshEntity(targetEntity)) {
            throw new AssertionError("Failed to add host or target entity for floating fort test.");
        }

        floatingFort.initAttrs(host, targetEntity, 0, (float) hostEntity.getY());
        if (!level.addFreshEntity(floatingFort)) {
            throw new AssertionError("Failed to add floating fort entity for test.");
        }

        boolean attacked = floatingFort.attackEntityWithHeavyAmmo(targetEntity);
        if (!attacked) {
            throw new AssertionError("Floating fort heavy attack should detonate when target is close.");
        }
        if (floatingFort.isAlive()) {
            throw new AssertionError("Floating fort should despawn after heavy impact attack.");
        }

        hostEntity.discard();
        targetEntity.discard();
        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hostileSearchlightPlacesLightOnlyAtNight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity entity = ModEntities.BB_KIRISHIMA_MOB.get().create(level);
        if (!(entity instanceof BasicEntityShipHostile hostile)) {
            throw new AssertionError("BB_KIRISHIMA_MOB is not BasicEntityShipHostile in searchlight test.");
        }

        hostile.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(hostile)) {
            throw new AssertionError("Failed to add hostile ship for searchlight test.");
        }

        hostile.setStateMinor(ID.M.LevelSearchlight, 1);
        BlockPos lightPos = hostile.blockPosition().above(2);

        level.setDayTime(6000L);
        level.setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        hostile.updateSearchlight();
        if (!level.getBlockState(lightPos).isAir()) {
            throw new AssertionError("Searchlight should not place light block during daytime.");
        }

        level.setDayTime(13000L);
        hostile.updateSearchlight();
        if (!level.getBlockState(lightPos).is(ModBlocks.LIGHT_AIR.get())) {
            throw new AssertionError("Searchlight should place LIGHT_AIR block at night.");
        }

        hostile.discard();
        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hostileSearchlightSkipsWhenNoFuel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity entity = ModEntities.BB_KIRISHIMA_MOB.get().create(level);
        if (!(entity instanceof BasicEntityShipHostile hostile)) {
            throw new AssertionError("BB_KIRISHIMA_MOB is not BasicEntityShipHostile in no-fuel searchlight test.");
        }

        hostile.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(hostile)) {
            throw new AssertionError("Failed to add hostile ship for no-fuel searchlight test.");
        }

        hostile.setStateMinor(ID.M.LevelSearchlight, 1);
        hostile.setStateFlag(ID.F.NoFuel, true);
        BlockPos lightPos = hostile.blockPosition().above(2);

        level.setDayTime(13000L);
        level.setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        hostile.updateSearchlight();
        if (!level.getBlockState(lightPos).isAir()) {
            throw new AssertionError("Searchlight should not place LIGHT_AIR when NoFuel flag is set.");
        }

        hostile.discard();
        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hostileSearchlightSkipsWhenNotAlive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity entity = ModEntities.BB_KIRISHIMA_MOB.get().create(level);
        if (!(entity instanceof BasicEntityShipHostile hostile)) {
            throw new AssertionError("BB_KIRISHIMA_MOB is not BasicEntityShipHostile in not-alive searchlight test.");
        }

        hostile.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(hostile)) {
            throw new AssertionError("Failed to add hostile ship for not-alive searchlight test.");
        }

        hostile.setStateMinor(ID.M.LevelSearchlight, 1);
        BlockPos lightPos = hostile.blockPosition().above(2);

        level.setDayTime(13000L);
        level.setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        hostile.discard();
        hostile.updateSearchlight();
        if (!level.getBlockState(lightPos).isAir()) {
            throw new AssertionError("Searchlight should not place LIGHT_AIR when entity is not alive.");
        }

        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hostileSearchlightNightWindowBoundaries(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity entity = ModEntities.BB_KIRISHIMA_MOB.get().create(level);
        if (!(entity instanceof BasicEntityShipHostile hostile)) {
            throw new AssertionError("BB_KIRISHIMA_MOB is not BasicEntityShipHostile in boundary searchlight test.");
        }

        hostile.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(hostile)) {
            throw new AssertionError("Failed to add hostile ship for boundary searchlight test.");
        }

        hostile.setStateMinor(ID.M.LevelSearchlight, 1);
        BlockPos lightPos = hostile.blockPosition().above(2);

        level.setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        level.setDayTime(12499L);
        hostile.updateSearchlight();
        if (!level.getBlockState(lightPos).isAir()) {
            throw new AssertionError("Searchlight should not place LIGHT_AIR at time 12499.");
        }

        level.setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        level.setDayTime(12500L);
        hostile.updateSearchlight();
        if (!level.getBlockState(lightPos).is(ModBlocks.LIGHT_AIR.get())) {
            throw new AssertionError("Searchlight should place LIGHT_AIR at time 12500.");
        }

        level.setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        level.setDayTime(23500L);
        hostile.updateSearchlight();
        if (!level.getBlockState(lightPos).is(ModBlocks.LIGHT_AIR.get())) {
            throw new AssertionError("Searchlight should place LIGHT_AIR at time 23500.");
        }

        level.setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        level.setDayTime(23501L);
        hostile.updateSearchlight();
        if (!level.getBlockState(lightPos).isAir()) {
            throw new AssertionError("Searchlight should not place LIGHT_AIR at time 23501.");
        }

        hostile.discard();
        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipPickItemGoalRespectsFuelAndPickFlag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity entity = ModEntities.BB_KONGOU.get().create(level);
        if (!(entity instanceof BasicEntityShip ship)) {
            throw new AssertionError("BB_KONGOU is not BasicEntityShip in pick-item goal test.");
        }

        ship.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(ship)) {
            throw new AssertionError("Failed to add ship entity for pick-item goal test.");
        }

        // [PORT] 1.10.2 -> 1.20.1: lock AI guard behavior for item pickup goal.
        ShipPickItemGoal goal = new ShipPickItemGoal(ship, 6.0F);

        ship.setStateFlag(ID.F.PickItem, true);
        ship.setStateFlag(ID.F.NoFuel, false);
        if (!goal.canUse()) {
            throw new AssertionError("ShipPickItemGoal should be usable when PickItem=true and NoFuel=false.");
        }

        ship.setStateFlag(ID.F.NoFuel, true);
        if (goal.canUse()) {
            throw new AssertionError("ShipPickItemGoal should be blocked when NoFuel=true.");
        }

        ship.setStateFlag(ID.F.NoFuel, false);
        ship.setStateFlag(ID.F.PickItem, false);
        if (goal.canUse()) {
            throw new AssertionError("ShipPickItemGoal should be blocked when PickItem=false.");
        }

        ship.setStateFlag(ID.F.PickItem, true);
        ship.setOrderedToSit(true);
        if (goal.canUse()) {
            throw new AssertionError("ShipPickItemGoal should be blocked when ship is sitting.");
        }
        ship.setOrderedToSit(false);

        ship.setStateMinor(ID.M.CraneState, 1);
        if (goal.canUse()) {
            throw new AssertionError("ShipPickItemGoal should be blocked when CraneState > 0.");
        }
        ship.setStateMinor(ID.M.CraneState, 0);

        ship.discard();
        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipFollowOwnerGoalRespectsCoreGuards(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity entity = ModEntities.BB_KONGOU.get().create(level);
        if (!(entity instanceof BasicEntityShip ship)) {
            throw new AssertionError("BB_KONGOU is not BasicEntityShip in follow-owner goal test.");
        }

        ServerPlayer owner = createFollowTestOwner(helper, level,
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "shincolle_follow_owner");
        owner.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);

        ship.moveTo(24.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(ship)) {
            throw new AssertionError("Failed to add ship entity for follow-owner goal test.");
        }

        // [PORT] 1.10.2 -> 1.20.1: lock owner-follow activation and guard branches.
        ship.tame(owner);
        ship.setOwnerUUID(owner.getUUID());
        ship.setEntitySit(false);
        ship.setStateFlag(ID.F.CanFollow, true);
        ship.setStateMinor(ID.M.CraneState, 0);
        ship.setStateMinor(ID.M.NumGrudge, 120);
        ship.setStateMinor(ID.M.FollowMin, 2);
        ship.setStateMinor(ID.M.FollowMax, 6);

        ShipFollowOwnerGoal goal = new ShipFollowOwnerGoal(ship);
        boolean ownerResolved = ship.getOwner() != null;

        if (ownerResolved) {
            if (!goal.canUse()) {
                throw new AssertionError(
                        "ShipFollowOwnerGoal should be usable with valid owner and follow conditions.");
            }

            ship.setStateFlag(ID.F.CanFollow, false);
            if (goal.canUse()) {
                throw new AssertionError("ShipFollowOwnerGoal should be blocked when CanFollow=false.");
            }

            ship.setStateFlag(ID.F.CanFollow, true);
            ship.setStateMinor(ID.M.NumGrudge, 0);
            if (goal.canUse()) {
                throw new AssertionError("ShipFollowOwnerGoal should be blocked when NumGrudge<=0.");
            }

            ship.setStateMinor(ID.M.NumGrudge, 120);
            ship.setStateMinor(ID.M.CraneState, 1);
            if (goal.canUse()) {
                throw new AssertionError("ShipFollowOwnerGoal should be blocked when CraneState>=1.");
            }

            ship.setStateMinor(ID.M.CraneState, 0);
            ship.setEntitySit(true);
            if (goal.canUse()) {
                throw new AssertionError("ShipFollowOwnerGoal should be blocked when ship is sitting.");
            }
        } else {
            if (goal.canUse()) {
                throw new AssertionError(
                        "ShipFollowOwnerGoal should not activate when owner resolution is unavailable.");
            }
        }

        ship.discard();
        helper.succeed();
    }

    // 2026/04/15：GitHub Copilotによって追加
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void lightCruiserSkillAttackGoalUsesLegacyPriority(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        assertLightCruiserGoalPriority(level, ModEntities.CL_TENRYUU.get(), "cl_tenryuu");
        assertLightCruiserGoalPriority(level, ModEntities.CL_TATSUTA.get(), "cl_tatsuta");

        helper.succeed();
    }

    // 2026/04/15：GitHub Copilotによって追加
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void mountFollowHostTeleportsWhenFarAndUnridden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity hostEntity = ModEntities.BB_KONGOU.get().create(level);
        if (!(hostEntity instanceof BasicEntityShip host)) {
            throw new AssertionError("BB_KONGOU is not BasicEntityShip in mount-follow test.");
        }

        Entity mountEntity = ModEntities.MOUNT_BAH.get().create(level);
        if (!(mountEntity instanceof BasicEntityMount mount)) {
            throw new AssertionError("MOUNT_BAH is not BasicEntityMount in mount-follow test.");
        }

        host.moveTo(48.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        mount.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);

        if (!level.addFreshEntity(host)) {
            throw new AssertionError("Failed to add host ship entity in mount-follow test.");
        }
        if (!level.addFreshEntity(mount)) {
            throw new AssertionError("Failed to add mount entity in mount-follow test.");
        }

        mount.setHost(host);
        mount.setAIList();

        double initialDistSq = mount.distanceToSqr(host);
        if (initialDistSq <= 1024.0D) {
            throw new AssertionError("Mount-follow test requires long initial distance. actual=" + initialDistSq);
        }

        for (int i = 0; i < 20; i++) {
            mount.tick();
            if (mount.distanceToSqr(host) <= 4.0D) {
                break;
            }
        }

        double finalDistSq = mount.distanceToSqr(host);
        if (finalDistSq > 4.0D) {
            throw new AssertionError("Mount should follow/teleport to host when far and unridden. finalDistSq="
                    + finalDistSq);
        }

        mount.discard();
        host.discard();
        helper.succeed();
    }

    // 2026/04/15：GitHub Copilotによって追加
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void mountFollowHostBlockedWhenRidden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity hostEntity = ModEntities.BB_KONGOU.get().create(level);
        if (!(hostEntity instanceof BasicEntityShip host)) {
            throw new AssertionError("BB_KONGOU is not BasicEntityShip in mount-ridden test.");
        }

        Entity mountEntity = ModEntities.MOUNT_BAH.get().create(level);
        if (!(mountEntity instanceof BasicEntityMount mount)) {
            throw new AssertionError("MOUNT_BAH is not BasicEntityMount in mount-ridden test.");
        }

        ServerPlayer rider = createFollowTestOwner(helper, level,
                UUID.fromString("00000000-0000-0000-0000-000000000007"),
                "shincolle_mount_rider");

        host.moveTo(48.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        mount.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        rider.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);

        if (!level.addFreshEntity(host)) {
            throw new AssertionError("Failed to add host ship entity in mount-ridden test.");
        }
        if (!level.addFreshEntity(mount)) {
            throw new AssertionError("Failed to add mount entity in mount-ridden test.");
        }

        mount.setHost(host);
        mount.setAIList();

        if (!rider.startRiding(mount, true)) {
            throw new AssertionError("Failed to mount rider onto BasicEntityMount in mount-ridden test.");
        }

        double initialDistSq = mount.distanceToSqr(host);
        if (initialDistSq <= 1024.0D) {
            throw new AssertionError("Mount-ridden test requires long initial distance. actual=" + initialDistSq);
        }

        for (int i = 0; i < 20; i++) {
            mount.tick();
        }

        double finalDistSq = mount.distanceToSqr(host);
        if (finalDistSq < 256.0D) {
            throw new AssertionError("Mount follow should be blocked while ridden. finalDistSq=" + finalDistSq);
        }

        rider.stopRiding();
        mount.discard();
        host.discard();
        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipSpawnEggSpecificClassPriorityAndLegacyTypeConversion(GameTestHelper helper) {
        ItemStack classOnly = new ItemStack(ModItems.SHIP_SPAWN_EGG.get());
        ShipSpawnEgg.setShipClass(classOnly, ID.ShipClass.DDShimakaze);

        ItemStack mixed = new ItemStack(ModItems.SHIP_SPAWN_EGG.get());
        CompoundTag mixedTag = mixed.getOrCreateTag();
        mixedTag.putByte("BuildType", (byte) 1);
        mixedTag.putInt(ShipSpawnEgg.TAG_SHIP_TYPE, ID.ShipClass.DDShimakaze + 2);

        int convertedClass = ShipSpawnEgg.getShipClass(mixed);
        if (convertedClass != ID.ShipClass.DDShimakaze) {
            throw new AssertionError("Legacy ShipType metadata conversion failed. expected="
                    + ID.ShipClass.DDShimakaze + " actual=" + convertedClass);
        }

        String expectedDescription = classOnly.getDescriptionId();
        String actualDescription = mixed.getDescriptionId();
        if (!expectedDescription.equals(actualDescription)) {
            throw new AssertionError("Specific-class egg name should override BuildType name. expected="
                    + expectedDescription + " actual=" + actualDescription);
        }

        int expectedIcon = ShipSpawnEgg.getEggIcon(classOnly);
        int actualIcon = ShipSpawnEgg.getEggIcon(mixed);
        if (expectedIcon != actualIcon) {
            throw new AssertionError("Specific-class egg icon should override BuildType icon. expected="
                    + expectedIcon + " actual=" + actualIcon);
        }

        ItemStack hostileLegacy = new ItemStack(ModItems.SHIP_SPAWN_EGG.get());
        hostileLegacy.getOrCreateTag().putInt(ShipSpawnEgg.TAG_SHIP_TYPE,
                ShipSpawnEgg.MOB_OFFSET + ID.ShipClass.DDI + 2);
        int hostileConverted = ShipSpawnEgg.getShipClass(hostileLegacy);
        int expectedHostileClass = ShipSpawnEgg.MOB_OFFSET + ID.ShipClass.DDI;
        if (hostileConverted != expectedHostileClass) {
            throw new AssertionError("Hostile legacy ShipType metadata conversion failed. expected="
                    + expectedHostileClass + " actual=" + hostileConverted);
        }

        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipSpawnEggIconMappingMatchesLegacyCategories(GameTestHelper helper) {
        ItemStack smallBuild = new ItemStack(ModItems.SHIP_SPAWN_EGG.get());
        smallBuild.getOrCreateTag().putByte("BuildType", (byte) 0);
        if (ShipSpawnEgg.getEggIcon(smallBuild) != 0) {
            throw new AssertionError("Small build egg icon mismatch. expected=0 actual="
                    + ShipSpawnEgg.getEggIcon(smallBuild));
        }

        ItemStack largeBuild = new ItemStack(ModItems.SHIP_SPAWN_EGG.get());
        largeBuild.getOrCreateTag().putByte("BuildType", (byte) 1);
        if (ShipSpawnEgg.getEggIcon(largeBuild) != 1) {
            throw new AssertionError("Large build egg icon mismatch. expected=1 actual="
                    + ShipSpawnEgg.getEggIcon(largeBuild));
        }

        assertEggIconForShipClass(ID.ShipClass.DDI, 2);
        assertEggIconForShipClass(ID.ShipClass.CLTenryuu, 3);
        assertEggIconForShipClass(ID.ShipClass.CAAtago, 4);
        assertEggIconForShipClass(ID.ShipClass.BBKongou, 5);
        assertEggIconForShipClass(ID.ShipClass.APWA, 6);
        assertEggIconForShipClass(ID.ShipClass.SSKA, 7);
        assertEggIconForShipClass(ID.ShipClass.CVWD, 8);
        assertEggIconForShipClass(ID.ShipClass.CVHime, 9);
        assertEggIconForShipClass(ID.ShipClass.CVKaga, 10);

        assertEggIconForShipClass(ID.ShipClass.DDI + ShipSpawnEgg.MOB_OFFSET, 2);
        assertEggIconForShipClass(ID.ShipClass.CVKaga + ShipSpawnEgg.MOB_OFFSET, 10);

        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipSpawnEggModelLayerAssignmentsMatchLegacy(GameTestHelper helper) {
        // [PORT] 1.10.2 -> 1.20.1: lock legacy icon->texture assignment to avoid silent
        // model drift in spawn eggs.
        assertModelLayer0("ship_spawn_egg.json", "shincolle:item/ship_spawn_egg_s");
        assertModelLayer0("ship_spawn_egg_l.json", "shincolle:item/ship_spawn_egg_l");
        assertModelLayer0("ship_spawn_egg_dd.json", "shincolle:item/ship_spawn_egg_0");
        assertModelLayer0("ship_spawn_egg_cl.json", "shincolle:item/ship_spawn_egg_1");
        assertModelLayer0("ship_spawn_egg_ca.json", "shincolle:item/ship_spawn_egg_2");
        assertModelLayer0("ship_spawn_egg_bb.json", "shincolle:item/ship_spawn_egg_3");
        assertModelLayer0("ship_spawn_egg_ap.json", "shincolle:item/ship_spawn_egg_4");
        assertModelLayer0("ship_spawn_egg_ss.json", "shincolle:item/ship_spawn_egg_5");
        assertModelLayer0("ship_spawn_egg_wd.json", "shincolle:item/ship_spawn_egg_6");
        assertModelLayer0("ship_spawn_egg_hime.json", "shincolle:item/ship_spawn_egg_7");
        assertModelLayer0("ship_spawn_egg_cv.json", "shincolle:item/ship_spawn_egg_8");

        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hostileGoalListContainsWanderAndOpenDoor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity entity = ModEntities.BB_KIRISHIMA_MOB.get().create(level);
        if (!(entity instanceof BasicEntityShipHostile hostile)) {
            throw new AssertionError("BB_KIRISHIMA_MOB is not BasicEntityShipHostile in hostile goal list test.");
        }

        hostile.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(hostile)) {
            throw new AssertionError("Failed to add hostile ship for goal list test.");
        }

        invokeNoArgProtected(hostile, "clearAITasks");
        invokeNoArgProtected(hostile, "setAIList");

        GoalSelector selector = extractGoalSelector(hostile);
        boolean hasWander = false;
        boolean hasOpenDoor = false;

        for (WrappedGoal wrappedGoal : selector.getAvailableGoals()) {
            Goal goal = wrappedGoal.getGoal();
            if (goal instanceof ShipHostileWanderGoal) {
                hasWander = true;
            }
            if (goal instanceof ShipOpenDoorGoal) {
                hasOpenDoor = true;
            }
        }

        if (!hasWander || !hasOpenDoor) {
            throw new AssertionError("Hostile goal list missing expected mobility goals. hasWander="
                    + hasWander + " hasOpenDoor=" + hasOpenDoor);
        }

        hostile.discard();
        helper.succeed();
    }

    // 2026/04/15：GitHub Copilotによって追加
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hostileTargetGoalPrioritiesMatchLegacy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity entity = ModEntities.BB_KIRISHIMA_MOB.get().create(level);
        if (!(entity instanceof BasicEntityShipHostile hostile)) {
            throw new AssertionError("BB_KIRISHIMA_MOB is not BasicEntityShipHostile in target-priority test.");
        }

        hostile.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(hostile)) {
            throw new AssertionError("Failed to add hostile ship for target-priority test.");
        }

        invokeNoArgProtected(hostile, "clearAITargetTasks");
        invokeNoArgProtected(hostile, "setAITargetList");

        GoalSelector targetSelector = extractTargetSelector(hostile);
        int revengePriority = findGoalPriority(targetSelector, ShipRevengeTargetGoal.class);
        int rangePriority = findGoalPriority(targetSelector, ShipRangeTargetGoal.class);

        if (revengePriority != 1) {
            throw new AssertionError("Hostile revenge target priority mismatch. expected=1 actual=" + revengePriority);
        }
        if (rangePriority != 3) {
            throw new AssertionError("Hostile range target priority mismatch. expected=3 actual=" + rangePriority);
        }

        hostile.discard();
        helper.succeed();
    }

    // 2026/04/20：GitHub Copilotによって追加
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hostileRangeTargetGoalAcquiresFriendlyShip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity hostileEntity = ModEntities.BB_KIRISHIMA_MOB.get().create(level);
        Entity friendlyEntity = ModEntities.BB_KONGOU.get().create(level);

        if (!(hostileEntity instanceof BasicEntityShipHostile hostile)) {
            throw new AssertionError("BB_KIRISHIMA_MOB is not BasicEntityShipHostile in hostile target-acquire test.");
        }
        if (!(friendlyEntity instanceof BasicEntityShip friendly)) {
            throw new AssertionError("BB_KONGOU is not BasicEntityShip in hostile target-acquire test.");
        }

        hostile.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        friendly.moveTo(4.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);

        if (!level.addFreshEntity(hostile)) {
            throw new AssertionError("Failed to add hostile ship for hostile target-acquire test.");
        }
        if (!level.addFreshEntity(friendly)) {
            throw new AssertionError("Failed to add friendly ship for hostile target-acquire test.");
        }

        invokeNoArgProtected(hostile, "clearAITargetTasks");
        invokeNoArgProtected(hostile, "setAITargetList");

        GoalSelector selector = extractTargetSelector(hostile);
        ShipRangeTargetGoal rangeGoal = null;
        for (WrappedGoal wrappedGoal : selector.getAvailableGoals()) {
            if (wrappedGoal.getGoal() instanceof ShipRangeTargetGoal goal) {
                rangeGoal = goal;
                break;
            }
        }

        if (rangeGoal == null) {
            throw new AssertionError("Hostile target selector has no ShipRangeTargetGoal instance.");
        }

        if (!rangeGoal.canUse()) {
            throw new AssertionError("Hostile ShipRangeTargetGoal failed to acquire nearby friendly ship.");
        }

        rangeGoal.start();
        Entity selected = hostile.getEntityTarget();
        if (!(selected instanceof BasicEntityShip)) {
            throw new AssertionError("Hostile acquired unexpected target type. expected=friendly ship actual="
                    + (selected == null ? "null" : selected.getType().toShortString()));
        }

        hostile.discard();
        friendly.discard();
        helper.succeed();
    }

    // 2026/04/20：GitHub Copilotによって追加
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void friendlyRangeTargetGoalAcquiresHostileShip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Entity friendlyEntity = ModEntities.BB_KONGOU.get().create(level);
        Entity hostileEntity = ModEntities.BB_KIRISHIMA_MOB.get().create(level);

        if (!(friendlyEntity instanceof BasicEntityShip friendly)) {
            throw new AssertionError("BB_KONGOU is not BasicEntityShip in friendly target-acquire test.");
        }
        if (!(hostileEntity instanceof BasicEntityShipHostile hostile)) {
            throw new AssertionError("BB_KIRISHIMA_MOB is not BasicEntityShipHostile in friendly target-acquire test.");
        }

        friendly.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        hostile.moveTo(4.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);

        if (!level.addFreshEntity(friendly)) {
            throw new AssertionError("Failed to add friendly ship for friendly target-acquire test.");
        }
        if (!level.addFreshEntity(hostile)) {
            throw new AssertionError("Failed to add hostile ship for friendly target-acquire test.");
        }

        invokeNoArgProtected(friendly, "clearAITargetTasks");
        invokeNoArgProtected(friendly, "setAITargetList");

        GoalSelector selector = extractTargetSelector(friendly);
        ShipRangeTargetGoal rangeGoal = null;
        for (WrappedGoal wrappedGoal : selector.getAvailableGoals()) {
            if (wrappedGoal.getGoal() instanceof ShipRangeTargetGoal goal) {
                rangeGoal = goal;
                break;
            }
        }

        if (rangeGoal == null) {
            throw new AssertionError("Friendly target selector has no ShipRangeTargetGoal instance.");
        }

        if (!rangeGoal.canUse()) {
            throw new AssertionError("Friendly ShipRangeTargetGoal failed to acquire nearby hostile ship.");
        }

        rangeGoal.start();
        Entity selected = friendly.getEntityTarget();
        if (!(selected instanceof BasicEntityShipHostile)) {
            throw new AssertionError("Friendly acquired unexpected target type. expected=hostile ship actual="
                    + (selected == null ? "null" : selected.getType().toShortString()));
        }

        friendly.discard();
        hostile.discard();
        helper.succeed();
    }

    // 2026/04/11：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipCalcRollTablesExcludeRemovedKanmusuClasses(GameTestHelper helper) {
        Set<Integer> forbiddenSmall = new HashSet<>();
        forbiddenSmall.add((int) ID.ShipClass.DDAkatsuki);
        forbiddenSmall.add((int) ID.ShipClass.DDHibiki);
        forbiddenSmall.add((int) ID.ShipClass.DDIkazuchi);
        forbiddenSmall.add((int) ID.ShipClass.DDInazuma);
        forbiddenSmall.add((int) ID.ShipClass.DDShimakaze);
        forbiddenSmall.add((int) ID.ShipClass.SSU511);
        forbiddenSmall.add((int) ID.ShipClass.SSRo500);
        forbiddenSmall.add((int) ID.ShipClass.CLTenryuu);
        forbiddenSmall.add((int) ID.ShipClass.CLTatsuta);
        forbiddenSmall.add((int) ID.ShipClass.CAAtago);
        forbiddenSmall.add((int) ID.ShipClass.CATakao);

        Set<Integer> forbiddenLarge = new HashSet<>();
        forbiddenLarge.add((int) ID.ShipClass.BBKongou);
        forbiddenLarge.add((int) ID.ShipClass.BBHiei);
        forbiddenLarge.add((int) ID.ShipClass.BBHaruna);
        forbiddenLarge.add((int) ID.ShipClass.BBKirishima);
        forbiddenLarge.add((int) ID.ShipClass.CVKaga);
        forbiddenLarge.add((int) ID.ShipClass.CVAkagi);
        forbiddenLarge.add((int) ID.ShipClass.BBNagato);
        forbiddenLarge.add((int) ID.ShipClass.BBYamato);

        RandomSource random = RandomSource.create(0x5C0FFEE);

        for (int i = 0; i < 2048; i++) {
            int[] matsSmall = new int[]{
                    60 + random.nextInt(700),
                    60 + random.nextInt(700),
                    60 + random.nextInt(700),
                    60 + random.nextInt(700)
            };
            int rolledSmall = ShipCalc.rollShipType(0, matsSmall, random);
            if (forbiddenSmall.contains(rolledSmall)) {
                throw new AssertionError("Small build roll returned forbidden class: " + rolledSmall);
            }

            int[] matsLarge = new int[]{
                    200 + random.nextInt(2400),
                    200 + random.nextInt(2400),
                    200 + random.nextInt(2400),
                    200 + random.nextInt(2400)
            };
            int rolledLarge = ShipCalc.rollShipType(1, matsLarge, random);
            if (forbiddenLarge.contains(rolledLarge)) {
                throw new AssertionError("Large build roll returned forbidden class: " + rolledLarge);
            }
        }

        helper.succeed();
    }

    // 2026/04/12：GitHub Copilotによって確認済み
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void largeShipyardMultiblockReformsAfterStaleCoreCleanup(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // [PORT] 1.10.2 -> 1.20.1: keep this fixture near template height so low-Y
        // worlds are covered by regression tests.
        BlockPos core = helper.absolutePos(new BlockPos(2, 4, 2));

        // Base layer: full 3x3 polymetal
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(core.offset(dx, -2, dz), ModBlocks.POLYMETAL.get().defaultBlockState(), 3);
            }
        }

        // Mid layer: four polymetal corners
        level.setBlock(core.offset(-1, -1, -1), ModBlocks.POLYMETAL.get().defaultBlockState(), 3);
        level.setBlock(core.offset(-1, -1, 1), ModBlocks.POLYMETAL.get().defaultBlockState(), 3);
        level.setBlock(core.offset(1, -1, -1), ModBlocks.POLYMETAL.get().defaultBlockState(), 3);
        level.setBlock(core.offset(1, -1, 1), ModBlocks.POLYMETAL.get().defaultBlockState(), 3);

        // Top center: heavy grudge core block
        level.setBlock(core, ModBlocks.GRUDGE_HEAVY.get().defaultBlockState(), 3);

        BlockPos staleCorePos = core.offset(6, 0, 0);
        if (!(level.getBlockEntity(core) instanceof TileMultiGrudgeHeavy coreTile)) {
            throw new AssertionError("Large shipyard core tile was not created.");
        }
        coreTile.setCorePos(staleCorePos);

        BlockPos polyPos = core.offset(-1, -1, -1);
        if (!(level.getBlockEntity(polyPos) instanceof BasicTileMulti polyTile)) {
            throw new AssertionError("Polymetal multiblock tile was not created.");
        }
        polyTile.setCorePos(staleCorePos);

        int formType = MulitBlockHelper.checkMultiBlockForm(level, core.getX(), core.getY(), core.getZ());
        if (formType <= 0) {
            throw new AssertionError("Large shipyard form check failed after stale core cleanup: " + formType);
        }
        MulitBlockHelper.setupStructure(level, core.getX(), core.getY(), core.getZ(), formType);

        if (!(level.getBlockEntity(core) instanceof TileMultiGrudgeHeavy formedCore) || !formedCore.hasCorePos()) {
            throw new AssertionError("Large shipyard core was not formed after stale core cleanup.");
        }
        if (!formedCore.getCorePos().equals(core)) {
            throw new AssertionError("Core position mismatch after structure setup: " + formedCore.getCorePos());
        }

        if (!(level.getBlockEntity(polyPos) instanceof BasicTileMulti formedPoly) || !formedPoly.hasCorePos()) {
            throw new AssertionError("Polymetal tile did not join formed structure.");
        }
        if (!formedPoly.getCorePos().equals(core)) {
            throw new AssertionError("Polymetal core position mismatch: " + formedPoly.getCorePos());
        }

        // Verify helper path also sees structure as occupied after successful setup.
        int checkResult = MulitBlockHelper.checkMultiBlockForm(level, core.getX(), core.getY(), core.getZ());
        if (checkResult != -1) {
            throw new AssertionError("Structure occupancy check should fail once formed, got: " + checkResult);
        }

        helper.succeed();
    }

    private static ServerPlayer createFollowTestOwner(GameTestHelper helper, ServerLevel level, UUID uuid,
                                                      String name) {
        try {
            Method m = GameTestHelper.class.getMethod("makeMockPlayer");
            Object obj = m.invoke(helper);
            if (obj instanceof ServerPlayer serverPlayer) {
                return serverPlayer;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback below.
        }

        return FakePlayerFactory.get(level, new GameProfile(uuid, name));
    }

    private static GoalSelector extractGoalSelector(Mob mob) {
        try {
            Field goalSelectorField = Mob.class.getDeclaredField("goalSelector");
            goalSelectorField.setAccessible(true);
            Object value = goalSelectorField.get(mob);
            if (value instanceof GoalSelector selector) {
                return selector;
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to inspect goal selector via reflection.", e);
        }

        throw new AssertionError("Failed to resolve goal selector via reflection.");
    }

    private static GoalSelector extractTargetSelector(Mob mob) {
        try {
            Field targetSelectorField = Mob.class.getDeclaredField("targetSelector");
            targetSelectorField.setAccessible(true);
            Object value = targetSelectorField.get(mob);
            if (value instanceof GoalSelector selector) {
                return selector;
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to inspect target selector via reflection.", e);
        }

        throw new AssertionError("Failed to resolve target selector via reflection.");
    }

    private static void assertLightCruiserGoalPriority(ServerLevel level, EntityType<?> type, String id) {
        Entity entity = type.create(level);
        if (!(entity instanceof BasicEntityShip ship)) {
            throw new AssertionError("Entity is not BasicEntityShip in light cruiser AI test: " + id);
        }

        ship.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(ship)) {
            throw new AssertionError("Failed to add entity for light cruiser AI test: " + id);
        }

        invokeNoArgProtected(ship, "clearAITasks");
        invokeNoArgProtected(ship, "setAIList");

        GoalSelector selector = extractGoalSelector(ship);
        int skillPriority = findGoalPriority(selector, ShipSkillAttackGoal.class);
        int rangePriority = findGoalPriority(selector, ShipRangeAttackGoal.class);

        if (skillPriority != 0) {
            throw new AssertionError("SkillAttack goal priority mismatch for " + id + ". expected=0 actual="
                    + skillPriority);
        }

        if (rangePriority != 11) {
            throw new AssertionError("RangeAttack goal priority mismatch for " + id + ". expected=11 actual="
                    + rangePriority);
        }

        ship.discard();
    }

    private static int findGoalPriority(GoalSelector selector, Class<? extends Goal> goalClass) {
        int bestPriority = Integer.MAX_VALUE;
        for (WrappedGoal wrappedGoal : selector.getAvailableGoals()) {
            if (goalClass.isInstance(wrappedGoal.getGoal())) {
                bestPriority = Math.min(bestPriority, wrappedGoal.getPriority());
            }
        }

        return bestPriority == Integer.MAX_VALUE ? -1 : bestPriority;
    }

    private static void invokePacketHandler(C2SGUIInputPacket packet, String methodName, ServerPlayer player) {
        try {
            Method method = C2SGUIInputPacket.class.getDeclaredMethod(methodName, ServerPlayer.class);
            method.setAccessible(true);
            method.invoke(packet, player);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to invoke packet handler method: " + methodName, e);
        }
    }

    private static void invokeNoArgProtected(Object instance, String methodName) {
        Class<?> current = instance.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                method.invoke(instance);
                return;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Failed to invoke protected method: " + methodName, e);
            }
        }

        throw new AssertionError("Failed to find protected method: " + methodName);
    }

    private static void assertCanCreate(ServerLevel level, EntityType<?> type, String id) {
        Entity entity = type.create(level);
        if (entity == null) {
            throw new AssertionError("EntityType#create returned null: " + id);
        }
        entity.discard();
    }

    private static void assertEggIconForShipClass(int shipClass, int expectedIcon) {
        ItemStack egg = new ItemStack(ModItems.SHIP_SPAWN_EGG.get());
        ShipSpawnEgg.setShipClass(egg, shipClass);
        int actualIcon = ShipSpawnEgg.getEggIcon(egg);
        if (actualIcon != expectedIcon) {
            throw new AssertionError("Ship egg icon mismatch for class=" + shipClass + " expected="
                    + expectedIcon + " actual=" + actualIcon);
        }
    }

    private static void assertModelLayer0(String modelFileName, String expectedTexture) {
        Path modelPath = Path.of("src", "main", "resources", "assets", "shincolle", "models", "item", modelFileName);
        try {
            String content = Files.readString(modelPath);
            String expectedLine = "\"layer0\": \"" + expectedTexture + "\"";
            if (!content.contains(expectedLine)) {
                throw new AssertionError("Spawn egg model texture mismatch in " + modelFileName + " expected="
                        + expectedTexture);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to validate spawn egg model file: " + modelPath, e);
        }
    }

    private static void assertCanSpawnAndTick(ServerLevel level, EntityType<?> type, String id) {
        Entity entity = type.create(level);
        if (entity == null) {
            throw new AssertionError("EntityType#create returned null: " + id);
        }

        entity.moveTo(0.5D, level.getSharedSpawnPos().getY() + 1D, 0.5D, 0F, 0F);
        if (!level.addFreshEntity(entity)) {
            throw new AssertionError("Failed to add entity to level: " + id);
        }

        try {
            entity.tick();
        } catch (Throwable t) {
            throw new AssertionError("Entity tick failed on dedicated server: " + id, t);
        } finally {
            entity.discard();
        }
    }

    private static void assertItemTooltipSafe(ServerLevel level, Item item, String id, TooltipFlag tooltipFlag) {
        ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) {
            throw new AssertionError("Failed to create ItemStack: " + id);
        }

        List<Component> tooltip = new ArrayList<>();
        item.appendHoverText(stack, level, tooltip, tooltipFlag);
    }
}
