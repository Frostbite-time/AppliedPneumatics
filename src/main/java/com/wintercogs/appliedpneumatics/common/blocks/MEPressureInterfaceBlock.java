package com.wintercogs.appliedpneumatics.common.blocks;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEPressureInterfaceBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * ME气压接口
 */
public class MEPressureInterfaceBlock extends AEBaseEntityBlock<MEPressureInterfaceBlockEntity>
{

    public MEPressureInterfaceBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        super.use(state, level, pos, player, hand, hitResult);
        if(!level.isClientSide()&&!player.isShiftKeyDown())
        {
            if(level.getBlockEntity(pos) instanceof MEPressureInterfaceBlockEntity be)
                MenuOpener.open(APMenus.ME_PRESSURE_INTERFACE_MENU.get(), player, MenuLocators.forBlockEntity(be));
        }
        return InteractionResult.SUCCESS;
    }
}
