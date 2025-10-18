package com.wintercogs.appliedpneumatics.common.amadron;

import com.google.gson.annotations.JsonAdapter;
import me.desht.pneumaticcraft.common.amadron.ShoppingBasket;

/**
 * 包装一层，让ShoppingBasket能被gson识别
 * <p>
 * 实际上这里使用的ShoppingBasket不再是不可变元素，但是我懒得改类名了，毕竟只是移植
 */
public record ImmutableBasketArg(
        @JsonAdapter(ImmutableBasketJsonAdapter.class)
        ShoppingBasket basket
) {}
