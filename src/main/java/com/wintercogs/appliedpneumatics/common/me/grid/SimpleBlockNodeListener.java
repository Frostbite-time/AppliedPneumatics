package com.wintercogs.appliedpneumatics.common.me.grid;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SimpleBlockNodeListener implements IGridNodeListener<BlockEntity>
{
    public static final SimpleBlockNodeListener INSTANCE = new SimpleBlockNodeListener();

    @Override
    public void onSaveChanges(BlockEntity owner, IGridNode node)
    {
        owner.setChanged();
    }

    @Override
    public void onStateChanged(BlockEntity owner,
                               IGridNode node,
                               IGridNodeListener.State state)
    {
        if (owner.getLevel() instanceof ServerLevel sl)
        {
            owner.setChanged();
            sl.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3);
        }
    }

    @Override
    public void onInWorldConnectionChanged(BlockEntity owner, IGridNode node)
    {
        if (owner.getLevel() instanceof ServerLevel sl)
        {
            sl.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3);
        }
    }

    @Override
    public void onOwnerChanged(BlockEntity owner, IGridNode node)
    {
        owner.setChanged();
    }

    @Override
    public void onGridChanged(BlockEntity owner, IGridNode node)
    {
        // 注意：此时不要立即信任网格状态；如需处理，延后到常规逻辑/首 tick。
    }
}
