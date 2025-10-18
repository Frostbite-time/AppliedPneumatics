package com.wintercogs.appliedpneumatics.common.amadron;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import me.desht.pneumaticcraft.common.amadron.ShoppingBasket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.lang.reflect.Type;

/** 让ImmutableBasket可以走AE的动作机制进行网络传输的适配器 */
public final class ImmutableBasketJsonAdapter implements JsonSerializer<ShoppingBasket>, JsonDeserializer<ShoppingBasket>
{

    @Override
    public JsonElement serialize(ShoppingBasket src, Type t, JsonSerializationContext ctx) {
        return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, src.toNBT());
    }

    @Override
    public ShoppingBasket deserialize(JsonElement json, Type t, JsonDeserializationContext ctx)
            throws JsonParseException {
        Tag tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
        if (!(tag instanceof CompoundTag compound))
            throw new JsonParseException("ShoppingBasket JSON must map to a CompoundTag.");
        return ShoppingBasket.fromNBT(compound);
    }
}