package com.lulan.shincolle.block;

import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

public class BlockWaypoint extends BasicBlockContainer {

    public BlockWaypoint() {
        super(Properties.of().mapColor(MapColor.NONE).strength(1.0F).noOcclusion().noCollission());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileEntityWaypoint waypoint) {
                player.getCapability(CapaTeitokuProvider.CAPABILITY).ifPresent(capa -> {
                    int uid = capa.getPlayerUID();
                    if (uid > 0) {
                        waypoint.setPlayerUID(uid);
                    }
                });
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWaypoint(pos, state);
    }
}
