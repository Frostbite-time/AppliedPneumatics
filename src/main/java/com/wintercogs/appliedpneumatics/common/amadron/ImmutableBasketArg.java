package com.wintercogs.appliedpneumatics.common.amadron;

import com.google.gson.annotations.JsonAdapter;
import me.desht.pneumaticcraft.common.amadron.ImmutableBasket;

/**
 * 包装一层，让ImmutableBasket能被gson识别
 */
public record ImmutableBasketArg(
        @JsonAdapter(ImmutableBasketJsonAdapter.class)
        ImmutableBasket basket
)
{
}
