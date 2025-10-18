package com.wintercogs.appliedpneumatics.common.init.clients;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.init.client.InitScreens;
import appeng.menu.me.common.MEStorageMenu;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.client.gui.AmadronWirelessTerminalGUI;
import com.wintercogs.appliedpneumatics.client.gui.MEAmadronProcessStationGUI;
import com.wintercogs.appliedpneumatics.client.gui.MEPressureInterfaceGUI;
import com.wintercogs.appliedpneumatics.client.gui.METemperatureInterfaceGUI;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import com.wintercogs.appliedpneumatics.common.menu.MEAmadronProcessStationMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AppliedPneumatics.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class APScreens
{
    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            MenuScreens.register(APMenus.ME_PRESSURE_INTERFACE_MENU.get(), MEPressureInterfaceGUI::new);
            MenuScreens.<MEAmadronProcessStationMenu, MEAmadronProcessStationGUI>register(APMenus.ME_AMADRON_PROCESS_STATION_MENU.get(), (menu, inv, title) -> new MEAmadronProcessStationGUI(menu, inv, title, menu.getScreenStyle()));
            MenuScreens.register(APMenus.AMADRON_WIRELESS_TERMINAL_MENU.get(), AmadronWirelessTerminalGUI::new);
            MenuScreens.register(APMenus.ME_TEMPERATURE_INTERFACE_MENU.get(), METemperatureInterfaceGUI::new);
            InitScreens.<MEStorageMenu, MEStorageScreen<MEStorageMenu>>register(
                    APMenus.PORTABLE_AIR_CELL_TYPE,
                    MEStorageScreen::new,
                    "/screens/terminals/portable_air_cell.json"
            );
        });
    }
}
