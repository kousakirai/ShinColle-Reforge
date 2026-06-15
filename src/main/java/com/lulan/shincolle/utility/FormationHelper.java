package com.lulan.shincolle.utility;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Helper for formation buff calculations and formation position calculation.
 * <p>
 * Formation types:
 * 1: Line Ahead (単縦陣)
 * 2: Double Line (複縦陣)
 * 3: Diamond (輪形陣)
 * 4: Echelon (梯形陣)
 * 5: Line Abreast (単横陣)
 * <p>
 * Ported from 1.10.2 FormationHelper.
 */
public class FormationHelper {

    /**
     * Get formation buff value by formation ID and slot.
     *
     * @param formatID   formation type (1=Line Ahead, 2=Double Line, etc.)
     * @param formatSlot slot position in formation (0-5)
     * @return float array of formation buff values indexed by ID.Attrs
     */
    public static float[] getFormationBuffValue(int formatID, int formatSlot) {
        int key = formatID * 10 + formatSlot;

        if (Values.FormationAttrs.containsKey(key)) {
            return Values.FormationAttrs.get(key).clone();
        }

        return AttrsAdv.getResetFormationValue();
    }

    // ========== Formation Direction ==========

    /**
     * Calculate formation face direction from movement vector.
     *
     * @return boolean[2]: [0]=along X axis, [1]=face positive direction
     */
    public static boolean[] getFormationDirection(double toX, double toZ, double fromX, double fromZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        boolean[] face = new boolean[2];

        // along X if |dx| > |dz|
        face[0] = Math.abs(dx) > Math.abs(dz);

        if (face[0]) {
            face[1] = dx >= 0;
        } else {
            face[1] = dz >= 0;
        }

        return face;
    }

    // ========== Guard Position Calculation ==========

    /**
     * Return guarding entity position in formation.
     * <p>
     * 1. Set guard target as flagship position
     * 2. Calc formation position by guard target posXZ and oldXZ
     */
    public static double[] getFormationGuardingPos(IShipAttackBase host, Entity target,
                                                   double oldX, double oldZ) {
        int formatID = host.getStateMinor(ID.M.FormatType);
        int formatPos = host.getStateMinor(ID.M.FormatPos);
        double[] pos = new double[]{target.getX(), target.getY(), target.getZ()};

        // no formation, return target position
        if (formatID <= 0)
            return pos;

        // check error position
        if (formatPos < 0 || formatPos > 5)
            formatPos = 0;

        // calc formation position by formation type
        boolean[] faceXP = getFormationDirection(target.getX(), target.getZ(), oldX, oldZ);

        int[] tempPos = calcFormationPos(formatID, formatPos, pos, faceXP);

        if (tempPos != null) {
            // check block is safe
            Level level = target.level();
            tempPos = BlockHelper.getSafeBlockWithin5x5(level, tempPos[0], tempPos[1], tempPos[2]);

            if (tempPos != null) {
                pos[0] = tempPos[0];
                pos[1] = tempPos[1];
                pos[2] = tempPos[2];
            }
        }

        return pos;
    }

    /**
     * Calculate formation position for a ship based on formation type.
     *
     * @param formatID    formation type
     * @param formatPos   slot position (0=flagship, 1-5)
     * @param flagshipPos flagship position {x, y, z}
     * @param faceXP      direction: [0]=along X, [1]=face positive
     * @return new position {x, y, z}
     */
    public static int[] calcFormationPos(int formatID, int formatPos, double[] flagshipPos, boolean[] faceXP) {
        int[] newPos = new int[]{
                Mth.floor(flagshipPos[0]),
                (int) (flagshipPos[1] + 0.5D),
                Mth.floor(flagshipPos[2])
        };

        // host is flagship
        if (formatPos == 0) {
            return newPos;
        }

        switch (formatID) {
            case 1: // line ahead
                for (int i = 0; i < formatPos; i++) {
                    newPos = nextLineAheadPos(faceXP[0], faceXP[1], newPos[0], newPos[1], newPos[2]);
                }
                break;
            case 4: // echelon
                for (int i = 0; i < formatPos; i++) {
                    newPos = nextEchelonPos(faceXP[1], newPos[0], newPos[1], newPos[2]);
                }
                break;
            case 2: // double line
                newPos = nextDoubleLinePos(faceXP[0], faceXP[1], formatPos, newPos[0], newPos[1], newPos[2]);
                break;
            case 3: // diamond
                newPos = nextDiamondPos(faceXP[0], faceXP[1], formatPos, newPos[0], newPos[1], newPos[2]);
                break;
            case 5: // line abreast
                newPos = nextLineAbreastPos(faceXP[0], formatPos, newPos[0], newPos[1], newPos[2]);
                break;
        }

        return newPos;
    }

