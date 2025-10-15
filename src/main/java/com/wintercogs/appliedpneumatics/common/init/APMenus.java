package com.wintercogs.appliedpneumatics.common.init;

import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.MEStorageMenu;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEPressureInterfaceBlockEntity;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.METemperatureInterfaceBlockEntity;
import com.wintercogs.appliedpneumatics.common.menu.AmadronWirelessTerminalMenu;
import com.wintercogs.appliedpneumatics.common.menu.MEAmadronProcessStationMenu;
import com.wintercogs.appliedpneumatics.common.menu.MEPressureInterfaceMenu;
import com.wintercogs.appliedpneumatics.common.menu.METemperatureInterfaceMenu;
import com.wintercogs.appliedpneumatics.common.menu.host.AmadronWirelessTerminalMenuHost;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class APMenus
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, AppliedPneumatics.MODID);

    // ae在1.20.1中的MenuTypeBuilder只能使用自己的命名空间，即使带有方块实体的特殊命名也有比较低的冲突风险
    // 但是我在这里想保持注册名和1.21.1版本一致，所以不加前缀区分了
    public static final Supplier<MenuType<MEPressureInterfaceMenu>> ME_PRESSURE_INTERFACE_MENU = MENU_TYPES.register("me_pressure_interface_menu",
            () -> MenuTypeBuilder.create(MEPressureInterfaceMenu::new, MEPressureInterfaceBlockEntity.class)
                    .build("me_pressure_interface")
    );

    public static final Supplier<MenuType<MEAmadronProcessStationMenu>> ME_AMADRON_PROCESS_STATION_MENU = MENU_TYPES.register("me_amadron_process_menu",
            () -> MenuTypeBuilder.create(MEAmadronProcessStationMenu::new, MEAmadronProcessStationBlockEntity.class)
                    .build("me_amadron_process_menu")
    );

    public static final Supplier<MenuType<AmadronWirelessTerminalMenu>> AMADRON_WIRELESS_TERMINAL_MENU = MENU_TYPES.register("amadron_wireless_terminal_menu",
            () -> MenuTypeBuilder.create(AmadronWirelessTerminalMenu::new, AmadronWirelessTerminalMenuHost.class)
                    .withMenuTitle(host -> Component.translatable("menu.title.appliedpneumatics.amadron_wireless_terminal"))
                    .build("amadron_wireless_terminal_menu")
    );

    public static final Supplier<MenuType<METemperatureInterfaceMenu>> ME_TEMPERATURE_INTERFACE_MENU = MENU_TYPES.register("me_temperature_interface_menu",
            () -> MenuTypeBuilder.create(METemperatureInterfaceMenu::new, METemperatureInterfaceBlockEntity.class)
                    .build("me_temperature_interface_menu")
    );

    public static final MenuType<MEStorageMenu> PORTABLE_AIR_CELL_TYPE = MenuTypeBuilder
            .<MEStorageMenu, IPortableTerminal>create(MEStorageMenu::new, IPortableTerminal.class)
            .build("portable_air_cell");

    public static void registerMenus(IEventBus eventBus)
    {
        MENU_TYPES.register(eventBus);
    }
}
