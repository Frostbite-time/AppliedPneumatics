package com.wintercogs.appliedpneumatics.common.me.strategies;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.storage.MEStorage;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

// 存储总线等使用的逻辑
public class AirExternalStorageStrategy implements ExternalStorageStrategy
{
    private final ServerLevel serverLevel;
    private final BlockPos blockPos;
    private final Direction side;

    public AirExternalStorageStrategy(ServerLevel serverLevel, BlockPos blockPos, Direction side)
    {
        this.serverLevel = serverLevel;
        this.blockPos = blockPos;
        this.side = side;
    }

    @Override
    public @Nullable MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback)
    {
        BlockEntity be = serverLevel.getBlockEntity(blockPos);
        if (be == null) return null;

        return be.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, side)
                .map(AirMachineExternalStorageFacade::new)
                .orElse(null);
    }
}
