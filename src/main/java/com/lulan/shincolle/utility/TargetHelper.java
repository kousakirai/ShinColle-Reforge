package com.lulan.shincolle.utility;

import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.server.ServerDataManager;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Target selection helper.
 * <p>
 * Provides Predicate-based selectors for ship AI target acquisition
 * and utility methods for target validation.
 * <p>
 * Ported from 1.10.2 TargetHelper. TeamHelper logic is inlined using
 * IShipOwner.getPlayerUID() for ownership checks.
 */
public class TargetHelper {

    // ========== Sorter ==========

    /**
     * Update AI targets — called every tick on server side.
     * Clears dead, friendly, or expired targets.
     */
    public static void updateTarget(IShipAttackBase host) {
        Entity hostEntity = (Entity) host;

        // clear dead or friendly attack target
        Entity atkTarget = host.getEntityTarget();
        if (atkTarget != null) {
            if (!atkTarget.isAlive()) {
                host.setEntityTarget(null);
            } else if (checkSameOwner(hostEntity, atkTarget)) {
                host.setEntityTarget(null);
            }
        }

        // clear dead or expired revenge target
        Entity rvgTarget = host.getEntityRevengeTarget();
        if (rvgTarget != null) {
            if (!rvgTarget.isAlive()) {
                host.setEntityRevengeTarget(null);
            } else if (host.getTickExisted() - host.getEntityRevengeTime() > 200) {
                host.setEntityRevengeTarget(null);
            }
        }

        // clear vanilla attack target for hostile ships
        if (host instanceof BasicEntityShipHostile hostile) {
            LivingEntity vanillaTarget = hostile.getTarget();
            if (vanillaTarget != null) {
                if (!vanillaTarget.isAlive()) {
                    hostile.setTarget(null);
                } else if (checkSameOwner(hostEntity, vanillaTarget)) {
                    hostile.setTarget(null);
                }
            }
        }

        // clear invisible target every 64 ticks if no detection equipment
        if ((host.getTickExisted() & 63) == 0) {
            Entity target = host.getEntityTarget();
            if (target != null && target.isInvisible()) {
                if (host.getStateMinor(ID.M.LevelFlare) < 1
                        && host.getStateMinor(ID.M.LevelSearchlight) < 1) {
                    host.setEntityTarget(null);
                }
            }
        }
    }

    // ========== Selector (friendly ship target) ==========

    /**
     * Set revenge target for all friendly ships within range of a player.
     */
    public static void setRevengeTargetAroundPlayer(Player player, double dist, Entity target) {
        if (player == null || target == null)
            return;

        AABB box = player.getBoundingBox().inflate(dist);
        List<BasicEntityShip> ships = player.level().getEntitiesOfClass(BasicEntityShip.class, box);

        for (BasicEntityShip ship : ships) {
            if (!ship.equals(target) && checkSameOwner(player, ship)) {
                ship.setEntityRevengeTarget(target);
                ship.setEntityRevengeTime();
            }
        }
    }

    // ========== RevengeSelector (friendly ship revenge) ==========

    /**
     * Set revenge target for all hostile ships within range.
     */
    public static void setRevengeTargetAroundHostileShip(BasicEntityShipHostile host, double dist, Entity target) {
        if (host == null || target == null)
            return;

        AABB box = host.getBoundingBox().inflate(dist);
        List<BasicEntityShipHostile> ships = host.level().getEntitiesOfClass(BasicEntityShipHostile.class, box);

        for (BasicEntityShipHostile ship : ships) {
            ship.setEntityRevengeTarget(target);
            ship.setEntityRevengeTime();
        }
    }

    // ========== SelectorForHostile (mob ship target) ==========

    /**
     * Check if an entity should never be attacked (projectiles, hangings, etc.).
     */
    public static boolean isEntityInvulnerable(Entity target) {
        if (target == null) {
            return true;
        }

        if (target instanceof Projectile || target instanceof AreaEffectCloud) {
            return true;
        }

        // [PORT] 1.10.2 -> 1.20.1: preserve server-side unattackable class list.
        if (!target.level().isClientSide()) {
            return checkUnattackTargetList(target);
        }

        return false;
    }

    // ========== RevengeSelectorForHostile ==========

    /**
     * Check if target class is in server-side unattackable class list.
     */
    public static boolean checkUnattackTargetList(Entity target) {
        if (target == null) {
            return false;
        }

        java.util.HashMap<Integer, String> unattackable = ServerDataManager.getUnattackableTargetClass();
        if (unattackable == null) {
            return false;
        }

        String targetClass = target.getClass().getSimpleName();
        return unattackable.containsKey(targetClass.hashCode());
    }

    // ========== updateTarget ==========

