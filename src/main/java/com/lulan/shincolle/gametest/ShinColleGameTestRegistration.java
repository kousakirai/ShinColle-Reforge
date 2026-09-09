package com.lulan.shincolle.gametest;

import com.lulan.shincolle.reference.Reference;

import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ShinColleGameTestRegistration {

    private ShinColleGameTestRegistration() {
    }

    // 2026/04/07：GitHub Copilotによって確認済み
    @SubscribeEvent
    public static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(ShinColleEntityRegistryGameTests.class);
        event.register(ShinColleShipStateGameTests.class);
        event.register(ShinColleCombatGameTests.class);

    }
}
