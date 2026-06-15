package com.lulan.shincolle.ai.path;

import net.minecraft.util.Mth;

/**
 * Path point for ship path navigator.
 * Ported from 1.10.2 ShipPathPoint.
 */
@Deprecated(forRemoval = true)
public class ShipPathPoint {

    public final int xCoord;
    public final int yCoord;
    public final int zCoord;
    private final int hash;
    /**
     * The index of this point in its assigned path
     */
    public int index = -1;
    /**
     * The distance along the path to this point
     */
    public float totalPathDistance;
    /**
     * The linear distance to the next point
     */
    public float distanceToNext;
    /**
     * The distance to the target
     */
    public float distanceToTarget;
    /**
     * The point preceding this in its assigned path
     */
    public ShipPathPoint previous;
    /**
     * True if the pathfinder has already visited this point
     */
    public boolean visited;
    /**
     * path cost calculation
     */
    public float distanceFromOrigin = 0F;
    public float cost = 0F;
    public float costMalus = 0F;

    public ShipPathPoint(int x, int y, int z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.hash = makeHash(x, y, z);
    }

    public static int makeHash(int x, int y, int z) {
        return y & 255 | (x & 32767) << 8 | (z & 32767) << 24
                | (x < 0 ? Integer.MIN_VALUE : 0) | (z < 0 ? 32768 : 0);
    }

    /**
     * Euclidean distance
     */
    public float distanceTo(ShipPathPoint point) {
        float f = point.xCoord - this.xCoord;
        float f1 = point.yCoord - this.yCoord;
        float f2 = point.zCoord - this.zCoord;
        return Mth.sqrt(f * f + f1 * f1 + f2 * f2);
    }

    /**
     * Euclidean distance squared
     */
    public float distanceToSquared(ShipPathPoint point) {
        float f = point.xCoord - this.xCoord;
        float f1 = point.yCoord - this.yCoord;
        float f2 = point.zCoord - this.zCoord;
        return f * f + f1 * f1 + f2 * f2;
    }

    /**
     * Manhattan distance
     */
    public float distanceManhattan(ShipPathPoint point) {
        float f = (float) Math.abs(point.xCoord - this.xCoord);
        float f1 = (float) Math.abs(point.yCoord - this.yCoord);
        float f2 = (float) Math.abs(point.zCoord - this.zCoord);
        return f + f1 + f2;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ShipPathPoint pathpoint)) {
            return false;
        }
        return this.hash == pathpoint.hash
                && this.xCoord == pathpoint.xCoord
                && this.yCoord == pathpoint.yCoord
                && this.zCoord == pathpoint.zCoord;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    /**
     * Returns true if this point has already been assigned to a path
     */
    public boolean isAssigned() {
        return this.index >= 0;
    }

    @Override
    public String toString() {
        return this.xCoord + ", " + this.yCoord + ", " + this.zCoord;
    }
}
