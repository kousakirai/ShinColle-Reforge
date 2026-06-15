package com.lulan.shincolle.item;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Combat Ration - consumable item that restores food and morale to ship
 * entities.
 * Original meta types with food/morale values:
 * 0 = food=900, morale=1400
 * 1 = food=3600, morale=1800
 * 2 = food=1200, morale=1600
 * 3 = food=3900, morale=2000
 * 4 = food=100, morale=3000
 * 5 = food=900, morale=4000
 */
public class CombatRation extends BasicItem implements IShipCombatRation {

    private final int type;

    public CombatRation() {
        this(0);
    }

    public CombatRation(int type) {
        super(new Properties().stacksTo(16));
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // Description per type (may contain newlines)
        String str = Component.translatable("gui.shincolle.combatration" + this.type).getString();
        String[] lines = str.split("\n");
        for (String line : lines) {
            tooltip.add(Component.literal(line));
        }

        // Morale bonus
        tooltip.add(Component.literal(ChatFormatting.LIGHT_PURPLE + "+" + getMoraleValue(0) + " " +
                Component.translatable("gui.shincolle.combatration").getString()));

        // Food bonus
        int food = (int) getFoodValue(0);
        tooltip.add(Component.literal(ChatFormatting.RED + "+" + food + "~" + food * 2 + " " +
                Component.translatable("item.shincolle.grudge").getString()));
    }

    public int getType() {
        return this.type;
    }

    @Override
    public float getFoodValue(int meta) {
        return switch (this.type) {
            case 1 -> 3600.0F;
            case 2 -> 1200.0F;
            case 3 -> 3900.0F;
            case 4 -> 100.0F;
            default -> 900.0F;
        };
    }

    @Override
    public float getSaturationValue(int meta) {
        return 10.0F;
    }

    @Override
    public int getSpecialEffect(int meta) {
        return 6;
    }

    @Override
    public int getMoraleValue(int meta) {
        return switch (this.type) {
            case 1 -> 1800;
            case 2 -> 1600;
            case 3 -> 2000;
            case 4 -> 3000;
            case 5 -> 4000;
            default -> 1400;
        };
    }

    /**
     * When the player holds a combat ration in their selected slot,
     * nearby owned ships are attracted toward the player.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (entity instanceof Player player && !level.isClientSide()) {
            if (isSelected && (player.tickCount & 15) == 0) {
                // get nearby ships
                AABB aabb = player.getBoundingBox().inflate(8D, 6D, 8D);
                List<BasicEntityShip> slist = level.getEntitiesOfClass(BasicEntityShip.class, aabb);

                for (BasicEntityShip s : slist) {
                    if (s != null && s.isAlive() && !s.getStateFlag(ID.F.NoFuel)
                            && !s.isOrderedToSit() && !s.isPassenger()
                            && TeamHelper.checkSameOwner(player, s)) {
                        if (player.distanceToSqr(s) > 4D) {
                            s.setStateEmotion(ID.S.Emotion, ID.Emotion.XD, true);
                            s.getNavigation().moveTo(player, 0.75D);

                            if (player.getRandom().nextInt(5) == 0) {
                                switch (player.getRandom().nextInt(3)) {
                                    case 1:
                                        s.applyParticleEmotion(9); // hungry
                                        break;
                                    case 2:
                                        s.applyParticleEmotion(30); // pif
                                        break;
                                    default:
                                        s.applyParticleEmotion(1); // heart
                                        break;
                                }
                            }
                        }

                        s.getLookControl().setLookAt(player, 50F, 50F);
                    }
                }
            }
        }
    }
}
