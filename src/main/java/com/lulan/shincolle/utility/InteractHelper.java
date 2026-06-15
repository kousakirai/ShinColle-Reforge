package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.crafting.ShipCalc;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.item.IShipCombatRation;
import com.lulan.shincolle.item.IShipFoodItem;
import com.lulan.shincolle.item.OwnerPaper;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.network.S2CSpawnParticlePacket;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.server.ServerDataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.List;

/**
 * methods for interact with ship
 */
public class InteractHelper {

    public InteractHelper() {
    }

    /**
     * modern kit interact method
     */
    public static boolean interactModernKit(BasicEntityShip ship, Player player, ItemStack stack) {
        // add 1 random bonus
        if (ship.getAttrs().addAttrsBonusRandom(new java.util.Random(ship.getRandom().nextLong()))) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            // set happy emotion
            ship.setStateEmotion(ID.S.Emotion, ID.Emotion.XD, true);

            // recalc attrs
            ship.calcShipAttributes(1, true);

            // play marriage sound
            ship.playSound(ship.getCustomSound(4, ship), (float) ConfigHandler.volumeShip(), 1F);
            return true;
        }

        return false;
    }

    /**
     * pointer interact method
     */
    public static boolean interactPointer(BasicEntityShip ship, Player player, ItemStack stack) {
        // set ai target
        ship.setAITarget(player);

        // is owner
        if (TeamHelper.checkSameOwner(player, ship) && !ship.getStateFlag(ID.F.NoFuel)) {
            if (ship.getMorale() < ID.Morale.L_Excited * 1.3F) {
                ship.addMorale(ConfigHandler.caressBaseMorale());
            }

            // show emotes
            ship.applyEmotesReaction(0);
        }
        // not owner or no fuel
        else {
            ship.applyEmotesReaction(1);
        }

        // clear ai target
        ship.setAITarget(null);

        return true;
    }

    /**
     * bucket interact method
     */
    public static boolean interactBucket(BasicEntityShip ship, Player player, ItemStack stack) {
        // hp not at max hp can use bucket
        if (ship.getHealth() < ship.getMaxHealth()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (ship instanceof BasicEntityShipSmall) {
                ship.heal(ship.getMaxHealth() * 0.1F + 5F); // 1 bucket = 10% hp for small ship
            } else {
                ship.heal(ship.getMaxHealth() * 0.05F + 10F); // 1 bucket = 5% hp for large ship
            }

            // airplane++
            if (ship instanceof BasicEntityShipCV cv) {
                cv.setNumAircraftLight(cv.getNumAircraftLight() + 1);
                cv.setNumAircraftHeavy(cv.getNumAircraftHeavy() + 1);
            }

            return true;
        }

        return false;
    }

    /**
     * wedding ring interact method
     */
    public static boolean interactWeddingRing(BasicEntityShip ship, Player player, ItemStack stack) {
        // stack-1 in non-creative mode
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // set marriage flag
        ship.setStateFlag(ID.F.IsMarried, true);

        // player marriage num +1
        CapaTeitoku capa = ServerDataManager.getTeitokuCapability(player);

        capa.setMarriageNum(capa.getMarriageNum() + 1);


        // play hearts effect
        ModNetworking.sendToAllTracking(
                new S2CSpawnParticlePacket((byte) 3, ship.getId(), new byte[0]), ship);

        // play marriage sound
        ship.playSound(ship.getCustomSound(4, ship), (float) ConfigHandler.volumeShip(), 1F);

        // add morale
        ship.setMorale(16000);

        // set shy emotion
        ship.setStateEmotion(ID.S.Emotion, ID.Emotion.SHY, true);

        // add 3 random bonus point
        for (int i = 0; i < 3; ++i) {
            ship.getAttrs().addAttrsBonusRandom(new java.util.Random(ship.getRandom().nextLong()));
        }

        // update attrs
        ship.calcShipAttributes(31, true);

        return true;
    }

    /**
     * kaitai hammer interact method
     */
    public static boolean interactKaitaiHammer(BasicEntityShip ship, Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            // damage +1 in non-creative mode
            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

            // set item amount
            ItemStack[] items = ShipCalc.getKaitaiItems(ship.getShipClass());


            for (ItemStack item : items) {
                if (item != null && !item.isEmpty()) {
                    ItemEntity entityItem = new ItemEntity(ship.level(),
                            ship.getX(), ship.getY() + 0.8D, ship.getZ(), item);
                    ship.level().addFreshEntity(entityItem);
                }
            }


            // drop inventory item
            for (int i = 0; i < ship.getCapaShipInventory().getSlots(); i++) {
                ItemStack invitem = ship.getCapaShipInventory().getStackInSlot(i);

                if (!invitem.isEmpty()) {
                    float f = ship.getRandom().nextFloat() * 0.8F + 0.1F;
                    float f1 = ship.getRandom().nextFloat() * 0.8F + 0.1F;
                    float f2 = ship.getRandom().nextFloat() * 0.8F + 0.1F;

                    ItemEntity entityItem = new ItemEntity(ship.level(),
                            ship.getX() + f, ship.getY() + f1, ship.getZ() + f2,
                            invitem.copy());
                    ship.level().addFreshEntity(entityItem);
                }
            }

            // kaitai sound
            ship.playSound(ModSounds.SHIP_KAITAI.get(), (float) ConfigHandler.volumeShip(), getSoundPitch(ship));
            ship.playSound(ship.getCustomSound(3, ship), (float) ConfigHandler.volumeShip(), getSoundPitch(ship));
        }

        // show emotes
        ship.applyEmotesReaction(2);

        ship.discard();

        return true;
    }

    /**
     * sound pitch for ship
     */
    public static float getSoundPitch(BasicEntityShip ship) {
        return (ship.getRandom().nextFloat() - ship.getRandom().nextFloat()) * 0.1F + 1F;
    }

    /**
     * change owner method:
     * 1. check owner paper has 2 signs
     * 2. check owner is A or B
     * 3. get player entity
     * 4. change ship's player UID
     * 5. change ship's owner UUID
     */
    public static boolean interactOwnerPaper(BasicEntityShip ship, Player player, ItemStack itemstack) {
        CompoundTag nbt = itemstack.getTag();
        boolean changeOwner = false;

        if (nbt != null) {
            int ida = nbt.getInt(OwnerPaper.SignIDA);
            int idb = nbt.getInt(OwnerPaper.SignIDB);
            int idtarget; // target player uid

            // 1. check 2 signs
            if (ida > 0 && idb > 0) {
                // 2. check owner is A or B
                if (ida == ship.getPlayerUID()) {
                    // A is owner
                    idtarget = idb;
                } else if (idb == ship.getPlayerUID()) {
                    // B is owner
                    idtarget = ida;
                } else {
                    return false;
                }

                // 3. check player online
                Player target = ServerDataManager.getPlayerByUID(idtarget);
                CapaTeitoku capa = target != null ? ServerDataManager.getTeitokuCapability(target) : null;

                if (capa != null) {
                    // 4. change ship's player UID
                    ship.setPlayerUID(idtarget);

                    // 5. change ship's owner UUID
                    ship.setOwnerUUID(target.getUUID());

                    // 6. change ship's owner name
                    ship.ownerName = target.getName().getString();

                    LogHelper.debug("DEBUG: change owner: to: pid " + idtarget + " uuid " + target.getUUID());
                    changeOwner = true;

                    // send sync packet
                    ship.sendSyncPacketID();

                    // set cry emotion
                    ship.setStateEmotion(ID.S.Emotion, ID.Emotion.T_T, true);
                }
            }
        }

        if (changeOwner) {
            // play marriage sound
            ship.playSound(ship.getCustomSound(4, ship), (float) ConfigHandler.volumeShip(), 1F);

            // consume item
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            return true;
        }

        return false;
    }

    /**
     * feed
     * <p>
     * 1. morale++
     * 2. show emotion
     * 3. sometimes reject food
     * 4. feed max morale = 4800
     */
    @SuppressWarnings("deprecation")
    public static boolean interactFeed(BasicEntityShip ship, Player player, ItemStack itemstack) {
        int type = 0;
        int mfood = 1;
        int addgrudge = 0;
        int addammo = 0;
        int addammoh = 0;
        int addsatur = 0;

        // max saturation, reject food
        if (ship.getFoodSaturation() >= ship.getFoodSaturationMax()) {
            if (ship.getEmotesTick() <= 0) {
                ship.setEmotesTick(40);
                ship.applyEmotesReaction(4); // bored
            }

            return false;
        }

        // is ship food
        if (itemstack.getItem() instanceof IShipFoodItem foodItem) {
            type = 2;
            int meta = getItemMeta(itemstack);
            int foodv = (int) foodItem.getFoodValue(meta);
            mfood = foodv + ship.getRandom().nextInt(foodv + 1);

            switch (foodItem.getSpecialEffect(meta)) {
                case 1: // grudge
                    addgrudge = 300 + ship.getRandom().nextInt(500);
                    break;
                case 2: // abyssium
                    ship.heal(ship.getMaxHealth() * 0.05F + 1F);
                    break;
                case 3: // ammo
                    switch (meta) {
                        case 0:
                            addammo = 30 + ship.getRandom().nextInt(10);
                            break;
                        case 1:
                            addammo = 270 + ship.getRandom().nextInt(90);
                            break;
                        case 2:
                            addammoh = 15 + ship.getRandom().nextInt(5);
                            break;
                        case 3:
                            addammoh = 135 + ship.getRandom().nextInt(45);
                            break;
                    }
                    break;
                case 4: // polymetal
                    // add airplane if CV
                    if (ship instanceof BasicEntityShipCV cv && ship.getRandom().nextInt(10) > 4) {
                        cv.setNumAircraftLight(cv.getNumAircraftLight() + 1);
                        cv.setNumAircraftHeavy(cv.getNumAircraftHeavy() + 1);
                    }
                    break;
                case 5: // toy plane
                    // add airplane if CV
                    if (ship instanceof BasicEntityShipCV cv) {
                        cv.setNumAircraftLight(cv.getNumAircraftLight() + ship.getRandom().nextInt(3) + 1);
                        cv.setNumAircraftHeavy(cv.getNumAircraftHeavy() + ship.getRandom().nextInt(3) + 1);
                    }
                    break;
                case 6: // combat ration
                    addsatur = 3;
                    addgrudge = mfood;
                    mfood = 0;

                    // add morale to happy
                    if (itemstack.getItem() instanceof IShipCombatRation ration) {
                        mfood = ration.getMoraleValue(meta);
                    }

                    // if ice cream, clean debuffs
                    if (meta == 4 || meta == 5) {
                        BuffHelper.removeDebuffs(ship);
                    }

                    break;
            }
        }
        // is vanilla food item
        else if (itemstack.getItem().isEdible()) {
            type = 1;
            var foodProps = itemstack.getItem().getFoodProperties();
            if (foodProps != null) {
                float fv = foodProps.getNutrition();
                float sv = foodProps.getSaturationModifier();
                if (fv < 1F)
                    fv = 1F;
                mfood = (int) ((fv + ship.getRandom().nextInt((int) fv + 5)) * sv * 20F);
                addgrudge = mfood;
            }
        }
        // is potion
        else if (itemstack.getItem() == Items.POTION
                || itemstack.getItem() == Items.SPLASH_POTION
                || itemstack.getItem() == Items.LINGERING_POTION) {
            type = 3;
            mfood = -100; // ship hate potion
            addgrudge = Values.N.BaseGrudge;
        }

        // can feed
        if (type > 0) {
            // set happy emotion
            ship.setStateEmotion(ID.S.Emotion, ID.Emotion.XD, true);

            // play sound
            if (ship.getStateTimer(ID.T.SoundTime) <= 0) {
                ship.setStateTimer(ID.T.SoundTime, 20 + ship.getRandom().nextInt(20));
                ship.playSound(ship.getCustomSound(7, ship), (float) ConfigHandler.volumeShip(), getSoundPitch(ship));
            }

            // apply potion effect
            List<MobEffectInstance> pbuffs = PotionUtils.getMobEffects(itemstack);

            if (!pbuffs.isEmpty()) {
                // apply buff/debuff potion
                for (MobEffectInstance pe : pbuffs) {
                    ship.addEffect(new MobEffectInstance(pe));
                }

                // apply instant potion
                float hp1p = ship.getMaxHealth() * 0.01F;
                if (hp1p < 1F)
                    hp1p = 1F;

                // apply heal
                int lvPotion = BuffHelper.checkPotionHeal(pbuffs);
                if (lvPotion > 0)
                    ship.heal((hp1p * 2F + 2F) * lvPotion);

                // apply damage
                lvPotion = BuffHelper.checkPotionDamage(pbuffs);
                if (lvPotion > 0)
                    ship.hurt(ship.damageSources().magic(), (hp1p * 2F + 2F) * lvPotion);

                // update potion buff
                ship.calcShipAttributes(8, true);
            }

            // morale++
            ship.addMorale(mfood);

            // saturation++
            ship.setFoodSaturation(ship.getFoodSaturation() + 1 + addsatur);

            // misc++
            ship.addGrudge((int) (addgrudge * ship.getAttrs().getAttrsBuffed(ID.Attrs.GRUDGE)));
            ship.addAmmoLight((int) (addammo * ship.getAttrs().getAttrsBuffed(ID.Attrs.AMMO)));
            ship.addAmmoHeavy((int) (addammoh * ship.getAttrs().getAttrsBuffed(ID.Attrs.AMMO)));

            // item--
            if (player != null && !player.getAbilities().instabuild) {
                ItemStack container = itemstack.getCraftingRemainingItem();
                itemstack.shrink(1);
                if (!container.isEmpty()) {
                    if (itemstack.isEmpty()) {
                        player.getInventory().setItem(player.getInventory().selected, container);
                    } else if (!player.getInventory().add(container)) {
                        player.drop(container, false);
                    }
                }
            }

            // show eat emotion
            if (ship.getEmotesTick() <= 0) {
                ship.setEmotesTick(40);
                ship.applyEmotesReaction(0); // normal/happy
            }

            return true;
        }

        return false;
    }

    /**
     * Helper to get item meta from NBT in 1.20.1
     * In 1.10.2, items had metadata (damage value). In 1.20.1,
     * our mod items use NBT "EquipMeta" or similar tags.
     */
    public static int getItemMeta(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("ItemMeta")) {
                return tag.getInt("ItemMeta");
            }
        }
        return 0;
    }

}
