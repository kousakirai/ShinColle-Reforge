package com.lulan.shincolle.ai.path;

import com.lulan.shincolle.reference.Enums.EnumPathType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A* pathfinder for ship/airplane entities through water and air.
 * Ignores gravity and buoyancy to build paths in 3D.
 * Ported from 1.10.2 ShipPathFinder.
 */
@Deprecated(forRemoval = true)
public class ShipPathFinder {

    private final BlockGetter world;
    private final ShipPathHeap path = new ShipPathHeap();
    private final Map<Integer, ShipPathPoint> pointMap = new HashMap<>();
    private final boolean canEntityFly;

    public ShipPathFinder(BlockGetter world, boolean canFly) {
        this.world = world;
        this.canEntityFly = canFly;
    }

    /**
     * Create path from entity to another entity
     */
    @Nullable
    public ShipPath findPath(Entity fromEnt, Entity toEnt, float range) {
        return this.findPath(fromEnt, toEnt.getX(), toEnt.getBoundingBox().minY, toEnt.getZ(), range);
    }

    /**
     * Create path from entity to block position
     */
    @Nullable
    public ShipPath findPath(Entity entity, int x, int y, int z, float range) {
        return this.findPath(entity, x + 0.5, y + 0.5, z + 0.5, range);
    }

    @Nullable
    private ShipPath findPath(Entity entity, double x, double y, double z, float range) {
        this.path.clearPath();
        this.pointMap.clear();

        int i = Mth.floor(entity.getBoundingBox().minY + 0.5D);

        ShipPathPoint startpp = this.openPoint(
                Mth.floor(entity.getBoundingBox().minX), i,
                Mth.floor(entity.getBoundingBox().minZ));
        ShipPathPoint endpp = this.openPoint(
                Mth.floor(x - entity.getBbWidth() * 0.5F),
                Mth.floor(y),
                Mth.floor(z - entity.getBbWidth() * 0.5F));

        ShipPathPoint entitySize = new ShipPathPoint(
                Mth.floor(entity.getBbWidth() + 1F),
                Mth.floor(entity.getBbHeight() + 1F),
                Mth.floor(entity.getBbWidth() + 1F));

        return this.findPathInternal(entity, startpp, endpp, entitySize, range);
    }

    /**
     * A* algorithm using Manhattan distance heuristic
     */
    @Nullable
    private ShipPath findPathInternal(Entity entity, ShipPathPoint startpp,
                                      ShipPathPoint endpp, ShipPathPoint entitySize, float range) {
        startpp.totalPathDistance = 0F;
        startpp.distanceToNext = startpp.distanceManhattan(endpp);
        startpp.distanceToTarget = startpp.distanceToNext;
        this.path.clearPath();
        this.path.addPoint(startpp);
        ShipPathPoint ppTemp = startpp;
        int findCount = 0;

        while (!this.path.isPathEmpty()) {
            ShipPathPoint ppDequeue = this.path.dequeue();
            findCount++;

            if (ppDequeue.equals(endpp)) {
                return this.createEntityPath(startpp, endpp);
            }

            if (findCount > 450)
                break;

            if (ppDequeue.distanceToSquared(endpp) < ppTemp.distanceToSquared(endpp)) {
                ppTemp = ppDequeue;
            }

            ppDequeue.visited = true;
            ShipPathPoint[] findOption = this.findPathOptions(entity, ppDequeue, entitySize, endpp, range);

            for (ShipPathPoint pp : findOption) {
                float dist = ppDequeue.distanceManhattan(pp);
                pp.distanceFromOrigin = ppDequeue.distanceFromOrigin + dist;
                pp.cost = dist + pp.costMalus;
                float dist2 = ppDequeue.totalPathDistance + pp.cost;

                if (pp.distanceFromOrigin < range && (!pp.isAssigned() || dist2 < pp.totalPathDistance)) {
                    pp.previous = ppDequeue;
                    pp.totalPathDistance = dist2;
                    pp.distanceToNext = pp.distanceManhattan(endpp) + pp.costMalus;

                    if (pp.isAssigned()) {
                        this.path.changeDistance(pp, pp.totalPathDistance + pp.distanceToNext);
                    } else {
                        pp.distanceToTarget = pp.totalPathDistance + pp.distanceToNext;
                        this.path.addPoint(pp);
                    }
                }
            }
        }

        if (ppTemp == startpp)
            return null;
        return this.createEntityPath(startpp, ppTemp);
    }

