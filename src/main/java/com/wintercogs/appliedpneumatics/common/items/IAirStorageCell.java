package com.wintercogs.appliedpneumatics.common.items;

import appeng.api.storage.cells.CellState;
import appeng.api.upgrades.IUpgradeableItem;
import com.wintercogs.appliedpneumatics.common.init.APDataComponents;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import net.minecraft.world.item.ItemStack;

public interface IAirStorageCell extends IUpgradeableItem
{
    int getTotalBytes();

    double getIdleDrain();

    static long getStoredAir(ItemStack stack)
    {
        return stack.getOrDefault(APDataComponents.AIR_STORED, 0L);
    }

    static long amountPerByte()
    {
        return AirKeyType.INSTANCE.getAmountPerByte();
    }

    static long usedBytes(long storedAir)
    {
        long apb = amountPerByte();
        return apb <= 0 ? 0 : (storedAir + apb - 1) / apb;
    }

    static long freeBytes(int totalBytes, long usedBytes)
    {
        long f = totalBytes - usedBytes;
        return Math.max(0, f);
    }

    static long unusedInCurrentByte(long storedAir)
    {
        long apb = amountPerByte();
        if (apb <= 0) return 0;
        long mod = storedAir % apb;
        return mod == 0 ? 0 : (apb - mod);
    }

    static long remainingAmount(int totalBytes, long storedAir)
    {
        long apb = amountPerByte();
        if (apb <= 0) return 0;
        long fb = freeBytes(totalBytes, usedBytes(storedAir));
        return fb * apb + unusedInCurrentByte(storedAir);
    }

    static CellState calcState(int totalBytes, long storedAir)
    {
        if (storedAir <= 0) return CellState.EMPTY;
        return remainingAmount(totalBytes, storedAir) > 0 ? CellState.TYPES_FULL : CellState.FULL;
    }
}
