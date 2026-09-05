package com.wintercogs.appliedpneumatics.common.me.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.parts.AEBasePart;
import appeng.util.Platform;
import com.wintercogs.appliedpneumatics.common.eventlistner.APDelayedBreaker;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.items.IAirStorageCell;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;


public class AirCellInventory implements StorageCell
{

    private long storedAir; // 内部缓存，操作时立即写回到 itemStack，persist 基本是空操作

    private final ItemStack itemStack; // 实际存储载体
    private final IAirStorageCell cellType;  // 提供 idleDrain / totalBytes / upgrades
    @Nullable
    private final ISaveProvider host;
    @Nullable
    private final APDelayedBreaker.BlockKey hostPos; // 炸了它 XD

    private final boolean hasSecurityUpgrade; // 安全卡升级
    private final boolean hasVacuumUpgrade; // 真空卡升级

    private boolean isPersisted = true;

    public AirCellInventory(ItemStack stack, IAirStorageCell cell, @Nullable ISaveProvider host)
    {
        this.itemStack = stack;
        this.cellType = cell;
        this.host = host; // 记录宿主
        BlockEntity be = tryGetHostBE(host);
        if (be != null && be.getLevel() != null)
            this.hostPos = new APDelayedBreaker.BlockKey(be.getLevel().dimension(), be.getBlockPos());
        else
            this.hostPos = null;

        // 首次读出已存空气量
        this.storedAir = getAirFromStack();

        // StorageCell的构建方法只会在插入驱动器的一瞬间触发
        // 这里一次性获取状态即可
        this.hasSecurityUpgrade = cellType.getUpgrades(itemStack).isInstalled(APItems.SECURITY_CARD.get());
        this.hasVacuumUpgrade = cellType.getUpgrades(itemStack).isInstalled(APItems.VACUUM_CARD.get());
    }


    // StorageCell接口实现-----------------------------------------------------------------
    @Override
    public CellState getStatus()
    {
        if (storedAir <= 0) return CellState.EMPTY;
        return getRemainingAmount() > 0 ? CellState.NOT_EMPTY : CellState.FULL;
    }

    @Override
    public double getIdleDrain()
    {
        return cellType.getIdleDrain();
    }

    @Override
    public void persist()
    {
        if (!isPersisted)
        {
            CompoundTag tag = itemStack.getOrCreateTag();
            tag.putLong(IAirStorageCell.AIR_STORED_TAG, storedAir);
            isPersisted = true;
        }
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source)
    {
        // 偏好AirKey
        // 真空卡存在时取消偏好，让AE先填充没有真空卡的那些元件
        return !hasVacuumUpgrade && what == AirKey.INSTANCE;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        if (amount <= 0 || what != AirKey.INSTANCE) return 0;

        long remaining = getRemainingAmount();
        if (remaining <= 0)
        {
            // 没空间
            if (hasVacuumUpgrade)
            {
                // 真空卡：全部视为接收，不触发破坏
                return amount;
            }
            else
            {
                // 无真空卡：如果也没有安全卡，安排炸来源机器（仅 MODULATE）
                if (!hasSecurityUpgrade && mode == Actionable.MODULATE)
                {
                    // 持久化元件，避免掉落前丢数据
                    markChanged();
                    // 延迟炸
                    if (hostPos != null)
                    {
                        APDelayedBreaker.breakNextTick(hostPos.dim(), hostPos.pos());
                    }
                }
                // 返回 0，表示没能存进去（机器将于下一 tick 被破坏）
                return 0;
            }
        }

        long toStore = Math.min(amount, remaining);

        if (mode == Actionable.MODULATE)
        {
            storedAir += toStore;
            markChanged();

            // 处理“溢出的部分”
            long overflow = amount - toStore;
            if (overflow > 0)
            {
                if (hasVacuumUpgrade)
                {
                    // 有真空卡：把溢出当作成功吞掉
                    return amount;
                }
                else if (!hasSecurityUpgrade)
                {
                    // 无真空且无安全卡：安排炸来源机器
                    if (hostPos != null)
                    {
                        // 元件已 markChanged()，可以安全延迟炸
                        APDelayedBreaker.breakNextTick(hostPos.dim(), hostPos.pos());
                    }
                }
                // 有安全卡：不炸，只按上限接收
            }
        }

        // 无真空卡时，按实际接收返回；有真空卡时，上面已在 overflow 分支返回过 amount
        return toStore;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        if (amount <= 0 || what != AirKey.INSTANCE) return 0;
        if (storedAir <= 0) return 0;

        long taken = Math.min(amount, storedAir);

        if (mode == Actionable.MODULATE)
        {
            storedAir -= taken;
            markChanged();
        }
        return taken;
    }

