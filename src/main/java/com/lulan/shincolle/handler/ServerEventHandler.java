package com.lulan.shincolle.handler;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.network.S2CGUISyncPacket;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.server.ServerDataManager;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.TeamHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Server-side event handler for ShinColle.
 * <p>
 * Manages the ServerDataManager lifecycle:
 * - Initializes when overworld loads
 * - Saves and resets on server stop
 * - Ticks every server tick
 * - Updates player UIDs on login
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    /**
     * Initialize ServerDataManager when the overworld level loads.
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() == Level.OVERWORLD) {
                ServerDataManager.init(serverLevel);
            }
        }
    }

    /**
     * Save and reset ServerDataManager when the server stops.
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ServerDataManager.reset();
    }

    /**
     * Tick the ServerDataManager every server tick.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerDataManager.onServerTick();
        }
    }

    /**
     * Player periodic server tick:
     * - updates ring ownership/active state
     * - keeps player UID initialized
     * - runs hostile mob/boss spawn ticks
     * - decrements team cooldown
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Player player = event.player;
        if (player == null || player.level().isClientSide()) {
            return;
        }

        CapaTeitoku capa = player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);
        // [PORT] 1.10.2 -> 1.20.1: keep ring possession tracking used by ring-gated
        // systems (spawn, movement buffs).
        if (capa == null) {
            return;
        }
        if ((player.tickCount & 15) == 0) {
            updateRingState(player, capa);
        }

        // every 32 ticks: ensure UID exists and run slower periodic logic
        if (player.tickCount > 0 && (player.tickCount & 31) == 0) {
            if (capa.getPlayerUID() < 100) {
                ServerDataManager.updatePlayerID(player);
                if (capa.getPlayerUID() < 100) {
                    LogHelper.debug("player tick: failed to initialize player UID, skip spawn tick");
                    return;
                }
            }

            if ((player.tickCount & 127) == 0) {
                EntityHelper.spawnMobShip(player, capa);
            }

            // Canonical timing restored team references after entities had time to
            // publish their UID cache, then refreshed periodically for late loads.
            if (player.tickCount == 64 || (player.tickCount & 255) == 0) {
                syncRelinkedTeamRuntimeState(player, capa);
            }
        }
        EntityHelper.spawnBossShip(player, capa);
        int teamCooldown = capa.getTeamCooldown();
        if (teamCooldown > 0) {
            capa.setTeamCooldown(teamCooldown - 1);
        }
    }

    /**
     * Assign or update player UID on login.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        updatePlayerCacheOnServer(event.getEntity());
    }

    /**
     * Update player cache on respawn.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        updatePlayerCacheOnServer(event.getEntity());
    }

    /**
     * Update player cache on dimension change.
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        updatePlayerCacheOnServer(event.getEntity());
    }

    /**
     * Save player data on logout.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            Player player = event.getEntity();
            CapaTeitoku capa = ServerDataManager.getTeitokuCapability(player);
            if (capa != null && capa.getPlayerUID() > 0) {
                updatePlayerCacheOnServer(player);
                LogHelper.info("player logged out: " + player.getGameProfile().getName()
                        + " uid=" + capa.getPlayerUID());
            }
        }
    }

    /**
     * Handle entity death side effects (kill count, morale and nearby emote
     * reaction).
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity deadEntity = event.getEntity();

        if (deadEntity instanceof BasicEntityShip ship) {
            // [PORT] 1.10.2 -> 1.20.1: keep ship cache update on ship death.
            ship.updateShipCacheDataWithoutNewID();
        }

        Entity killerEntity = event.getSource().getDirectEntity();
        if (killerEntity == null) {
            killerEntity = event.getSource().getEntity();
        }

        if (killerEntity instanceof BasicEntityShip killerShip) {
            // [PORT] 1.10.2 -> 1.20.1: restore direct-kill reward contract.
            killerShip.addKills();
            killerShip.addMorale(2);
        } else if (killerEntity instanceof IShipAttackBase attackBase
                && attackBase.getHostEntity() instanceof BasicEntityShip hostShip) {
            // [PORT] 1.10.2 -> 1.20.1: summon/projectile kills count for host ship.
            hostShip.addKills();
        }

        if (deadEntity instanceof BasicEntityShip deadShip) {
            // [PORT] 1.10.2 -> 1.20.1: restore nearby shock reaction when ship dies.
            deadShip.applyParticleEmotion(8);
            EntityHelper.applyShipEmotesAOE(deadShip.level(), deadShip.getX(), deadShip.getY(), deadShip.getZ(), 16D,
                    6);
        } else if (deadEntity instanceof BasicEntityShipHostile) {
            // [PORT] 1.10.2 -> 1.20.1: keep hostile death AOE reaction path.
            EntityHelper.applyShipEmotesAOEHostile(
                    deadEntity.level(), deadEntity.getX(), deadEntity.getY(), deadEntity.getZ(), 48D, 6);
        }
    }

    private static void updatePlayerCacheOnServer(Player player) {
        if (player != null && !player.level().isClientSide()) {
            ServerDataManager.updatePlayerID(player);
            CapaTeitoku capa = ServerDataManager.getTeitokuCapability(player);
            if (capa != null) {
                boolean changed = capa.clearTeamRuntimeState();
                if (ServerDataManager.isInitialized()) {
                    changed |= TeamHelper.relinkAllTeamRuntimeState(player, capa);
                }
                if (changed && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    ModNetworking.sendToPlayer(S2CGUISyncPacket.syncShipsAll(capa), serverPlayer);
                }
            }
        }
    }

    private static void syncRelinkedTeamRuntimeState(Player player, CapaTeitoku capa) {
        if (ServerDataManager.isInitialized()
                && TeamHelper.relinkAllTeamRuntimeState(player, capa)
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(S2CGUISyncPacket.syncShipsAll(capa), serverPlayer);
        }
    }

    private static void updateRingState(Player player, CapaTeitoku capa) {
        ItemStack ring = findRingStack(player);
        boolean hasRing = !ring.isEmpty();

        if (capa.hasRing() && !hasRing) {
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            capa.setRingFlying(false);
            capa.setRingActive(false);
        }

        capa.setHasRing(hasRing);

        if (!ring.isEmpty() && ring.hasTag()) {
            assert ring.getTag() != null;
            capa.setRingActive(ring.getTag().getBoolean("isActive"));
        }
    }

    private static ItemStack findRingStack(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == ModItems.MARRIAGE_RING.get()) {
                return stack;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == ModItems.MARRIAGE_RING.get()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
