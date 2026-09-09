package com.lulan.shincolle.client;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.network.C2SInputPacket;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only bridge for the canonical mount movement bit mask. */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MountInputHandler {
    private static int movementCooldown;

    private MountInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (movementCooldown > 0) {
            movementCooldown--;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !(player.getVehicle() instanceof BasicEntityMount mount)) {
            return;
        }

        int keys = collectMovementKeys(minecraft, mount);
        if (keys == 0 || movementCooldown > 0) {
            return;
        }

        // Apply the same input locally for prediction and on the server through
        // the bounded, owner-authorized packet path.
        mount.setMountKeyInput(keys);
        ModNetworking.sendToServer(new C2SInputPacket(C2SInputPacket.MountMove, keys));
        movementCooldown = 2;
    }

    private static int collectMovementKeys(Minecraft minecraft, BasicEntityMount mount) {
        int keys = 0;
        if (minecraft.options.keyUp.isDown()) {
            keys |= 1;
        }
        if (minecraft.options.keyDown.isDown()) {
            keys |= 2;
        }
        if (minecraft.options.keyLeft.isDown()) {
            keys |= 4;
        }
        if (minecraft.options.keyRight.isDown()) {
            keys |= 8;
        }
        if (minecraft.options.keyJump.isDown()
                && (mount.onGround() || EntityHelper.checkEntityIsInLiquid(mount))) {
            keys |= 16;
        }
        return keys;
    }
}
