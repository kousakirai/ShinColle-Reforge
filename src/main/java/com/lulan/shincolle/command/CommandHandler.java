package com.lulan.shincolle.command;

import com.lulan.shincolle.utility.LogHelper;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Event handler that registers all ShinColle commands during the
 * RegisterCommandsEvent.
 * Must be registered on MinecraftForge.EVENT_BUS.
 */
public class CommandHandler {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LogHelper.info("ShinColle: Registering commands...");

        ShipCmdChangeOwner.register(event.getDispatcher());
        ShipCmdEmotes.register(event.getDispatcher());
        ShipCmdGetShip.register(event.getDispatcher());
        ShipCmdKill.register(event.getDispatcher());
        ShipCmdShipAttrs.register(event.getDispatcher());
        ShipCmdClearDrop.register(event.getDispatcher());
        ShipCmdShipInfo.register(event.getDispatcher());
        ShipCmdStopAI.register(event.getDispatcher());
        ShipCmdUpdateOwnerUID.register(event.getDispatcher());
        ShipCmdDebugAI.register(event.getDispatcher());

        LogHelper.info("ShinColle: Commands registered.");
    }
}
