package com.lulan.shincolle.handler;

import com.lulan.shincolle.reference.unitclass.Attrs;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration handler for ShinColle mod (1.20.1 port).
 * <p>
 * Uses ForgeConfigSpec with builder pattern. All original config properties
 * from the 1.10.2 version are preserved and organized by category:
 * general, ship, sound, world, intermod.
 * <p>
 * Cached static fields (limitShipAttrs, modernLimit, etc.) are maintained
 * for backward compatibility with code that accesses them directly.
 * Call {@link #syncConfig()} after config load/reload to update cached values.
 */
public class ConfigHandler {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    // ConfigHandler.java 内、他のstaticキャッシュフィールドと並べて配置
    /** デバッグ用: AI Goal名を頭上に表示するか（ワールド跨ぎで保存しない） */
    public static boolean DEBUG_SHOW_AI_GOALS = false;

    /** デバッグ用: Goalのstart/stopをログ出力するか（ワールド跨ぎで保存しない） */
    public static boolean DEBUG_LOG_GOAL_LIFECYCLE = false;
    // ========== Cached values for backward compatibility ==========
    /**
     * Max ship level (not configurable)
     */
    public static final int maxLevel = 150;
    /**
     * Max attrs limit, -1 = no limit, index by ID.Attrs
     */
    public static double[] limitShipAttrs = new double[Attrs.AttrsLength];
    /**
     * Modern bonus point limit per attribute
     */
    public static int modernLimit = 3;
    /**
     * Ship attributes scale: HP, firepower, armor, attack speed, move speed, range
     */
    public static double[] scaleShip = new double[]{1D, 1D, 1D, 1D, 1D, 1D};
    /**
     * Small boss base attributes: HP, firepower, armor, attack speed, move speed,
     * range
     */
    public static double[] scaleBossSmall = new double[]{1600D, 120D, 0.5D, 1.6D, 0.38D, 18D};
    /**
     * Large boss base attributes: HP, firepower, armor, attack speed, move speed,
     * range
     */
    public static double[] scaleBossLarge = new double[]{3200D, 240D, 0.75D, 2D, 0.35D, 22D};
    /**
     * Small mob base attributes: HP, firepower, armor, attack speed, move speed,
     * range
     */
    public static double[] scaleMobSmall = new double[]{250D, 25D, 0.15D, 0.7D, 0.45D, 12D};
    /**
     * Large mob base attributes: HP, firepower, armor, attack speed, move speed,
     * range
     */
    public static double[] scaleMobLarge = new double[]{500D, 50D, 0.30D, 0.9D, 0.4D, 15D};
    /**
     * Ship held item scaling: scale, offsetX, offsetY, offsetZ
     */
    public static double[] scaleHeldItem = new double[]{2.5D, 0D, 0D, 0D};
    /**
     * Ammo consumption per ship type: DD CL CA CAV CLT CVL CV BB BBV SS AP
     */
    public static int[] consumeAmmoShip = new int[]{1, 2, 2, 2, 2, 3, 3, 4, 4, 1, 1};
    /**
     * Grudge consumption idle per ship type: DD CL CA CAV CLT CVL CV BB BBV SS AP
     */
    public static int[] consumeGrudgeShip = new int[]{5, 7, 8, 9, 8, 11, 12, 15, 14, 4, 3};
    /**
     * Grudge consumption per action: LAtk, HAtk, LAir, HAir, moving
     */
    public static int[] consumeGrudgeAction = new int[]{4, 8, 6, 12, 3};
    /**
     * Grudge consumption per task: cook, fish, mine, craft
     */
    public static int[] consumeGrudgeTask = new int[]{3, 30, 300, 2};
    /**
     * Base attack speed: melee, Latk, Hatk, CV, Air
     */
    public static int[] baseAttackSpeed = new int[]{40, 80, 120, 100, 100};
    /**
     * Fixed attack delay: melee, Latk, Hatk, CV, Air
     */
    public static int[] fixedAttackDelay = new int[]{0, 20, 50, 35, 35};
    /**
     * Exp gain: melee, LAtk, HAtk, LAir, HAir, move/b, pick
     */
    public static int[] expGain = new int[]{2, 4, 12, 8, 24, 1, 2};
    /**
     * Exp gain by task: cook, fish, mine, craft
     */
    public static int[] expGainTask = new int[]{2, 20, 10, 1};
    /**
     * Mob spawn: Max, Prob, GroupNum, MinPS, MaxPS
     */
    public static int[] mobSpawn = new int[]{50, 10, 1, 1, 1};
    /**
     * Ring ability related to married number: breath, fly, dig, fog, immune fire
     */
    public static int[] ringAbility = new int[]{0, 6, 30, 20, 12};
    /**
     * Fishing time: base, random (ticks)
     */
    public static int[] tickFishing = new int[]{400, 600};
    /**
     * Mining time: base, random (ticks)
     */
    public static int[] tickMining = new int[]{100, 200};
    /**
     * Liquid drum: base, enchant rate
     */
    public static int[] drumLiquid = new int[]{40, 5};
    /**
     * Infinite liquid pump: min water depth, min lava depth
     */
    public static int[] infLiquid = new int[]{12, 8};
    /**
     * Ship teleport: cooldown (ticks), distance (blocks^2)
     */
    public static int[] shipTeleport = new int[]{200, 256};
    /**
     * Task enable: cook, fish, mine, craft
     */
    public static boolean[] enableTask = new boolean[]{true, true, true, true};
    /**
     * Small shipyard: max fuel storage, build speed, fuel magnification
     */
    public static double[] tileShipyardSmall = new double[]{460800D, 48D, 1D};

    /**
     * Large shipyard: max fuel storage, build speed, fuel magnification
     */
    public static double[] tileShipyardLarge = new double[]{1382400D, 48D, 1D};

    /**
     * Volcano Core: max fuel storage, fuel consume speed, fuel value per grudge
     * item
     */
    public static double[] tileVolCore = new double[]{9600D, 16D, 240D};

    /**
     * Crane: internal fluid tank capacity (mB), internal energy capacity (EU)
     */
    public static int[] tileCrane = new int[]{2048000, 160000000};

    /**
     * HUD position: x, y (0.5 = middle of window)
     */
    public static double[] posHUD = new double[]{0.5D, 0.6D};

    /**
     * PolyGravel replaced block: stone, gravel, sand, dirt
     */
    public static boolean[] polyGravelBaseBlock = new boolean[]{true, true, false, false};

    // ========== ForgeConfigSpec ==========

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();

        // Initialize default limit values
        for (int i = 0; i < limitShipAttrs.length; i++) {
            limitShipAttrs[i] = -1D;
        }
    }

    // ========== Static initializer ==========

    /**
     * Synchronize cached static fields from ForgeConfigSpec values.
     * Called after config load/reload via ModConfigEvent.
     */
    public static void syncConfig() {
        modernLimit = COMMON.attrsLimitModernization.get();

        // Sync double arrays
        syncDoubleArray(limitShipAttrs, COMMON.limitShipAttrsConfig.get());
        syncDoubleArray(scaleShip, COMMON.scaleShipConfig.get());
        syncDoubleArray(scaleBossSmall, COMMON.scaleBossSmallConfig.get());
        syncDoubleArray(scaleBossLarge, COMMON.scaleBossLargeConfig.get());
        syncDoubleArray(scaleMobSmall, COMMON.scaleMobSmallConfig.get());
        syncDoubleArray(scaleMobLarge, COMMON.scaleMobLargeConfig.get());
        syncDoubleArray(scaleHeldItem, COMMON.scaleHeldItemConfig.get());
        syncDoubleArray(tileShipyardSmall, COMMON.tileShipyardSmallConfig.get());
        syncDoubleArray(tileShipyardLarge, COMMON.tileShipyardLargeConfig.get());
        syncDoubleArray(tileVolCore, COMMON.tileVolCoreConfig.get());
        syncDoubleArray(posHUD, COMMON.posHUDConfig.get());

        // Sync int arrays
        syncIntArray(consumeAmmoShip, COMMON.consumeAmmoShipConfig.get());
        syncIntArray(consumeGrudgeShip, COMMON.consumeGrudgeShipConfig.get());
        syncIntArray(consumeGrudgeAction, COMMON.consumeGrudgeActionConfig.get());
        syncIntArray(consumeGrudgeTask, COMMON.consumeGrudgeTaskConfig.get());
        syncIntArray(baseAttackSpeed, COMMON.baseAttackSpeedConfig.get());
        syncIntArray(fixedAttackDelay, COMMON.fixedAttackDelayConfig.get());
        syncIntArray(expGain, COMMON.expGainConfig.get());
        syncIntArray(expGainTask, COMMON.expGainTaskConfig.get());
        syncIntArray(mobSpawn, COMMON.mobSpawnConfig.get());
        syncIntArray(ringAbility, COMMON.ringAbilityConfig.get());
        syncIntArray(tickFishing, COMMON.tickFishingConfig.get());
        syncIntArray(tickMining, COMMON.tickMiningConfig.get());
        syncIntArray(drumLiquid, COMMON.drumLiquidConfig.get());
        syncIntArray(infLiquid, COMMON.infLiquidConfig.get());
        syncIntArray(shipTeleport, COMMON.shipTeleportConfig.get());
        syncIntArray(tileCrane, COMMON.tileCraneConfig.get());

        // Sync boolean arrays
        syncBooleanArray(enableTask, COMMON.enableTaskConfig.get());
        syncBooleanArray(polyGravelBaseBlock, COMMON.polyGravelBaseBlockConfig.get());
    }

    // ========== Config sync ==========

    private static void syncDoubleArray(double[] target, List<? extends Double> source) {
        for (int i = 0; i < Math.min(target.length, source.size()); i++) {
            target[i] = source.get(i);
        }
    }

    // ========== Array sync helpers ==========

    private static void syncIntArray(int[] target, List<? extends Integer> source) {
        for (int i = 0; i < Math.min(target.length, source.size()); i++) {
            target[i] = source.get(i);
        }
    }

    private static void syncBooleanArray(boolean[] target, List<? extends Boolean> source) {
        for (int i = 0; i < Math.min(target.length, source.size()); i++) {
            target[i] = source.get(i);
        }
    }

    public static boolean debugMode() {
        return COMMON.debugMode.get();
    }

    // ========== Static convenience accessors for backward compatibility ==========

    public static boolean easyMode() {
        return COMMON.easyMode.get();
    }

    public static boolean friendlyFire() {
        return COMMON.friendlyFire.get();
    }

    public static boolean alwaysShowTeamCircle() {
        return COMMON.alwaysShowTeamCircle.get();
    }

    public static boolean showNameTag() {
        return COMMON.showNameTag.get();
    }

    public static boolean polymetalAsMn() {
        return COMMON.polymetalAsMn.get();
    }

    public static boolean mobShipsAttackPlayer() {
        return COMMON.mobShipsAttackPlayer.get();
    }

    public static boolean depthHadalVortex() {
        return COMMON.depthHadalVortex.get();
    }

    public static boolean useWakamoto() {
        return COMMON.useWakamoto.get();
    }

    public static boolean canTimekeeping() {
        return COMMON.canTimekeeping.get();
    }

    public static boolean canFlare() {
        return COMMON.canFlare.get();
    }

    public static boolean canSearchlight() {
        return COMMON.canSearchlight.get();
    }

    public static boolean canTeleport() {
        return COMMON.canTeleport.get();
    }

    public static boolean checkRing() {
        return COMMON.checkRing.get();
    }

    public static int bossCooldown() {
        return COMMON.bossCooldown.get();
    }

    public static int closeGuiDistance() {
        return COMMON.closeGuiDistance.get();
    }

    public static int chunkLoaderMode() {
        return COMMON.chunkLoaderMode.get();
    }

    public static int deathTime() {
        return COMMON.deathTime.get();
    }

    public static int despawnBoss() {
        return COMMON.despawnBoss.get();
    }

    public static int despawnMinion() {
        return COMMON.despawnMinion.get();
    }

    public static int despawnEgg() {
        return COMMON.despawnEgg.get();
    }

    public static int recycleSmall() {
        return COMMON.recycleSmall.get();
    }

    public static int recycleLarge() {
        return COMMON.recycleLarge.get();
    }

    public static int radarUpdate() {
        return COMMON.radarUpdate.get();
    }

    public static int commandShipNum() {
        return COMMON.commandShipNum.get();
    }

    public static int teamCooldown() {
        return COMMON.teamCooldown.get();
    }

    public static int spawnBossNumber() {
        return COMMON.spawnBossNumber.get();
    }

    public static int spawnMobNumber() {
        return COMMON.spawnMobNumber.get();
    }

    public static int pairingDistChest() {
        return COMMON.pairingDistChest.get();
    }

    public static int pairingDistWaypoint() {
        return COMMON.pairingDistWaypoint.get();
    }

    public static int nameTagDistance() {
        return COMMON.nameTagDistance.get();
    }

    public static int shipAttackPlayer() {
        return COMMON.shipAttackPlayer.get();
    }

    public static int dmgTakenSvS() {
        return COMMON.dmgTakenSvS.get();
    }

    public static int expModifier() {
        return COMMON.expModifier.get();
    }

    public static int attrsLimitModernization() {
        return COMMON.attrsLimitModernization.get();
    }

    public static int cdSearchLight() {
        return COMMON.cdSearchLight.get();
    }

    public static int cdAirplaneRecovery() {
        return COMMON.cdAirplaneRecovery.get();
    }

    public static int caressBaseMorale() {
        return COMMON.caressBaseMorale.get();
    }

    public static int polymetalOreRate() {
        return COMMON.polymetalOreRate.get();
    }

    public static int polymetalGravelRate() {
        return COMMON.polymetalGravelRate.get();
    }

    public static double dropRateGrudge() {
        return COMMON.dropRateGrudge.get();
    }

    public static double volumeTimekeeping() {
        return COMMON.volumeTimekeeping.get();
    }

    public static double volumeShip() {
        return COMMON.volumeShip.get();
    }

    public static double volumeAttack() {
        return COMMON.volumeAttack.get();
    }

    public static class Common {

        // ==================== GENERAL ====================
        public final BooleanValue debugMode;
        public final BooleanValue easyMode;
        public final BooleanValue friendlyFire;
        public final BooleanValue alwaysShowTeamCircle;
        public final BooleanValue showNameTag;
        public final BooleanValue polymetalAsMn;
        public final BooleanValue mobShipsAttackPlayer;
        public final BooleanValue depthHadalVortex;
        public final BooleanValue useWakamoto;

        public final IntValue bossCooldown;
        public final IntValue closeGuiDistance;
        public final IntValue chunkLoaderMode;
        public final IntValue deathTime;
        public final IntValue despawnBoss;
        public final IntValue despawnMinion;
        public final IntValue despawnEgg;
        public final IntValue recycleSmall;
        public final IntValue recycleLarge;
        public final IntValue radarUpdate;
        public final IntValue commandShipNum;
        public final IntValue teamCooldown;
        public final IntValue spawnBossNumber;
        public final IntValue spawnMobNumber;
        public final IntValue pairingDistChest;
        public final IntValue pairingDistWaypoint;
        public final IntValue nameTagDistance;
        public final IntValue shipAttackPlayer;

        public final DoubleValue dropRateGrudge;

        // General tile entity / array configs
        public final ConfigValue<List<? extends Double>> tileShipyardSmallConfig;
        public final ConfigValue<List<? extends Double>> tileShipyardLargeConfig;
        public final ConfigValue<List<? extends Double>> tileVolCoreConfig;
        public final ConfigValue<List<? extends Integer>> tileCraneConfig;
        public final ConfigValue<List<? extends Integer>> ringAbilityConfig;
        public final ConfigValue<List<? extends Integer>> infLiquidConfig;
        public final ConfigValue<List<? extends Double>> posHUDConfig;

        // ==================== SHIP COMBAT ====================
        public final IntValue dmgTakenSvS;
        public final IntValue expModifier;
        public final IntValue attrsLimitModernization;
        public final IntValue cdSearchLight;
        public final IntValue cdAirplaneRecovery;
        public final IntValue caressBaseMorale;

        public final BooleanValue canTimekeeping;
        public final BooleanValue canFlare;
        public final BooleanValue canSearchlight;
        public final BooleanValue canTeleport;
        public final BooleanValue checkRing;

        // Ship array configs
        public final ConfigValue<List<? extends Double>> scaleShipConfig;
        public final ConfigValue<List<? extends Double>> limitShipAttrsConfig;
        public final ConfigValue<List<? extends Double>> scaleBossSmallConfig;
        public final ConfigValue<List<? extends Double>> scaleBossLargeConfig;
        public final ConfigValue<List<? extends Double>> scaleMobSmallConfig;
        public final ConfigValue<List<? extends Double>> scaleMobLargeConfig;
        public final ConfigValue<List<? extends Double>> scaleHeldItemConfig;
        public final ConfigValue<List<? extends Integer>> consumeAmmoShipConfig;
        public final ConfigValue<List<? extends Integer>> consumeGrudgeShipConfig;
        public final ConfigValue<List<? extends Integer>> consumeGrudgeActionConfig;
        public final ConfigValue<List<? extends Integer>> consumeGrudgeTaskConfig;
        public final ConfigValue<List<? extends Integer>> baseAttackSpeedConfig;
        public final ConfigValue<List<? extends Integer>> fixedAttackDelayConfig;
        public final ConfigValue<List<? extends Integer>> expGainConfig;
        public final ConfigValue<List<? extends Integer>> expGainTaskConfig;
        public final ConfigValue<List<? extends Integer>> mobSpawnConfig;
        public final ConfigValue<List<? extends Integer>> drumLiquidConfig;
        public final ConfigValue<List<? extends Integer>> shipTeleportConfig;
        public final ConfigValue<List<? extends Integer>> tickFishingConfig;
        public final ConfigValue<List<? extends Integer>> tickMiningConfig;
        public final ConfigValue<List<? extends Boolean>> enableTaskConfig;

        // ==================== SOUND VOLUMES ====================
        public final DoubleValue volumeTimekeeping;
        public final DoubleValue volumeShip;
        public final DoubleValue volumeAttack;

        // ==================== WORLD GEN ====================
        public final IntValue polymetalOreRate;
        public final IntValue polymetalGravelRate;
        public final ConfigValue<List<? extends Boolean>> polyGravelBaseBlockConfig;

        Common(ForgeConfigSpec.Builder builder) {

            // ==================== GENERAL ====================
            builder.comment("General settings").push("general");

            debugMode = builder
                    .comment("Enable debug messages (WARNING: very spammy)")
                    .define("debugMode", false);

            easyMode = builder
                    .comment("Easy mode: decrease Large Construction resource requirements, increase ammo/grudge gained from items")
                    .define("easyMode", false);

            friendlyFire = builder
                    .comment("Enable/disable friendly fire damage. false: disable damage done by non-owner players")
                    .define("friendlyFire", true);

            alwaysShowTeamCircle = builder
                    .comment("Always show team circle indicator particle")
                    .define("alwaysShowTeamCircle", false);

            showNameTag = builder
                    .comment("Always show custom name tag above ships")
                    .define("showNameTag", true);

            polymetalAsMn = builder
                    .comment("true: Polymetallic Nodules = Manganese Dust, Polymetallic Ore = Manganese Ore")
                    .define("polymetalAsMn", false);

            mobShipsAttackPlayer = builder
                    .comment("For mob ships, true: attack player automatically")
                    .define("mobShipsAttackPlayer", true);

            depthHadalVortex = builder
                    .comment("Enable depth effect while rendering Hadal Vortex block")
                    .define("depthHadalVortex", false);

            useWakamoto = builder
                    .comment("Enable Wakamoto sound for mounts")
                    .define("useWakamoto", true);

            bossCooldown = builder
                    .comment("Boss spawn cooldown in ticks (4800 = 4 minutes)")
                    .defineInRange("bossCooldown", 4800, 20, 1728000);

            closeGuiDistance = builder
                    .comment("Close inventory GUI if ship is X blocks away from player")
                    .defineInRange("closeGuiDistance", 64, 2, 64);

            chunkLoaderMode = builder
                    .comment("Chunk loader mode: 0=disable, 1=only 1 chunk each ship, 2=3x3 chunks each ship")
                    .defineInRange("chunkLoaderMode", 2, 0, 2);

            deathTime = builder
                    .comment("Ship death animation time in ticks")
                    .defineInRange("deathTime", 400, 0, 3600);

            despawnBoss = builder
                    .comment("Despawn time of boss ships in ticks, -1 = do NOT despawn")
                    .defineInRange("despawnBoss", 12000, -1, 1728000);

            despawnMinion = builder
                    .comment("Despawn time of non-boss hostile ships in ticks, -1 = do NOT despawn")
                    .defineInRange("despawnMinion", 600, -1, 1728000);

            despawnEgg = builder
                    .comment("Despawn time of spawn egg of ship mob in ticks, -1 = do NOT despawn")
                    .defineInRange("despawnEgg", 12000, -1, 1728000);

            recycleSmall = builder
                    .comment("Recycle amount by Dismantle Hammer for common ships (e.g. Ro500)")
                    .defineInRange("recycleSmall", 20, 0, 1000);

            recycleLarge = builder
                    .comment("Recycle amount by Dismantle Hammer for rare ships (e.g. Yamato)")
                    .defineInRange("recycleLarge", 20, 0, 1000);

            radarUpdate = builder
                    .comment("Radar update interval (ticks) in Admiral's Desk GUI")
                    .defineInRange("radarUpdate", 64, 20, 6000);

            commandShipNum = builder
                    .comment("Number of ships per page for command: /ship list")
                    .defineInRange("commandShipNum", 5, 1, 5000);

            teamCooldown = builder
                    .comment("Create/Disband Team cooldown in ticks")
                    .defineInRange("teamCooldown", 6000, 20, 1728000);

            spawnBossNumber = builder
                    .comment("Large hostile ship (boss) count per spawn event")
                    .defineInRange("spawnBossNumber", 2, 1, 10);

            spawnMobNumber = builder
                    .comment("Small hostile ship count per spawn event")
                    .defineInRange("spawnMobNumber", 4, 1, 10);

            pairingDistChest = builder
                    .comment("Max pairing distance between waypoint and chest (blocks)")
                    .defineInRange("pairingDistChest", 16, 0, 64);

            pairingDistWaypoint = builder
                    .comment("Max pairing distance between waypoints (blocks)")
                    .defineInRange("pairingDistWaypoint", 48, 0, 64);

            nameTagDistance = builder
                    .comment("Show name tag if player is within X blocks of ship")
                    .defineInRange("nameTagDistance", 16, 1, 64);

            shipAttackPlayer = builder
                    .comment("For pet ships: 0=don't attack player, 1=attack hostile player, " +
                            "2=attack hostile and neutral player, 3=attack all players even if not in a team")
                    .defineInRange("shipAttackPlayer", 0, 0, 3);

            dropRateGrudge = builder
                    .comment("Grudge drop rate multiplier (e.g. 0.5 = 50% chance, 5.5 = drop 5 + 50% chance for 1 more)")
                    .defineInRange("dropRateGrudge", 1.0D, 0.0D, 64.0D);

            tileShipyardSmallConfig = builder
                    .comment("Small shipyard settings: [max fuel storage, build speed, fuel magnification]")
                    .defineList("tileShipyardSmall",
                            Arrays.asList(460800D, 48D, 1D),
                            e -> e instanceof Double);

            tileShipyardLargeConfig = builder
                    .comment("Large shipyard settings: [max fuel storage, build speed, fuel magnification]")
                    .defineList("tileShipyardLarge",
                            Arrays.asList(1382400D, 48D, 1D),
                            e -> e instanceof Double);

            tileVolCoreConfig = builder
                    .comment("Volcano Core settings: [max fuel storage, fuel consume speed, fuel value per grudge item]")
                    .defineList("tileVolCore",
                            Arrays.asList(9600D, 16D, 240D),
                            e -> e instanceof Double);

            tileCraneConfig = builder
                    .comment("Crane settings: [internal fluid tank capacity (mB), internal energy capacity (EU)]")
                    .defineList("tileCrane",
                            Arrays.asList(2048000, 160000000),
                            e -> e instanceof Integer);

            ringAbilityConfig = builder
                    .comment("Ring ability related to married number (-1=disable, 0~N=active/max limit): "
                            +
                            "[water breath, fly in water, dig speed boost, fog in liquid, immune to fire]")
                    .defineList("ringAbility",
                            Arrays.asList(0, 6, 30, 20, 12),
                            e -> e instanceof Integer);

            infLiquidConfig = builder
                    .comment("Can ship pump infinite liquid without destroying block: [min water depth, min lava depth]")
                    .defineList("infLiquid",
                            Arrays.asList(12, 8),
                            e -> e instanceof Integer);

            posHUDConfig = builder
                    .comment("HUD position of mount skills: [x, y] (0.5 = middle of window)")
                    .defineList("posHUD",
                            Arrays.asList(0.5D, 0.6D),
                            e -> e instanceof Double);

            builder.pop();

            // ==================== SHIP COMBAT ====================
            builder.comment("Ship combat and behavior settings").push("ship");

            dmgTakenSvS = builder
                    .comment("Ship vs Ship damage modifier in percent (100 = 100%, 20 = 20%)")
                    .defineInRange("dmgTakenSvS", 100, 0, 10000);

            expModifier = builder
                    .comment("Ship experience modifier. Example: 20 means level 150 requires 150*20+20 = 3020 exp")
                    .defineInRange("expModifier", 20, 1, 10000);

            attrsLimitModernization = builder
                    .comment("Max upgrade level per attribute by Modernization Toolkit")
                    .defineInRange("attrsLimitModernization", 3, 3, 100);

            cdSearchLight = builder
                    .comment("Cooldown (ticks) for placing light block of searchlight")
                    .defineInRange("cdSearchLight", 4, 1, 256);

            cdAirplaneRecovery = builder
                    .comment("Base cooldown for airplane recovery. Actual time = cdAirplaneRecovery / attack_speed + 20")
                    .defineInRange("cdAirplaneRecovery", 3600, 1, 30000);

            caressBaseMorale = builder
                    .comment("Base morale value gained per CaressTick (4 ticks)")
                    .defineInRange("caressBaseMorale", 20, 1, 5000);

            canTimekeeping = builder
                    .comment("Play timekeeping sound every 1000 ticks (1 Minecraft hour)")
                    .define("canTimekeeping", true);

            canFlare = builder
                    .comment("Can ships spawn Flare lighting effect (CLIENT SIDE only)")
                    .define("canFlare", true);

            canSearchlight = builder
                    .comment("Can ships spawn Searchlight lighting effect (CLIENT SIDE only)")
                    .define("canSearchlight", true);

            canTeleport = builder
                    .comment("Can ship teleport to owner/guarding position if too far away. " +
                            "NOTE: set false if ships often disappear/despawn after teleport!")
                    .define("canTeleport", true);

            checkRing = builder
                    .comment("Should check wedding ring when spawning NON-BOSS ship mob")
                    .define("checkRing", true);

            scaleShipConfig = builder
                    .comment("Ship attributes SCALE: [HP, firepower, armor, attack speed, move speed, range]")
                    .defineList("scaleShip",
                            Arrays.asList(1D, 1D, 1D, 1D, 1D, 1D),
                            e -> e instanceof Double);

            limitShipAttrsConfig = builder
                    .comment("Ship attributes max limit (-1 = no limit): " +
                            "[HP, damage(light), damage(heavy), damage(air_light), damage(air_heavy), "
                            +
                            "armor%, attack speed, move speed, range(blocks), critical, " +
                            "double hit, triple hit, miss reduction, anti-air, anti-ss, " +
                            "dodge, xp gain, grudge gain, ammo gain, hp regen, knockback resist]")
                    .defineList("limitShipAttrs",
                            Arrays.asList(-1D, -1D, -1D, -1D, -1D,
                                    0.8D, 4D, 0.6D, 64D, 0.9D,
                                    0.9D, 0.9D, 0.9D, -1D, -1D,
                                    0.75D, -1D, -1D, -1D, -1D,
                                    1D),
                            e -> e instanceof Double);

            scaleBossSmallConfig = builder
                    .comment("Small boss base attribute values: [HP, firepower, armor, attack speed, move speed, range]")
                    .defineList("scaleBossSmall",
                            Arrays.asList(1600D, 120D, 0.5D, 1.6D, 0.38D, 18D),
                            e -> e instanceof Double);

            scaleBossLargeConfig = builder
                    .comment("Large boss base attribute values: [HP, firepower, armor, attack speed, move speed, range]")
                    .defineList("scaleBossLarge",
                            Arrays.asList(3200D, 240D, 0.75D, 2D, 0.35D, 22D),
                            e -> e instanceof Double);

            scaleMobSmallConfig = builder
                    .comment("Small mob ship base attribute values: [HP, firepower, armor, attack speed, move speed, range]")
                    .defineList("scaleMobSmall",
                            Arrays.asList(250D, 25D, 0.15D, 0.7D, 0.45D, 12D),
                            e -> e instanceof Double);

            scaleMobLargeConfig = builder
                    .comment("Large mob ship base attribute values: [HP, firepower, armor, attack speed, move speed, range]")
                    .defineList("scaleMobLarge",
                            Arrays.asList(500D, 50D, 0.30D, 0.9D, 0.4D, 15D),
                            e -> e instanceof Double);

            scaleHeldItemConfig = builder
                    .comment("Ship held item scaling: [scale, offset X, offset Y, offset Z]")
                    .defineList("scaleHeldItem",
                            Arrays.asList(2.5D, 0D, 0D, 0D),
                            e -> e instanceof Double);

            consumeAmmoShipConfig = builder
                    .comment("Ammo consumption per ship type: [DD, CL, CA, CAV, CLT, CVL, CV, BB, BBV, SS, AP] (max 45)")
                    .defineList("consumeAmmoShip",
                            Arrays.asList(1, 2, 2, 2, 2, 3, 3, 4, 4, 1, 1),
                            e -> e instanceof Integer);

            consumeGrudgeShipConfig = builder
                    .comment("Grudge consumption idle per ship type: [DD, CL, CA, CAV, CLT, CVL, CV, BB, BBV, SS, AP] (max 120)")
                    .defineList("consumeGrudgeShip",
                            Arrays.asList(5, 7, 8, 9, 8, 11, 12, 15, 14, 4, 3),
                            e -> e instanceof Integer);

            consumeGrudgeActionConfig = builder
                    .comment("Grudge consumption per action: [Light attack, Heavy attack, Light aircraft, Heavy aircraft, Moving per block]")
                    .defineList("consumeGrudgeAction",
                            Arrays.asList(4, 8, 6, 12, 3),
                            e -> e instanceof Integer);

            consumeGrudgeTaskConfig = builder
                    .comment("Grudge consumption per task: [cooking, fishing, mining, crafting]")
                    .defineList("consumeGrudgeTask",
                            Arrays.asList(3, 30, 300, 2),
                            e -> e instanceof Integer);

            baseAttackSpeedConfig = builder
                    .comment("Base attack speed for: [Melee, Light attack, Heavy attack, Carrier attack, Airplane attack]. "
                            +
                            "Example: base 160, fixed delay 30 = (160 / ship_attack_speed + 30) ticks per attack")
                    .defineList("baseAttackSpeed",
                            Arrays.asList(40, 80, 120, 100, 100),
                            e -> e instanceof Integer);

            fixedAttackDelayConfig = builder
                    .comment("Fixed attack delay for: [Melee, Light attack, Heavy attack, Carrier attack, Airplane attack]")
                    .defineList("fixedAttackDelay",
                            Arrays.asList(0, 20, 50, 35, 35),
                            e -> e instanceof Integer);

            expGainConfig = builder
                    .comment("Exp gain for: [Melee, Light Attack, Heavy Attack, Light Aircraft, Heavy Aircraft, Move per Block (AP only), Other Action (AP only)]")
                    .defineList("expGain",
                            Arrays.asList(2, 4, 12, 8, 24, 1, 2),
                            e -> e instanceof Integer);

            expGainTaskConfig = builder
                    .comment("Exp gain per task: [Cooking, Fishing, Mining, Crafting]")
                    .defineList("expGainTask",
                            Arrays.asList(2, 20, 10, 1),
                            e -> e instanceof Integer);

            mobSpawnConfig = builder
                    .comment("Mob ship spawn settings: [max number in world, spawn probability (roll per player every 128 ticks), "
                            +
                            "groups each spawn, min each group, max each group]")
                    .defineList("mobSpawn",
                            Arrays.asList(50, 10, 1, 1, 1),
                            e -> e instanceof Integer);

            drumLiquidConfig = builder
                    .comment("Liquid transport rate: [base transfer rate (mB/t), additional rate per enchantment (mB/t)]. "
                            +
                            "Total Rate = (ShipLV * 0.1 + 1) * (BaseRate * #TotalPumps + EnchantRate * #TotalEnchantments)")
                    .defineList("drumLiquid",
                            Arrays.asList(40, 5),
                            e -> e instanceof Integer);

            shipTeleportConfig = builder
                    .comment("Ship teleport when following and guarding: [cooldown (ticks), distance (blocks^2)]")
                    .defineList("shipTeleport",
                            Arrays.asList(200, 256),
                            e -> e instanceof Integer);

            tickFishingConfig = builder
                    .comment("Fishing time setting: [base, random] in ticks")
                    .defineList("tickFishing",
                            Arrays.asList(400, 600),
                            e -> e instanceof Integer);

            tickMiningConfig = builder
                    .comment("Mining time setting: [base, random] in ticks")
                    .defineList("tickMining",
                            Arrays.asList(100, 200),
                            e -> e instanceof Integer);

            enableTaskConfig = builder
                    .comment("Enable/disable tasks: [cooking, fishing, mining, crafting]")
                    .defineList("enableTask",
                            Arrays.asList(true, true, true, true),
                            e -> e instanceof Boolean);

            builder.pop();

            // ==================== SOUND ====================
            builder.comment("Sound volume settings").push("sound");

            volumeTimekeeping = builder
                    .comment("Timekeeping sound volume multiplier")
                    .defineInRange("volumeTimekeeping", 1.0D, 0.0D, 10.0D);

            volumeShip = builder
                    .comment("Ship voice sound volume multiplier")
                    .defineInRange("volumeShip", 1.0D, 0.0D, 10.0D);

            volumeAttack = builder
                    .comment("Attack sound effect volume multiplier")
                    .defineInRange("volumeAttack", 0.7D, 0.0D, 10.0D);

            builder.pop();

            // ==================== WORLD GEN ====================
            builder.comment("World generation settings").push("world");

            polymetalOreRate = builder
                    .comment("Polymetallic Ore clusters per chunk")
                    .defineInRange("polymetalOreRate", 7, 0, 100);

            polymetalGravelRate = builder
                    .comment("Polymetallic Gravel clusters per chunk")
                    .defineInRange("polymetalGravelRate", 4, 0, 100);

            polyGravelBaseBlockConfig = builder
                    .comment("PolyGravel replacement blocks: [stone, gravel, sand, dirt]")
                    .defineList("polyGravelBaseBlock",
                            Arrays.asList(true, true, false, false),
                            e -> e instanceof Boolean);

            builder.pop();
        }
    }
}
