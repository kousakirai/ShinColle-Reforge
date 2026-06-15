package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.path.ShipMoveControl;

public interface IShipNavigator {

    ShipMoveControl getShipMoveControl();

    boolean canFly();

    boolean isJumping();

    float getMoveSpeed();

    float getJumpSpeed();
}
