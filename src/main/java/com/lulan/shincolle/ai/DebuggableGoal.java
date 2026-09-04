package com.lulan.shincolle.ai;

import com.lulan.shincolle.handler.ConfigHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.transformer.Config;

import static com.lulan.shincolle.ShinColle.LOGGER;

public abstract class DebuggableGoal extends Goal {
    protected final Mob mob;

    protected DebuggableGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public final void start() {
        if (ConfigHandler.debugMode()) {
            LOGGER.info("[{}] START: {}", mob.getId(), getClass().getSimpleName());
        }
        onStart();
    }

    @Override
    public final void stop() {
        if (ConfigHandler.debugMode()) {
            LOGGER.info("[{}] STOP: {}", mob.getId(), getClass().getSimpleName());
        }
        onStop();
    }

    protected abstract void onStart();
    protected abstract void onStop();
}