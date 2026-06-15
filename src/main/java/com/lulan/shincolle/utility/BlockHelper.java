package com.lulan.shincolle.utility;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.server.ServerDataManager;
import com.lulan.shincolle.tileentity.TileEntityLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Random;

/**
 * Block utility helper for safe block finding, liquid checking,
 * light block placement, ray tracing, and chunk operations.
 * <p>
 * Ported from 1.10.2 BlockHelper.
 */
public class BlockHelper {

    private static final Random rand = new Random();

    public BlockHelper() {
    }

    /**
     * Find safe block around target block within 5x5 area.
     *
     * @return safe position {x,y,z} or null if no safe position found
     */
    public static int[] getSafeBlockWithin5x5(Level level, int x, int y, int z) {
        return getSafeBlockWithinRange(level, x, y, z, 3, 4, 3);
    }

    /**
     * Get safe block within NxN block area.
     * <p>
     * rangeXZ: 0=1x1, 1=3x3, 2=5x5, 3=7x7 ...
     *
     * @return safe {x,y,z} or null (no safe block)
     */
    public static int[] getSafeBlockWithinRange(Level level, int x, int y, int z, int ranX, int ranY, int ranZ) {
        int[] pos = new int[]{x, y, z};

        // find x2,y2,z2 = 0, 1, -1, 2, -2, 3, -3, ...
        // find block priority: Y > Z > X
        int xlimit = ranX * 2;
        int ylimit = ranY * 2;
        int zlimit = ranZ * 2;
        int x2 = 0;
        int y2;
        int z2;
        int x3, y3, z3;
        int addx;
        int addy;
        int addz;

        for (int ix = 0; ix <= xlimit; ix++) {
            addx = (ix & 1) == 0 ? ix * -1 : ix;
            x2 += addx;
            z2 = 0;

            for (int iz = 0; iz <= zlimit; iz++) {
                addz = (iz & 1) == 0 ? iz * -1 : iz;
                z2 += addz;
                y2 = 0;

                for (int iy = 0; iy <= ylimit; iy++) {
                    addy = (iy & 1) == 0 ? iy * -1 : iy;
                    y2 += addy;

                    x3 = pos[0] + x2;
                    y3 = pos[1] + y2;
                    z3 = pos[2] + z2;

                    if (checkBlockSafe(level, x3, y3, z3)) {
                        int decY = 0;

                        while (decY <= Math.abs(y2)) {
                            if (checkBlockCanStandAt(level.getBlockState(new BlockPos(x3, y3 - decY - 1, z3)))) {
                                pos[0] = x3;
                                pos[1] = y3 - decY;
                                pos[2] = z3;
                                return pos;
                            }
                            decY++;
                        }
                    }
                }
            }
        }

        LogHelper.debug("DEBUG: find block fail");
        return null;
    }

    /**
     * Check block is safe (not solid block) for entity at position.
     */
    public static boolean checkBlockSafe(Entity target) {
        if (target == null)
            return false;
        return checkBlockSafe(target.level(), Mth.floor(target.getX()), (int) target.getY(),
                Mth.floor(target.getZ()));
    }

    /**
     * Check block is safe (not solid block), WITH passable checking.
     */
    public static boolean checkBlockSafe(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);

        return checkBlockSafe(state)
                || state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Check block is safe (not solid block), NO passable checking.
     */
    public static boolean checkBlockSafe(BlockState state) {
        return state == null || state.isAir() || checkBlockIsLiquid(state)
                || state.getBlock() == ModBlocks.WAYPOINT.get();
    }

    /**
     * Check block is liquid (not air or solid block).
     */
    public static boolean checkBlockIsLiquid(BlockState state) {
        if (state != null) {
            FluidState fluid = state.getFluidState();
            return !fluid.isEmpty();
        }
        return false;
    }