    /**
     * Check if target class is in player's custom attack target list.
     */
    public static boolean checkAttackTargetList(Entity host, Entity target) {
        if (target == null || !(host instanceof IShipAttackBase attackHost)) {
            return false;
        }

        int pid = attackHost.getPlayerUID();
        java.util.HashMap<Integer, String> targetList = ServerDataManager.getPlayerTargetClass(pid);
        if (targetList == null) {
            return false;
        }

        String targetClass = target.getClass().getSimpleName();
        if (!targetList.containsKey(targetClass.hashCode())) {
            return false;
        }

        // don't attack owner's own tameables even if class is listed
        if (target instanceof OwnableEntity) {
            return !checkSameOwner(host, target);
        }

        return true;
    }

    // ========== Revenge propagation ==========

    /**
     * Check if host can detect invisible entities (has flare or searchlight).
     */
    private static boolean canDetectInvisible(Entity host) {
        if (host instanceof BasicEntityShip ship) {
            return ship.getStateMinor(ID.M.LevelFlare) >= 1
                    || ship.getStateMinor(ID.M.LevelSearchlight) >= 1;
        }
        // summon → check host's host
        if (host instanceof IShipOwner owner) {
            Entity hostEntity = owner.getHostEntity();
            if (hostEntity instanceof BasicEntityShip ship) {
                return ship.getStateMinor(ID.M.LevelFlare) >= 1
                        || ship.getStateMinor(ID.M.LevelSearchlight) >= 1;
            }
        }
        return false;
    }

    /**
     * Check if two entities have the same owner (same PlayerUID).
     * Returns true if they belong to the same player or are the same player.
     */
    public static boolean checkSameOwner(Entity a, Entity b) {
        int uidA = getOwnerUID(a);
        int uidB = getOwnerUID(b);

        // both have valid UIDs
        if (uidA > 0 && uidB > 0) {
            return uidA == uidB;
        }

        // if a is the owner of b or b is the owner of a
        if (a instanceof Player player && b instanceof BasicEntityShip ship) {
            return ship.isOwnedBy(player);
        }
        if (b instanceof Player player && a instanceof BasicEntityShip ship) {
            return ship.isOwnedBy(player);
        }

        // check OwnableEntity (vanilla tameable)
        if (b instanceof OwnableEntity ownable) {
            return a.equals(ownable.getOwner());
        }
        if (a instanceof OwnableEntity ownable) {
            return b.equals(ownable.getOwner());
        }

        return false;
    }

    // ========== Utility methods ==========

    /**
     * Check if two entities are allies (same owner or allied team).
     * Delegates to TeamHelper for full team-based alliance checking.
     */
    public static boolean checkIsAlly(Entity a, Entity b) {
        return TeamHelper.checkIsAlly(a, b);
    }

    /**
     * Check if target is banned (hostile) relative to host.
     * Delegates to TeamHelper for team-based ban checking.
     */
    public static boolean checkIsBanned(Entity host, Entity target) {
        return TeamHelper.checkIsBanned(host, target);
    }

    /**
     * Get the owner player UID for an entity.
     * Returns -1 if entity has no owner.
     */
    private static int getOwnerUID(Entity entity) {
        if (entity instanceof IShipOwner owner) {
            return owner.getPlayerUID();
        }
        // players don't have a UID in this system — return -1
        return -1;
    }

    private static boolean isInvalidTarget(Entity host, Entity target) {
        return target == null || !target.isAlive() || host == null || host.equals(target);
    }

    /**
     * Sort entities by distance from a reference entity (nearest first).
     */
    public static class Sorter implements Comparator<Entity> {
        private final Entity origin;

        public Sorter(Entity origin) {
            this.origin = origin;
        }

        @Override
        public int compare(Entity a, Entity b) {
            double da = this.origin.distanceToSqr(a);
            double db = this.origin.distanceToSqr(b);
            return Double.compare(da, db);
        }
    }

    /**
     * Standard target selector for friendly (tamed) ships.
     * Checks PVP flag, anti-air, anti-sub, on-sight, invisibility, and team.
     */
    public static class Selector implements Predicate<Entity> {
        protected final Entity host;
        protected boolean isPVP;
        protected boolean isAA;
        protected boolean isASM;

        public Selector(Entity host) {
            this.host = host;
        }

