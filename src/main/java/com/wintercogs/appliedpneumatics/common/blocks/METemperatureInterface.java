package com.wintercogs.appliedpneumatics.common.blocks;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.METemperatureInterfaceBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APBlockStates;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class METemperatureInterface extends AEBaseEntityBlock<METemperatureInterfaceBlockEntity>
{
    public METemperatureInterface(Properties props)
    {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(APBlockStates.TEMP_STATE, APBlockStates.TemperatureState.ROOM_TEMPERATURE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(APBlockStates.TEMP_STATE);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult)
    {
        super.useWithoutItem(state, level, pos, player, hitResult);
        if (!level.isClientSide() && !player.isShiftKeyDown())
        {
            if (level.getBlockEntity(pos) instanceof METemperatureInterfaceBlockEntity be)
                MenuOpener.open(APMenus.ME_TEMPERATURE_INTERFACE_MENU.get(), player, MenuLocators.forBlockEntity(be));
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }
}
