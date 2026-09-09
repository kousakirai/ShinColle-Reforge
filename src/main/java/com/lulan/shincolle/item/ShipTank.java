package com.lulan.shincolle.item;

import com.lulan.shincolle.capability.CapaFluidContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Ship Tank - fluid/resource tank for ship entities.
 * Original meta types with capacities:
 * 0 = 32000 mB
 * 1 = 128000 mB
 * 2 = 512000 mB
 * 3 = 2048000 mB
 */
public class ShipTank extends BasicItem {

    private final int type;
    private final int capacity;

    public ShipTank() {
        this(0);
    }

    public ShipTank(int type) {
        super(new Properties().stacksTo(1));
        this.type = type;
        switch (type) {
            case 0:
                this.capacity = 32000;
                break;
            case 1:
                this.capacity = 128000;
                break;
            case 2:
                this.capacity = 512000;
                break;
            case 3:
                this.capacity = 2048000;
                break;
            default:
                this.capacity = 32000;
                break;
        }
    }

    public int getType() {
        return this.type;
    }

    public int getTankCapacity() {
        return this.capacity;
    }

    /**
     * Handles a normal right-click on a block.  The server receives the same
     * UseOnContext from vanilla for the actual interaction, so no client
     * supplied block position is trusted here.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos adjacentPos = clickedPos.relative(clickedFace);

        if (!level.mayInteract(player, clickedPos)
                || !player.mayUseItemAt(adjacentPos, clickedFace, stack)) {
            return InteractionResult.FAIL;
        }

        LazyOptional<IFluidHandlerItem> tankOptional = FluidUtil.getFluidHandler(stack);
        if (!tankOptional.isPresent()) {
            return InteractionResult.FAIL;
        }

        // FluidUtil handles vanilla BucketPickup and Forge fluid blocks.  It
        // returns a replacement stack, which must be written back to the
        // actual hand because it operates on a copy.
        FluidActionResult pickup = FluidUtil.tryPickUpFluid(
                stack, player, level, clickedPos, clickedFace);
        if (pickup.isSuccess()) {
            player.setItemInHand(context.getHand(), pickup.getResult());
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        IFluidHandler tank = tankOptional.orElse(null);
        if (tank == null) {
            return InteractionResult.FAIL;
        }

        // Sneaking drains a fluid handler block into the tank; normal use
        // transfers one bucket from the tank into the block.
        LazyOptional<IFluidHandler> blockOptional = FluidUtil.getFluidHandler(
                level, clickedPos, clickedFace);
        if (blockOptional.isPresent()) {
            IFluidHandler blockHandler = blockOptional.orElse(null);
            if (blockHandler != null) {
                FluidStack transferred = player.isShiftKeyDown()
                        ? FluidUtil.tryFluidTransfer(tank, blockHandler,
                                FluidType.BUCKET_VOLUME, true)
                        : FluidUtil.tryFluidTransfer(blockHandler, tank,
                                FluidType.BUCKET_VOLUME, true);
                if (!transferred.isEmpty()) {
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }

        FluidStack simulated = tank.drain(FluidType.BUCKET_VOLUME,
                IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty() || simulated.getAmount() < FluidType.BUCKET_VOLUME) {
            return InteractionResult.FAIL;
        }

        BlockState clickedState = level.getBlockState(clickedPos);
        boolean canContainFluid = clickedState.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(level, clickedPos, clickedState, simulated.getFluid());
        BlockPos placePos = canContainFluid
                ? clickedPos
                : (clickedState.canBeReplaced() && clickedFace == Direction.UP
                        ? clickedPos : adjacentPos);

        if (!level.mayInteract(player, placePos)
                || !player.mayUseItemAt(placePos, clickedFace, stack)) {
            return InteractionResult.FAIL;
        }

        return tryPlaceContainedLiquid(player, level, placePos, context.getHand(), tank)
                ? InteractionResult.sidedSuccess(level.isClientSide())
                : InteractionResult.FAIL;
    }

    /**
     * Handles a right-click with no block hit.  This preserves the legacy
     * one-block-in-front placement without accepting a client-supplied
     * position packet.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos pos = getBlockInFrontOfPlayer(player);
        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, Direction.UP, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        LazyOptional<IFluidHandlerItem> tankOptional = FluidUtil.getFluidHandler(stack);
        IFluidHandler tank = tankOptional.orElse(null);
        if (tank == null || !tryPlaceContainedLiquid(player, level, pos, hand, tank)) {
            return InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static BlockPos getBlockInFrontOfPlayer(Player player) {
        float yaw = player.getYRot() % 360.0F;
        if (yaw > 180.0F) {
            yaw -= 360.0F;
        } else if (yaw < -180.0F) {
            yaw += 360.0F;
        }

        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY() + 1;
        int z = player.blockPosition().getZ();
        if (yaw <= -112.5F && yaw > -157.5F) return new BlockPos(x + 1, y, z - 1);
        if (yaw <= -67.5F && yaw > -112.5F) return new BlockPos(x + 1, y, z);
        if (yaw <= -22.5F && yaw > -67.5F) return new BlockPos(x + 1, y, z + 1);
        if (yaw <= 22.5F && yaw > -22.5F) return new BlockPos(x, y, z + 1);
        if (yaw <= 67.5F && yaw > 22.5F) return new BlockPos(x - 1, y, z + 1);
        if (yaw <= 112.5F && yaw > 67.5F) return new BlockPos(x - 1, y, z);
        if (yaw <= 157.5F && yaw > 112.5F) return new BlockPos(x - 1, y, z - 1);
        return new BlockPos(x, y, z - 1);
    }

    /**
     * Places exactly one bucket of the contained fluid, consuming the tank
     * only after FluidUtil confirms that the destination can accept it.
     */
    public static boolean tryPlaceContainedLiquid(Player player, Level level,
                                                   BlockPos pos, InteractionHand hand,
                                                   IFluidHandler fluidSource) {
        if (fluidSource == null) {
            return false;
        }

        FluidStack simulated = fluidSource.drain(FluidType.BUCKET_VOLUME,
                IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty() || simulated.getAmount() < FluidType.BUCKET_VOLUME) {
            return false;
        }

        return FluidUtil.tryPlaceFluid(player, level, hand, pos, fluidSource, simulated);
    }

    /**
     * Attach fluid capability to this item, enabling it to store fluids.
     */
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ShipTankFluidProvider(stack, this.capacity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.shincolle.shiptank").withStyle(ChatFormatting.GRAY));

        // Show fluid contents and capacity
        String name = "";
        int amount = 0;

        var fluidOpt = FluidUtil.getFluidContained(stack);
        if (fluidOpt.isPresent()) {
            FluidStack fs = fluidOpt.get();
            name = fs.getDisplayName().getString();
            amount = fs.getAmount();
        }

        tooltip.add(Component.literal(ChatFormatting.AQUA + name + ChatFormatting.WHITE +
                " " + amount + " / " + capacity + " mB"));
    }

    /**
     * Capability provider that wraps CapaFluidContainer for fluid handling.
     */
    private static class ShipTankFluidProvider implements ICapabilityProvider {

        private final CapaFluidContainer fluidHandler;
        private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> holder;

        public ShipTankFluidProvider(ItemStack stack, int capacity) {
            this.fluidHandler = new CapaFluidContainer(stack, capacity);
            this.holder = net.minecraftforge.common.util.LazyOptional.of(() -> fluidHandler);
        }

        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> cap,
                @Nullable net.minecraft.core.Direction side) {
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM) {
                return holder.cast();
            }
            return net.minecraftforge.common.util.LazyOptional.empty();
        }
    }
}
