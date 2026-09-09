package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.item.TargetWrench;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Utility methods for tile entity operations.
 * Ported from 1.10.2 TileEntityHelper.
 */
public class TileEntityHelper {

    /**
     * Pair two waypoints together (from→to link).
     * Sets nextWaypoint on source and lastWaypoint on target (if no cycle).
     *
     * @param player    commanding player
     * @param level     server level
     * @param posFrom   source waypoint position
     * @param posTo     target waypoint position
     */
    public static void pairingWaypoints(ServerPlayer player, ServerLevel level,
                                        BlockPos posFrom, BlockPos posTo) {
        int playerUID = getSenderPlayerUID(player);
        if (!canPair(player, level, posFrom, posTo, ConfigHandlerPairing.WAYPOINT)) {
            return;
        }
        if (playerUID <= 0) {
            return;
        }

        BlockEntity beFrom = level.getBlockEntity(posFrom);
        BlockEntity beTo = level.getBlockEntity(posTo);

        if (!(beFrom instanceof TileEntityWaypoint wpFrom) ||
                !(beTo instanceof TileEntityWaypoint wpTo)) {
            player.sendSystemMessage(Component.literal("[ShinColle] Waypoint pairing failed: invalid blocks"));
            return;
        }

        // Check ownership
        if (wpFrom.getPlayerUID() != playerUID || wpTo.getPlayerUID() != playerUID) {
            player.sendSystemMessage(Component.literal("[ShinColle] Waypoint pairing failed: not your waypoints"));
            return;
        }

        // Set next waypoint on source
        wpFrom.setNextWaypoint(posTo);

        // Set last waypoint on target (unless it would create a 2-node cycle)
        if (!wpTo.getNextWaypoint().equals(posFrom)) {
            wpTo.setLastWaypoint(posFrom);
        }

        player.sendSystemMessage(Component.literal(
                "[ShinColle] Waypoints paired: (" +
                        posFrom.getX() + "," + posFrom.getY() + "," + posFrom.getZ() + ") -> (" +
                        posTo.getX() + "," + posTo.getY() + "," + posTo.getZ() + ")"));
    }

    /**
     * Pair a waypoint with a chest/inventory container.
     *
     * @param player    commanding player
     * @param level     server level
     * @param posWp     waypoint position
     * @param posChest  chest/inventory position
     */
    public static void pairingWaypointAndChest(ServerPlayer player, ServerLevel level,
                                               BlockPos posWp, BlockPos posChest) {
        int playerUID = getSenderPlayerUID(player);
        if (!canPair(player, level, posWp, posChest, ConfigHandlerPairing.CHEST)) {
            return;
        }
        if (playerUID <= 0) {
            return;
        }

        BlockEntity beWp = level.getBlockEntity(posWp);
        BlockEntity beChest = level.getBlockEntity(posChest);

        if (!(beWp instanceof TileEntityWaypoint wp)) {
            player.sendSystemMessage(Component.literal("[ShinColle] Chest pairing failed: invalid waypoint"));
            return;
        }

        // Check ownership
        if (wp.getPlayerUID() != playerUID) {
            player.sendSystemMessage(Component.literal("[ShinColle] Chest pairing failed: not your waypoint"));
            return;
        }

        // Check target is a container
        if (!(beChest instanceof Container)) {
            player.sendSystemMessage(Component.literal("[ShinColle] Chest pairing failed: target is not a container"));
            return;
        }

        wp.setPairedChest(posChest);

        player.sendSystemMessage(Component.literal(
                "[ShinColle] Waypoint-chest paired: WP(" +
                posWp.getX() + "," + posWp.getY() + "," + posWp.getZ() + ") -> Chest(" +
                        posChest.getX() + "," + posChest.getY() + "," + posChest.getZ() + ")"));
    }

    private enum ConfigHandlerPairing {
        WAYPOINT,
        CHEST
    }

    private static int getSenderPlayerUID(ServerPlayer player) {
        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);
        return capa == null ? -1 : capa.getPlayerUID();
    }

    private static boolean canPair(ServerPlayer player, ServerLevel level,
                                   BlockPos first, BlockPos second,
                                   ConfigHandlerPairing pairing) {
        if (player == null || level == null || level.isClientSide()) {
            return false;
        }
        if (!isHoldingTargetWrench(player)) {
            return false;
        }
        if (!level.hasChunkAt(first) || !level.hasChunkAt(second)) {
            return false;
        }
        if (!level.mayInteract(player, first) || !level.mayInteract(player, second)) {
            return false;
        }
        if (player.distanceToSqr(first.getX() + 0.5D, first.getY() + 0.5D, first.getZ() + 0.5D) >= 64.0D
                && player.distanceToSqr(second.getX() + 0.5D, second.getY() + 0.5D, second.getZ() + 0.5D) >= 64.0D) {
            return false;
        }

        int maxDistance = pairing == ConfigHandlerPairing.WAYPOINT
                ? ConfigHandler.pairingDistWaypoint()
                : ConfigHandler.pairingDistChest();
        return isWithinPairingDistance(first, second, maxDistance);
    }

    public static boolean isWithinPairingDistance(BlockPos first, BlockPos second, int maxDistance) {
        if (first == null || second == null || maxDistance < 0) {
            return false;
        }
        long dx = (long) first.getX() - second.getX();
        long dy = (long) first.getY() - second.getY();
        long dz = (long) first.getZ() - second.getZ();
        long distanceSq = dx * dx + dy * dy + dz * dz;
        long maxDistanceSq = (long) maxDistance * maxDistance;
        return distanceSq <= maxDistanceSq;
    }

    private static boolean isHoldingTargetWrench(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof TargetWrench
                || player.getOffhandItem().getItem() instanceof TargetWrench;
    }
}
