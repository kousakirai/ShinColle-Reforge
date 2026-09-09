package com.lulan.shincolle.utility;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialization utility class with static helpers for FriendlyByteBuf.
 * Provides read/write methods for arrays, maps, lists, and nullable strings
 * used by ShinColle network packets.
 */
public class PacketHelper {

    // ========== Array Serialization ==========

    /**
     * Write an int array prefixed by its length.
     */
    public static void writeIntArray(FriendlyByteBuf buf, int[] arr) {
        if (arr == null) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(arr.length);
        for (int v : arr) {
            buf.writeInt(v);
        }
    }

    /**
     * Read an int array prefixed by its length.
     */
    public static int[] readIntArray(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            arr[i] = buf.readInt();
        }
        return arr;
    }

    /**
     * Read a length-prefixed int array with an explicit packet-specific bound.
     */
    public static int[] readIntArray(FriendlyByteBuf buf, int maxLength) {
        int len = buf.readVarInt();
        if (len < 0 || len > maxLength) {
            throw new DecoderException("Int array length " + len + " is outside allowed range 0.." + maxLength);
        }
        if (len > buf.readableBytes() / Integer.BYTES) {
            throw new DecoderException("Int array length " + len
                    + " exceeds readable payload bytes: " + buf.readableBytes());
        }

        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            arr[i] = buf.readInt();
        }
        return arr;
    }

    /**
     * Write a float array prefixed by its length.
     */
    public static void writeFloatArray(FriendlyByteBuf buf, float[] arr) {
        if (arr == null) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(arr.length);
        for (float v : arr) {
            buf.writeFloat(v);
        }
    }

    /**
     * Read a float array prefixed by its length.
     */
    public static float[] readFloatArray(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        float[] arr = new float[len];
        for (int i = 0; i < len; i++) {
            arr[i] = buf.readFloat();
        }
        return arr;
    }

    /**
     * Write a boolean array packed as bytes, prefixed by the boolean count.
     */
    public static void writeBooleanArray(FriendlyByteBuf buf, boolean[] arr) {
        if (arr == null) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(arr.length);
        for (boolean v : arr) {
            buf.writeBoolean(v);
        }
    }

    /**
     * Read a boolean array prefixed by its length.
     */
    public static boolean[] readBooleanArray(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        boolean[] arr = new boolean[len];
        for (int i = 0; i < len; i++) {
            arr[i] = buf.readBoolean();
        }
        return arr;
    }

    /**
     * Write a byte array prefixed by its length.
     */
    public static void writeByteArray(FriendlyByteBuf buf, byte[] arr) {
        if (arr == null) {
            buf.writeVarInt(0);
            buf.writeByteArray(new byte[0]);
            return;
        }
        buf.writeVarInt(arr.length);
        buf.writeByteArray(arr);
    }

    /**
     * Read a byte array prefixed by its length.
     */
    public static byte[] readByteArray(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        return buf.readByteArray(len);
    }

    // ========== Map Serialization ==========

    /**
     * Write a Map of Integer to Integer, prefixed by entry count.
     */
    public static void writeIntMap(FriendlyByteBuf buf, Map<Integer, Integer> map) {
        if (map == null) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(map.size());
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    /**
     * Read a Map of Integer to Integer, prefixed by entry count.
     */
    public static Map<Integer, Integer> readIntMap(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<Integer, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            int key = buf.readInt();
            int value = buf.readInt();
            map.put(key, value);
        }
        return map;
    }

    // ========== List Serialization ==========

    /**
     * Write a List of Integers, prefixed by entry count.
     */
    public static void writeIntList(FriendlyByteBuf buf, List<Integer> list) {
        if (list == null) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(list.size());
        for (int v : list) {
            buf.writeInt(v);
        }
    }

    /**
     * Read a List of Integers, prefixed by entry count.
     */
    public static List<Integer> readIntList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readInt());
        }
        return list;
    }

    /**
     * Write a List of Strings, prefixed by entry count.
     * Each string is written as a UTF string.
     */
    public static void writeStringList(FriendlyByteBuf buf, List<String> list) {
        if (list == null) {
            buf.writeVarInt(0);
            return;
        }
        buf.writeVarInt(list.size());
        for (String s : list) {
            buf.writeUtf(s);
        }
    }

    /**
     * Read a List of Strings, prefixed by entry count.
     */
    public static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf());
        }
        return list;
    }

    // ========== Nullable String ==========

    /**
     * Write a nullable string. Writes a boolean flag first, then the string if
     * non-null.
     */
    public static void writeNullableString(FriendlyByteBuf buf, String s) {
        if (s == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeUtf(s);
        }
    }

    /**
     * Read a nullable string. Reads a boolean flag first, then the string if
     * present.
     */
    public static String readNullableString(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            return buf.readUtf();
        }
        return null;
    }

    /**
     * Read a nullable string with an explicit packet-specific character bound.
     */
    public static String readNullableString(FriendlyByteBuf buf, int maxChars) {
        if (buf.readBoolean()) {
            return buf.readUtf(maxChars);
        }
        return null;
    }
}