    /**
     * Find available movement directions from current point (10-connected:
     * N/S/E/W/Up/Down + 4 diagonals)
     */
    private ShipPathPoint[] findPathOptions(Entity entity, ShipPathPoint currentpp,
                                            ShipPathPoint entitySize, ShipPathPoint targetpp, float range) {
        EnumPathType type = getPathType(entity, currentpp.xCoord, currentpp.yCoord + 1, currentpp.zCoord, entitySize);
        int pathYOffset = 0;

        if (type == EnumPathType.FLUID || type == EnumPathType.OPEN) {
            pathYOffset = Mth.floor(Math.max(1F, entity.getStepHeight()));
        }

        ShipPathPoint[] pp = new ShipPathPoint[10];
        // Down
        pp[0] = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord - 1, currentpp.zCoord, entitySize,
                pathYOffset);
        // Up (only if fluid/open above)
        pp[1] = pathYOffset > 0
                ? this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord + 1, currentpp.zCoord, entitySize,
                pathYOffset)
                : null;
        // N/S/E/W
        pp[6] = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord, currentpp.zCoord - 1, entitySize,
                pathYOffset);
        pp[7] = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord, currentpp.zCoord + 1, entitySize,
                pathYOffset);
        pp[8] = this.getSafePoint(entity, currentpp.xCoord + 1, currentpp.yCoord, currentpp.zCoord, entitySize,
                pathYOffset);
        pp[9] = this.getSafePoint(entity, currentpp.xCoord - 1, currentpp.yCoord, currentpp.zCoord, entitySize,
                pathYOffset);

        boolean fn = pp[6] == null || pp[6].costMalus != 0F;
        boolean fs = pp[7] == null || pp[7].costMalus != 0F;
        boolean fe = pp[8] == null || pp[8].costMalus != 0F;
        boolean fw = pp[9] == null || pp[9].costMalus != 0F;

        // Diagonals (only when both adjacent sides are passable)
        pp[2] = (fn && fw) ? this.getSafePoint(entity, currentpp.xCoord - 1, currentpp.yCoord, currentpp.zCoord - 1,
                entitySize, pathYOffset) : null;
        pp[3] = (fn && fe) ? this.getSafePoint(entity, currentpp.xCoord + 1, currentpp.yCoord, currentpp.zCoord - 1,
                entitySize, pathYOffset) : null;
        pp[4] = (fs && fw) ? this.getSafePoint(entity, currentpp.xCoord - 1, currentpp.yCoord, currentpp.zCoord + 1,
                entitySize, pathYOffset) : null;
        pp[5] = (fs && fe) ? this.getSafePoint(entity, currentpp.xCoord + 1, currentpp.yCoord, currentpp.zCoord + 1,
                entitySize, pathYOffset) : null;

        // Filter to valid, unvisited, in-range points
        List<ShipPathPoint> temp = new ArrayList<>();
        for (ShipPathPoint spp : pp) {
            if (spp != null && !spp.visited && spp.distanceTo(targetpp) < range) {
                temp.add(spp);
            }
        }
        return temp.toArray(new ShipPathPoint[0]);
    }

    /**
     * Get a safe point, checking passability and fall distance
     */
    @Nullable
    private ShipPathPoint getSafePoint(Entity entity, int x, int y, int z,
                                       ShipPathPoint entitySize, int pathYOffset) {
        ShipPathPoint pp = null;
        EnumPathType pathCase = getPathType(entity, x, y, z, entitySize);

        if (pathCase == EnumPathType.FLUID || pathCase == EnumPathType.OPENABLE) {
            return openPoint(x, y, z);
        }

        if (pathCase == EnumPathType.OPEN) {
            pp = openPoint(x, y, z);
        }

        // If blocked, try stepping up
        if (pp == null) {
            if (pathYOffset > 1 || (pathYOffset > 0 && pathCase != EnumPathType.FENCE)) {
                pp = getSafePoint(entity, x, y + 1, z, entitySize, pathYOffset - 1);
            }
        }

        if (pp != null) {
            // Flying entities don't need ground check
            if (this.canEntityFly)
                return pp;

            // Non-flying: find safe landing point below
            int fallCount = 0;
            while (y > 0) {
                EnumPathType downCase = getPathType(entity, x, y - 1, z, entitySize);

                if (downCase == EnumPathType.FLUID) {
                    pp = this.openPoint(x, y - 1, z);
                    break;
                }
                if (downCase != EnumPathType.OPEN)
                    break;
                if (fallCount++ > 64)
                    return null;

                --y;
                if (y > 0) {
                    pp = this.openPoint(x, y, z);
                }
            }
        }
        return pp;
    }

    private ShipPathPoint openPoint(int x, int y, int z) {
        int hash = ShipPathPoint.makeHash(x, y, z);

        return this.pointMap.computeIfAbsent(hash, k -> new ShipPathPoint(x, y, z));
    }

    /**
     * Determine the path type at position (x,y,z) considering entity size
     */
    public EnumPathType getPathType(Entity entity, int x, int y, int z, ShipPathPoint entitySize) {
        boolean pathInLiquid = false;
        int doorCount = 0;

        // Check origin position for liquid
        BlockState originState = this.world.getBlockState(new BlockPos(x, y, z));
        FluidState originFluid = originState.getFluidState();
        if (!originFluid.isEmpty()) {
            pathInLiquid = true;
        }

        // Check all blocks in entity bounding box
        for (int x1 = x; x1 < x + entitySize.xCoord; ++x1) {
            for (int y1 = y + entitySize.yCoord - 1; y1 >= y; --y1) {
                for (int z1 = z; z1 < z + entitySize.zCoord; ++z1) {
                    BlockPos pos = new BlockPos(x1, y1, z1);
                    BlockState state = this.world.getBlockState(pos);

                    if (!state.isAir()) {
                        var block = state.getBlock();

                        // Fences and walls
                        if (block instanceof FenceBlock || block instanceof WallBlock) {
                            if (y1 == y)
                                return EnumPathType.FENCE;
                            return EnumPathType.BLOCKED;
                        }

                        // Rails and similar passable blocks
                        if (block instanceof BaseRailBlock) {
                            // OPEN - these are passable
                        }
                        // Doors
                        else if (block instanceof DoorBlock) {
                            doorCount++;
                            if (block == Blocks.IRON_DOOR) {
                                return EnumPathType.BLOCKED;
                            }
                            return EnumPathType.OPENABLE;
                        }
                        // Fence gates
                        else if (block instanceof FenceGateBlock) {
                            return EnumPathType.OPENABLE;
                        }
                        // Liquids
                        else if (!state.getFluidState().isEmpty()) {
                            // FLUID handled below
                        }
                        // Lily pads and solid blocks
                        else if (block instanceof WaterlilyBlock
                                || !state.getCollisionShape(this.world, pos).isEmpty()) {
                            return EnumPathType.BLOCKED;
                        }
                    }
                }
            }
        }

        return pathInLiquid ? EnumPathType.FLUID : EnumPathType.OPEN;
    }

    /**
     * Build the final path by tracing back from end to start
     */
    private ShipPath createEntityPath(ShipPathPoint startpp, ShipPathPoint endpp) {
        int count = 1;
        ShipPathPoint temp;

        for (temp = endpp; temp.previous != null; temp = temp.previous) {
            ++count;
        }

        ShipPathPoint[] pathtemp = new ShipPathPoint[count];
        temp = endpp;
        --count;

        for (pathtemp[count] = endpp; temp.previous != null; pathtemp[count] = temp) {
            temp = temp.previous;
            --count;
        }

        return new ShipPath(pathtemp);
    }
}
