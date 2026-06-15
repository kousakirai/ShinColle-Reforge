package com.lulan.shincolle.ai.path;

import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipNavigator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Custom path navigator for ship and airplane entities.
 * Finds paths through water/air, ignoring gravity and buoyancy.
 * Entities using this must implement IShipNavigator.
 * <p>
 * Ported from 1.10.2 ShipPathNavigate (standalone, not extending vanilla).
 */
@Deprecated
public class ShipPathNavigate {

    private final Mob host;
    private final IShipNavigator hostShip;
    private final int hostCeilWidth;
    private final int hostCeilHeight;
    private Level world;
    @Nullable
    private ShipPath currentPath;
    private double speed;
    private int pathTicks;
    private int ticksAtLastPos;
    private Vec3 lastPosCheck = Vec3.ZERO;
    private Vec3 lastPosStuck = Vec3.ZERO;
    private long timeoutTimer = 0L;
    private long lastTimeoutCheck = 0L;
    private double timeoutLimit;
    private final float maxDistanceToWaypoint;
    private BlockPos targetPos;

    public ShipPathNavigate(Mob entity) {
        this.host = entity;
        this.hostShip = (IShipNavigator) entity;
        this.world = entity.level();

        this.maxDistanceToWaypoint = (float) Math.max(
                this.host.getBbWidth() * 0.75D, 0.75D);
        this.hostCeilWidth = Mth.ceil(this.host.getBbWidth());
        this.hostCeilHeight = Mth.ceil(this.host.getBbHeight());
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public float getPathSearchRange() {
        return host instanceof IShipAttackBase ? 70F : 48F;
    }

    /**
     * Try to find and set a path to XYZ. Returns true if successful.
     */
    public boolean tryMoveToXYZ(double x, double y, double z, double speed) {
        ShipPath path = this.getPathToXYZ(Mth.floor(x), (int) y, Mth.floor(z));
        return this.setPath(path, speed);
    }

    /**
     * Get path to block coordinates
     */
    public ShipPath getPathToXYZ(double x, double y, double z) {
        if (!this.canNavigate())
            return null;
        return this.getShipPathToXYZ(this.host, Mth.floor(x), (int) y, Mth.floor(z),
                this.getPathSearchRange(), this.hostShip.canFly());
    }

    public ShipPath getShipPathToXYZ(Entity entity, int x, int y, int z, float range, boolean canFly) {
        BlockPos pos = new BlockPos(x, y, z);
        if (this.currentPath != null && !this.currentPath.isFinished() && pos.equals(this.targetPos)) {
            return this.currentPath;
        }

        this.targetPos = pos;
        this.world = entity.level();
        BlockPos hostPos = entity.blockPosition();
        int padding = (int) (range + 8.0F);
        PathNavigationRegion region = new PathNavigationRegion(
                this.world,
                hostPos.offset(-padding, -padding, -padding),
                hostPos.offset(padding, padding, padding));
        return new ShipPathFinder(region, canFly).findPath(entity, x, y, z, range);
    }

    /**
     * Get path to a living entity
     */
    public ShipPath getPathToEntityLiving(Entity entity) {
        if (!this.canNavigate())
            return null;
        return this.getPathEntityToEntity(this.host, entity,
                this.getPathSearchRange(), this.hostShip.canFly());
    }

    public ShipPath getPathEntityToEntity(Entity entity, Entity targetEntity, float range, boolean canFly) {
        BlockPos pos = targetEntity.blockPosition();
        if (this.currentPath != null && !this.currentPath.isFinished() && pos.equals(this.targetPos)) {
            return this.currentPath;
        }

        this.targetPos = pos;
        this.world = entity.level();
        BlockPos hostPos = entity.blockPosition().above();
        int padding = (int) (range + 16.0F);
        PathNavigationRegion region = new PathNavigationRegion(
                this.world,
                hostPos.offset(-padding, -padding, -padding),
                hostPos.offset(padding, padding, padding));
        return new ShipPathFinder(region, canFly).findPath(entity, targetEntity, range);
    }

    /**
     * Try to find and set a path to entity. Returns true if successful.
     */
    public boolean tryMoveToEntityLiving(Entity entity, double speed) {
        ShipPath path = this.getPathToEntityLiving(entity);
        return path != null && this.setPath(path, speed);
    }

    /**
     * Set the current path. Returns true if path was set successfully.
     */
    public boolean setPath(ShipPath pathEntity, double speed) {
        if (pathEntity == null) {
            this.currentPath = null;
            return false;
        }

        if (pathEntity.getCurrentPathLength() == 0) {
            return false;
        }

        this.currentPath = pathEntity;
        this.speed = speed;
        Vec3 vec3 = this.getEntityPosition();
        this.ticksAtLastPos = this.pathTicks;
        this.lastPosCheck = vec3;
        return true;
    }

    public ShipPath getPath() {
        return this.currentPath;
    }

    /**
     * Navigation tick - called every tick from entity update
     */
    public void onUpdateNavigation() {
        if (host.tickCount > 40) {
            ++this.pathTicks;

            if (!this.noPath()) {
                if (this.canNavigate()) {
                    this.pathFollow();
                }

                if (!this.noPath()) {
                    assert this.currentPath != null;
                    Vec3 vec3 = this.currentPath.getPosition(this.host);

                    if (vec3 != null) {
                        BlockPos blockPos = BlockPos.containing(vec3).below();
                        BlockState state = this.world.getBlockState(blockPos);
                        AABB blockAABB = state.getShape(this.world, blockPos).isEmpty()
                                ? AABB.unitCubeFromLowerCorner(Vec3.ZERO)
                                : state.getShape(this.world, blockPos).bounds();
                        vec3 = vec3.subtract(0.0D, 1.0D - blockAABB.maxY, 0.0D);

                        this.hostShip.getShipMoveControl().setWantedPosition(
                                vec3.x, vec3.y + 0.1D, vec3.z, this.speed);
                    }
                }
            }
        }
    }

    /**
     * Follow path: advance waypoint index and check for shortcuts
     */
    private void pathFollow() {
        Vec3 hostPos = this.getEntityPosition();
        assert this.currentPath != null;
        int i = this.currentPath.getCurrentPathLength();

        for (int j = this.currentPath.getCurrentPathIndex(); j < this.currentPath.getCurrentPathLength(); ++j) {
            if ((double) this.currentPath.getPathPointFromIndex(j).yCoord != Math.floor(hostPos.y)) {
                i = j;
                break;
            }
        }

        Vec3 nowPos = this.currentPath.getCurrentPos();

        if (Math.abs(this.host.getX() - nowPos.x - 0.5D) < this.maxDistanceToWaypoint
                && Math.abs(this.host.getZ() - nowPos.z - 0.5D) < this.maxDistanceToWaypoint) {
            this.currentPath.setCurrentPathIndex(
                    this.currentPath.getCurrentPathIndex() + 1);
        }

        for (int j1 = i - 1; j1 >= this.currentPath.getCurrentPathIndex(); --j1) {
            if (this.isDirectPathBetweenPoints(hostPos,
                    this.currentPath.getVectorFromIndex(this.host, j1),
                    this.hostCeilWidth, this.hostCeilHeight, this.hostCeilWidth)) {
                this.currentPath.setCurrentPathIndex(j1);
                break;
            }
        }

        this.checkForStuck(hostPos);
    }

    /**
     * Check if entity is stuck and clear path if so
     */
    private void checkForStuck(Vec3 pos) {
        int checkTick = this.pathTicks - this.ticksAtLastPos;

        if (checkTick % 32 == 0) {
            boolean isStuck = false;
            double dist = pos.distanceToSqr(this.lastPosCheck);

            if (dist < 1D) {
                isStuck = true;

                assert currentPath != null;
                if (!currentPath.isFinished()) {
                    Vec3 targetVec = currentPath.getVectorFromIndex(
                            this.host, currentPath.getCurrentPathIndex());
                    float dx = (float) (targetVec.x - host.getX());
                    float dz = (float) (targetVec.z - host.getZ());

                    double targetX = dx > 0.1F ? 1D : dx < -0.1F ? -1D : 0D;
                    double targetZ = dz > 0.1F ? 1D : dz < -0.1F ? -1D : 0D;

                    Vec3 motion = this.host.getDeltaMovement();
                    this.host.setDeltaMovement(
                            this.speed * 0.5D * targetX,
                            motion.y,
                            this.speed * 0.5D * targetZ);

                    if (host.getRandom().nextInt(2) == 0) {
                        host.getJumpControl().jump();
                        float spd = (float) (host.getSpeed() * 0.35D);
                        double mx = motion.x;
                        double mz = motion.z;
                        if (dx > 0.2F)
                            mx += spd;
                        if (dx < -0.2F)
                            mx -= spd;
                        if (dz > 0.2F)
                            mz += spd;
                        if (dz < -0.2F)
                            mz -= spd;
                        this.host.setDeltaMovement(mx, motion.y, mz);
                    }
                }
            }

            if (checkTick > 100) {
                if (isStuck) {
                    this.clearPathEntity();
                }
                this.ticksAtLastPos = this.pathTicks;
            }

            this.lastPosCheck = pos;
        }

        if (this.currentPath != null && !this.currentPath.isFinished()) {
            Vec3 vec3d = this.currentPath.getCurrentPos();

            if (!vec3d.equals(this.lastPosStuck)) {
                this.lastPosStuck = vec3d;
                double d0 = pos.distanceTo(this.lastPosStuck);
                this.timeoutLimit = this.host.getSpeed() > 0F
                        ? d0 / (double) this.host.getSpeed() * 1000D
                        : 0D;
            } else {
                this.timeoutTimer += System.currentTimeMillis() - this.lastTimeoutCheck;
            }

            if (this.timeoutLimit > 0D && (double) this.timeoutTimer > this.timeoutLimit * 3D) {
                this.lastPosStuck = Vec3.ZERO;
                this.timeoutTimer = 0L;
                this.timeoutLimit = 0D;
                this.clearPathEntity();
            }

            this.lastTimeoutCheck = System.currentTimeMillis();
        }
    }

    public boolean noPath() {
        return this.currentPath == null || this.currentPath.isFinished();
    }

    public void clearPathEntity() {
        this.currentPath = null;
    }

    /**
     * Alias for clearPathEntity, compatible with stop() calls
     */
    public void stop() {
        this.clearPathEntity();
    }

    private Vec3 getEntityPosition() {
        return new Vec3(this.host.getX(), this.host.getY() + 0.5D, this.host.getZ());
    }

    private boolean canNavigate() {
        return !host.isPassenger()
                && (this.hostShip.canFly() || this.host.onGround() || isEntityFree());
    }

    /**
     * Check if entity is in a passable position (fluid, air, etc.)
     */
    private boolean isEntityFree() {
        BlockPos pos = this.host.blockPosition();
        BlockState state = this.world.getBlockState(pos);
        return state.isAir() || !state.getFluidState().isEmpty()
                || state.getCollisionShape(this.world, pos).isEmpty();
    }

    /**
     * Check if direct unobstructed path exists between two points
     */
    private boolean isDirectPathBetweenPoints(Vec3 pos1, Vec3 pos2,
                                              int sizeX, int sizeY, int sizeZ) {
        int x1 = Mth.floor(pos1.x);
        int y1 = (int) pos1.y;
        int z1 = Mth.floor(pos1.z);
        double xOffset = pos2.x - pos1.x;
        double yOffset = pos2.y - pos1.y;
        double zOffset = pos2.z - pos1.z;
        double offsetSq = xOffset * xOffset + zOffset * zOffset + yOffset * yOffset;

        if (offsetSq < 1.0E-8D)
            return false;

        double offsetVec = 1D / Math.sqrt(offsetSq);
        xOffset *= offsetVec;
        yOffset *= offsetVec;
        zOffset *= offsetVec;

        if (!this.isSafeToStandAt(x1, y1, z1, sizeX + 2, sizeY + 1, sizeZ + 2,
                pos1, xOffset, zOffset)) {
            return false;
        }

        double unitX = 1D / Math.abs(xOffset);
        double unitY = 1D / Math.abs(yOffset);
        double unitZ = 1D / Math.abs(zOffset);

        double proX = (double) x1 - pos1.x;
        double proY = (double) y1 - pos1.y;
        double proZ = (double) z1 - pos1.z;

        if (xOffset >= 0D)
            ++proX;
        if (yOffset >= 0D)
            ++proY;
        if (zOffset >= 0D)
            ++proZ;

        proX = proX / xOffset;
        proY = proY / yOffset;
        proZ = proZ / zOffset;

        int dirX = xOffset < 0D ? -1 : 1;
        int dirY = yOffset < 0D ? -1 : 1;
        int dirZ = zOffset < 0D ? -1 : 1;

        int x2 = Mth.floor(pos2.x);
        int y2 = Mth.floor(pos2.y);
        int z2 = Mth.floor(pos2.z);

        int xIntOffset = x2 - x1;
        int yIntOffset = y2 - y1;
        int zIntOffset = z2 - z1;

        while (xIntOffset * dirX > 0 || yIntOffset * dirY > 0 || zIntOffset * dirZ > 0) {
            if (proX <= proY && proX <= proZ) {
                proX += unitX;
                x1 += dirX;
                xIntOffset = x2 - x1;
            } else if (proY <= proX && proY <= proZ) {
                proY += unitY;
                y1 += dirY;
                yIntOffset = y2 - y1;
            } else {
                proZ += unitZ;
                z1 += dirZ;
                zIntOffset = z2 - z1;
            }

            if (!this.isSafeToStandAt(x1, y1, z1, sizeX, sizeY, sizeZ,
                    pos1, xOffset, zOffset)) {
                return false;
            }
        }

        return true;
    }

    private boolean isSafeToStandAt(int x, int y, int z,
                                    int sizeX, int sizeY, int sizeZ,
                                    Vec3 orgPos, double vecX, double vecZ) {
        if (this.hostShip.canFly())
            return true;

        int x2 = x - sizeX / 2;
        int z2 = z - sizeZ / 2;

        if (!this.isPositionClear(x2, y, z2, sizeX, sizeY, sizeZ, orgPos, vecX, vecZ)) {
            return false;
        }

        for (int cx = x2; cx < x2 + sizeX; ++cx) {
            for (int cz = z2; cz < z2 + sizeZ; ++cz) {
                double dx2 = cx + 0.5D - orgPos.x;
                double dz2 = cz + 0.5D - orgPos.z;

                if (dx2 * vecX + dz2 * vecZ >= 0D) {
                    BlockPos belowPos = new BlockPos(cx, y - 1, cz);
                    BlockState belowState = this.world.getBlockState(belowPos);

                    if (belowState.isAir() && belowState.getFluidState().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isPositionClear(int xOff, int yOff, int zOff,
                                    int sizeX, int sizeY, int sizeZ,
                                    Vec3 orgPos, double vecX, double vecZ) {
        for (BlockPos blockpos : BlockPos.betweenClosed(
                new BlockPos(xOff, yOff, zOff),
                new BlockPos(xOff + sizeX - 1, yOff + sizeY - 1, zOff + sizeZ - 1))) {
            double d0 = blockpos.getX() + 0.5D - orgPos.x;
            double d1 = blockpos.getZ() + 0.5D - orgPos.z;

            if (d0 * vecX + d1 * vecZ >= 0D) {
                BlockState state = this.world.getBlockState(blockpos);
                if (!state.isAir() && !state.getCollisionShape(this.world, blockpos).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
