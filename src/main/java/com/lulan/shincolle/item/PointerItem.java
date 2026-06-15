package com.lulan.shincolle.item;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.C2SGUIInputPacket;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.tileentity.ITileGuardPoint;
import com.lulan.shincolle.utility.ClientRuntimeHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import java.util.List;

/**
 * Pointer Item - command tool for controlling ship entities.
 * <p>
 * Left click:
 * entity(own ship): add to team / set focus
 * entity(other): register target class
 * air + sneak: cycle mode (single/group/formation)
 * air + sneak+sprint: clear team
 * air + sprint (formation mode): change formation
 * <p>
 * Right click:
 * entity(own ship): toggle sit
 * entity(own ship)+sneak: open ship GUI
 * entity(non-owner): attack or move
 * entity + sprint: guard entity (move only)
 * block: move to position
 * air + sneak: open formation GUI
 * <p>
 * Sneak + left click on own ship: remove from team
 */
public class PointerItem extends BasicItem {

    public static final int MODE_SINGLE = 0;
    public static final int MODE_GROUP = 1;
    public static final int MODE_FORMATION = 2;
    // Client-side formation change state (static: only one local player on client)
    private static boolean formatFlag = false;
    private static int formatAddID = 0;
    private static int formatCD = 0;

    public PointerItem() {
        super(new Properties().stacksTo(1));
    }

    public static int toggleCaressMode(int currentMode) {
        if (currentMode >= 0 && currentMode <= 2) {
            return currentMode + 3;
        } else if (currentMode >= 3 && currentMode <= 5) {
            return currentMode - 3;
        }
        return MODE_SINGLE;
    }

    /**
     * Get pointer mode from NBT
     */
    public static int getMode(ItemStack stack) {
        if (stack.hasTag()) {
            assert stack.getTag() != null;
            return stack.getTag().getByte("Mode");
        }
        return MODE_SINGLE;
    }

