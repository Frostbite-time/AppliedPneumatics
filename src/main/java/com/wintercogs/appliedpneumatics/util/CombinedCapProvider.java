package com.wintercogs.appliedpneumatics.util;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CombinedCapProvider implements ICapabilitySerializable<CompoundTag>
{
    private final @Nullable ICapabilityProvider parent;
    private final ICapabilityProvider child;

    public CombinedCapProvider(@Nullable ICapabilityProvider parent, ICapabilityProvider child)
    {
        this.parent = parent;
        this.child = child;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        // 先尝试子类新增的能力；没有再问父类
        LazyOptional<T> mine = child.getCapability(cap, side);
        if (mine.isPresent()) return mine;
        return parent != null ? parent.getCapability(cap, side) : LazyOptional.empty();
    }

    // 若双方都可序列化：把两边的 NBT 分别收/放到不同子标签，避免键冲突
    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag out = new CompoundTag();
        if (child instanceof ICapabilitySerializable<?> s1) out.put("child", ((ICapabilitySerializable<CompoundTag>) s1).serializeNBT());
        if (parent instanceof ICapabilitySerializable<?> s2) out.put("parent", ((ICapabilitySerializable<CompoundTag>) s2).serializeNBT());
        return out;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt)
    {
        if (child instanceof ICapabilitySerializable<?> s1) ((ICapabilitySerializable<CompoundTag>) s1).deserializeNBT(nbt.getCompound("child"));
        if (parent instanceof ICapabilitySerializable<?> s2) ((ICapabilitySerializable<CompoundTag>) s2).deserializeNBT(nbt.getCompound("parent"));
    }
}