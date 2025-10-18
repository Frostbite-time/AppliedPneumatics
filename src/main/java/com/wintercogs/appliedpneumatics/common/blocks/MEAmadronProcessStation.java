package com.wintercogs.appliedpneumatics.common.blocks;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MEAmadronProcessStation extends AEBaseEntityBlock<MEAmadronProcessStationBlockEntity>
{
    public MEAmadronProcessStation(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        super.use(state, level, pos, player, hand, hitResult);
        if(!level.isClientSide()&&!player.isShiftKeyDown())
        {
            if(level.getBlockEntity(pos) instanceof MEAmadronProcessStationBlockEntity be)
                MenuOpener.open(APMenus.ME_AMADRON_PROCESS_STATION_MENU.get(), player, MenuLocators.forBlockEntity(be));
        }
        return InteractionResult.SUCCESS;
    }

}