    /**
     * Set pointer mode in NBT
     */
    public static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putByte("Mode", (byte) mode);
    }

    /**
     * Extract BasicEntityShip from an entity (handles mounts).
     */
    private static BasicEntityShip getShipFromEntity(Entity entity) {
        if (entity instanceof BasicEntityShip ship) {
            return ship;
        }
        if (entity instanceof BasicEntityMount mount) {
            Entity host = mount.getHostEntity();
            if (host instanceof BasicEntityShip ship) {
                return ship;
            }
        }
        return null;
    }

    // ===== Left Click =====

    /**
     * Ray trace for entities at extended range, excluding the player and vehicle.
     */
    private static EntityHitResult rayTraceEntities(Player player, double range) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookDir.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(1.0);

        Entity vehicle = player.getVehicle();
        Entity vehicleHost = (vehicle instanceof BasicEntityMount mount) ? mount.getHostEntity() : null;

        return ProjectileUtil.getEntityHitResult(
                player, eyePos, endPos, searchBox,
                e -> !e.isSpectator() && e.isPickable()
                        && e != player && e != vehicle && e != vehicleHost,
                range * range);
    }

    /**
     * Find a ship in the currently selected team by ship UID.
     *
     * @return slot index (0 to SLOT_NUM-1), or -1 if not found
     */
    private static int findShipInTeam(CapaTeitoku capa, int shipUID) {
        int teamId = capa.getSelectTeam();
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            if (capa.getTeamMember(teamId, i) == shipUID) {
                return i;
            }
        }
        return -1;
    }

    // ===== Right Click =====

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // ===== Inventory Tick =====

    /**
     * Left click handler. Client side performs ray tracing and sends packets.
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player))
            return true;
        if (!player.level().isClientSide())
            return true;

        return handleLeftClickClient(stack, player);
    }

    // ===== Client-side Left Click Logic =====

    /**
     * Prevent this item from dealing damage to entities on left click.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        return true;
    }

    // ===== Client-side Right Click Logic =====

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int mode = getMode(stack);

        if (mode > 2)
            return InteractionResultHolder.success(stack);

        if (level.isClientSide()) {
            handleRightClickClient(stack, player, mode);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (!level.isClientSide() || !(entity instanceof Player player))
            return;

        // Formation change CD timer
        if (PointerItem.formatFlag) {
            PointerItem.formatCD--;
            if (PointerItem.formatCD <= 0) {
                sendFormationChange(player);
            }
        }
    }

    private boolean handleLeftClickClient(ItemStack stack, Player player) {
        int mode = getMode(stack);
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);

        boolean isSneaking = player.isShiftKeyDown();
        boolean isSprinting = player.isSprinting();

        // Ray trace for entities at 64 blocks
        EntityHitResult entityHit = rayTraceEntities(player, 64.0);

        if (entityHit != null) {
            Entity hitEntity = entityHit.getEntity();

            // Ship or mount
            BasicEntityShip ship = getShipFromEntity(hitEntity);

            if (ship != null) {
                // Is owner
                if (TeamHelper.checkSameOwner(player, ship)) {
                    int teamSlot = findShipInTeam(capa, ship.getStateMinor(ID.M.ShipUID));

                    if (isSneaking) {
                        // Sneak + left click: remove from team
                        if (teamSlot >= 0) {
                            if (mode == MODE_SINGLE) {
                                // In single mode, focus another ship before removing
                                int teamId = capa.getSelectTeam();
                                for (int j = 0; j < CapaTeitoku.SLOT_NUM; j++) {
                                    if (j != teamSlot && capa.getTeamSID(teamId, j) > 0) {
                                        ModNetworking.sendToServer(new C2SGUIInputPacket(
                                                C2SGUIInputPacket.SetSelect,
                                                new int[]{player.getId(), 0, teamId}));
                                        break;
                                    }
                                }
                            }
                            // Send remove packet (toggle in AddTeam)
                            ModNetworking.sendToServer(new C2SGUIInputPacket(
                                    C2SGUIInputPacket.AddTeam,
                                    new int[]{player.getId(), 0, ship.getId()}));
                            return true;
                        }
                    } else {
                        if (teamSlot >= 0) {
                            // Already in team: set focus
                            ModNetworking.sendToServer(new C2SGUIInputPacket(
                                    C2SGUIInputPacket.SetSelect,
                                    new int[]{player.getId(), 0, capa.getSelectTeam()}));
                        } else {
                            // Not in team: add to team
                            ModNetworking.sendToServer(new C2SGUIInputPacket(
                                    C2SGUIInputPacket.AddTeam,
                                    new int[]{player.getId(), 0, ship.getId()}));

                            // In single mode: auto-focus the added ship
                            if (mode == MODE_SINGLE) {
                                ModNetworking.sendToServer(new C2SGUIInputPacket(
                                        C2SGUIInputPacket.SetSelect,
                                        new int[]{player.getId(), 0, capa.getSelectTeam()}));
                            }
                        }
                        return true;
                    }
                }
            } else {
                // Other entity: register target class
                String tarName = hitEntity.getClass().getSimpleName();
                player.sendSystemMessage(
                        Component.translatable("chat.shincolle.pointer.settargetclass", "  " + tarName));
                ModNetworking.sendToServer(new C2SGUIInputPacket(
                        C2SGUIInputPacket.SetTarClass,
                        new int[]{player.getId(), 0},
                        tarName));
                return true;
            }
        }

        // Click on air
        if (isSneaking) {
            if (isSprinting) {
                // Sneak + Sprint: clear team
                ModNetworking.sendToServer(new C2SGUIInputPacket(
                        C2SGUIInputPacket.ClearTeam,
                        new int[]{player.getId(), 0}));

            } else {
                // Sneak only: cycle pointer mode
                switch (mode) {
                    case MODE_SINGLE:
                        mode = MODE_GROUP;
                        break;
                    case MODE_GROUP:
                        mode = MODE_FORMATION;
                        break;
                    default:
                        mode = MODE_SINGLE;
                        break;
                }
                setMode(stack, mode);
                ModNetworking.sendToServer(new C2SGUIInputPacket(
                        C2SGUIInputPacket.SyncPlayerItem,
                        new int[]{player.getId(), 0, mode}));
            }
            return true;
        }


        // Sprint in formation mode: queue formation change
        if (isSprinting && mode == MODE_FORMATION) {
            PointerItem.formatFlag = true;
            PointerItem.formatAddID++;
            PointerItem.formatCD = 20;
            return false;
        }

        return true;
    }

    // ===== Utility =====

    private void handleRightClickClient(ItemStack stack, Player player, int mode) {
        boolean isSneaking = player.isShiftKeyDown();
        boolean isSprinting = player.isSprinting();
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);
        int markerTeamId = capa.getSelectTeam();

        // Ray trace for entities at 64 blocks
        EntityHitResult entityHit = rayTraceEntities(player, 64.0);

        if (entityHit != null) {
            Entity hitEntity = entityHit.getEntity();

            // Sprint + right click on entity: guard entity (move only)
            if (isSprinting) {
                ModNetworking.sendToServer(new C2SGUIInputPacket(
                        C2SGUIInputPacket.GuardEntity,
                        new int[]{player.getId(), 0, hitEntity.getId()}));
                ParticleHelper.spawnAttackParticleAt(player.level(), hitEntity.getX(), hitEntity.getY(),
                        hitEntity.getZ(), 2);
                return;
            }

            // Ship or mount
            BasicEntityShip ship = getShipFromEntity(hitEntity);

            if (ship != null) {
                if (TeamHelper.checkSameOwner(player, ship)) {
                    if (isSneaking) {
                        // Sneak + right click on own ship: open ship GUI
                        ModNetworking.sendToServer(new C2SGUIInputPacket(
                                C2SGUIInputPacket.OpenShipGUI,
                                new int[]{player.getId(), 0, ship.getId()}));
                    } else {
                        // Right click on own ship within mount range: let vanilla handle mounting
                        if (hitEntity instanceof BasicEntityMount
                                && player.distanceToSqr(hitEntity) <= 16D) {
                            return;
                        }
                        // Right click on own ship: toggle sit
                        ModNetworking.sendToServer(new C2SGUIInputPacket(
                                C2SGUIInputPacket.SetSitting,
                                new int[]{player.getId(), 0, mode, ship.getId()}));
                    }
                } else {
                    // Not owner: attack or move depending on friendlyFire config
                    handleAttackOrMove(player, hitEntity, mode);
                }
            } else {
                // Other entity (player, mob)
                if (hitEntity instanceof Player) {
                    handleAttackOrMove(player, hitEntity, mode);
                } else if (!hitEntity.isInvisible()) {
                    // Non-player mob: attack
                    ModNetworking.sendToServer(new C2SGUIInputPacket(
                            C2SGUIInputPacket.AttackTarget,
                            new int[]{player.getId(), 0, hitEntity.getId()}));
                    ParticleHelper.spawnAttackParticleAt(player.level(), hitEntity.getX(), hitEntity.getY(),
                            hitEntity.getZ(), 2);
                }
            }
            return;
        }

        // No entity hit
        if (isSneaking) {
            // Sneak + right click on air: open formation GUI
            ModNetworking.sendToServer(new C2SGUIInputPacket(
                    C2SGUIInputPacket.OpenItemGUI,
                    new int[]{player.getId(), 0, 0}));
            return;
        }

        // Ray trace for blocks (including liquids) at 64 blocks
        HitResult blockHit = player.pick(64.0, 1.0F, true);

        if (blockHit.getType() == HitResult.Type.BLOCK && blockHit instanceof BlockHitResult blockResult) {
            var blockPos = blockResult.getBlockPos();
            var direction = blockResult.getDirection();
            int x = blockPos.getX();
            int y = blockPos.getY();
            int z = blockPos.getZ();

            // Check if target is a waypoint/guard point or liquid
            BlockState state = player.level().getBlockState(blockPos);
            BlockEntity tile = player.level().getBlockEntity(blockPos);

            // If not liquid and not a guard point, adjust position based on hit side
            if (state.getFluidState().isEmpty() && !(tile instanceof ITileGuardPoint)) {
                var offset = direction.getNormal();
                x += offset.getX();
                y += offset.getY();
                z += offset.getZ();
            }

            int guardType = isSprinting ? 0 : 1; // 0 = move only, 1 = move and attack

            ModNetworking.sendToServer(new C2SGUIInputPacket(
                    C2SGUIInputPacket.SetMove,
                    new int[]{player.getId(), 0, mode, guardType, x, y, z}));

            ParticleHelper.spawnAttackParticleAt(player.level(), x + 0.5D, y, z + 0.5D,
                    0.3D, markerTeamId, 0D, 25);
        }
    }

    /**
     * Attack or move-to based on friendly fire config and visibility.
     */
    private void handleAttackOrMove(Player player, Entity target, int mode) {
        if (ConfigHandler.friendlyFire() && !target.isInvisible()) {
            // Attack target
            ModNetworking.sendToServer(new C2SGUIInputPacket(
                    C2SGUIInputPacket.AttackTarget,
                    new int[]{player.getId(), 0, target.getId()}));
            ParticleHelper.spawnAttackParticleAt(player.level(), target.getX(), target.getY(), target.getZ(), 2);
        } else {
            // Move to target position (include target coordinates)
            ModNetworking.sendToServer(new C2SGUIInputPacket(
                    C2SGUIInputPacket.SetMove,
                    new int[]{player.getId(), 0, mode, 0,
                            (int) target.getX(), (int) target.getY(), (int) target.getZ()}));
            ParticleHelper.spawnAttackParticleAt(player.level(), target.getX(), target.getY(), target.getZ(), 2);
        }
    }

    /**
     * Send the queued formation change after the CD expires.
     */
    private void sendFormationChange(Player player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);


        int teamId = capa.getSelectTeam();
        int fid = (capa.getFormatID(teamId) + PointerItem.formatAddID) % 6;

        player.sendSystemMessage(Component.literal(
                Component.translatable("chat.shincolle.pointer.changeformation").getString() + " " +
                        Component.translatable("gui.shincolle.formation.format" + fid).getString()));

        ModNetworking.sendToServer(new C2SGUIInputPacket(
                C2SGUIInputPacket.SetFormation,
                // [PORT] 1.10.2 -> 1.20.1: use selected team ID so pointer formation change
                // applies to active team.
                new int[]{player.getId(), teamId, fid}));


        PointerItem.formatCD = 0;
        PointerItem.formatAddID = 0;
        PointerItem.formatFlag = false;
    }

    // ===== Tooltip =====

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Player player = ClientRuntimeHelper.getClientPlayer();
        if (player == null)
            return;

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);


        int mode = getMode(stack);
        int teamId = capa.getSelectTeam();
        int fid = capa.getFormatID(teamId);

        String formationName = "";
        if (fid >= 0) {
            formationName = ChatFormatting.GOLD
                    + Component.translatable("gui.shincolle.formation.format" + fid).getString();
        }

        // Show mode and formation name
        String modeKey;
        ChatFormatting modeColor;
        switch (mode) {
            case MODE_GROUP:
                modeKey = "gui.shincolle.pointer1";
                modeColor = ChatFormatting.RED;
                break;
            case MODE_FORMATION:
                modeKey = "gui.shincolle.pointer2";
                modeColor = ChatFormatting.GOLD;
                break;
            default:
                modeKey = "gui.shincolle.pointer0";
                modeColor = ChatFormatting.AQUA;
                break;
        }

        tooltip.add(Component.literal(modeColor + Component.translatable(modeKey).getString() + " : " + formationName));
        tooltip.add(
                Component.literal(ChatFormatting.GRAY + Component.translatable("gui.shincolle.pointer3").getString()));

        // Current team ID
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "" + ChatFormatting.UNDERLINE +
                String.format("%s %d", Component.translatable("gui.shincolle.pointer4").getString(), teamId + 1)));

        // Team members
        int j = 1;
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            int sid = capa.getTeamSID(teamId, i);
            if (sid > 0) {
                int uid = capa.getTeamMember(teamId, i);
                if (uid > 0) {
                    tooltip.add(Component.literal(
                            ChatFormatting.WHITE + String.format("%d: Ship #%d", j, uid)));
                } else {
                    tooltip.add(Component.literal(
                            ChatFormatting.DARK_RED + "" + ChatFormatting.OBFUSCATED +
                                    Component.translatable("gui.shincolle.formation.nosignal").getString()));
                }
                j++;
            }
        }
    }
}
