package com.lulan.shincolle.gametest;

import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.init.ModEntities;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Regression coverage for the two highest-risk combat/resource paths on
 * BasicEntityShip: ammo consumption + inventory resupply (decrAmmoNum /
 * decrSupplies / attackEntityWith*Ammo) and the damage-source guard clauses
 * at the top of hurt().
 * <p>
 * Full damage-number coverage of hurt() (DEF reduction, dodge, ship-vs-ship
 * scaling, resist/light buffs) is intentionally NOT included here: those
 * paths depend on RandomSource rolls and are better suited to unit tests
 * against CombatHelper directly with an injected/seeded RandomSource, once
 * that class is decoupled from the entity. This file only locks in the
 * deterministic, non-random guard clauses.
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShinColleCombatGameTests {

    private ShinColleCombatGameTests() {
    }

    private static BasicEntityShip createShip(ServerLevel level) {
        EntityType<?> type = ModEntities.DESTROYER_I.get();
        if (!(type.create(level) instanceof BasicEntityShip ship)) {
            throw new AssertionError("destroyer_i did not create a BasicEntityShip instance.");
        }
        return ship;
    }

    // ========== decrAmmoNum ==========

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void decrAmmoNumConsumesDirectlyWhenSufficientStock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);

        ship.setStateMinor(ID.M.NumAmmoLight, 50);

        boolean result = ship.decrAmmoNum(0, 20);

        if (!result) {
            throw new AssertionError("decrAmmoNum must succeed when stock already covers the request.");
        }
        if (ship.getStateMinor(ID.M.NumAmmoLight) != 30) {
            throw new AssertionError("Ammo was not decremented by the requested amount. actual="
                    + ship.getStateMinor(ID.M.NumAmmoLight));
        }

        ship.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void decrAmmoNumFailsAndLeavesStockUnchangedWhenNoResupplyAvailable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);

        ship.setStateMinor(ID.M.NumAmmoLight, 5); // empty inventory: no resupply item present

        boolean result = ship.decrAmmoNum(0, 50);

        if (result) {
            throw new AssertionError("decrAmmoNum must fail when stock and inventory are both insufficient.");
        }
        if (ship.getStateMinor(ID.M.NumAmmoLight) != 5) {
            throw new AssertionError("Ammo stock must be left untouched on a failed consumption. actual="
                    + ship.getStateMinor(ID.M.NumAmmoLight));
        }

        ship.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void decrAmmoNumRefillsFromInventoryAndConsumesSupplyItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);

        ship.setStateMinor(ID.M.NumAmmoLight, 0);

        CapaShipInventory inventory = ship.getCapaShipInventory();
        int supplySlot = 10; // beyond the 6 equip slots, inside page-1 storage
        inventory.setStackInSlot(supplySlot, new ItemStack(ModItems.AMMO.get(), 5));

        boolean result = ship.decrAmmoNum(0, 10);

        if (!result) {
            throw new AssertionError("decrAmmoNum must succeed once inventory resupply covers the request.");
        }
        // Values.N.BaseLightAmmo (30) at the documented default 100% ammo-gain
        // multiplier (ID.Attrs.AMMO), minus the 10 consumed by this call.
        if (ship.getStateMinor(ID.M.NumAmmoLight) != 20) {
            throw new AssertionError("Refilled ammo stock does not match BaseLightAmmo(30) - requested(10). actual="
                    + ship.getStateMinor(ID.M.NumAmmoLight));
        }
        if (inventory.getStackInSlot(supplySlot).getCount() != 4) {
            throw new AssertionError("Exactly one light-ammo supply item must be consumed from the inventory. actual="
                    + inventory.getStackInSlot(supplySlot).getCount());
        }

        ship.discard();
        helper.succeed();
    }

    // ========== attackEntityWith*Ammo resource gating ==========

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void attackWithLightAmmoFailsWithoutSideEffectsWhenAmmoInsufficient(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);

        ship.setAmmoConsumption(50);
        ship.setStateMinor(ID.M.NumAmmoLight, 10); // insufficient, empty inventory
        int expBefore = ship.getStateMinor(ID.M.ExpCurrent);
        int grudgeBefore = ship.getStateMinor(ID.M.NumGrudge);
        int moraleBefore = ship.getMorale();
        int combatTickBefore = ship.getCombatTick();

        boolean result = ship.attackEntityWithAmmo(ship);

        if (result) {
            throw new AssertionError("Attack must fail when light ammo is insufficient and cannot be resupplied.");
        }
        assertNoResourceSideEffects(ship, expBefore, grudgeBefore, moraleBefore, combatTickBefore);

        ship.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void attackWithHeavyAmmoFailsWithoutSideEffectsWhenAmmoInsufficient(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);

        ship.setAmmoConsumption(50);
        ship.setStateMinor(ID.M.NumAmmoHeavy, 10); // insufficient, empty inventory
        int expBefore = ship.getStateMinor(ID.M.ExpCurrent);
        int grudgeBefore = ship.getStateMinor(ID.M.NumGrudge);
        int moraleBefore = ship.getMorale();
        int combatTickBefore = ship.getCombatTick();

        boolean result = ship.attackEntityWithHeavyAmmo(ship);

        if (result) {
            throw new AssertionError("Attack must fail when heavy ammo is insufficient and cannot be resupplied.");
        }
        assertNoResourceSideEffects(ship, expBefore, grudgeBefore, moraleBefore, combatTickBefore);

        ship.discard();
        helper.succeed();
    }

    private static void assertNoResourceSideEffects(BasicEntityShip ship, int expBefore, int grudgeBefore,
                                                    int moraleBefore, int combatTickBefore) {
        if (ship.getStateMinor(ID.M.ExpCurrent) != expBefore) {
            throw new AssertionError("A failed attack must not grant experience. before=" + expBefore
                    + " after=" + ship.getStateMinor(ID.M.ExpCurrent));
        }
        if (ship.getStateMinor(ID.M.NumGrudge) != grudgeBefore) {
            throw new AssertionError("A failed attack must not consume grudge. before=" + grudgeBefore
                    + " after=" + ship.getStateMinor(ID.M.NumGrudge));
        }
        if (ship.getMorale() != moraleBefore) {
            throw new AssertionError("A failed attack must not change morale. before=" + moraleBefore
                    + " after=" + ship.getMorale());
        }
        if (ship.getCombatTick() != combatTickBefore) {
            throw new AssertionError("A failed attack must not update the combat tick.");
        }
    }

    // ========== hurt() guard clauses ==========

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hurtIgnoresImmuneEnvironmentalSources(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);
        float startHealth = ship.getHealth();

        String[] labels = new String[]{"inWall", "starve", "cactus", "fall"};
        DamageSource[] immuneSources = new DamageSource[]{
                ship.damageSources().inWall(),
                ship.damageSources().starve(),
                ship.damageSources().cactus(),
                ship.damageSources().fall(),
        };

        for (int i = 0; i < immuneSources.length; i++) {
            boolean result = ship.hurt(immuneSources[i], 1000F);
            if (result) {
                throw new AssertionError("hurt() must ignore immune source: " + labels[i]);
            }
        }

        if (ship.getHealth() != startHealth) {
            throw new AssertionError("Health must be unaffected by immune sources. start=" + startHealth
                    + " actual=" + ship.getHealth());
        }

        ship.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hurtTeleportsToRescueHeightOnFallOutOfWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);
        ship.moveTo(5D, -40D, 5D, 0F, 0F);

        boolean result = ship.hurt(ship.damageSources().fellOutOfWorld(), 1000F);

        if (result) {
            throw new AssertionError("fellOutOfWorld must be absorbed by the rescue teleport, not applied as damage.");
        }
        if (ship.getY() != 4D) {
            throw new AssertionError("Ship must be rescued to y=4. actual=" + ship.getY());
        }

        ship.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public static void hurtIgnoresSelfInflictedDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BasicEntityShip ship = createShip(level);
        ship.invulnerableTime = 0; // bypass spawn-grace so the self-damage branch is actually exercised
        float startHealth = ship.getHealth();

        boolean result = ship.hurt(ship.damageSources().mobAttack(ship), 1000F);

        if (result) {
            throw new AssertionError("hurt() must ignore self-inflicted damage sources.");
        }
        if (ship.getHealth() != startHealth) {
            throw new AssertionError("Health must be unaffected by self-inflicted damage. start=" + startHealth
                    + " actual=" + ship.getHealth());
        }

        ship.discard();
        helper.succeed();
    }
}
