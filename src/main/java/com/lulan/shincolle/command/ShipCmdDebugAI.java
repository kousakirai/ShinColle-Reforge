package com.lulan.shincolle.command;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.LogHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /shincolle debug ... : AI Goalデバッグ表示や関連トグルをまとめたコマンド。
 * 開発・デバッグ用途のため、権限レベル2（OP）を要求する。
 */
public class ShipCmdDebugAI {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("shincolle")
                        .then(Commands.literal("debug")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("aigoals")
                                        .executes(ShipCmdDebugAI::toggleShowAiGoals))
                                .then(Commands.literal("lifecycle")
                                        .executes(ShipCmdDebugAI::toggleLogLifecycle))
                                .then(Commands.literal("status")
                                        .executes(ShipCmdDebugAI::showStatus))
                        )
        );
    }

    private static int toggleShowAiGoals(CommandContext<CommandSourceStack> ctx) {
        ConfigHandler.DEBUG_SHOW_AI_GOALS = !ConfigHandler.DEBUG_SHOW_AI_GOALS;
        boolean newVal = ConfigHandler.DEBUG_SHOW_AI_GOALS;

        LogHelper.info("ShinColle: AI Goal debug display -> " + newVal);
        ctx.getSource().sendSuccess(() ->
                Component.literal("[ShinColle] AI Goal表示: " + newVal), false);
        return 1;
    }

    private static int toggleLogLifecycle(CommandContext<CommandSourceStack> ctx) {
        ConfigHandler.DEBUG_LOG_GOAL_LIFECYCLE = !ConfigHandler.DEBUG_LOG_GOAL_LIFECYCLE;
        boolean newVal = ConfigHandler.DEBUG_LOG_GOAL_LIFECYCLE;

        LogHelper.info("ShinColle: Goal lifecycle logging -> " + newVal);
        ctx.getSource().sendSuccess(() ->
                Component.literal("[ShinColle] Goalライフサイクルログ: " + newVal), false);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "[ShinColle] aigoals=%b, lifecycle=%b",
                ConfigHandler.DEBUG_SHOW_AI_GOALS,
                ConfigHandler.DEBUG_LOG_GOAL_LIFECYCLE
        )), false);
        return 1;
    }
}