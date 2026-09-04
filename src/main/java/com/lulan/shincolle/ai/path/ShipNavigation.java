package com.lulan.shincolle.ai.path;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ShipNavigation extends FlyingPathNavigation {

    private final ShipPathFinderCore shipPathFinder;
    private final boolean canFly;

    public ShipNavigation(Mob mob, Level level, boolean canFly) {
        super(mob, level);
        this.canFly = canFly;
        this.nodeEvaluator = new ShipNodeEvaluator(canFly);
        this.shipPathFinder    = new ShipPathFinderCore((ShipNodeEvaluator) nodeEvaluator);
        // 1.10.2版: getPathSearchRange相当
        // IShipAttackBaseかどうかで70/48を切り替える場合はここで設定
        this.setMaxVisitedNodesMultiplier(4.0F);
    }

    /** ShipNodeEvaluatorを使ったPathFinderをバニラに登録 */
    @Override
    protected @NotNull PathFinder createPathFinder(int maxVisitedNodes) {
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    /** createPath をオーバーライドしてShipPathFinderCoreに委譲 */
    @Override
    @Nullable
    public Path createPath(BlockPos targetPos, int accuracy) {
        // 探索範囲を構築 (1.10.2版: ChunkCache生成相当)
        int range = (int) getPathSearchRange();
        int i = range + 8;
        BlockPos mobPos = mob.blockPosition();
        PathNavigationRegion region = new PathNavigationRegion(
                level,
                mobPos.offset(-i, -i, -i),
                mobPos.offset( i,  i,  i)
        );

        return this.shipPathFinder.findPath(region, mob, targetPos, range);
    }

    /** 1.10.2版: getPathSearchRange相当 */
    public float getPathSearchRange() {
        // IShipAttackBaseの判定はmobのインスタンスで行う
        return 48F; // 必要に応じてmobの型で70/48を切り替え
    }

    /** pathFollow: isDirectPathBetweenPoints によるショートカット判定を追加 */
    @Override
    protected void followThePath() {
        // バニラのfollowThePath呼び出し
        super.followThePath();

        // 1.10.2版のpathFollow内ショートカット処理
        // 「y高度が異なる点を探し、そこまでの直線移動が可能なら経由点を飛ばす」
        if (path == null || path.isDone()) return;

        Vec3 mobPos = getTempMobPos();
        int pathLength = path.getNodeCount();

        // y座標がmob現在地と異なる最初の点を探す
        int targetIdx = pathLength;
        for (int j = path.getNextNodeIndex(); j < pathLength; j++) {
            Node node = path.getNode(j);
            if ((double) node.y != Math.floor(mobPos.y)) {
                targetIdx = j;
                break;
            }
        }

        // その点から現在インデックスまで逆順に走査し、直線移動可能な点を探してスキップ
        int entitySizeXZ = Mth.ceil(mob.getBbWidth());
        int entitySizeY  = Mth.ceil(mob.getBbHeight());

        for (int j = targetIdx - 1; j >= path.getNextNodeIndex(); j--) {
            Node node = path.getNode(j);
            Vec3 nodeVec = new Vec3(node.x, node.y, node.z);
            if (isDirectPathBetweenPoints(mobPos, nodeVec, entitySizeXZ, entitySizeY, entitySizeXZ)) {
                path.setNextNodeIndex(j);
                break;
            }
        }
    }

    /** 1.10.2版: isDirectPathBetweenPoints の移植
     *  直線経路上の全ブロックをチェックし、障害物がなければtrue */
    private boolean isDirectPathBetweenPoints(Vec3 pos1, Vec3 pos2,
                                              int sizeX, int sizeY, int sizeZ) {
        // 飛行可能なら障害物チェック不要で常にtrue
        if (this.canFly) return true;

        int x1 = Mth.floor(pos1.x);
        int y1 = (int) pos1.y;
        int z1 = Mth.floor(pos1.z);

        double xOff = pos2.x - pos1.x;
        double yOff = pos2.y - pos1.y;
        double zOff = pos2.z - pos1.z;
        double lenSq = xOff*xOff + yOff*yOff + zOff*zOff;

        if (lenSq < 1.0E-8D) return false;

        double inv = 1D / Math.sqrt(lenSq);
        xOff *= inv; yOff *= inv; zOff *= inv;

        sizeX += 2; sizeY += 1; sizeZ += 2;
        if (!isSafeToStandAt(x1, y1, z1, sizeX, sizeY, sizeZ, pos1, xOff, zOff)) return false;
        sizeX -= 2; sizeY -= 1; sizeZ -= 2;

        double unitX = 1D / Math.abs(xOff);
        double unitY = 1D / Math.abs(yOff);
        double unitZ = 1D / Math.abs(zOff);

        double proX = (double) x1 - pos1.x; if (xOff >= 0D) proX++;
        double proY = (double) y1 - pos1.y; if (yOff >= 0D) proY++;
        double proZ = (double) z1 - pos1.z; if (zOff >= 0D) proZ++;

        proX /= xOff; proY /= yOff; proZ /= zOff;

        int dirX = xOff < 0D ? -1 : 1;
        int dirY = yOff < 0D ? -1 : 1;
        int dirZ = zOff < 0D ? -1 : 1;

        int x2 = Mth.floor(pos2.x), y2 = Mth.floor(pos2.y), z2 = Mth.floor(pos2.z);
        int dX = x2 - x1, dY = y2 - y1, dZ = z2 - z1;

        while (dX * dirX > 0 || dY * dirY > 0 || dZ * dirZ > 0) {
            // 進行度が最小の軸を1ステップ進める
            if (proX < proY && proX < proZ) {
                proX += unitX; x1 += dirX; dX = x2 - x1;
            } else if (proY < proZ) {
                proY += unitY; y1 += dirY; dY = y2 - y1;
            } else {
                proZ += unitZ; z1 += dirZ; dZ = z2 - z1;
            }
            if (!isSafeToStandAt(x1, y1, z1, sizeX, sizeY, sizeZ, pos1, xOff, zOff)) return false;
        }

        return true;
    }

    /** 1.10.2版: isSafeToStandAt の移植 */
    private boolean isSafeToStandAt(int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                                    Vec3 origin, double vecX, double vecZ) {
        int x0 = x - sizeX / 2;
        int z0 = z - sizeZ / 2;

        if (!isPositionClear(x0, y, z0, sizeX, sizeY, sizeZ, origin, vecX, vecZ)) return false;

        for (int bx = x0; bx < x0 + sizeX; bx++) {
            for (int bz = z0; bz < z0 + sizeZ; bz++) {
                double dx = bx + 0.5D - origin.x;
                double dz = bz + 0.5D - origin.z;
                if (dx * vecX + dz * vecZ >= 0D) {
                    BlockState below = level.getBlockState(new BlockPos(bx, y - 1, bz));
                    // 下がair(空洞)なら落ちるのでNG
                    if (below.isAir()) return false;
                }
            }
        }
        return true;
    }

    /** 1.10.2版: isPositionClear の移植 */
    private boolean isPositionClear(int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                                    Vec3 origin, double vecX, double vecZ) {
        for (BlockPos pos : BlockPos.betweenClosed(x, y, z,
                x + sizeX - 1,
                y + sizeY - 1,
                z + sizeZ - 1)) {
            double dx = pos.getX() + 0.5D - origin.x;
            double dz = pos.getZ() + 0.5D - origin.z;
            if (dx * vecX + dz * vecZ >= 0D) {
                BlockState state = level.getBlockState(pos);
                // CollisionShapeが空でなければ通れない
                if (!state.getCollisionShape(level, pos).isEmpty()) return false;
            }
        }
        return true;
    }
}