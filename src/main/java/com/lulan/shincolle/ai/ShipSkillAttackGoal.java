package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.reference.ID;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Skill attack goal - delegates to entity's updateSkillAttack method.
 * Active when ID.S.Phase != 0.
 * Ported from EntityAIShipSkillAttack (setMutexBits: 15)
 */
public class ShipSkillAttackGoal extends Goal {

    private final IShipAttackBase host;
    private final Mob entity;

    public ShipSkillAttackGoal(IShipAttackBase host) {
        this.host = host;
        this.entity = (Mob) host;
        // mutexBits 15 = all flags
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.host.getIsSitting() || this.host.getStateMinor(ID.M.CraneState) > 0) {
            // reset phase
            if (this.host.getStateEmotion(ID.S.Phase) > 0) {
                this.host.setStateEmotion(ID.S.Phase, 0, true);
            }
            return false;
        }

        if (this.host.getIsRiding()) {
            if (this.entity.getVehicle() instanceof BasicEntityMount) {
                return false;
            }
        }

        return this.host.getStateEmotion(ID.S.Phase) > 0;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void tick() {
        this.host.updateSkillAttack(this.entity.getTarget());
    }
}
