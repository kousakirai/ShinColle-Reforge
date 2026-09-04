package com.lulan.shincolle.network;

import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Central network channel registration for ShinColle.
 * <p>
 * In 1.20.1 Forge, all packets are registered on a single SimpleChannel.
 * This class handles:
 * - Channel creation with protocol versioning
 * - Registration of all 6 packet types
 * - Helper methods for sending packets in various distribution patterns
 * <p>
 * Packet directions:
 * S2C (Server to Client): S2CEntitySyncPacket, S2CSpawnParticlePacket,
 * S2CGUISyncPacket, S2CReactPacket
 * C2S (Client to Server): C2SGUIInputPacket, C2SInputPacket
 */
public class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Reference.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    /**
     * Register all packet types on the channel.
     * Must be called during FMLCommonSetupEvent.
     */
    public static void register() {
        LogHelper.info("ShinColle: Registering network packets...");

        // S2C packets (Server to Client)
        CHANNEL.messageBuilder(S2CEntitySyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CEntitySyncPacket::encode)
                .decoder(S2CEntitySyncPacket::new)
                .consumerMainThread(S2CEntitySyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CSpawnParticle.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSpawnParticle::encode)
                .decoder(S2CSpawnParticle::decode)
                .consumerMainThread(S2CSpawnParticle::handle)
                .add();

        CHANNEL.messageBuilder(S2CGUISyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CGUISyncPacket::encode)
                .decoder(S2CGUISyncPacket::new)
                .consumerMainThread(S2CGUISyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CReactPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CReactPacket::encode)
                .decoder(S2CReactPacket::new)
                .consumerMainThread(S2CReactPacket::handle)
                .add();

        // C2S packets (Client to Server)
        CHANNEL.messageBuilder(C2SGUIInputPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SGUIInputPacket::encode)
                .decoder(C2SGUIInputPacket::new)
                .consumerMainThread(C2SGUIInputPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SInputPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SInputPacket::encode)
                .decoder(C2SInputPacket::new)
                .consumerMainThread(C2SInputPacket::handle)
                .add();

        LogHelper.info("ShinColle: Network packets registered (" + packetId + " packets).");
    }

    // ========== Send Helpers ==========

    /**
     * Send a packet to a specific player.
     */
    public static void sendToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /**
     * Send a packet from the client to the server.
     */
    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    /**
     * Send a packet to all players tracking the given entity.
     */
    public static void sendToAllTracking(Object msg, net.minecraft.world.entity.Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    /**
     * Send a packet to all players tracking the given entity AND the entity itself
     * (if the entity is a player).
     */
    public static void sendToAllTrackingAndSelf(Object msg, net.minecraft.world.entity.Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    /**
     * Send a packet to all players near a specific point.
     */
    public static void sendToNear(Object msg, PacketDistributor.TargetPoint point) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> point), msg);
    }

    /**
     * Send a packet to all connected players.
     */
    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }
}