    // ========== Formation Position Functions ==========

    /**
     * Calculate next LINE AHEAD position.
     *
     * <pre>
     *        0
     *        1
     *        2
     *        3
     *        4
     *        5
     * </pre>
     */
    public static int[] nextLineAheadPos(boolean alongX, boolean faceP, int x, int y, int z) {
        int[] pos = new int[]{x, y, z};

        if (alongX) {
            if (faceP) {
                pos[0] -= 3;
            } else {
                pos[0] += 3;
            }
        } else {
            if (faceP) {
                pos[2] -= 3;
            } else {
                pos[2] += 3;
            }
        }

        return pos;
    }

    /**
     * Calculate next DOUBLE LINE position.
     *
     * <pre>
     *      2  3
     *      0  1
     *      4  5
     * </pre>
     */
    public static int[] nextDoubleLinePos(boolean alongX, boolean faceP, int formatPos, int x, int y, int z) {
        int[] pos = new int[]{x, y, z};

        switch (formatPos) {
            case 1:
                if (alongX) {
                    pos[2] += 3;
                } else {
                    pos[0] += 3;
                }
                break;
            case 2:
                if (alongX) {
                    pos[0] += faceP ? 3 : -3;
                } else {
                    pos[2] += faceP ? 3 : -3;
                }
                break;
            case 3:
                if (alongX) {
                    pos[0] += faceP ? 3 : -3;
                    pos[2] += 3;
                } else {
                    pos[0] += 3;
                    pos[2] += faceP ? 3 : -3;
                }
                break;
            case 4:
                if (alongX) {
                    pos[0] += faceP ? -3 : 3;
                } else {
                    pos[2] += faceP ? -3 : 3;
                }
                break;
            case 5:
                if (alongX) {
                    pos[0] += faceP ? -3 : 3;
                    pos[2] += 3;
                } else {
                    pos[0] += 3;
                    pos[2] += faceP ? -3 : 3;
                }
                break;
        }

        return pos;
    }

    /**
     * Calculate DIAMOND position.
     *
     * <pre>
     *         1
     *      2  5  3
     *         0
     *         4
     * </pre>
     */
    public static int[] nextDiamondPos(boolean alongX, boolean faceP, int formatPos, int x, int y, int z) {
        int[] pos = new int[]{x, y, z};

        switch (formatPos) {
            case 1:
                if (alongX) {
                    pos[0] += faceP ? 5 : -5;
                } else {
                    pos[2] += faceP ? 5 : -5;
                }
                break;
            case 2:
                if (alongX) {
                    pos[0] += faceP ? 1 : -1;
                    pos[2] -= 4;
                } else {
                    pos[0] -= 4;
                    pos[2] += faceP ? 1 : -1;
                }
                break;
            case 3:
                if (alongX) {
                    pos[0] += faceP ? 1 : -1;
                    pos[2] += 4;
                } else {
                    pos[0] += 4;
                    pos[2] += faceP ? 1 : -1;
                }
                break;
            case 4:
                if (alongX) {
                    pos[0] += faceP ? -3 : 3;
                } else {
                    pos[2] += faceP ? -3 : 3;
                }
                break;
            case 5:
                if (alongX) {
                    pos[0] += faceP ? 2 : -2;
                } else {
                    pos[2] += faceP ? 2 : -2;
                }
                break;
        }

        return pos;
    }

    /**
     * Calculate next ECHELON position.
     *
     * <pre>
     *                   0
     *              1
     *            2
     *          3
     *        4
     *      5
     * </pre>
     */
    public static int[] nextEchelonPos(boolean faceP, int x, int y, int z) {
        int[] pos = new int[]{x, y, z};

        if (faceP) {
            pos[0] -= 2;
            pos[2] -= 2;
        } else {
            pos[0] += 2;
            pos[2] += 2;
        }

        return pos;
    }

