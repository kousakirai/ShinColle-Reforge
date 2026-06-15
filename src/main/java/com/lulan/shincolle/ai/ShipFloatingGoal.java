package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipFloating;
import com.lulan.shincolle.entity.IShipGuardian;
import com.lulan.shincolle.reference.ID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Floating goal - makes ships rise toward water surface.
 * Ported from EntityAIShipFloating (setMutexBits: 8)
 * <p>
 * 5-tier graduated upward velocity based on depth:
 * depth > 4.0: +0.025
 * depth > 2.0: +0.015
 * depth > 1.3: +0.007
 * depth > 0.47: +0.003
 * depth > 0.15: +0.0015
 */
public class ShipFloatingGoal extends Goal {
    private static final double DEPTH_TIER_1 = 4D;
    private static final double DEPTH_TIER_2 = 2D;
    private static final double DEPTH_TIER_3 = 1.3D;
    private static final double DEPTH_TIER_4 = 0.47D;
    private static final double DEPTH_TIER_5 = 0.15D;

    private static final double FLOAT_SPEED_TIER_1 = 0.025D;
    private static final double FLOAT_SPEED_TIER_2 = 0.015D;
    private static final double FLOAT_SPEED_TIER_3 = 0.007D;
    private static final double FLOAT_SPEED_TIER_4 = 0.003D;
    private static final double FLOAT_SPEED_TIER_5 = 0.0015D;

    private final IShipFloating host;
    private final BasicEntityShip hostShip;
    private final BasicEntityMount hostMount;
    private final LivingEntity hostLiving;

    public ShipFloatingGoal(IShipFloating entity) {
        this.host = entity;
        this.hostLiving = (LivingEntity) entity;

        if (entity instanceof BasicEntityShip ship) {
            this.hostShip = ship;
            this.hostMount = null;
        } else if (entity instanceof BasicEntityMount mount) {
            this.hostShip = null;
            this.hostMount = mount;
        } else {
            this.hostShip = null;
            this.hostMount = null;
        }

        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    /**
     * Check if ship is in guard position (should suppress floating).
     * Returns true if the ship is close enough to its guard target.
     */
    public static boolean isInGuardPosition(IShipGuardian host) {
        if (!(host instanceof Entity ent))
            return false;

        // if the block above is air, allow floating
        if (ent.level().getBlockState(ent.blockPosition().above()).isAir()) {
            return false;
        }

        // guard mode (CanFollow = false)
        if (!host.getStateFlag(ID.F.CanFollow)) {
            float fMin = host.getStateMinor(ID.M.FollowMin) + ent.getBbWidth() * 0.5F;
            float fMinSq = fMin * fMin;

            // guarding entity
            if (host.getGuardedEntity() != null) {
                double distSq = ent.distanceToSqr(host.getGuardedEntity());
                return distSq < fMinSq;
            }
            // guarding position
            else if (host.getStateMinor(ID.M.GuardY) > 0) {
                double dx = ent.getX() - host.getStateMinor(ID.M.GuardX);
                double dy = ent.getY() - host.getStateMinor(ID.M.GuardY);
                double dz = ent.getZ() - host.getStateMinor(ID.M.GuardZ);
                double distSq = dx * dx + dy * dy + dz * dz;
                return distSq < fMinSq && ent.getY() >= host.getStateMinor(ID.M.GuardY);
            }
        }
        // follow mode (CanFollow = true)
        else {
            float fMax = host.getStateMinor(ID.M.FollowMax) + ent.getBbWidth() * 0.5F;
            float fMaxSq = fMax * fMax;

            Entity hostEntity = host.getHostEntity();
            if (hostEntity != null) {
                double distSq = hostEntity.distanceToSqr(ent);
                return distSq <= fMaxSq;
            }
        }

        return false;
    }

    @Override
    public boolean canUse() {
        // ship type
        if (hostShip != null) {
            return canFloatShip(hostShip);
        }
        // mount type
        else if (hostMount != null && hostMount.getHostEntity() != null) {
            return canFloatMount(hostMount);
        }

        // fallback
        return host.getShipDepth() > host.getShipFloatingDepth();
    }

    @Override
    public void tick() {
        double depth = this.host.getShipDepth();

        // 5-tier graduated float speeds matching original
        if (depth > DEPTH_TIER_1) {
            applyVerticalBoost(FLOAT_SPEED_TIER_1);
        } else if (depth > DEPTH_TIER_2) {
            applyVerticalBoost(FLOAT_SPEED_TIER_2);
        } else if (depth > DEPTH_TIER_3) {
            applyVerticalBoost(FLOAT_SPEED_TIER_3);
        } else if (depth > DEPTH_TIER_4) {
            applyVerticalBoost(FLOAT_SPEED_TIER_4);
        } else if (depth > DEPTH_TIER_5) {
            applyVerticalBoost(FLOAT_SPEED_TIER_5);
        }
    }

    private boolean canFloatShip(BasicEntityShip ship) {
        if (!ship.getStateFlag(ID.F.CanFloatUp) || ship.getShipDepth() <= ship.getShipFloatingDepth()) {
            return false;
        }

        // block floating when: riding, sitting, crane, navigating, or in guard position
        return !(ship.isPassenger()
                || ship.isOrderedToSit()
                || ship.getStateMinor(ID.M.CraneState) > 0
                || !ship.getNavigation().isDone()
                || isInGuardPosition(ship));
    }

    private boolean canFloatMount(BasicEntityMount mount) {
        if (mount.getShipDepth() <= mount.getShipFloatingDepth()) {
            return false;
        }

        Entity hostEntity = mount.getHostEntity();
        if (hostEntity instanceof BasicEntityShip ship) {
            if (ship.isOrderedToSit()
                    || ship.getStateMinor(ID.M.CraneState) > 0
                    || !ship.getNavigation().isDone()
                    || isInGuardPosition(ship)) {
                return false;
            }
        }

        // check mount's own navigator and guard
        return mount.getNavigation().isDone() && !isInGuardPosition(mount);
    }

    private void applyVerticalBoost(double amount) {
        this.hostLiving.setDeltaMovement(this.hostLiving.getDeltaMovement().add(0D, amount, 0D));
    }
}