    /**
     * Check block is liquid and check liquid level.
     *
     * @param level 0 = source block only
     */
    public static boolean checkBlockIsLiquid(BlockState state, int level) {
        if (state != null) {
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                if (level == 0) {
                    return fluid.isSource();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Check nearby block has at least one liquid block.
     */
    public static boolean checkBlockNearbyIsLiquid(Level level, int x, int y, int z, int range) {
        for (int ix = -range; ix <= range; ix++) {
            for (int iy = -range; iy <= range; iy++) {
                for (int iz = -range; iz <= range; iz++) {
                    if (checkBlockIsLiquid(level.getBlockState(new BlockPos(x + ix, y + iy, z + iz)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check block nearby is the same block, only sample 3 columns.
     */
    public static boolean checkBlockNearbyIsSameBlock(Level level, Block target, int x, int y, int z, int rangeXZ,
                                                      int rangeY) {
        int[][] posXZ = new int[][]{
                {rand.nextInt(rangeXZ) - rangeXZ / 2, rand.nextInt(rangeXZ) - rangeXZ / 2},
                {rand.nextInt(rangeXZ) - rangeXZ / 2, rand.nextInt(rangeXZ) - rangeXZ / 2},
                {rand.nextInt(rangeXZ) - rangeXZ / 2, rand.nextInt(rangeXZ) - rangeXZ / 2}};

        for (int[] pos : posXZ) {
            for (int y2 = 0; y2 < rangeY; y2++) {
                if (level.getBlockState(new BlockPos(x + pos[0], y - y2, z + pos[1])).getBlock() != target) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check ship entity can stand at the block.
     */
    @SuppressWarnings("deprecation")
    public static boolean checkBlockCanStandAt(BlockState state) {
        if (state != null) {
            if (!state.isAir() && !state.is(Blocks.FIRE)) {
                return checkBlockIsLiquid(state) || state.blocksMotion();
            }
        }
        return false;
    }

    /**
     * Find random position with block check for AIRPLANE.
     * <p>
     * mode 0: random quadrant around target
     * mode 1: behind target (flank)
     * mode 2: straight ahead (follow motion)
     */
    public static double[] findRandomPosition(Entity host, Entity target, double minDist, double randDist, int mode) {
        double[] newPos = new double[]{0D, 0D, 0D};

        for (int i = 0; i < 25; i++) {
            switch (mode) {
                case 0: // random quadrant around target
                    newPos[0] = rand.nextDouble() * randDist + minDist;
                    newPos[1] = rand.nextDouble() * randDist * 0.5D + target.getY() + target.getBbHeight() * 0.75D;
                    newPos[2] = rand.nextDouble() * randDist + minDist;

                    switch (rand.nextInt(4)) {
                        case 0:
                            newPos[0] = target.getX() + newPos[0];
                            newPos[2] = target.getZ() - newPos[2];
                            break;
                        case 1:
                            newPos[0] = target.getX() - newPos[0];
                            newPos[2] = target.getZ() + newPos[2];
                            break;
                        case 2:
                            newPos[0] = target.getX() - newPos[0];
                            newPos[2] = target.getZ() - newPos[2];
                            break;
                        case 3:
                            newPos[0] = target.getX() + newPos[0];
                            newPos[2] = target.getZ() + newPos[2];
                            break;
                    }
                    break;
                case 1: // flank: move to behind target
                    newPos[0] = rand.nextDouble() * randDist + minDist;
                    newPos[1] = rand.nextDouble() * randDist * 0.5D + target.getY() + target.getBbHeight() * 0.75D;
                    newPos[2] = rand.nextDouble() * randDist + minDist;

                    double dx = host.getX() - target.getX();
                    double dz = host.getZ() - target.getZ();

                    if (dx > 0) {
                        newPos[0] = target.getX() - newPos[0];
                    } else {
                        newPos[0] = target.getX() + newPos[0];
                    }


                    newPos[2] = target.getZ() - newPos[2];

                    break;
                case 2: // straight ahead
                    newPos[0] = rand.nextDouble() * randDist + minDist;
                    newPos[1] = rand.nextDouble() * randDist * 0.5D + target.getY() + target.getBbHeight() * 0.75D;
                    newPos[2] = rand.nextDouble() * randDist + minDist;

                    if (host.getDeltaMovement().x < 0) {
                        newPos[0] = target.getX() - newPos[0];
                    } else {
                        newPos[0] = target.getX() + newPos[0];
                    }

                    if (host.getDeltaMovement().z < 0) {
                        newPos[2] = target.getZ() - newPos[2];
                    } else {
                        newPos[2] = target.getZ() + newPos[2];
                    }
                    break;
            }

            if (checkBlockSafe(host.level(), (int) newPos[0], (int) newPos[1], (int) newPos[2])) {
                return newPos;
            }
        }

        // fail, return target position
        newPos[0] = target.getX();
        newPos[1] = target.getY() + 2D;
        newPos[2] = target.getZ();
        return newPos;
    }

    /**
     * Find random safe pos by random angle on target for SHIP ATTACK.
     */
    public static BlockPos findRandomSafePos(Entity target) {
        if (target == null)
            return BlockPos.ZERO;

        BlockPos pos = target.blockPosition();
        BlockPos newpos;

        int loops = 20;
        float[] posoffset;

        while (loops > 0) {
            loops--;

            posoffset = CalcHelper.rotateXZByAxis(6F, 0F, rand.nextFloat() * 360F * Values.N.DIV_PI_180, 1F);
            newpos = pos.offset((int) posoffset[1], 0, (int) posoffset[0]);

            if (checkBlockSafe(target)) {
                return newpos;
            }
        }

        return pos;
    }

    /**
     * Find top safe pos on target for SHIP ATTACK.
     */
    public static BlockPos findTopSafePos(Entity target) {
        if (target == null)
            return BlockPos.ZERO;

        BlockPos pos = target.blockPosition();
        BlockPos newpos;

        int loops = (int) target.getBbHeight() + 7;

        while (loops > 0) {
            loops--;

            newpos = pos.offset(0, loops, 0);

            if (checkBlockSafe(target)) {
                return newpos;
            }
        }

        return pos;
    }

    /**
     * Ray trace for block, include liquid block.
     * Used for naval gun targeting on water surface.
     */
    @OnlyIn(Dist.CLIENT)
    public static BlockHitResult getPlayerMouseOverBlockOnWater(double dist, float duringTicks) {
        Entity viewer = getClientViewer();
        if (viewer == null)
            return null;

        return getMouseOverBlock(viewer, dist, duringTicks, true, false);
    }

    /**
     * Ray trace for block, no liquid block (through water).
     */
    @OnlyIn(Dist.CLIENT)
    public static BlockHitResult getPlayerMouseOverBlockThroughWater(double dist, float duringTicks) {
        Entity viewer = getClientViewer();
        if (viewer == null)
            return null;

        return getMouseOverBlock(viewer, dist, duringTicks, false, true);
    }

    /**
     * Get client viewer entity, accounting for mount view override.
     */
    @OnlyIn(Dist.CLIENT)
    private static Entity getClientViewer() {
        Entity viewer = ClientRuntimeHelper.getClientCameraEntity();

        if (viewer == null) {
            viewer = ClientRuntimeHelper.getClientPlayer();
        }
        if (viewer == null)
            return null;

        // change viewer if on ship's mount
        if (viewer.getVehicle() instanceof BasicEntityMount mount) {
            Entity ship = mount.getHostEntity();
            if (ship != null) {
                viewer = ship;
            }
        }

        return viewer;
    }

    /**
     * Ray trace for block only.
     *
     * @param stopOnLiquid      true to stop ray on liquid blocks
     * @param passThroughLiquid true to ignore liquid blocks
     */
    @OnlyIn(Dist.CLIENT)
    public static BlockHitResult getMouseOverBlock(Entity viewer, double dist, float duringTicks,
                                                   boolean stopOnLiquid, boolean passThroughLiquid) {
        if (viewer == null)
            return null;

        // [PORT] 1.10.2 -> 1.20.1: keep client partial tick usage while avoiding
        // direct net.minecraft.client references in common utility code.
        duringTicks = ClientRuntimeHelper.getClientFrameTime(duringTicks);

        Vec3 eyePos = viewer.getEyePosition(duringTicks);
        Vec3 lookVec = viewer.getViewVector(duringTicks);
        Vec3 endPos = eyePos.add(lookVec.x * dist, lookVec.y * dist, lookVec.z * dist);

        ClipContext.Fluid fluidMode;
        if (stopOnLiquid) {
            fluidMode = ClipContext.Fluid.ANY;
        } else if (passThroughLiquid) {
            fluidMode = ClipContext.Fluid.NONE;
        } else {
            fluidMode = ClipContext.Fluid.NONE;
        }

        return viewer.level().clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, fluidMode, viewer));
    }

    /**
     * Place light block near position.
     */
    public static void placeLightBlock(Level level, BlockPos pos) {
        for (int i = -1; i <= 1; i++) {
            for (int j = 1; j <= 2; j++) {
                for (int k = -1; k <= 1; k++) {
                    BlockPos pos2 = pos.offset(i, j, k);
                    Block block = level.getBlockState(pos2).getBlock();

                    if (block == Blocks.AIR) {
                        level.setBlock(pos2, ModBlocks.LIGHT_AIR.get().defaultBlockState(), 1);
                    } else if (block == Blocks.WATER) {
                        level.setBlock(pos2, ModBlocks.LIGHT_LIQUID.get().defaultBlockState(), 1);
                    } else {
                        continue;
                    }

                    // init TileEntityLightBlock
                    BlockEntity tile = level.getBlockEntity(pos2);
                    if (tile instanceof TileEntityLightBlock lightTile) {
                        lightTile.setTicksRemaining(200);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Reset nearby light block lifespan.
     */
    public static void updateNearbyLightBlock(Level level, BlockPos pos) {
        for (int i = -1; i <= 1; i++) {
            for (int j = 1; j <= 2; j++) {
                for (int k = -1; k <= 1; k++) {
                    BlockEntity tile = level.getBlockEntity(pos.offset(i, j, k));

                    if (tile instanceof TileEntityLightBlock lightTile) {
                        lightTile.setTicksRemaining(200);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Get chunks within range.
     *
     * @param mode 1=1 chunk, 2=3x3 chunks
     */
    public static HashSet<ChunkPos> getChunksWithinRange(Level level, int x, int z, int mode) {
        HashSet<ChunkPos> chunks = new HashSet<>();

        switch (mode) {
            case 1: // 1 chunk
                chunks.add(new ChunkPos(x, z));
                break;
            case 2: // 3x3 chunks
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        chunks.add(new ChunkPos(x + i, z + j));
                    }
                }
                break;
        }

        return chunks;
    }

    /**
     * Get topmost water height at position.
     */
    public static int getToppestWaterHeight(Level level, int x, int y, int z) {
        int cy = y + 1;
        BlockState b = level.getBlockState(new BlockPos(x, y, z));

        if (checkBlockIsLiquid(b)) {
            while (cy < level.getMaxBuildHeight()) {
                b = level.getBlockState(new BlockPos(x, cy, z));

                if (checkBlockIsLiquid(b)) {
                    cy++;
                } else {
                    break;
                }
            }
        }

        return cy;
    }

    /**
     * Check is openable door (trapdoor, wooden door, fence gate).
     */
    public static boolean isOpenableDoor(BlockState state) {
        if (state != null) {
            if (state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES)) {
                return true;
            }

            return state.is(BlockTags.WOODEN_DOORS);
        }
        return false;
    }

    /**
     * Check tile owner: true = target entity is the owner of the tile.
     */
    public static boolean checkTileOwner(Entity target, BlockEntity tile) {
        if (tile != null && target != null) {
            int uid = getEntityPlayerUID(target);
            int uidTile;

            if (tile instanceof IShipOwner owner) {
                uidTile = owner.getPlayerUID();
                return uid == uidTile;
            }
        }
        return false;
    }

    /**
     * Get player UID from entity.
     * For Player: from CapaTeitoku capability.
     * For IShipOwner: from getPlayerUID().
     */
    private static int getEntityPlayerUID(Entity entity) {
        if (entity instanceof Player player) {
            var capa = ServerDataManager.getTeitokuCapability(player);

            return capa.getPlayerUID();
        }
        if (entity instanceof IShipOwner owner) {
            return owner.getPlayerUID();
        }
        return -1;
    }

    /**
     * Get nearby liquid block, return null if no liquid.
     *
     * @param checkHostPos check block under host
     * @param getRandom    try random positions first
     * @param rad          search radius (rectangle)
     * @param depth        check liquid depth, disable if depth <= 0
     */
    public static BlockPos getNearbyLiquid(Entity host, boolean checkHostPos, boolean getRandom, int rad, int depth) {
        if (host == null)
            return null;

        BlockPos pos;
        BlockState state;

        for (int y = 1; y > -3; y--) {
            // try random positions first
            if (getRandom) {
                int maxtry = (int) (0.5F * rad * rad);

                for (int j = 0; j < maxtry; j++) {
                    int x = rand.nextInt(rad + 1) * 2 - rad;
                    int z = rand.nextInt(rad + 1) * 2 - rad;

                    pos = new BlockPos(Mth.floor(host.getX()) + x, (int) host.getY() + y,
                            Mth.floor(host.getZ()) + z);
                    state = host.level().getBlockState(pos);

                    if (checkBlockIsLiquid(state, 0)) {
                        if (depth <= 0) {
                            return pos;
                        } else if (checkLiquidDepth(host.level(), pos, depth)) {
                            return pos;
                        }
                    }
                }
            }

            // systematic scan
            for (int x = -rad; x < rad + 1; x++) {
                for (int z = -rad; z < rad + 1; z++) {
                    if (x == 0 && z == 0)
                        continue;

                    pos = new BlockPos(Mth.floor(host.getX()) + x, (int) host.getY() + y,
                            Mth.floor(host.getZ()) + z);
                    state = host.level().getBlockState(pos);

                    if (checkBlockIsLiquid(state, 0)) {
                        if (depth <= 0) {
                            return pos;
                        } else if (checkLiquidDepth(host.level(), pos, depth)) {
                            return pos;
                        }
                    }
                }
            }
        }

        // check host position
        if (checkHostPos) {
            for (int y = 1; y > -2; y--) {
                pos = new BlockPos(Mth.floor(host.getX()), (int) host.getY() + y, Mth.floor(host.getZ()));
                state = host.level().getBlockState(pos);

                if (checkBlockIsLiquid(state, 0)) {
                    if (depth <= 0) {
                        return pos;
                    } else if (checkLiquidDepth(host.level(), pos, depth)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Place searchlight (light air block) at position if the position is air.
     * Used by ship entities to illuminate nearby area at night.
     */
    public static void placeSearchlight(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            level.setBlockAndUpdate(pos, ModBlocks.LIGHT_AIR.get().defaultBlockState());
        }
    }

    /**
     * Check if liquid extends to required depth below pos.
     */
    private static boolean checkLiquidDepth(Level level, BlockPos pos, int depth) {
        for (int dy = -1; dy > -depth; dy--) {
            BlockState state = level.getBlockState(pos.offset(0, dy, 0));
            if (!checkBlockIsLiquid(state, 0)) {
                return false;
            }
        }
        return true;
    }
}