    /**
     * Calculate LINE ABREAST position.
     *
     * <pre>
     *  4  2  0  1  3  5
     * </pre>
     */
    public static int[] nextLineAbreastPos(boolean alongX, int formatPos, int x, int y, int z) {
        int[] pos = new int[]{x, y, z};

        switch (formatPos) {
            case 1:
                if (alongX) {
                    pos[2] += 3;
                } else {
                    pos[0] += 3;
                }
                break;
            case 2:
                if (alongX) {
                    pos[2] -= 3;
                } else {
                    pos[0] -= 3;
                }
                break;
            case 3:
                if (alongX) {
                    pos[2] += 6;
                } else {
                    pos[0] += 6;
                }
                break;
            case 4:
                if (alongX) {
                    pos[2] -= 6;
                } else {
                    pos[0] -= 6;
                }
                break;
            case 5:
                if (alongX) {
                    pos[2] += 9;
                } else {
                    pos[0] += 9;
                }
                break;
        }

        return pos;
    }

    // ========== Ship Guard Commands ==========

    /**
     * Set ship guard to a block position, and check if guard position is the same.
     * GuardType = 1: guard a block
     * <p>
     * If same position and not forceSet, cancel guard mode.
     * Otherwise apply guard mode with block guard.
     */
    public static void applyShipGuard(BasicEntityShip ship, int x, int y, int z, boolean forceSet) {
        if (ship == null)
            return;

        int gx = ship.getStateMinor(ID.M.GuardX);
        int gy = ship.getStateMinor(ID.M.GuardY);
        int gz = ship.getStateMinor(ID.M.GuardZ);

        // clear attack target
        ship.setTarget(null);
        ship.setEntityTarget(null);

        // same guard position, cancel guard mode
        if (!forceSet && gx == x && gy == y && gz == z) {
            ship.setGuardedPos(-1, -1, -1, 0, 0);
            ship.setGuardedEntity(null);
            ship.setStateFlag(ID.F.CanFollow, true);
        }
        // apply guard mode
        else {
            ship.setEntitySit(false);
            ship.setGuardedEntity(null);
            ship.setGuardedPos(x, y, z, 0, 1);
            ship.setStateFlag(ID.F.CanFollow, false);

            if (!ship.getStateFlag(ID.F.NoFuel)) {
                ship.applyEmotesReaction(5);

                Entity mount = ship.getVehicle();
                if (mount instanceof BasicEntityMount bm) {
                    bm.getNavigation().moveTo(x, y, z, 1D);
                    bm.getLookControl().setLookAt(x, y, z, 30F, 40F);
                } else {
                    ship.getNavigation().moveTo(x, y, z, 1D);
                    ship.getLookControl().setLookAt(x, y, z, 30F, 40F);
                }
            }
        }
    }

    /**
     * Set ship guard to an entity.
     * GuardType = 2: guard an entity
     * <p>
     * If same entity target, cancel guard mode.
     * Otherwise apply entity guard mode.
     */
    public static void applyShipGuardEntity(BasicEntityShip ship, Entity guarded) {
        if (ship == null || guarded == null)
            return;

        Entity current = ship.getGuardedEntity();

        // same guard target, cancel guard
        if (current != null && current.getId() == guarded.getId()) {
            ship.setGuardedPos(-1, -1, -1, 0, 0);
            ship.setGuardedEntity(null);
            ship.setStateFlag(ID.F.CanFollow, true);
        }
        // apply guard
        else {
            ship.setEntitySit(false);
            ship.setGuardedPos(-1, -1, -1, 0, 2);
            ship.setGuardedEntity(guarded);
            ship.setStateFlag(ID.F.CanFollow, false);

            if (!ship.getStateFlag(ID.F.NoFuel)) {
                ship.applyEmotesReaction(5);

                Entity mount = ship.getVehicle();
                if (mount instanceof BasicEntityMount bm) {
                    bm.getNavigation().moveTo(guarded, 1D);
                } else {
                    ship.getNavigation().moveTo(guarded, 1D);
                }
            }
        }
    }
}
