package com.wintercogs.appliedpneumatics.common.amadron;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.desht.pneumaticcraft.common.amadron.ImmutableBasket;

import java.lang.reflect.Type;

/**
 * 让ImmutableBasket可以走AE的动作机制进行网络传输的适配器
 */
public final class ImmutableBasketJsonAdapter implements JsonSerializer<ImmutableBasket>, JsonDeserializer<ImmutableBasket>
{

    @Override
    public JsonElement serialize(ImmutableBasket src, Type t, JsonSerializationContext ctx)
    {
        DataResult<JsonElement> r = ImmutableBasket.CODEC.encodeStart(JsonOps.INSTANCE, src);
        return r.result().orElseThrow(() -> new JsonParseException(err(r)));
    }

    @Override
    public ImmutableBasket deserialize(JsonElement json, Type t, JsonDeserializationContext ctx)
            throws JsonParseException
    {
        DataResult<ImmutableBasket> r = ImmutableBasket.CODEC.parse(JsonOps.INSTANCE, json);
        return r.result().orElseThrow(() -> new JsonParseException(err(r)));
    }

    private static String err(DataResult<?> r)
    {
        return r.error().map(e -> e.message()).orElse("ImmutableBasket CODEC <-> JSON failed");
    }
}