        @Override
        public boolean test(Entity target) {
            // update flags from host
            if (host instanceof BasicEntityShip ship) {
                this.isPVP = ship.getStateFlag(ID.F.PVPFirst);
                this.isAA = ship.getStateFlag(ID.F.AntiAir);
                this.isASM = ship.getStateFlag(ID.F.AntiSS);
            } else {
                this.isPVP = false;
                this.isAA = false;
                this.isASM = false;
            }

            // null / alive / self check
            if (isInvalidTarget(host, target)) {
                return false;
            }

            // player targeting
            if (target instanceof Player player) {
                if (player.getAbilities().invulnerable)
                    return false;

                switch (ConfigHandler.shipAttackPlayer()) {
                    case 0: // don't attack players
                        break;
                    case 1: // attack hostile players
                        if (checkIsBanned(host, target))
                            return true;
                        break;
                    case 2: // attack hostile and neutral players
                        if (!checkIsAlly(host, target))
                            return true;
                        break;
                    case 3: // attack all players except owner
                        if (!checkSameOwner(host, target))
                            return true;
                        break;
                }
            }

            // invulnerable entities (projectiles, hangings, etc.)
            if (isEntityInvulnerable(target))
                return false;

            // invisible target check
            if (target.isInvisible()) {
                if (!canDetectInvisible(host))
                    return false;
            }

            // on-sight check for ship host
            if (host instanceof BasicEntityShip ship) {
                if (ship.getStateFlag(ID.F.OnSightChase)) {
                    if (!ship.getSensing().hasLineOfSight(target))
                        return false;
                }
            } else if (host instanceof Mob mob) {
                if (!mob.getSensing().hasLineOfSight(target))
                    return false;
            }

            // anti-air target (airplanes, missiles)
            if (target instanceof BasicEntityAirplane) {
                return isAA && checkIsBanned(host, target);
            }

            // anti-submarine target
            if (target instanceof IShipInvisible) {
                return isASM && checkIsBanned(host, target);
            }

            // PVP: attack hostile ships/mounts
            if (this.isPVP && (target instanceof BasicEntityShip || target instanceof BasicEntityMount)) {
                if (checkIsBanned(host, target))
                    return true;
            }

            // Target hostile ships (they extend Mob, not Monster, so they
            // won't be caught by the Monster check below)
            if (target instanceof BasicEntityShipHostile) {
                return true;
            }

            // vanilla monsters and slimes
            if (target instanceof Monster || target instanceof Slime) {
                return true;
            }

            // Explicit custom targets are the final canonical target class.
            return checkAttackTargetList(host, target);
        }
    }

    /**
     * Revenge target selector for friendly ships.
     * More permissive than standard Selector — accepts any non-ally attacker.
     */
    public static class RevengeSelector implements Predicate<Entity> {
        protected final Entity host;

        public RevengeSelector(Entity host) {
            this.host = host;
        }

        @Override
        public boolean test(Entity target) {
            if (isInvalidTarget(host, target)) {
                return false;
            }

            // don't revenge on invulnerable players
            if (target instanceof Player player && player.getAbilities().invulnerable) {
                return false;
            }

            if (isEntityInvulnerable(target))
                return false;

            // invisible check
            if (target.isInvisible()) {
                if (!canDetectInvisible(host))
                    return false;
            }

            // ship/summon targets: check ally state
            if (target instanceof IShipOwner) {
                return !checkIsAlly(host, target);
            }

            // other entities: attack if not same owner
            return !checkSameOwner(host, target);
        }
    }

    /**
     * Target selector for hostile (mob) ships.
     * Targets players (if config allows), friendly ships, and their summons.
     */
    public static class SelectorForHostile implements Predicate<Entity> {
        private final Entity host;

        public SelectorForHostile(Entity host) {
            this.host = host;
        }

        @Override
        public boolean test(Entity target) {
            if (isInvalidTarget(host, target)) {
                return false;
            }

            // player targeting
            if (target instanceof Player player) {
                if (player.getAbilities().invulnerable)
                    return false;
                return ConfigHandler.mobShipsAttackPlayer();
            }

            if (isEntityInvulnerable(target))
                return false;

            if (!target.isInvisible()) {
                // don't attack other hostile ships
                if (target instanceof BasicEntityShipHostile)
                    return false;

                // attack friendly ships and mounts
                if (target instanceof BasicEntityShip || target instanceof BasicEntityMount) {
                    return true;
                }

                // attack summons if not same faction
                if (target instanceof IShipOwner) {
                    return !checkSameOwner(host, target);
                }
            }

            return false;
        }
    }

    /**
     * Revenge target selector for hostile (mob) ships.
     * Attacks back anything except other hostile ships.
     */
    public static class RevengeSelectorForHostile implements Predicate<Entity> {
        private final Entity host;

        public RevengeSelectorForHostile(Entity host) {
            this.host = host;
        }

        @Override
        public boolean test(Entity target) {
            if (isInvalidTarget(host, target)) {
                return false;
            }

            if (target instanceof Player player) {
                return !player.getAbilities().invulnerable;
            }

            if (isEntityInvulnerable(target))
                return false;

            if (!target.isInvisible()) {
                // don't attack other hostile ships
                if (target instanceof BasicEntityShipHostile)
                    return false;

                // attack friendly ships
                if (target instanceof BasicEntityShip)
                    return true;

                // attack summons if not same faction
                if (target instanceof IShipOwner) {
                    return !checkSameOwner(host, target);
                }

                // attack anything else not same owner
                return !checkSameOwner(host, target);
            }

            return false;
        }
    }

}
