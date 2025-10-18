package com.wintercogs.appliedpneumatics.common.items;

import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import com.wintercogs.appliedpneumatics.common.me.crafting.AmadronPatternDetails;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class AmadronPatternItem extends EncodedPatternItem
{
    public AmadronPatternItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable AmadronPatternDetails decode(ItemStack itemStack, Level level, boolean tryRecovery)
    {
        return decode(AEItemKey.of(itemStack), level);
    }

    @Override
    public @Nullable AmadronPatternDetails decode(AEItemKey what, Level level)
    {
        if (what != null && what.hasTag())
        {
            try
            {
                return new AmadronPatternDetails(what);
            }
            catch (Exception e)
            {
                return null; // 静默处理错误
            }
        }
        else
        {
            return null;
        }
    }
}
