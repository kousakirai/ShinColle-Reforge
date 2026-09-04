package com.lulan.shincolle.network;

import java.util.function.Supplier;

import com.lulan.shincolle.utility.PacketHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

/**SERVER TO CLIENT : SPAWN PARTICLE PACKET
 * 用於指定位置生成particle
 */
public class S2CSpawnParticle
{
    private Entity entity;
    private int entityID, entityID2;
    private byte packetType, particleType;
    private boolean setAtkTime;
    private int[] valueInt1;
    private float[] valueFloat1;

    private static final class PID
    {
        private static final byte Entity_Animate = 0;
        private static final byte Entity_Pos_Look_Animate = 1;
        private static final byte Entity_Pos_Look = 2;
        private static final byte Entity_Target_Animate = 3;
        private static final byte Entity_Par3 = 4;
        private static final byte Ints_Path = 5;
    }

    public S2CSpawnParticle() {}

    public S2CSpawnParticle(Entity entity, int type, boolean setAtkTime)
    {
        this.entity = entity;
        this.packetType = PID.Entity_Animate;
        this.setAtkTime = setAtkTime;
        this.particleType = (byte) type;

    }

    public S2CSpawnParticle(Entity entity, int type, double posX, double posY, double posZ,
                            double lookX, double lookY, double lookZ, boolean setAtkTime)
    {
        this.entity = entity;
        this.packetType = PID.Entity_Pos_Look_Animate;
        this.setAtkTime = setAtkTime;
        this.particleType = (byte) type;

        this.valueFloat1 = new float[] {
                (float) posX, (float) posY, (float) posZ,
                (float) lookX, (float) lookY, (float) lookZ
        };

    }

    public S2CSpawnParticle(int type, double posX, double posY, double posZ,
                            double lookX, double lookY, double lookZ)
    {
        this.packetType = PID.Entity_Pos_Look;
        this.particleType = (byte) type;

        this.valueFloat1 = new float[] {
                (float) posX, (float) posY, (float) posZ,
                (float) lookX, (float) lookY, (float) lookZ
        };
    }

    public S2CSpawnParticle(Entity entity, Entity target, double par1, double par2, double par3,
                            int type, boolean setAtkTime)
    {
        this.packetType = PID.Entity_Target_Animate;
        this.setAtkTime = setAtkTime;
        this.particleType = (byte) type;
        this.entityID = entity.getId();
        this.entityID2 = target.getId();

        this.valueFloat1 = new float[] { (float) par1, (float) par2, (float) par3 };

    }

    public S2CSpawnParticle(Entity entity, int type, double par1, double par2, double par3)
    {
        this.entity = entity;
        this.packetType = PID.Entity_Par3;
        this.particleType = (byte) type;

        this.valueFloat1 = new float[] { (float) par1, (float) par2, (float) par3 };

    }

    public S2CSpawnParticle(int type, int[] data)
    {
        this.packetType = PID.Ints_Path;
        this.particleType = (byte) type;
        this.valueInt1 = data;
    }

    //=== decode: CLIENT SIDE ===
    public static S2CSpawnParticle decode(FriendlyByteBuf buf)
    {
        S2CSpawnParticle msg = new S2CSpawnParticle();
        msg.packetType = buf.readByte();

        switch (msg.packetType)
        {
            case PID.Entity_Animate ->
            {
                msg.entityID = buf.readInt();
                msg.particleType = buf.readByte();
                msg.setAtkTime = buf.readBoolean();
            }
            case PID.Entity_Pos_Look_Animate ->
            {
                msg.entityID = buf.readInt();
                msg.particleType = buf.readByte();
                msg.setAtkTime = buf.readBoolean();
                msg.valueFloat1 = PacketHelper.readFloatArray(buf);
            }
            case PID.Entity_Pos_Look ->
            {
                msg.particleType = buf.readByte();
                msg.valueFloat1 = PacketHelper.readFloatArray(buf);
            }
            case PID.Entity_Target_Animate ->
            {
                msg.particleType = buf.readByte();
                msg.setAtkTime = buf.readBoolean();
                msg.entityID = buf.readInt();
                msg.entityID2 = buf.readInt();
                msg.valueFloat1 = PacketHelper.readFloatArray(buf);
            }
            case PID.Entity_Par3 ->
            {
                msg.entityID = buf.readInt();
                msg.particleType = buf.readByte();
                msg.valueFloat1 = PacketHelper.readFloatArray(buf);
            }
            case PID.Ints_Path ->
            {
                msg.particleType = buf.readByte();
                msg.valueInt1 = PacketHelper.readIntArray(buf);
            }
        }
        return msg;
    }

