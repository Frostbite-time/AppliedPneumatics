package com.wintercogs.appliedpneumatics.common.me.strategies;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.util.APMath;
import com.wintercogs.appliedpneumatics.util.AirHandlerHelper;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerItem;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public class AirContainerItemStrategy implements ContainerItemStrategy<AirKey, AirContainerItemStrategy.Context>
{

    @Override
    public @Nullable GenericStack getContainedStack(ItemStack stack)
    {
        return stack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY)
                .filter(handler -> handler.getAir() > 0)
                .map(handler -> new GenericStack(AirKey.INSTANCE, handler.getAir()))
                .orElse(null);
    }

    @Override
    public @Nullable AirContainerItemStrategy.Context findCarriedContext(Player player, AbstractContainerMenu menu)
    {
        if (menu.getCarried().getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).isPresent())
        {
            return new AirContainerItemStrategy.CarriedContext(player, menu);
        }
        return null;
    }

    @Override
    public @Nullable AirContainerItemStrategy.Context findPlayerSlotContext(Player player, int slot)
    {
        if (player.getInventory().getItem(slot).getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).isPresent())
        {
            return new AirContainerItemStrategy.PlayerInvContext(player, slot);
        }

        return null;
    }

    @Override
    public long extract(AirContainerItemStrategy.Context context, AirKey what, long amount, Actionable mode)
    {
        if (amount <= 0) return 0;

        ItemStack stack = context.getStack();
        ItemStack copy  = stack.copyWithCount(1);

        LazyOptional<IAirHandlerItem> opt = copy.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY);

        int maxExtract = opt
                .map(handler ->
                {
                    long available = Math.max(0L, handler.getAir());
                    long want = Math.min(available, amount);
                    return APMath.ClampToInt(want);
                })
                .orElse(0);

        if (!mode.isSimulate() && maxExtract > 0)
        {
            stack.shrink(1);
            opt.ifPresent(handler -> handler.addAir(-maxExtract));
            context.addOverflow(copy);
        }

        return maxExtract;
    }

    @Override
    public long insert(AirContainerItemStrategy.Context context, AirKey what, long amount, Actionable mode)
    {
        if (amount <= 0) return 0;

        ItemStack stack = context.getStack();
        ItemStack copy  = stack.copyWithCount(1);

        LazyOptional<IAirHandlerItem> opt = copy.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY);

        int maxInsert = opt
                .map(handler ->
                {
                    long capacity = AirHandlerHelper.getMaxAirInAirHandler(handler);
                    long space = Math.max(0L, capacity - handler.getAir());
                    long want = Math.min(space, amount);
                    return APMath.ClampToInt(want);
                })
                .orElse(0);

        if (!mode.isSimulate() && maxInsert > 0)
        {
            stack.shrink(1);
            opt.ifPresent(handler -> handler.addAir(maxInsert));
            context.addOverflow(copy);
        }

        return maxInsert;
    }

    @Override
    public void playFillSound(Player player, AirKey what)
    {
        var fillSound = Fluids.WATER.getFluidType().getSound(player, SoundActions.BUCKET_FILL);
        if (fillSound != null)
        {
            player.playNotifySound(fillSound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public void playEmptySound(Player player, AirKey what)
    {
        var fillSound = Fluids.WATER.getFluidType().getSound(player, SoundActions.BUCKET_EMPTY);
        if (fillSound != null)
        {
            player.playNotifySound(fillSound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public @Nullable GenericStack getExtractableContent(AirContainerItemStrategy.Context context)
    {
        return getContainedStack(context.getStack());
    }

    interface Context {
        ItemStack getStack();

        void setStack(ItemStack stack);

        void addOverflow(ItemStack stack);
    }

    private record CarriedContext(Player player, AbstractContainerMenu menu) implements Context
    {
        @Override
        public ItemStack getStack() {
            return menu.getCarried();
        }

        @Override
        public void setStack(ItemStack stack) {
            menu.setCarried(stack);
        }

        public void addOverflow(ItemStack stack) {
            if (menu.getCarried().isEmpty()) {
                menu.setCarried(stack);
            } else {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    private record PlayerInvContext(Player player, int slot) implements Context
    {
        @Override
        public ItemStack getStack() {
            return player.getInventory().getItem(slot);
        }

        @Override
        public void setStack(ItemStack stack) {
            player.getInventory().setItem(slot, stack);
        }

        public void addOverflow(ItemStack stack) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }
}
