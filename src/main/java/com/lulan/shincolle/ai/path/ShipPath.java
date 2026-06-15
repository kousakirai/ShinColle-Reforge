package com.lulan.shincolle.ai.path;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Path container for ship path navigator - an ordered array of ShipPathPoints.
 * Ported from 1.10.2 ShipPath.
 */
@Deprecated(forRemoval = true)
public class ShipPath {

    private final ShipPathPoint[] points;
    private final int pathLength;
    private int currentPathIndex;

    public ShipPath(ShipPathPoint[] pathpoints) {
        this.points = pathpoints;
        this.pathLength = pathpoints.length;
    }

    public boolean isFinished() {
        return this.currentPathIndex >= this.pathLength;
    }

    public ShipPathPoint getFinalPathPoint() {
        return this.pathLength > 0 ? this.points[this.pathLength - 1] : null;
    }

    public ShipPathPoint getPathPointFromIndex(int i) {
        return this.points[i];
    }

    public int getCurrentPathLength() {
        return this.pathLength;
    }

    public int getCurrentPathIndex() {
        return this.currentPathIndex;
    }

    public void setCurrentPathIndex(int i) {
        this.currentPathIndex = i;
    }

    /**
     * Get position vector for a path point, adjusted for entity width
     */
    public Vec3 getVectorFromIndex(Entity entity, int i) {
        if (i >= points.length)
            i = points.length - 1;

        double d0 = this.points[i].xCoord + ((int) (entity.getBbWidth() + 1.0F)) * 0.5D;
        double d1 = this.points[i].yCoord;
        double d2 = this.points[i].zCoord + ((int) (entity.getBbWidth() + 1.0F)) * 0.5D;
        return new Vec3(d0, d1, d2);
    }

    /**
     * Get current target position vector
     */
    public Vec3 getPosition(Entity entity) {
        return this.getVectorFromIndex(entity, this.currentPathIndex);
    }

    /**
     * Get current path point as raw coordinates
     */
    public Vec3 getCurrentPos() {
        ShipPathPoint pathpoint = this.points[this.currentPathIndex];
        return new Vec3(pathpoint.xCoord, pathpoint.yCoord, pathpoint.zCoord);
    }

    public boolean isSamePath(ShipPath path) {
        if (path == null)
            return false;
        if (path.points.length != this.points.length)
            return false;

        for (int i = 0; i < this.points.length; ++i) {
            if (this.points[i].xCoord != path.points[i].xCoord
                    || this.points[i].yCoord != path.points[i].yCoord
                    || this.points[i].zCoord != path.points[i].zCoord) {
                return false;
            }
        }
        return true;
    }
}
