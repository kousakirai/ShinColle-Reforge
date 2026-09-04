package com.lulan.shincolle.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * クライアント専用: Mod一覧画面の「設定」ボタンから開けるコンフィグGUIを登録する。
 */
public class ClientConfigScreen {

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parentScreen) -> new ShinColleConfigScreen(parentScreen)
                )
        );
    }
}