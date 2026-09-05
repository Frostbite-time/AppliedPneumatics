package com.wintercogs.appliedpneumatics.common.me.crafting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record EncodedAmadronPattern(ResourceLocation offerId)
{
    public CompoundTag toNBT()
    {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("offerId", offerId.toString());
        return nbt;
    }

    public static EncodedAmadronPattern fromNBT(CompoundTag nbt)
    {
        String offerId = nbt.getString("offerId");
        return new EncodedAmadronPattern(new ResourceLocation(offerId));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj instanceof EncodedAmadronPattern pattern)
            return offerId.equals(pattern.offerId);
        return false;
    }

    @Override
    public int hashCode()
    {
        return offerId.hashCode();
    }
}
