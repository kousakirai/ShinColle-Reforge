package com.lulan.shincolle.gametest;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.init.ModEntities;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Regression coverage for BasicEntityShip's persistent state:
 * - NBT save/load round trip (StateMinor / StateFlag / owner / custom name / texture)
 * - Level cap guard + attribute recalculation
 * - Resource counter clamping (morale / ammo / grudge)
 * <p>
 * These tests intentionally avoid depending on ConfigHandler-derived multipliers
 * (e.g. XP gain) so they stay deterministic across balance-config changes and are
 * safe to keep green while the StateFlag/StateMinor parallel-array representation
 * is refactored towards a type-safe model in a later pass.
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShinColleShipStateGameTests {

    private ShinColleShipStateGameTests() {
    }

    private static BasicEntityShip createShip(ServerLevel level) {
        EntityType<?> type = ModEntities.DESTROYER_I.get();
        if (!(type.create(level) instanceof BasicEntityShip ship)) {
            throw new AssertionError("destroyer_i did not create a BasicEntityShip instance.");
        }
        return ship;
    }

    // ========== Persistence ==========

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipStatePersistenceRoundTripPreservesCoreFields(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BasicEntityShip source = createShip(level);
        source.setStateMinor(ID.M.ShipLevel, 42);
        source.setStateMinor(ID.M.NumAmmoLight, 123);
        source.setStateMinor(ID.M.NumGrudge, 7);
        source.setStateFlag(ID.F.IsMarried, true);
        source.setStateFlag(ID.F.CanFollow, false);
        source.setTextureID(5);
        source.ownerName = "TesterOwner";
        source.setCustomName(Component.literal("TestFlagship"));

        CompoundTag nbt = new CompoundTag();
        source.addAdditionalSaveData(nbt);
        source.discard();

        BasicEntityShip target = createShip(level);
        target.readAdditionalSaveData(nbt);

        if (target.getStateMinor(ID.M.ShipLevel) != 42) {
            throw new AssertionError("ShipLevel did not round-trip. actual="
                    + target.getStateMinor(ID.M.ShipLevel));
        }
        if (target.getStateMinor(ID.M.NumAmmoLight) != 123) {
            throw new AssertionError("NumAmmoLight did not round-trip. actual="
                    + target.getStateMinor(ID.M.NumAmmoLight));
        }
        if (target.getStateMinor(ID.M.NumGrudge) != 7) {
            throw new AssertionError("NumGrudge did not round-trip. actual="
                    + target.getStateMinor(ID.M.NumGrudge));
        }
        if (!target.getStateFlag(ID.F.IsMarried)) {
            throw new AssertionError("IsMarried flag did not round-trip.");
        }
        if (target.getStateFlag(ID.F.CanFollow)) {
            throw new AssertionError("CanFollow flag did not round-trip.");
        }
        if (target.getTextureID() != 5) {
            throw new AssertionError("TextureID did not round-trip. actual=" + target.getTextureID());
        }
        if (!"TesterOwner".equals(target.ownerName)) {
            throw new AssertionError("ownerName did not round-trip. actual=" + target.ownerName);
        }
        if (!target.hasCustomName()
                || target.getCustomName() == null
                || !"TestFlagship".equals(target.getCustomName().getString())) {
            throw new AssertionError("Custom name did not round-trip.");
        }

        target.discard();
        helper.succeed();
    }

    // ========== Leveling ==========

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipLevelSetterGuardsUpperCapAndRecalculatesHealth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);

        ship.setShipLevel(55, true);
        if (ship.getStateMinor(ID.M.ShipLevel) != 55) {
            throw new AssertionError("Level was not applied for an in-range value. actual="
                    + ship.getStateMinor(ID.M.ShipLevel));
        }
        if (ship.getHealth() != ship.getMaxHealth()) {
            throw new AssertionError("Health was not fully restored after a level-up recalculation. hp="
                    + ship.getHealth() + " max=" + ship.getMaxHealth());
        }

        // setShipLevel only applies values below 151; out-of-range calls must be no-ops
        // for the level field so save data can never end up with an invalid level.
        ship.setShipLevel(200, true);
        if (ship.getStateMinor(ID.M.ShipLevel) != 55) {
            throw new AssertionError("Out-of-range level request must not change the stored level. actual="
                    + ship.getStateMinor(ID.M.ShipLevel));
        }

        ship.discard();
        helper.succeed();
    }

    // ========== Resource counters ==========

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipMoraleAddClampsToValidRange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);

        ship.setMorale(100);
        ship.addMorale(-9999);
        if (ship.getMorale() != 0) {
            throw new AssertionError("Morale must clamp at the floor of 0. actual=" + ship.getMorale());
        }

        ship.addMorale(50000);
        if (ship.getMorale() != 16000) {
            throw new AssertionError("Morale must clamp at the ceiling of 16000. actual=" + ship.getMorale());
        }

        ship.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void shipResourceCountersClampAtZeroOnNegativeAdd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);

        ship.addAmmoLight(10);
        ship.addAmmoLight(-9999);
        if (ship.getStateMinor(ID.M.NumAmmoLight) != 0) {
            throw new AssertionError("NumAmmoLight must clamp at 0. actual="
                    + ship.getStateMinor(ID.M.NumAmmoLight));
        }

        ship.addAmmoHeavy(10);
        ship.addAmmoHeavy(-9999);
        if (ship.getStateMinor(ID.M.NumAmmoHeavy) != 0) {
            throw new AssertionError("NumAmmoHeavy must clamp at 0. actual="
                    + ship.getStateMinor(ID.M.NumAmmoHeavy));
        }

        ship.addGrudge(10);
        ship.addGrudge(-9999);
        if (ship.getStateMinor(ID.M.NumGrudge) != 0) {
            throw new AssertionError("NumGrudge must clamp at 0. actual="
                    + ship.getStateMinor(ID.M.NumGrudge));
        }

        int killsBefore = ship.getStateMinor(ID.M.Kills);
        ship.addKills();
        ship.addKills();
        if (ship.getStateMinor(ID.M.Kills) != killsBefore + 2) {
            throw new AssertionError("addKills() must increment Kills by exactly 1 per call. actual="
                    + ship.getStateMinor(ID.M.Kills));
        }

        ship.discard();
        helper.succeed();
    }
}