    //=== encode: SERVER SIDE ===
    public static void encode(S2CSpawnParticle msg, FriendlyByteBuf buf)
    {
        switch (msg.packetType)
        {
            case PID.Entity_Animate ->
            {
                if (msg.entity == null) return;
                buf.writeByte(0);
                buf.writeInt(msg.entity.getId());
                buf.writeByte(msg.particleType);
                buf.writeBoolean(msg.setAtkTime);
            }
            case PID.Entity_Pos_Look_Animate ->
            {
                if (msg.entity == null) return;
                buf.writeByte(1);
                buf.writeInt(msg.entity.getId());
                buf.writeByte(msg.particleType);
                buf.writeBoolean(msg.setAtkTime);
                PacketHelper.writeFloatArray(buf, msg.valueFloat1);
            }
            case PID.Entity_Pos_Look ->
            {
                buf.writeByte(2);
                buf.writeByte(msg.particleType);
                PacketHelper.writeFloatArray(buf, msg.valueFloat1);
            }
            case PID.Entity_Target_Animate ->
            {
                buf.writeByte(3);
                buf.writeByte(msg.particleType);
                buf.writeBoolean(msg.setAtkTime);
                buf.writeInt(msg.entityID);
                buf.writeInt(msg.entityID2);
                PacketHelper.writeFloatArray(buf, msg.valueFloat1);
            }
            case PID.Entity_Par3 ->
            {
                if (msg.entity == null) return;
                buf.writeByte(4);
                buf.writeInt(msg.entity.getId());
                buf.writeByte(msg.particleType);
                PacketHelper.writeFloatArray(buf, msg.valueFloat1);
            }
            case PID.Ints_Path ->
            {
                if (msg.valueInt1 == null || msg.valueInt1.length <= 0) return;
                buf.writeByte(5);
                buf.writeByte(msg.particleType);
                PacketHelper.writeIntArray(buf, msg.valueInt1);
            }
        }
    }

    //=== handle ===
    public static void handle(S2CSpawnParticle msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Entity ent;
            switch (msg.packetType)
            {
                case PID.Entity_Pos_Look ->
                        ParticleHelper.spawnAttackParticleAt(
                                msg.valueFloat1[0], msg.valueFloat1[1], msg.valueFloat1[2],
                                msg.valueFloat1[3], msg.valueFloat1[4], msg.valueFloat1[5],
                                msg.particleType);
                case PID.Entity_Par3 ->
                {
                    ent = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(msg.entityID) : null;
                    // msg.valueFloat1[0] = h, msg.valueFloat1[1] = type, msg.valueFloat1[2] = 未使用
                    ParticleHelper.spawnAttackParticleAtEntity(ent,
                            msg.valueFloat1[0], msg.valueFloat1[1], msg.valueFloat1[2],
                            msg.particleType);
                }
                case PID.Entity_Animate ->
                {
                    ent = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(msg.entityID) : null;
                    ParticleHelper.spawnAttackParticle(ent, msg.particleType, msg.setAtkTime);
                }
                case PID.Entity_Target_Animate ->
                {
                    ent = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(msg.entityID) : null;
                    Entity target = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(msg.entityID2) : null;
                    ParticleHelper.spawnAttackParticleAtEntity(ent, target,
                            msg.valueFloat1[0], msg.valueFloat1[1], msg.valueFloat1[2],
                            msg.particleType, msg.setAtkTime);
                }
                case PID.Ints_Path ->
                {
                    int len, cid;
                    byte parType;

                    if (msg.particleType == 0)
                    {
                        len = (msg.valueInt1.length - 1) / 3;
                        cid = msg.valueInt1[0];

                        for (int i = 0; i < len; i++)
                        {
                            parType = (i == cid) ? (byte) 32 : (byte) 33;
                            ParticleHelper.spawnAttackParticleAt(
                                    msg.valueInt1[i * 3 + 1] + 0.5D,
                                    msg.valueInt1[i * 3 + 2] + 0.5D,
                                    msg.valueInt1[i * 3 + 3] + 0.5D,
                                    0D, 0D, 0D, parType);
                        }
                    }
                    else
                    {
                        len = (msg.valueInt1.length - 1) / 3;
                        cid = msg.valueInt1[0];

                        for (int i = 0; i < len; i++)
                        {
                            parType = (i == cid) ? (byte) 16 : (byte) 29;
                            ParticleHelper.spawnAttackParticleAt(
                                    msg.valueInt1[i * 3 + 1] + 0.5D,
                                    msg.valueInt1[i * 3 + 2] + 0.5D,
                                    msg.valueInt1[i * 3 + 3] + 0.5D,
                                    0D, 0D, 0D, parType);
                        }
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
