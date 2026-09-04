package com.lulan.shincolle.ai.path;

import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ShipPathFinderCore {

    private final ShipNodeEvaluator evaluator;
    private final BinaryHeap openSet = new BinaryHeap();

    /** 1.10.2版: findCount > 450 で打ち切り */
    private static final int MAX_FIND_COUNT = 450;

    public ShipPathFinderCore(ShipNodeEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public Path findPath(PathNavigationRegion region, Mob mob,
                         BlockPos targetPos, float maxRange) {

        this.openSet.clear();
        this.evaluator.prepare(region, mob);

        // 開始・終了ノード生成 (1.10.2版のstartpp/endppに相当)
        Node startNode = evaluator.getStart();
        Target goal    = evaluator.getGoal(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5
        );
        Node endNode = goal; // TargetはNodeのサブクラス

        Path result = findPathBetween(startNode, endNode, maxRange);

        this.evaluator.done();
        return result;
    }

    private Path findPathBetween(Node startNode, Node endNode, float maxRange) {

        // 1.10.2版: startpp.totalPathDistance=0, distanceToNext=distanceManhattan(end)
        startNode.g = 0F;
        startNode.h = manhattan(startNode, endNode);
        startNode.f = startNode.h;

        this.openSet.clear();
        this.openSet.insert(startNode);

        Node bestNode = startNode; // 1.10.2版のppTemp相当
        int findCount = 0;

        while (!this.openSet.isEmpty()) {
            Node current = this.openSet.pop(); // f値最小のノードを取り出す
            findCount++;

            // ゴール到達
            if (current.equals(endNode)) {
                return buildPath(startNode, endNode, true);
            }

            // 1.10.2版: findCount > 450 で強制終了
            if (findCount > MAX_FIND_COUNT) {
                break;
            }

            // 1.10.2版: 終点に最も近いノードをbestNodeとして記録
            if (distSq(current, endNode) < distSq(bestNode, endNode)) {
                bestNode = current;
            }

            // 訪問済みにする (1.10.2版: ppDequeue.visited = true)
            current.closed = true;

            // 隣接ノードを取得 (1.10.2版: findPathOptions相当)
            Node[] neighbors = new Node[32];
            int neighborCount = evaluator.getNeighbors(neighbors, current);

            for (int i = 0; i < neighborCount; i++) {
                Node neighbor = neighbors[i];

                // 1.10.2版: pp.distanceFromOrigin < range のチェック
                if (distSq(startNode, neighbor) > maxRange * maxRange) continue;

                // 1.10.2版: 曼哈頓距離でコスト計算
                float dist      = manhattan(current, neighbor);        // 1.10.2: distanceManhattan
                float costMalus = neighbor.costMalus;
                float newG      = current.g + dist + costMalus;        // 1.10.2: totalPathDistance + cost

                // より良いパスが見つかった場合に更新 (1.10.2版と同じ条件)
                if (!neighbor.inOpenSet() || newG < neighbor.g) {
                    neighbor.cameFrom = current;
                    neighbor.g = newG;
                    neighbor.h = manhattan(neighbor, endNode) + costMalus; // 1.10.2: distanceManhattan(endpp) + costMalus
                    float newF = neighbor.g + neighbor.h;

                    if (neighbor.inOpenSet()) {
                        // 既にopenSetにある場合は距離を更新して再ソート
                        this.openSet.changeCost(neighbor, newF);       // 1.10.2: changeDistance
                    } else {
                        neighbor.f = newF;
                        this.openSet.insert(neighbor);                 // 1.10.2: addPoint
                    }
                }
            }
        }

        // ゴールに到達できなかった場合: bestNode(最も近かった点)までの部分パスを返す
        // 1.10.2版: ppTemp == startpp なら null, そうでなければ部分パス
        if (bestNode == startNode) {
            return null;
        }
        return buildPath(startNode, bestNode, false);
    }

    /** 1.10.2版: createEntityPath相当 - cameFromを辿ってPathを構築 */
    private Path buildPath(Node start, Node end, boolean reached) {
        List<Node> nodes = new ArrayList<>();
        Node current = end;

        // cameFromを辿って逆順にノードを収集
        while (current.cameFrom != null) {
            nodes.add(0, current);
            current = current.cameFrom;
        }
        nodes.add(0, start);

        return new Path(nodes, new BlockPos(end.x, end.y, end.z), reached);
    }

    /** 曼哈頓距離 (1.10.2版: distanceManhattan) */
    private float manhattan(Node a, Node b) {
        return Math.abs(b.x - a.x) + Math.abs(b.y - a.y) + Math.abs(b.z - a.z);
    }

    /** 二乗距離 (1.10.2版: distanceToSquared) */
    private float distSq(Node a, Node b) {
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        return dx*dx + dy*dy + dz*dz;
    }
}