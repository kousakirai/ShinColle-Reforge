package com.lulan.shincolle.ai.path;

/**
 * Min-heap for A* pathfinding, ordered by distanceToTarget.
 * Ported from 1.10.2 ShipPathHeap.
 */
@Deprecated(forRemoval = true)
public class ShipPathHeap {

    private ShipPathPoint[] pathPoints = new ShipPathPoint[128];
    private int count;

    public ShipPathPoint addPoint(ShipPathPoint point) {
        if (point.index >= 0) {
            throw new IllegalStateException("Point already in heap");
        }

        // expand if full
        if (this.count == this.pathPoints.length) {
            ShipPathPoint[] expanded = new ShipPathPoint[this.count << 1];
            System.arraycopy(this.pathPoints, 0, expanded, 0, this.count);
            this.pathPoints = expanded;
        }

        this.pathPoints[this.count] = point;
        point.index = this.count;
        this.sortToRoot(this.count++);
        return point;
    }

    public void clearPath() {
        this.count = 0;
    }

    /**
     * Remove and return the point with smallest distanceToTarget
     */
    public ShipPathPoint dequeue() {
        ShipPathPoint pathpoint = this.pathPoints[0];
        this.pathPoints[0] = this.pathPoints[--this.count];
        this.pathPoints[this.count] = null;

        if (this.count > 0) {
            this.sortToLeaf(0);
        }

        pathpoint.index = -1;
        return pathpoint;
    }

    /**
     * Update a point's distance and re-sort
     */
    public void changeDistance(ShipPathPoint point, float dist) {
        float oldDist = point.distanceToTarget;
        point.distanceToTarget = dist;

        if (dist < oldDist) {
            this.sortToRoot(point.index);
        } else {
            this.sortToLeaf(point.index);
        }
    }

    /**
     * Bubble up toward root
     */
    private void sortToRoot(int id) {
        ShipPathPoint fromNode = this.pathPoints[id];
        float f = fromNode.distanceToTarget;
        int j;

        for (; id > 0; id = j) {
            j = (id - 1) >> 1;
            ShipPathPoint parentNode = this.pathPoints[j];

            if (f >= parentNode.distanceToTarget) {
                break;
            }

            this.pathPoints[id] = parentNode;
            parentNode.index = id;
        }

        this.pathPoints[id] = fromNode;
        fromNode.index = id;
    }

    /**
     * Bubble down toward leaf
     */
    private void sortToLeaf(int id) {
        ShipPathPoint fromNode = this.pathPoints[id];
        float fromDist = fromNode.distanceToTarget;

        while (true) {
            int left = 1 + (id << 1);
            int right = left + 1;

            if (left >= this.count) {
                break;
            }

            ShipPathPoint leftNode = this.pathPoints[left];
            float leftDist = leftNode.distanceToTarget;
            float rightDist;

            if (right >= this.count) {
                rightDist = Float.POSITIVE_INFINITY;
            } else {
                rightDist = this.pathPoints[right].distanceToTarget;
            }

            if (leftDist < rightDist) {
                if (leftDist >= fromDist)
                    break;
                this.pathPoints[id] = leftNode;
                leftNode.index = id;
                id = left;
            } else {
                if (rightDist >= fromDist)
                    break;
                ShipPathPoint rightNode = this.pathPoints[right];
                this.pathPoints[id] = rightNode;
                rightNode.index = id;
                id = right;
            }
        }

        this.pathPoints[id] = fromNode;
        fromNode.index = id;
    }

    public boolean isPathEmpty() {
        return this.count == 0;
    }
}
