package com.lulan.shincolle.client;

import com.lulan.shincolle.handler.ConfigHandler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ShinColleConfigScreen extends Screen {

    private final Screen parent;

    protected ShinColleConfigScreen(Screen parent) {
        super(Component.literal("ShinColle Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
                        Component.literal("Show AI Goals: " + ConfigHandler.DEBUG_SHOW_AI_GOALS),
                        btn -> {
                            boolean newVal = !ConfigHandler.DEBUG_SHOW_AI_GOALS;
                            ConfigHandler.DEBUG_SHOW_AI_GOALS = newVal;
                            btn.setMessage(Component.literal("Show AI Goals: " + newVal));
                        })
                .bounds(this.width / 2 - 100, 40, 200, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("完了"),
                        btn -> onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}