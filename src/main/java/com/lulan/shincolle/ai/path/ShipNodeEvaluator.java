package com.lulan.shincolle.ai.path;

import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumSet;

public class ShipNodeEvaluator extends NodeEvaluator {

    /** 飛行可能かどうか(IShipNavigator.canFly()相当) */
    private final boolean canFly;

    private PathNavigationRegion level;

    public ShipNodeEvaluator(boolean canFly) {
        this.canFly = canFly;
    }

    @Override
    public void prepare(PathNavigationRegion region, Mob mob) {
        super.prepare(region, mob);
        // バニラのprepareでlevel/mob/entityWidth/entityHeight/entityDepthがセットされる
        // canOpenDoorsやcanPassDoorsはここでセットしておく
        this.level = region;
        this.canOpenDoors = true;
        this.canPassDoors = true;
        this.canFloat = true;
    }

    @Override
    public void done() {
        super.done();
        this.level = null;
    }
    @Override
    public Node getStart() {
        // 1.10.2版: startpp = entity.boundingBox.minY + 0.5D のfloor
        int x = Mth.floor(mob.getX());
        int y = Mth.floor(mob.getBoundingBox().minY + 0.5D);
        int z = Mth.floor(mob.getZ());
        return getNode(x, y, z);
    }

    @Override
    public Target getGoal(double x, double y, double z) {
        // 1.10.2版: endpp = floor(x - width*0.5), floor(y), floor(z - width*0.5)
        return getTargetFromNode(getNode(
                Mth.floor(x - mob.getBbWidth() * 0.5F),
                Mth.floor(y),
                Mth.floor(z - mob.getBbWidth() * 0.5F)
        ));
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter pLevel, int pX, int pY, int pZ, Mob pMob) {
       return null;
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter pLevel, int pX, int pY, int pZ) {
        return getShipPathType(pLevel, pX, pY, pZ);
    }

