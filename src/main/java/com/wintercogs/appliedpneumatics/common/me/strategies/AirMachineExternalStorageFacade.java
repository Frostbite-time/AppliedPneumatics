package com.wintercogs.appliedpneumatics.common.me.strategies;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.localization.GuiText;
import appeng.me.storage.ExternalStorageFacade;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.util.APMath;
import com.wintercogs.appliedpneumatics.util.AirHandlerHelper;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class AirMachineExternalStorageFacade extends ExternalStorageFacade
{
    private final IAirHandlerMachine airHandler;

    public AirMachineExternalStorageFacade(IAirHandlerMachine airHandler)
    {
        this.airHandler = airHandler;
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source)
    {
        return false;
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public @Nullable GenericStack getStackInSlot(int slot)
    {
        return new GenericStack(AirKey.INSTANCE, airHandler.getAir());
    }

    @Override
    public AEKeyType getKeyType()
    {
        return AirKeyType.INSTANCE;
    }

    @Override
    protected int insertExternal(AEKey what, int amount, Actionable mode)
    {
        if (!(what instanceof AirKey)) return 0;
        // 最大余量与意图插入数的最小值
        long space = Math.max(0, AirHandlerHelper.getMaxAirInPressure(airHandler) - airHandler.getAir());
        long wantInsert = Math.min(space, amount);
        int maxInsert = APMath.ClampToInt(wantInsert);
        if (!mode.isSimulate() && maxInsert > 0)
            airHandler.addAir(maxInsert);
        return maxInsert;
    }

    @Override
    protected int extractExternal(AEKey what, int amount, Actionable mode)
    {
        if (!(what instanceof AirKey)) return 0;

        long available = Math.max(0, airHandler.getAir());
        long wantExtract = Math.min(available, amount);
        int maxExtract = APMath.ClampToInt(wantExtract);
        if (!mode.isSimulate() && maxExtract > 0)
            airHandler.addAir(-maxExtract);
        return maxExtract;
    }

    @Override
    public boolean containsAnyFuzzy(Set<AEKey> keys)
    {
        return keys.contains(AirKey.INSTANCE);
    }

    @Override
    public void getAvailableStacks(KeyCounter out)
    {
        super.getAvailableStacks(out);
        out.add(AirKey.INSTANCE, airHandler.getAir());
    }

    @Override
    public Component getDescription()
    {
        return GuiText.ExternalStorage.text(AirKeyType.INSTANCE.getDescription());
    }
}
