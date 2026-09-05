package com.wintercogs.appliedpneumatics.common.me.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record EncodedAmadronPattern(ResourceLocation offerId)
{
    public static final Codec<EncodedAmadronPattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("offerId").forGetter(EncodedAmadronPattern::offerId)
    ).apply(instance, EncodedAmadronPattern::new));

    public static final StreamCodec<FriendlyByteBuf, EncodedAmadronPattern> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    EncodedAmadronPattern::offerId,
                    EncodedAmadronPattern::new
            );

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
