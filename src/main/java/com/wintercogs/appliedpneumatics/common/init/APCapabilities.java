package com.wintercogs.appliedpneumatics.common.init;


import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEPressureInterfaceBlockEntity;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.METemperatureInterfaceBlockEntity;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import com.wintercogs.appliedpneumatics.common.items.PortableAirStorageCell;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AppliedPneumatics.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class APCapabilities
{
    @SubscribeEvent
    public static void onRegisterCaps(RegisterCapabilitiesEvent event)
    {
        MEPressureInterfaceBlockEntity.onRegisterCaps(event);
        AmadronWirelessTerminalItem.onRegisterCaps(event);
        MEAmadronProcessStationBlockEntity.onRegisterCaps(event);
        METemperatureInterfaceBlockEntity.onRegisterCaps(event);
        PortableAirStorageCell.onRegisterCaps(event);
    }
}
