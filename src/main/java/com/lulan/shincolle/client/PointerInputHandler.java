package com.lulan.shincolle.client;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.PointerItem;
import com.lulan.shincolle.network.C2SGUIInputPacket;
import com.lulan.shincolle.network.C2SInputPacket;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only pointer input bridge.
 * <p>
 * Restores legacy key behavior:
 * - Sprint + hotbar 1-9: switch selected team (without changing selected hotbar slot)
 * - Player list key (TAB by default) on main-hand pointer: toggle caress mode (0-2 <-> 3-5)
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PointerInputHandler {

    private static int mountInputCooldown;

    private PointerInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }

        handleMountMovement(player, mc);

        ItemStack pointerInUse = getPointerInUse(player);
        if (pointerInUse.isEmpty()) {
            return;
        }

        boolean sprintDown = mc.options.keySprint.isDown();

        if (sprintDown) {
            handleSprintTeamSwitch(player, mc);
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() == ModItems.POINTER.get()) {
            while (mc.options.keyPlayerList.consumeClick()) {
                int mode = PointerItem.toggleCaressMode(PointerItem.getMode(mainHand));
                PointerItem.setMode(mainHand, mode);

                ModNetworking.sendToServer(new C2SGUIInputPacket(
                        C2SGUIInputPacket.SyncPlayerItem,
                        new int[]{player.getId(), 0, mode}));
            }
        }
    }

    private static void handleMountMovement(LocalPlayer player, Minecraft mc) {
        if (mountInputCooldown > 0) {
            mountInputCooldown--;
        }

        if (!(player.getVehicle() instanceof BasicEntityMount mount)) {
            mountInputCooldown = 0;
            return;
        }

        int keys = 0;
        if (mc.options.keyUp.isDown()) {
            keys |= 1;
        }
        if (mc.options.keyDown.isDown()) {
            keys |= 2;
        }
        if (mc.options.keyLeft.isDown()) {
            keys |= 4;
        }
        if (mc.options.keyRight.isDown()) {
            keys |= 8;
        }
        if (mc.options.keyJump.isDown()
                && (mount.onGround() || EntityHelper.checkEntityIsInLiquid(mount))) {
            keys |= 16;
        }

        if (keys != 0) {
            // Keep client prediction in lockstep with the server's ten-tick lease.
            mount.setMountKeyInput(keys);
            if (mountInputCooldown == 0) {
                ModNetworking.sendToServer(new C2SInputPacket(C2SInputPacket.MountMove, keys));
                mountInputCooldown = 2;
            }
        }
    }

    private static void handleSprintTeamSwitch(LocalPlayer player, Minecraft mc) {
        int originalSlot = player.getInventory().selected;
        boolean consumed = false;

        for (int i = 0; i < mc.options.keyHotbarSlots.length; i++) {
            while (mc.options.keyHotbarSlots[i].consumeClick()) {
                consumed = true;
                if (i < CapaTeitoku.TEAM_NUM) {
                    ModNetworking.sendToServer(new C2SGUIInputPacket(
                            C2SGUIInputPacket.SetSelect,
                            new int[]{player.getId(), 0, i}));
                }
            }
        }

        if (consumed) {
            // [PORT] 1.10.2 -> 1.20.1: keep pointer in hand while using sprint+hotbar team
            // shortcuts.
            player.getInventory().selected = originalSlot;
        }
    }

    private static ItemStack getPointerInUse(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && main.getItem() == ModItems.POINTER.get()) {
            return main;
        }

        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.getItem() == ModItems.POINTER.get()) {
            return off;
        }

        return ItemStack.EMPTY;
    }
}
