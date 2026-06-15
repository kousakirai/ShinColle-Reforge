package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.config.ConfigMining;
import com.lulan.shincolle.crafting.InventoryCraftingFake;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.other.EntityShipFishingHook;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModEntities;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.server.ServerDataManager;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper for ship tasks: cooking, mining, fishing, crafting, pumping.
 * <p>
 * Ported from 1.10.2 TaskHelper.
 */
public class TaskHelper {

    public TaskHelper() {
    }

    /**
     * Update task, called every 8 ticks.
     * <p>
     * StateMinor[ID.M.Task]: task ID
     * StateMinor[ID.M.TaskSide]: bit flags for sides and options
     */
    public static void onUpdateTask(BasicEntityShip host) {
        if (host.getStateFlag(ID.F.NoFuel) || !host.isAlive())
            return;

        switch (host.getStateMinor(ID.M.Task)) {
            case 1: // cooking
                if (ConfigHandler.enableTask[0])
                    onUpdateCooking(host);
                break;
            case 2: // fishing
                if (ConfigHandler.enableTask[1])
                    onUpdateFishing(host);
                break;
            case 3: // mining
                if (ConfigHandler.enableTask[2])
                    onUpdateMining(host);
                break;
            case 4: // crafting
                if (ConfigHandler.enableTask[3])
                    onUpdateCrafting(host);
                break;
        }
    }

    /**
     * Crafting task:
     * Craft itemstack stored in mainhand slot (slot 22).
     */
    public static void onUpdateCrafting(BasicEntityShip host) {
        if (host == null)
            return;

        CapaShipInventory invShip = host.getCapaShipInventory();
        ItemStack paper = invShip.getStackInSlot(22); // mainhand slot
        if (paper.isEmpty() || paper.getItem() != ModItems.RECIPE_PAPER.get())
            return;

        // check guard position
        BlockPos pos = new BlockPos(host.getGuardedPos(0), host.getGuardedPos(1), host.getGuardedPos(2));
        if (pos.getY() <= 0)
            return;

        // check guard type
        if (host.getGuardedPos(4) != 1)
            return;

        // check dimension
        if (!host.level().dimension().location().toString()
                .equals(getDimensionKey(host.getGuardedPos(3))))
            return;

        // check guard position is waypoint
        BlockEntity te = host.level().getBlockEntity(pos);
        if (!(te instanceof TileEntityWaypoint waypoint))
            return;

        // check waypoint has paired chest
        pos = waypoint.getPairedChest();
        if (pos == null || pos.getY() <= 0)
            return;

        // check paired chest has inventory capability
        IItemHandler chest = CapaHelper.getCapaInventory(host.level().getBlockEntity(pos), -1);
        if (chest == null)
            return;

        // check distance
        if (host.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 25D) {
            host.getNavigation().moveTo(host.getGuardedPos(0), host.getGuardedPos(1),
                    host.getGuardedPos(2), 1D);
            return;
        }

        // check recipe is valid
        InventoryCraftingFake recipe = new InventoryCraftingFake(3, 3);
        ItemStack result = ItemStack.EMPTY;

        if (paper.hasTag()) {
            CompoundTag nbt = paper.getTag();
            assert nbt != null;
            ListTag tagList = nbt.getList("Recipe", Tag.TAG_COMPOUND);

            for (int i = 0; i < 9; i++) {
                CompoundTag itemTags = tagList.getCompound(i);
                int slot = itemTags.getInt("Slot");

                if (slot >= 0 && slot < 9) {
                    recipe.setItem(slot, ItemStack.of(itemTags));
                }
            }

            // get result
            Optional<CraftingRecipe> recipeResult = host.level().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, recipe, host.level());
            if (recipeResult.isPresent()) {
                result = recipeResult.get().assemble(recipe, host.level().registryAccess());
            }

            if (result.isEmpty())
                return;
        } else {
            return;
        }

        // start crafting
        int maxCraft = host.getLevel() / 20 + 1;
        int taskSide = host.getStateMinor(ID.M.TaskSide);
        boolean checkMetadata = (taskSide & Values.N.Pow2[18]) == Values.N.Pow2[18];
        boolean checkNbt = (taskSide & Values.N.Pow2[20]) == Values.N.Pow2[20];
        InventoryCraftingFake recipeTemp = new InventoryCraftingFake(3, 3);
        boolean canAddExp = false;
        int maxtimes = maxCraft;

