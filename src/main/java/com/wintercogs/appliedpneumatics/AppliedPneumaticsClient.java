package com.wintercogs.appliedpneumatics;

import com.wintercogs.appliedpneumatics.client.me.AEClientPlugin;
import net.minecraftforge.eventbus.api.IEventBus;

public class AppliedPneumaticsClient
{
    public static void clientInit()
    {

    }

    public static void clientCommonSetup()
    {
        AEClientPlugin.register();
        AppliedPneumatics.LOGGER.info("AppliedPneumatics client side setup");
    }

    public static void clientRegister(IEventBus modBus, IEventBus gameBus)
    {
        AEClientPlugin.registerStorageLED(modBus);
    }
}
