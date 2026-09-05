package com.wintercogs.appliedpneumatics.common.me.keys.types;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class AirKeyType extends AEKeyType
{
    public static final Component NAME = Component.translatable("appliedpneumatics.me.key.air");
    private static final ResourceLocation ID = ResourceLocation.tryBuild(AppliedPneumatics.MODID, "air_type");

    public static final AirKeyType INSTANCE = new AirKeyType();

    private AirKeyType()
    {
        super(ID, AirKey.class, NAME);
    }

    // IO端口以1000ml为一次操作标准进行传输
    @Override
    public int getAmountPerOperation()
    {
        return 1000;
    }

    @Override
    public int getAmountPerUnit()
    {
        return 1000;
    }

    // 每字节250ml空气
    @Override
    public int getAmountPerByte()
    {
        return 250;
    }

    @Override
    public @Nullable AEKey readFromPacket(FriendlyByteBuf friendlyByteBuf)
    {
        return AirKey.INSTANCE;
    }

    @Override
    public @Nullable AEKey loadKeyFromTag(CompoundTag compoundTag)
    {
        return AirKey.INSTANCE;
    }

    @Override
    public @Nullable String getUnitSymbol()
    {
        return "L";
    }
}
