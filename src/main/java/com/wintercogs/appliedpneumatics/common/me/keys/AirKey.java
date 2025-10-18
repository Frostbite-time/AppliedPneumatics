package com.wintercogs.appliedpneumatics.common.me.keys;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 气动工艺的空气所代表的AEKey
 */
public class AirKey extends AEKey
{
    private static final ResourceLocation ID = ResourceLocation.tryBuild(AppliedPneumatics.MODID,"air_key");

    public static final AirKey INSTANCE = new AirKey();

    private AirKey() {}

    @Override
    public AEKeyType getType()
    {
        return AirKeyType.INSTANCE;
    }

    @Override
    public AEKey dropSecondary()
    {
        return this;
    }

    @Override
    public CompoundTag toTag()
    {
        return new CompoundTag();
    }

    // 按每字节一
    @Override
    public Object getPrimaryKey()
    {
        return this;
    }

    @Override
    public ResourceLocation getId()
    {
        return ID;
    }

    @Override
    public void writeToPacket(FriendlyByteBuf friendlyByteBuf)
    {

    }

    @Override
    protected Component computeDisplayName()
    {
        return AirKeyType.NAME;
    }

    // 无掉落物
    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {}

    @Override
    public int hashCode()
    {
        return AirKey.class.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof AirKey;
    }
}
