package com.wintercogs.appliedpneumatics.common.air;

import com.wintercogs.appliedpneumatics.common.items.IAirStorageCell;
import com.wintercogs.appliedpneumatics.util.APMath;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerItem;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

/** 用于将PortableAirStorageCell包装为IAirHandler能力用于暴露 */
public class PortableAirCellItemStackHandler implements IAirHandlerItem
{
    private final ItemStack container;
    private final IAirStorageCell storageCell;

    public PortableAirCellItemStackHandler(ItemStack container)
    {
        Validate.isTrue(container.getItem() instanceof IAirStorageCell, "itemstack " + String.valueOf(container) + " must be an IAirStorageCell!", new Object[0]);
        this.storageCell = (IAirStorageCell)container.getItem();
        this.container = container;
    }

    @Override
    public @NotNull ItemStack getContainer()
    {
        return container;
    }

    @Override
    public float getPressure()
    {
        return (float) IAirStorageCell.getStoredAir(container) / ((storageCell.getTotalBytes() * IAirStorageCell.amountPerByte()) / maxPressure());
    }

    @Override
    public int getAir()
    {
        return APMath.ClampToInt(IAirStorageCell.getStoredAir(container));
    }

    @Override
    public void addAir(int amount)
    {
        long wanted = IAirStorageCell.getStoredAir(container) + amount;
        long actual = Math.min(Math.max(0, wanted), storageCell.getTotalBytes() * IAirStorageCell.amountPerByte());
        container.set(APDataComponents.AIR_STORED, actual);
    }

    @Override
    public int getBaseVolume()
    {
        return (int) ((storageCell.getTotalBytes() * IAirStorageCell.amountPerByte()) / maxPressure());
    }

    @Override
    public void setBaseVolume(int size) {}

    @Override
    public int getVolume()
    {
        return getBaseVolume();
    }

    @Override
    public float maxPressure()
    {
        return 20;
    }
}