        while (maxCraft > 0) {
            maxtimes--;
            if (maxtimes < 0)
                break;


            // move materials from chest to ship's inventory slot 12~20
            for (int i = 0; i < 9; i++) {
                if (!invShip.getStackInSlot(i + 12).isEmpty()) {
                    recipeTemp.setItem(i, invShip.getStackInSlot(i + 12));
                    continue;
                }

                ItemStack tempStack = recipe.getItem(i);
                if (tempStack.isEmpty()) {
                    recipeTemp.setItem(i, ItemStack.EMPTY);
                    continue;
                }

                // get item from chest
                ItemStack fromChest = getAndRemoveItemFromHandler(chest, tempStack, 1);
                invShip.setStackInSlot(i + 12, fromChest);
                recipeTemp.setItem(i, invShip.getStackInSlot(i + 12));
            }

            // check recipeTemp valid
            Optional<CraftingRecipe> resultOpt = host.level().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, recipeTemp, host.level());

            if (resultOpt.isPresent()) {
                ItemStack resultTemp = resultOpt.get().assemble(recipeTemp, host.level().registryAccess());

                if (!ItemStack.isSameItemSameTags(resultTemp, result)) {
                    break;
                }

                maxCraft--;
                canAddExp = true;

                // move result to chest or drop
                moveItemToHandler(chest, resultTemp);
                if (!resultTemp.isEmpty()) {
                    dropItemOnGround(host, resultTemp);
                }

                // material -1
                for (int i = 0; i < 9; i++) {
                    ItemStack matStack = invShip.getStackInSlot(i + 12);
                    if (!matStack.isEmpty()) {
                        matStack.shrink(1);
                        if (matStack.isEmpty()) {
                            invShip.setStackInSlot(i + 12, ItemStack.EMPTY);
                        }
                    }
                }

                // move remaining items (bucket, bottle...)
                NonNullList<ItemStack> remainStacks = resultOpt.get()
                        .getRemainingItems(recipeTemp);
                for (ItemStack remain : remainStacks) {
                    if (!remain.isEmpty()) {
                        moveItemToHandler(chest, remain);
                        if (!remain.isEmpty()) {
                            dropItemOnGround(host, remain);
                        }
                    }
                }
            } else {
                break;
            }
        }

        // add exp and consume grudge
        if (canAddExp) {
            host.addShipExp(ConfigHandler.expGainTask[3]);
            host.decrGrudgeNum(ConfigHandler.consumeGrudgeTask[3]);
            host.addMorale(-10);

            host.swing(InteractionHand.MAIN_HAND);

            if (host.getRandom().nextInt(5) == 0) {
                switch (host.getRandom().nextInt(5)) {
                    case 1:
                        host.applyParticleEmotion(2);
                        break;
                    case 2:
                        host.applyParticleEmotion(7);
                        break;
                    case 3:
                        host.applyParticleEmotion(13);
                        break;
                    case 4:
                        host.applyParticleEmotion(30);
                        break;
                    default:
                        host.applyParticleEmotion(21);
                        break;
                }
            }
        }
    }

    /**
     * Mining task:
     * Put pickaxe in mainhand (slot 22).
     * Generate ores per X ticks based on level and height.
     */
    public static void onUpdateMining(BasicEntityShip host) {
        if (host == null)
            return;

        ItemStack pickaxe = host.getCapaShipInventory().getStackInSlot(22);
        if (pickaxe.isEmpty() || !isToolEffective(pickaxe, 0))
            return;

        // check not moving
        Vec3 motion = host.getDeltaMovement();
        if (Math.abs(motion.x) > 0.1F || Math.abs(motion.z) > 0.1F || motion.y > 0.1F) {
            return;
        } else {
            // random move
            if ((host.getTickExisted() & 63) == 0) {
                host.getNavigation().moveTo(
                        host.getX() + host.getRandom().nextInt(9) - 4,
                        host.getY() + host.getRandom().nextInt(5) - 2,
                        host.getZ() + host.getRandom().nextInt(9) - 4, 1D);
                return;
            }
        }

        // swing arm and emotes
        if (host.getRandom().nextInt(5) > 2) {
            host.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

            if (host.getRandom().nextInt(10) > 8) {
                switch (host.getRandom().nextInt(5)) {
                    case 2:
                        host.applyParticleEmotion(11);
                        break;
                    case 3:
                        host.applyParticleEmotion(5);
                        break;
                    case 4:
                        host.applyParticleEmotion(30);
                        break;
                    default:
                        host.applyParticleEmotion(0);
                        break;
                }
            }
        }

        // finish mining
        if ((host.tickCount & 31) == 0
                && host.tickCount - host.getStateTimer(ID.T.TaskTime) > ConfigHandler.tickMining[0]
                + host.getRandom().nextInt(ConfigHandler.tickMining[1])) {
            int stone = 0;
            boolean canMine = false;

            // check nearby solid block > N
            for (int dy = -3; dy < 5 && !canMine; dy++) {
                for (int dx = -3; dx < 4 && !canMine; dx++) {
                    for (int dz = -3; dz < 4; dz++) {
                        BlockPos checkPos = new BlockPos(
                                Mth.floor(host.getX()) + dx,
                                Mth.floor(host.getY()) + dy,
                                Mth.floor(host.getZ()) + dz);
                        BlockState state = host.level().getBlockState(checkPos);

                        if (state.requiresCorrectToolForDrops()) {
                            stone++;
                        }
                        if (stone > 120) {
                            canMine = true;
                            break;
                        }
                    }
                }
            }

            if (!canMine)
                return;

            // generate mining result
            generateMiningResult(host);

            // add exp and consume grudge
            host.addShipExp(ConfigHandler.expGainTask[2]);
            host.decrGrudgeNum(ConfigHandler.consumeGrudgeTask[2]);
            host.addMorale(-200);

            switch (host.getRandom().nextInt(5)) {
                case 1:
                    host.applyParticleEmotion(11);
                    break;
                case 2:
                    host.applyParticleEmotion(14);
                    break;
                case 3:
                    host.applyParticleEmotion(4);
                    break;
                case 4:
                    host.applyParticleEmotion(30);
                    break;
                default:
                    host.applyParticleEmotion(0);
                    break;
            }

            host.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            host.setStateTimer(ID.T.TaskTime, host.tickCount);
        }
    }

    /**
     * Fishing task:
     * Put fishing rod in mainhand (slot 22).
     * Detect water block with depth >= 3 blocks.
     */
    public static void onUpdateFishing(BasicEntityShip host) {
        if (host == null)
            return;

        ItemStack rod = host.getCapaShipInventory().getStackInSlot(22);
        if (rod.isEmpty() || rod.getItem() != Items.FISHING_ROD)
            return;

        // check guard position
        BlockPos pos = new BlockPos(host.getGuardedPos(0), host.getGuardedPos(1), host.getGuardedPos(2));
        if (pos.getY() <= 0)
            return;
        if (host.getGuardedPos(4) != 1)
            return;

        // check dimension
        if (!host.level().dimension().location().toString()
                .equals(getDimensionKey(host.getGuardedPos(3))))
            return;

        // move to guard point
        if (host.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 10D) {
            host.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1D);
            return;
        }

        // not in moving
        Vec3 motion = host.getDeltaMovement();
        if (Math.abs(motion.x) > 0.1F || Math.abs(motion.z) > 0.1F || motion.y > 0.1F)
            return;

        // check water block
        pos = BlockHelper.getNearbyLiquid(host, false, true, 5, 3);
        if (pos == null)
            return;

        // if no hook -> cast fishing rod
        if (host.fishHook == null) {
            host.swing(InteractionHand.MAIN_HAND);

            EntityShipFishingHook hook = new EntityShipFishingHook(ModEntities.FISHING_HOOK.get(), host.level());
            hook.setHost(host);
            hook.setPos(
                    pos.getX() + 0.1D + host.getRandom().nextDouble() * 0.8D,
                    pos.getY() + 1D,
                    pos.getZ() + 0.1D + host.getRandom().nextDouble() * 0.8D);
            host.level().addFreshEntity(hook);
            host.fishHook = hook;

            switch (host.getRandom().nextInt(4)) {
                case 1:
                    host.applyParticleEmotion(14);
                    break;
                case 2:
                    host.applyParticleEmotion(7);
                    break;
                case 3:
                    host.applyParticleEmotion(11);
                    break;
                default:
                    host.applyParticleEmotion(30);
                    break;
            }
            return;
        }

        // hook exists, wait random time
        if (!host.fishHook.isRemoved() && host.fishHook.tickCount > ConfigHandler.tickFishing[0]
                + host.getRandom().nextInt(Math.max(1, ConfigHandler.tickFishing[1]))) {
            generateFishingResult(host);
            host.fishHook.discard();

            host.addShipExp(ConfigHandler.expGainTask[1]);
            host.decrGrudgeNum(ConfigHandler.consumeGrudgeTask[1]);
            host.addMorale(300);

            switch (host.getRandom().nextInt(5)) {
                case 1:
                    host.applyParticleEmotion(1);
                    break;
                case 2:
                    host.applyParticleEmotion(7);
                    break;
                case 3:
                    host.applyParticleEmotion(16);
                    break;
                case 4:
                    host.applyParticleEmotion(30);
                    break;
                default:
                    host.applyParticleEmotion(0);
                    break;
            }

            host.swing(InteractionHand.MAIN_HAND);
        }

        // fishing timeout
        if (host.fishHook != null
                && host.fishHook.tickCount > ConfigHandler.tickFishing[0] + ConfigHandler.tickFishing[1]) {
            host.fishHook.discard();
        }
    }

    /**
     * Cooking task:
     * Smelt itemstack in mainhand (slot 22), optional fuel in offhand (slot 23).
     */
    public static void onUpdateCooking(BasicEntityShip host) {
        if (host == null)
            return;

        CapaShipInventory inv = host.getCapaShipInventory();
        ItemStack mainstack = inv.getStackInSlot(22);
        ItemStack offstack = inv.getStackInSlot(23);
        if (mainstack.isEmpty())
            return;

        // check guard position
        BlockPos pos = new BlockPos(host.getGuardedPos(0), host.getGuardedPos(1), host.getGuardedPos(2));
        if (pos.getY() <= 0)
            return;
        if (host.getGuardedPos(4) != 1)
            return;

        // check dimension
        if (!host.level().dimension().location().toString()
                .equals(getDimensionKey(host.getGuardedPos(3))))
            return;

        // check guard position is waypoint
        BlockEntity te = host.level().getBlockEntity(pos);
        if (!(te instanceof TileEntityWaypoint waypoint))
            return;

        // check waypoint has paired chest
        pos = waypoint.getPairedChest();
        if (pos == null || pos.getY() <= 0)
            return;

        // check paired chest is a furnace
        te = host.level().getBlockEntity(pos);
        if (!(te instanceof AbstractFurnaceBlockEntity furnace))
            return;

        // check distance
        if (host.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 25D) {
            host.getNavigation().moveTo(host.getGuardedPos(0), host.getGuardedPos(1),
                    host.getGuardedPos(2), 1D);
            return;
        }

        // check smelt recipe
        Optional<SmeltingRecipe> smeltRecipe = host.level().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(mainstack), host.level());
        if (smeltRecipe.isEmpty())
            return;

        ItemStack resultStack = smeltRecipe.get().assemble(new SimpleContainer(mainstack),
                host.level().registryAccess());
        if (resultStack.isEmpty())
            return;

        boolean swing = false;

        // put target item into furnace input (slot 0)
        ItemStack furnaceInput = furnace.getItem(0);
        if (furnaceInput.isEmpty() || (ItemStack.isSameItemSameTags(furnaceInput, mainstack)
                && furnaceInput.getCount() < furnaceInput.getMaxStackSize())) {
            // find matching item in ship inventory (not slot 22/23)
            int targetSlot = findMatchingSlot(inv, mainstack, 22, 23);
            if (targetSlot >= 0) {
                ItemStack toMove = inv.getStackInSlot(targetSlot);
                int toAdd = Math.min(toMove.getCount(),
                        furnaceInput.isEmpty() ? mainstack.getMaxStackSize()
                                : furnaceInput.getMaxStackSize() - furnaceInput.getCount());
                if (toAdd > 0) {
                    if (furnaceInput.isEmpty()) {
                        furnace.setItem(0, toMove.copyWithCount(toAdd));
                    } else {
                        furnaceInput.grow(toAdd);
                    }
                    toMove.shrink(toAdd);
                    if (toMove.isEmpty())
                        inv.setStackInSlot(targetSlot, ItemStack.EMPTY);
                    swing = true;
                }
            }
        }

        // put fuel into furnace fuel slot (slot 1)
        if (!offstack.isEmpty()) {
            ItemStack furnaceFuel = furnace.getItem(1);
            if (furnaceFuel.isEmpty() || (ItemStack.isSameItemSameTags(furnaceFuel, offstack)
                    && furnaceFuel.getCount() < furnaceFuel.getMaxStackSize())) {
                int fuelSlot = findMatchingSlot(inv, offstack, 22, 23);
                if (fuelSlot >= 0) {
                    ItemStack fuelToMove = inv.getStackInSlot(fuelSlot);
                    int toAdd = Math.min(fuelToMove.getCount(),
                            furnaceFuel.isEmpty() ? offstack.getMaxStackSize()
                                    : furnaceFuel.getMaxStackSize() - furnaceFuel.getCount());
                    if (toAdd > 0) {
                        if (furnaceFuel.isEmpty()) {
                            furnace.setItem(1, fuelToMove.copyWithCount(toAdd));
                        } else {
                            furnaceFuel.grow(toAdd);
                        }
                        fuelToMove.shrink(toAdd);
                        if (fuelToMove.isEmpty())
                            inv.setStackInSlot(fuelSlot, ItemStack.EMPTY);
                        swing = true;
                    }
                }
            }
        }

        // take output from furnace (slot 2)
        ItemStack output = furnace.getItem(2);
        if (!output.isEmpty() && ItemStack.isSameItemSameTags(output, resultStack)) {
            // move output to ship inventory
            if (inv.addItemStackToInventory(output.copy())) {
                furnace.setItem(2, ItemStack.EMPTY);
                swing = true;

                // add exp and consume grudge
                host.addShipExp(ConfigHandler.expGainTask[0]);
                host.decrGrudgeNum(ConfigHandler.consumeGrudgeTask[0]);
                host.addMorale(100);

                // generate charcoal by fail chance
                float failChance = (float) (ConfigHandler.maxLevel - host.getLevel())
                        / (float) ConfigHandler.maxLevel * 0.2F + 0.05F;

                if (host.getRandom().nextFloat() < failChance) {
                    ItemStack coal = new ItemStack(Items.CHARCOAL, 1);
                    ItemEntity entityitem = new ItemEntity(host.level(),
                            pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D, coal);
                    entityitem.setDeltaMovement(
                            host.getRandom().nextGaussian() * 0.05D,
                            host.getRandom().nextGaussian() * 0.05D + 0.2D,
                            host.getRandom().nextGaussian() * 0.05D);
                    host.level().addFreshEntity(entityitem);
                    host.applyEmotesReaction(6);
                } else {
                    if (host.getRandom().nextInt(7) == 0) {
                        switch (host.getRandom().nextInt(5)) {
                            case 1:
                                host.applyParticleEmotion(1);
                                break;
                            case 2:
                                host.applyParticleEmotion(7);
                                break;
                            case 3:
                                host.applyParticleEmotion(16);
                                break;
                            case 4:
                                host.applyParticleEmotion(30);
                                break;
                            default:
                                host.applyParticleEmotion(0);
                                break;
                        }
                    }
                }
            }
        }

        if (swing) {
            host.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Check tool is suitable for task. targetType: 0=pickaxe, 1=shovel, 2=axe
     */
    public static boolean isToolEffective(ItemStack stack, int targetType) {
        if (stack.isEmpty())
            return false;

        return switch (targetType) {
            case 0 -> stack.is(ItemTags.PICKAXES);
            case 1 -> stack.is(ItemTags.SHOVELS);
            case 2 -> stack.is(ItemTags.AXES);
            default -> false;
        };
    }

    /**
     * Generate mining result using ConfigMining loot tables.
     * Filters entries by dimension, biome, ship level, Y position, and tool tier.
     * Performs weighted random selection and applies fortune enchantment bonus.
     */
    @SuppressWarnings("deprecation")
    public static void generateMiningResult(LivingEntity host) {
        if (!(host instanceof BasicEntityShip ship))
            return;

        ItemStack pickaxe = ship.getCapaShipInventory().getStackInSlot(22);
        if (pickaxe.isEmpty())
            return;

        // check ConfigMining is loaded
        if (!ConfigMining.isLoaded()) {
            LogHelper.info("WARN: ConfigMining not loaded, skipping mining result");
            return;
        }

        // get dimension key (strip "minecraft:" prefix to match config format)
        String dimKey = ship.level().dimension().location().getPath(); // "overworld", "the_nether", "the_end"

        // get biome key (strip namespace prefix)
        String biomeKey = ConfigMining.GENERAL_BIOME;
        BlockPos shipPos = ship.blockPosition();
        Holder<net.minecraft.world.level.biome.Biome> biomeHolder = ship.level().getBiome(shipPos);
        Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>> biomeKeyOpt = biomeHolder
                .unwrapKey();
        if (biomeKeyOpt.isPresent()) {
            biomeKey = biomeKeyOpt.get().location().getPath();
        }

        // get entries matching dimension and biome
        List<ConfigMining.ItemEntry> entries = ConfigMining.getEntries(dimKey, biomeKey);
        if (entries.isEmpty())
            return;

        // get ship stats for filtering
        int shipLevel = ship.getLevel();
        int yPos = (int) ship.getY();
        int toolLevel = getPickaxeTier(pickaxe);
        int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, pickaxe);

        // filter entries by ship level, Y position, and tool tier
        List<ConfigMining.ItemEntry> filtered = new ArrayList<>();
        for (ConfigMining.ItemEntry entry : entries) {
            if (shipLevel >= entry.lvShip() && yPos <= entry.lvHeight() && toolLevel >= entry.lvTool()) {
                filtered.add(entry);
            }
        }

        if (filtered.isEmpty())
            return;

        // build cumulative weight list for weighted random selection
        List<Integer> cumulativeWeights = new ArrayList<>();
        cumulativeWeights.add(filtered.get(0).weight());
        for (int i = 1; i < filtered.size(); i++) {
            cumulativeWeights.add(cumulativeWeights.get(i - 1) + filtered.get(i).weight());
        }

        // roll weighted random
        int totalWeight = cumulativeWeights.get(cumulativeWeights.size() - 1);
        int roll = ship.getRandom().nextInt(totalWeight);
        int selectedIndex = 0;
        for (int i = 0; i < cumulativeWeights.size(); i++) {
            if (roll < cumulativeWeights.get(i)) {
                selectedIndex = i;
                break;
            }
        }

        // get selected entry
        ConfigMining.ItemEntry selected = filtered.get(selectedIndex);

        // look up item by resource location
        net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.tryParse(selected.itemName()));
        if (item == null || item == Items.AIR)
            return;

        // calculate stack size
        int stackSize = selected.min();
        if (selected.max() > selected.min()) {
            stackSize = selected.min() + ship.getRandom().nextInt(selected.max() - selected.min() + 1);
        }

        // apply fortune enchantment bonus
        if (selected.enchant() > 0F && fortuneLevel > 0) {
            stackSize = (int) (stackSize * (1F + fortuneLevel * selected.enchant()));
        }

        if (stackSize < 1)
            stackSize = 1;

        // create and add item
        ItemStack result = new ItemStack(item, stackSize);
        if (!ship.getCapaShipInventory().addItemStackToInventory(result)) {
            dropItemOnGround(ship, result);
        }
    }

    /**
     * Generate fishing result using vanilla loot tables.
     */
    @SuppressWarnings("deprecation")
    public static void generateFishingResult(LivingEntity host) {
        float luck = 0F;
        ServerLevel serverLevel;

        if (!(host.level() instanceof ServerLevel))
            return;
        serverLevel = (ServerLevel) host.level();

        if (host instanceof BasicEntityShip ship) {
            // get luck from enchantment
            ItemStack mainHand = ship.getCapaShipInventory().getStackInSlot(22);
            ItemStack offHand = ship.getCapaShipInventory().getStackInSlot(23);
            int lv1 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FISHING_LUCK, mainHand);
            int lv2 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FISHING_LUCK, offHand);
            luck = Math.max(lv1, lv2);

            // add level modifier
            luck += (float) ship.getLevel() / ConfigHandler.maxLevel * 1.5F;
        } else if (!(host instanceof IShipAttackBase)) {
            return;
        }

        // get fishing loot table
        LootTable lootTable = serverLevel.getServer().getLootData()
                .getLootTable(BuiltInLootTables.FISHING);

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, host.position())
                .withParameter(LootContextParams.TOOL, host instanceof BasicEntityShip ship
                        ? ship.getCapaShipInventory().getStackInSlot(22)
                        : ItemStack.EMPTY)
                .withParameter(LootContextParams.THIS_ENTITY, host)
                .withLuck(luck)
                .create(LootContextParamSets.FISHING);

        for (ItemStack itemstack : lootTable.getRandomItems(params)) {
            if (host instanceof BasicEntityShip ship) {
                if (!ship.getCapaShipInventory().addItemStackToInventory(itemstack)) {
                    dropItemOnGround(host, itemstack);
                }
            } else {
                dropItemOnGround(host, itemstack);
            }
        }
    }

    /**
     * Pump fluid under ship (width: 3x3, depth: 2).
     * Also collects XP orbs nearby.
     */
    public static void onUpdatePumping(BasicEntityShip ship) {
        // calc pump speed based on level
        int delay = 63;
        int level = ship.getLevel();

        if (level >= 145)
            delay = 3;
        else if (level >= 115)
            delay = 7;
        else if (level >= 75)
            delay = 15;
        else if (level >= 30)
            delay = 31;

        // pump liquid
        if ((ship.tickCount & delay) == 0) {
            CapaShipInventory inv = ship.getCapaShipInventory();

            // check pump equip if not transport ship
            // requires fluid drum (meta 1), not just any drum type
            if (ship.getShipType() != ID.ShipType.TRANSPORT || !ship.getStateFlag(ID.F.IsMarried)) {
                if (!checkItemWithMetaInShipInventory(inv, ModItems.EQUIP_DRUM.get(), 1, 0, 6))
                    return;
            }

            // check fluid block
            BlockPos pos = BlockHelper.getNearbyLiquid(ship, true, false, 3, 0);

            if (pos != null && !ship.level().isClientSide()) {
                // check player permission
                Player player = ServerDataManager.getPlayerByUID(ship.getPlayerUID());

                if (player != null) {
                    BlockState state = ship.level().getBlockState(pos);

                    if (BlockHelper.checkBlockIsLiquid(state, 0)) {
                        // remove source block (simplified from original fluid tank filling)
                        boolean doRemove = !BlockHelper.checkBlockNearbyIsSameBlock(
                                ship.level(), state.getBlock(), pos.getX(), pos.getY(), pos.getZ(),
                                3, ConfigHandler.infLiquid[0]);

                        if (doRemove) {
                            ship.level().removeBlock(pos, false);
                            if (ship.getRandom().nextInt(3) == 0) {
                                ship.playSound(SoundEvents.BUCKET_FILL, 0.5F,
                                        ship.getRandom().nextFloat() * 0.4F + 0.8F);
                            }
                        }
                    }
                }
            }
        }

        // collect xp orb
        if ((ship.tickCount & 3) == 0) {
            CapaShipInventory inv = ship.getCapaShipInventory();

            // check pump equip if not transport ship
            // requires fluid drum (meta 1), not just any drum type
            if (ship.getShipType() != ID.ShipType.TRANSPORT || !ship.getStateFlag(ID.F.IsMarried)) {
                if (!checkItemWithMetaInShipInventory(inv, ModItems.EQUIP_DRUM.get(), 1, 0, 6))
                    return;
            }

            // check bottle in inventory
            ItemStack bot = new ItemStack(Items.GLASS_BOTTLE);
            int botid = findMatchingSlot(inv, bot);
            if (botid < 0)
                return;
            bot = inv.getStackInSlot(botid);

            // find xp orbs
            AABB aabb = ship.getBoundingBox().inflate(7D, 7D, 7D);
            List<ExperienceOrb> xpList = ship.level().getEntitiesOfClass(ExperienceOrb.class, aabb);

            if (!xpList.isEmpty()) {
                for (ExperienceOrb xp : xpList) {
                    double dist = ship.distanceToSqr(xp);

                    if (dist > 9D) {
                        // pull xp orb
                        Dist4d pullvec = CalcHelper.getDistanceFromA2B(ship, xp);
                        xp.push(pullvec.x * -0.25D, pullvec.y * -0.25D, pullvec.z * -0.25D);
                    } else {
                        // collect xp orb
                        ship.level().playSound(null, ship.getX(), ship.getY(), ship.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F,
                                0.5F * ((ship.getRandom().nextFloat() - ship.getRandom().nextFloat()) * 0.7F
                                        + 1.8F));

                        int xpValue = xp.getValue();
                        if (xpValue > 0) {
                            ship.setStateMinor(ID.M.XP, ship.getStateMinor(ID.M.XP) + xpValue);
                        }

                        xp.discard();
                    }
                }
            }

            // transfer xp to bottle (1 bottle per update)
            int xpvalue = ship.getStateMinor(ID.M.XP);

            if (xpvalue >= 8) {
                ship.setStateMinor(ID.M.XP, xpvalue - 8);

                bot.shrink(1);
                if (bot.isEmpty())
                    inv.setStackInSlot(botid, ItemStack.EMPTY);

                ItemStack xpbot = new ItemStack(Items.EXPERIENCE_BOTTLE);
                if (!inv.addItemStackToInventory(xpbot)) {
                    dropItemOnGround(ship, xpbot);
                }
            }
        }
    }

    // ========== Utility Methods ==========

    /**
     * Get pickaxe tier level from ItemStack.
     * 0=wood/gold, 1=stone, 2=iron, 3=diamond, 4=netherite.
     */
    private static int getPickaxeTier(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TieredItem tiered))
            return 0;

        net.minecraft.world.item.Tier tier = tiered.getTier();
        if (tier == Tiers.NETHERITE)
            return 4;
        if (tier == Tiers.DIAMOND)
            return 3;
        if (tier == Tiers.IRON)
            return 2;
        if (tier == Tiers.STONE)
            return 1;
        return 0; // WOOD, GOLD, or unknown
    }

    /**
     * Get dimension key string from numeric ID (for legacy compatibility).
     */
    private static String getDimensionKey(int dimId) {
        return switch (dimId) {
            case -1 -> "minecraft:the_nether";
            case 0 -> "minecraft:overworld";
            case 1 -> "minecraft:the_end";
            default -> "minecraft:overworld";
        };
    }

    /**
     * Find slot in ship inventory matching target item, excluding specific slots.
     */
    private static int findMatchingSlot(CapaShipInventory inv, ItemStack target, int... excludeSlots) {
        for (int i = 0; i < inv.getSlots(); i++) {
            boolean excluded = false;
            for (int ex : excludeSlots) {
                if (i == ex) {
                    excluded = true;
                    break;
                }
            }
            if (excluded)
                continue;

            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, target)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if item exists in ship inventory within slot range.
     */
    private static boolean checkItemInShipInventory(CapaShipInventory inv, net.minecraft.world.item.Item item,
                                                    int startSlot, int endSlot) {
        for (int i = startSlot; i <= endSlot && i < inv.getSlots(); i++) {
            if (inv.getStackInSlot(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if item with specific EquipMeta exists in ship inventory within slot
     * range.
     * In the original 1.10.2 code, this was done via item damage/meta values.
     * In 1.20.1, equipment variants are stored in the "EquipMeta" NBT tag.
     *
     * @param inv          the ship inventory to search
     * @param item         the item type to match
     * @param requiredMeta the EquipMeta NBT value to match
     * @param startSlot    the first slot index to check (inclusive)
     * @param endSlot      the last slot index to check (inclusive)
     * @return true if a matching item with the correct meta is found
     */
    private static boolean checkItemWithMetaInShipInventory(CapaShipInventory inv, net.minecraft.world.item.Item item,
                                                            int requiredMeta, int startSlot, int endSlot) {
        for (int i = startSlot; i <= endSlot && i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == item) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("EquipMeta") && tag.getInt("EquipMeta") == requiredMeta) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get and remove matching item from IItemHandler.
     */
    private static ItemStack getAndRemoveItemFromHandler(IItemHandler handler, ItemStack target, int amount) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, target)) {
                return handler.extractItem(i, amount, false);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Move item stack into IItemHandler, returns remainder.
     */
    private static void moveItemToHandler(IItemHandler handler, ItemStack stack) {
        for (int i = 0; i < handler.getSlots() && !stack.isEmpty(); i++) {
            ItemStack remainder = handler.insertItem(i, stack, false);
            stack.setCount(remainder.getCount());
        }
    }

    /**
     * Drop item on ground near entity.
     */
    public static void dropItemOnGround(LivingEntity host, ItemStack stack) {
        if (stack.isEmpty() || host.level().isClientSide())
            return;

        ItemEntity item = new ItemEntity(host.level(), host.getX(), host.getY(), host.getZ(), stack.copy());
        item.setDeltaMovement(
                host.getRandom().nextGaussian() * 0.08D,
                host.getRandom().nextGaussian() * 0.05D + 0.2D,
                host.getRandom().nextGaussian() * 0.08D);
        host.level().addFreshEntity(item);
        stack.setCount(0);
    }
}
