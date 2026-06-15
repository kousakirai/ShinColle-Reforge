package com.lulan.shincolle.playerskill;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.TeamHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Handles player skill execution when riding mounts or with ship passengers.
 * <p>
 * Server-side: processes skill packets via handlePlayerSkill/castPlayerSkill.
 * Client-side: key handling requires networking system (deferred).
 */
public class ShipSkillHandler {

    /**
     * Get ship skill host type.
     * -1: no skill
     * 0: skill of mounts (player riding BasicEntityMount)
     * 2: skill of rider (BasicEntityShip riding player)
     */
    public static int getShipSkillHostType(Player player) {
        if (player.getVehicle() instanceof BasicEntityMount)
            return 0;
        else if (!player.getPassengers().isEmpty() &&
                player.getPassengers().get(0) instanceof BasicEntityShip)
            return 2;

        return -1;
    }

    /**
     * Process player skill on packet receiving, SERVER SIDE.
     *
     * @param player the player who sent the skill packet
     * @param data   0: attack type, 1: targetEID or pos.X, 2: pos.Y, 3: pos.Z
     */
    public static void handlePlayerSkill(Player player, int[] data) {
        BasicEntityShip ship = null;

        // If player is riding ship mounts
        if (player.getVehicle() instanceof BasicEntityMount mount) {
            Entity hostEntity = mount.getHostEntity();
            if (hostEntity instanceof BasicEntityShip hostShip) {
                ship = hostShip;
            }
            if (ship != null) {
                castPlayerSkill(ship, player, data);
            }
        }
        // If ship is riding player
        else if (!player.getPassengers().isEmpty() &&
                player.getPassengers().get(0) instanceof BasicEntityShip passengerShip) {
            castPlayerSkill(passengerShip, player, data);
        }
    }

    /**
     * //     * Cast player skill.
     *
     * @param ship   the ship entity performing the attack
     * @param player the controlling player
     * @param data   0: attack type, 1: targetEID or pos.X, 2: pos.Y, 3: pos.Z
     */
    public static void castPlayerSkill(BasicEntityShip ship, Player player, int[] data) {
        if (ship == null || !TeamHelper.checkSameOwner(player, ship))
            return;

        int skill;

        // Check attack type
        switch (data[0]) {
            case 0:
                if (!ship.getStateFlag(ID.F.AtkType_Light))
                    return;
                skill = ID.T.MountSkillCD1;
                break;
            case 1:
                if (!ship.getStateFlag(ID.F.AtkType_Heavy))
                    return;
                skill = ID.T.MountSkillCD2;
                break;
            case 2:
                if (!ship.getStateFlag(ID.F.AtkType_AirLight))
                    return;
                skill = ID.T.MountSkillCD3;
                break;
            case 3:
                if (!ship.getStateFlag(ID.F.AtkType_AirHeavy))
                    return;
                skill = ID.T.MountSkillCD4;
                break;
            default:
                return;
        }

        // Check skill cooldown
        if (ship.getStateTimer(skill) > 0)
            return;

        // Check target exist
        Entity target = null;
        BlockPos targetPos = null;
        float range = ship.getAttrs() != null ? ship.getAttrs().getAttackRange() : 16F;
        float rangeSq = range * range;

        if (data[2] < 0) {
            // Target is entity (data[1] = entity ID)
            target = player.level().getEntity(data[1]);

            if (target != null && ship.distanceToSqr(target) > rangeSq) {
                target = null;
            }
        } else {
            // Target is block position
            targetPos = new BlockPos(data[1], data[2], data[3]);

            if (ship.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5) > rangeSq) {
                targetPos = null;
            }
        }

        // No target, return
        if (target == null && targetPos == null)
            return;

        // Check target isn't friendly
        if (target != null && TeamHelper.checkSameOwner(ship, target))
            return;

        // Calculate attack delay based on attack speed
        float atkSpd = ship.getAttrs() != null ? ship.getAttrs().getAttackSpeed() : 1F;

        // Execute attack based on type
        switch (data[0]) {
            case 0: // light attack
                if (target != null) {
                    ship.attackEntityWithAmmo(target);
                    ship.setStateTimer(ID.T.MountSkillCD1, getAttackDelay(atkSpd, 1));
                }
                break;
            case 1: // heavy attack
                if (target != null) {
                    ship.attackEntityWithHeavyAmmo(target);
                } else {
                    ship.attackEntityWithHeavyAmmo(targetPos);
                }


                ship.setStateTimer(ID.T.MountSkillCD2, getAttackDelay(atkSpd, 2));

                break;
            case 2: // light air attack
                if (ship instanceof BasicEntityShipCV cv && target != null) {
                    cv.attackEntityWithAircraft(target);
                    int airDelay = getAttackDelay(atkSpd, 3);
                    ship.setStateTimer(ID.T.MountSkillCD3, airDelay);
                    ship.setStateTimer(ID.T.MountSkillCD4, airDelay);
                }
                break;
            case 3: // heavy air attack
                if (ship instanceof BasicEntityShipCV cv && target != null) {
                    cv.attackEntityWithHeavyAircraft(target);
                    int airDelay = getAttackDelay(atkSpd, 4);
                    ship.setStateTimer(ID.T.MountSkillCD3, airDelay);
                    ship.setStateTimer(ID.T.MountSkillCD4, airDelay);
                }
                break;
        }
    }

    /**
     * Calculate attack delay in ticks based on attack speed and type.
     *
     * @param atkSpd attack speed value
     * @param type   attack type (1=light, 2=heavy, 3=air light, 4=air heavy)
     * @return delay in ticks
     */
    private static int getAttackDelay(float atkSpd, int type) {
        float baseDelay = switch (type) {
            case 1 -> 20F; // light attack base delay
            case 2 -> 40F; // heavy attack base delay
            case 3 -> 60F; // air light attack base delay
            case 4 -> 80F; // air heavy attack base delay
            default -> 20F;
        };

        if (atkSpd > 0F) {
            return Math.max(4, (int) (baseDelay / atkSpd));
        }
        return (int) baseDelay;
    }
}