    @Override
    public void getAvailableStacks(KeyCounter out)
    {
        if (storedAir > 0)
        {
            out.add(AirKey.INSTANCE, storedAir);
        }
    }

    @Override
    public Component getDescription()
    {
        return itemStack.getHoverName();
    }

    // 辅助方法---------------------------------------------------------------------------------------------

    /**
     * 写回策略：有 host -> 只通知；无 host -> 直接落盘
     */
    private void markChanged()
    {
        if (Platform.isClient()) return;

        isPersisted = false;
        if (host != null)
        {
            host.saveChanges(); // 交给宿主持久化
        }
        else
        {
            persist(); // 没有宿主就自己写回
        }
    }

    /**
     * 总字节数：来自物品定义
     */
    public long getTotalBytes()
    {
        return cellType.getTotalBytes();
    }

    /**
     * 每个字节可存的空气量
     */
    public long getAmountPerByte()
    {
        return AirKeyType.INSTANCE.getAmountPerByte();
    }

    /**
     * 已用字节 = ceil(storedAir / amountPerByte)
     */
    public long getUsedBytes()
    {
        long apb = getAmountPerByte();
        return (storedAir + apb - 1) / apb;
    }

    public long getFreeBytes()
    {
        long free = getTotalBytes() - getUsedBytes();
        return Math.max(0, free);
    }

    /**
     * 当前未占满的那个字节里还能塞多少空气（0 表示正好卡边界或为空）
     */
    public long getUnusedInCurrentByte()
    {
        long apb = getAmountPerByte();
        long mod = storedAir % apb;
        return (mod == 0) ? 0 : (apb - mod);
    }

    /**
     * 还能存多少空气，当前半字节空隙 + 剩余字节完整容量
     */
    public long getRemainingAmount()
    {
        long apb = getAmountPerByte();
        return getFreeBytes() * apb + getUnusedInCurrentByte();
    }

    public long getAirFromStack()
    {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (tag.contains(IAirStorageCell.AIR_STORED_TAG))
        {
            return tag.getLong(IAirStorageCell.AIR_STORED_TAG);
        }
        writeAirToStack(0);
        return 0;
    }

    private void writeAirToStack(long value)
    {
        long air = Math.max(0, value);
        CompoundTag tag = itemStack.getOrCreateTag();
        tag.putLong(IAirStorageCell.AIR_STORED_TAG, air);
    }

    public static @Nullable BlockEntity tryGetHostBE(ISaveProvider host)
    {
        if (host == null) return null;

        // 1. 直接是 BlockEntity
        if (host instanceof BlockEntity be)
        {
            return be;
        }

        // 2. 是 IPart
        if (host instanceof IPart part)
        {
            if (part instanceof AEBasePart aeBasePart)
            {
                return aeBasePart.getBlockEntity();
            }
            else
            {
                IGridNode node = part.getGridNode();
                if (node != null)
                {
                    if (node.getOwner() instanceof BlockEntity be)
                        return be;
                    else if (node instanceof AEBasePart aeBasePart)
                        return aeBasePart.getBlockEntity();
                }
            }
        }

        // 3. 是 IActionHost
        if (host instanceof IActionHost actionHost)
        {
            IGridNode node = actionHost.getActionableNode();
            if (node != null)
            {
                if (node.getOwner() instanceof BlockEntity be)
                    return be;
                else if (node instanceof AEBasePart aeBasePart)
                    return aeBasePart.getBlockEntity();
            }
        }

        // 4. Lambda 情况：反射查找捕获字段
        Class<?> hostClass = host.getClass();
        try
        {
            for (Field field : hostClass.getDeclaredFields())
            {
                field.setAccessible(true);
                Object value = field.get(host);
                if (value == null) continue;

                // 递归解析
                if (value instanceof BlockEntity be)
                {
                    return be;
                }
                if (value instanceof IPart part)
                {
                    if (part instanceof AEBasePart aeBasePart)
                    {
                        return aeBasePart.getBlockEntity();
                    }
                    else
                    {
                        IGridNode node = part.getGridNode();
                        if (node != null)
                        {
                            if (node.getOwner() instanceof BlockEntity be)
                                return be;
                            else if (node instanceof AEBasePart aeBasePart)
                                return aeBasePart.getBlockEntity();
                        }
                    }
                }
                if (value instanceof IActionHost actionHost)
                {
                    IGridNode node = actionHost.getActionableNode();
                    if (node != null)
                    {
                        if (node.getOwner() instanceof BlockEntity be)
                            return be;
                        else if (node instanceof AEBasePart aeBasePart)
                            return aeBasePart.getBlockEntity();
                    }
                }
            }
        }
        catch (Throwable ignored)
        {
            // 出错就返回 null
        }

        return null;
    }

}