    public BlockPathTypes getShipPathType(BlockGetter getter, int x, int y, int z) {

        int sizeX = Mth.floor(mob.getBbWidth() + 1F);
        int sizeY = Mth.floor(mob.getBbHeight() + 1F);
        int sizeZ = sizeX; // 正方形を想定

        EnumSet<BlockPathTypes> pathSet = EnumSet.noneOf(BlockPathTypes.class);
        boolean pathInLiquid = false;
        int doorCount = 0;

        // 原点が液体かチェック (1.10.2版: pathInLiquid判定)
        FluidState fluidAtOrigin = getter.getFluidState(new BlockPos(x, y, z));
        if (!fluidAtOrigin.isEmpty()) {
            pathInLiquid = true;
        }

        // entityのサイズ分ブロックをチェック
        for (int x1 = x; x1 < x + sizeX; x1++) {
            for (int y1 = y + sizeY - 1; y1 >= y; y1--) { // 上から下へ (FENCE判定のため)
                for (int z1 = z; z1 < z + sizeZ; z1++) {

                    BlockPos pos = new BlockPos(x1, y1, z1);
                    BlockState state = getter.getBlockState(pos);
                    Block block = state.getBlock();

                    // 空気はスキップ
                    if (state.isAir()) {
                        pathSet.add(BlockPathTypes.OPEN);
                        continue;
                    }

                    // FENCE / WALL (BlockTags使用)
                    if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) ||
                            state.is(BlockTags.FENCE_GATES) && !state.getValue(FenceGateBlock.OPEN)) {
                        // entityと同じ高さならFENCE(飛び越え候補), それ以上ならBLOCKED
                        if (y1 == y) return BlockPathTypes.FENCE;
                        return BlockPathTypes.BLOCKED;
                    }

                    // FenceGate (開いている場合はOPEN)
                    if (block instanceof FenceGateBlock) {
                        if (state.getValue(FenceGateBlock.OPEN)) {
                            pathSet.add(BlockPathTypes.OPEN);
                        } else {
                            return BlockPathTypes.DOOR_WOOD_CLOSED; // OPENABLE相当
                        }
                        continue;
                    }

                    // ドア
                    if (block instanceof DoorBlock doorBlock) {
                        doorCount++;
                        // canOpenByHand = false のドアは手で開けられない(鉄ドア相当) → BLOCKED
                        // doorCountが4を超える場合もBLOCKED (1.10.2版と同じ、ドアは2格高なので2枚分=4)
                        boolean isIronLike = !doorBlock.type().canOpenByHand();
                        if (isIronLike || doorCount > 4) return BlockPathTypes.BLOCKED;
                        // 木製ドア相当: 開いていればOPEN、閉じていればOPENABLE相当
                        boolean isOpen = state.getValue(DoorBlock.OPEN);
                        pathSet.add(isOpen ? BlockPathTypes.OPEN : BlockPathTypes.DOOR_WOOD_CLOSED);
                        continue;
                    }

                    // 液体 (水/溶岩/その他MOD液体)
                    FluidState fluid = state.getFluidState();
                    if (!fluid.isEmpty()) {
                        pathSet.add(fluid.is(FluidTags.LAVA) ? BlockPathTypes.LAVA : BlockPathTypes.WATER);
                        continue;
                    }

                    // レール (通過可能な例外)
                    if (block instanceof BaseRailBlock) {
                        pathSet.add(BlockPathTypes.OPEN);
                        continue;
                    }

                    // カスタムブロック(BlockWaypoint相当)があればここで追加

                    // スイレンの葉 (lily pad) はBLOCKED
                    if (block == Blocks.LILY_PAD) {
                        return BlockPathTypes.BLOCKED;
                    }

                    // その他: CollisionShapeが空でなければBLOCKED
                    VoxelShape shape = state.getCollisionShape(getter, pos);
                    if (!shape.isEmpty()) {
                        return BlockPathTypes.BLOCKED;
                    }

                    pathSet.add(BlockPathTypes.OPEN);
                }
            }
        }

        // 液体中のpathはWATER/LAVAを優先
        if (pathInLiquid) {
            return pathSet.contains(BlockPathTypes.LAVA) ? BlockPathTypes.LAVA : BlockPathTypes.WATER;
        }

        return BlockPathTypes.OPEN;
    }

    @Override
    public int getNeighbors(Node[] output, Node current) {
        int count = 0;

        // 1.10.2版: pathYOffset = 頭上がOPENかFLUIDならstepHeightのfloor
        BlockPathTypes above = getShipPathType(this.level, current.x, current.y + 1, current.z);
        int pathYOffset = (above == BlockPathTypes.OPEN || above == BlockPathTypes.WATER)
                ? Mth.floor(Math.max(1F, mob.maxUpStep()))
                : 0;

        // 基本6方向 + 斜め4方向(コーナーカット防止付き)
        Node n  = getSafeNode(current.x,     current.y,     current.z - 1, pathYOffset);
        Node s  = getSafeNode(current.x,     current.y,     current.z + 1, pathYOffset);
        Node e  = getSafeNode(current.x + 1, current.y,     current.z,     pathYOffset);
        Node w  = getSafeNode(current.x - 1, current.y,     current.z,     pathYOffset);
        Node d  = getSafeNode(current.x,     current.y - 1, current.z,     pathYOffset);
        Node u  = pathYOffset > 0
                ? getSafeNode(current.x, current.y + 1, current.z, pathYOffset)
                : null;

        // コーナーカット防止フラグ (1.10.2版のfn/fs/fe/fw相当)
        boolean fn = n == null || n.costMalus != 0F;
        boolean fs = s == null || s.costMalus != 0F;
        boolean fe = e == null || e.costMalus != 0F;
        boolean fw = w == null || w.costMalus != 0F;

        Node nw = (fn && fw) ? getSafeNode(current.x - 1, current.y, current.z - 1, pathYOffset) : null;
        Node ne = (fn && fe) ? getSafeNode(current.x + 1, current.y, current.z - 1, pathYOffset) : null;
        Node sw = (fs && fw) ? getSafeNode(current.x - 1, current.y, current.z + 1, pathYOffset) : null;
        Node se = (fs && fe) ? getSafeNode(current.x + 1, current.y, current.z + 1, pathYOffset) : null;

        // nullでないノードをoutputに追加
        for (Node node : new Node[]{d, u, nw, ne, sw, se, n, s, e, w}) {
            if (node != null && !node.closed) {
                output[count++] = node;
            }
        }

        return count;
    }

    /** getSafePoint の移植 */
    private Node getSafeNode(int x, int y, int z, int pathYOffset) {
        BlockPathTypes type = getShipPathType(this.level, x, y, z);

        // FLUID / OPENABLE はそのまま通過可能
        if (type == BlockPathTypes.WATER || type == BlockPathTypes.LAVA ||
                type == BlockPathTypes.DOOR_WOOD_CLOSED) {
            return getNode(x, y, z);
        }

        if (type == BlockPathTypes.OPEN) {
            // 飛べるなら落下チェック不要
            if (canFly) return getNode(x, y, z);

            // 飛べない場合: 下に落下先を探す (最大64格, 1.10.2版と同じ)
            Node node = getNode(x, y, z);
            int fallY = y;
            int tries = 0;

            while (fallY > mob.level().getMinBuildHeight()) {
                BlockPathTypes below = getShipPathType(this.level, x, fallY - 1, z);

                if (below == BlockPathTypes.WATER || below == BlockPathTypes.LAVA) {
                    return getNode(x, fallY - 1, z); // 液体に着水
                }
                if (below != BlockPathTypes.OPEN) {
                    return getNode(x, fallY, z); // 着地点
                }
                if (++tries > 64) return null; // 落下しすぎ
                fallY--;
            }
            return null;
        }

        // BLOCKED の場合: pathYOffsetがあれば1格上を試みる (1.10.2版のstepHeight乗り越え)
        if (type == BlockPathTypes.BLOCKED || type == BlockPathTypes.FENCE) {
            if (pathYOffset > 1 || (pathYOffset > 0 && type != BlockPathTypes.FENCE)) {
                return getSafeNode(x, y + 1, z, pathYOffset - 1);
            }
            return null;
        }

        return null;
    }
}
