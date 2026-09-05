package com.wintercogs.appliedpneumatics.common.items;

import appeng.api.inventories.InternalInventory;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class AmadronProcessUpgradeItem extends Item
{
    public AmadronProcessUpgradeItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context)
    {
        if (context.getLevel().isClientSide())
            return InteractionResult.PASS;
        else
        {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            Player player = context.getPlayer();

            if (player == null) return InteractionResult.PASS;
            if (!(blockEntity instanceof MEAmadronProcessStationBlockEntity be)) return InteractionResult.PASS;

            if (be.getBlockState().getBlock() == APBlocks.ME_AMADRON_PROCESS_STATION.get())
            {
                if (be.getJobAmount() > 0 || !be.getInputInv().isEmpty() || !be.getOutputInv().isEmpty())
                {
                    player.sendSystemMessage(Component.translatable("tooltip.appliedpneumatics.amadron_upgrade.amadron_process_busy"));
                    return InteractionResult.PASS;
                }

                // 收集样板
                InternalInventory patternInv = be.getTerminalPatternInventory();
                List<ItemStack> patterns = new ArrayList<>(patternInv.size());
                for (int i = 0; i < patternInv.size(); i++)
                {
                    patterns.add(i, patternInv.getStackInSlot(i).copy());
                }
                patternInv.clear();
                be.cancelAllJobs(Component.empty());

                level.setBlockAndUpdate(pos, APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION.get().defaultBlockState());
                BlockEntity newBlockEntity = level.getBlockEntity(pos);
                if (newBlockEntity instanceof MEAmadronProcessStationBlockEntity newBE)
                {
                    for (int i = 0; i < patterns.size(); i++)
                    {
                        ItemStack stack = patterns.get(i);
                        if (stack != null && !stack.isEmpty())
                            newBE.getTerminalPatternInventory().insertItem(i, stack, false);
                    }
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }
    }
}
