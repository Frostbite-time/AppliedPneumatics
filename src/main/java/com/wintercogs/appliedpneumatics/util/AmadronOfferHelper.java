package com.wintercogs.appliedpneumatics.util;

import me.desht.pneumaticcraft.common.amadron.AmadronOfferManager;
import me.desht.pneumaticcraft.common.recipes.amadron.AmadronOffer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 利用反射帮助拿取AmadronOfferManager中的私有字段
 */
public class AmadronOfferHelper
{
    private static Field ACTIVE_OFFERS_FIELD;

    /**
     * 通过 offerId 从 AmadronOfferManager 的 activeOffers 中拿对应的 AmadronOffer。
     */
    @SuppressWarnings("unchecked")
    public static @Nullable AmadronOffer getActiveOffer(ResourceLocation offerId)
    {
        try
        {
            // 第一次调用时初始化反射字段并缓存
            if (ACTIVE_OFFERS_FIELD == null)
            {
                ACTIVE_OFFERS_FIELD = AmadronOfferManager.class.getDeclaredField("activeOffers");
                ACTIVE_OFFERS_FIELD.setAccessible(true);
            }

            AmadronOfferManager manager = AmadronOfferManager.getInstance();
            Map<ResourceLocation, AmadronOffer> activeOffers = (Map<ResourceLocation, AmadronOffer>) ACTIVE_OFFERS_FIELD.get(manager);

            if (activeOffers == null) return null;

            return activeOffers.get(offerId);
        }
        catch (Throwable e)
        {
            return null;
        }
    }
}