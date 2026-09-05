package com.wintercogs.appliedpneumatics.common.me.strategies;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.me.storage.ExternalStorageFacade;
import appeng.parts.automation.HandlerStrategy;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.util.APMath;
import com.wintercogs.appliedpneumatics.util.AirHandlerHelper;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import org.jetbrains.annotations.Nullable;

public final class AirHandlerStrategy extends HandlerStrategy<IAirHandlerMachine, GenericStack>
{
    public static final AirHandlerStrategy INSTANCE = new AirHandlerStrategy();

    private AirHandlerStrategy()
    {
        super(AirKeyType.INSTANCE);
    }

    @Override
    public boolean isSupported(AEKey what)
    {
        return what instanceof AirKey;
    }

    @Override
    public ExternalStorageFacade getFacade(IAirHandlerMachine handler)
    {
        return new AirMachineExternalStorageFacade(handler);
    }

    @Override
    public @Nullable GenericStack getStack(AEKey what, long amount)
    {
        if (what instanceof AirKey && amount > 0) return new GenericStack(what, amount);

        return null;
    }

    @Override
    public long insert(IAirHandlerMachine airHandler, AEKey what, long amount, Actionable mode)
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
}
