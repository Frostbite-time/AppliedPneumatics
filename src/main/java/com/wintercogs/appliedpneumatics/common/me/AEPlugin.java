package com.wintercogs.appliedpneumatics.common.me;

import appeng.api.behaviors.*;
import appeng.api.features.GridLinkables;
import appeng.api.features.P2PTunnelAttunement;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.parts.automation.StackWorldBehaviors;
import appeng.parts.automation.StorageExportStrategy;
import appeng.parts.automation.StorageImportStrategy;
import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.items.AirStorageCell;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import com.wintercogs.appliedpneumatics.common.items.PortableAirStorageCell;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import com.wintercogs.appliedpneumatics.common.me.storage.AirCellHandler;
import com.wintercogs.appliedpneumatics.common.me.strategies.AirContainerItemStrategy;
import com.wintercogs.appliedpneumatics.common.me.strategies.AirExternalStorageStrategy;
import com.wintercogs.appliedpneumatics.common.me.strategies.AirHandlerStrategy;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.RegistryObject;

public class AEPlugin
{
    public static String CELL_UPGRADE_GROUP = "appliedpneumatics.group.cell_upgrade";
    public static String PORTABLE_CELL_UPGRADE_GROUP = "appliedpneumatics.group.portable_cell_upgrade";

    /**
     * 必须在早期注册的项目使用init
     */
    public static void init()
    {
        AEKeyTypes.register(AirKeyType.INSTANCE);
        GenericSlotCapacities.register(AirKeyType.INSTANCE, 64000L); // 作为非气压容器的普通me接口，每槽位最多64000ml空气
    }

    /**
     * 在CommonSetup中注册的放在此处
     */
    public static void register()
    {
        // 气体专用存储元件
        StorageCells.addCellHandler(new AirCellHandler());
        // 存储总线
        ExternalStorageStrategy.register(AirKeyType.INSTANCE, AirExternalStorageStrategy::new);
        // 输入总线
        StackWorldBehaviors.registerImportStrategy(AirKeyType.INSTANCE, AEPlugin::createAirImport);
        // 输出总线
        StackWorldBehaviors.registerExportStrategy(AirKeyType.INSTANCE, AEPlugin::createAirExport);
        // PickupStrategy与PlacementStrategy不予注册（空气不可能出现在世界中与破坏或者成型面板交互）

        // UI中与物品交互的逻辑
        ContainerItemStrategy.register(AirKeyType.INSTANCE, AirKey.class, new AirContainerItemStrategy());

        // p2p协调
        P2PTunnelAttunement.registerAttunementApi(APItems.AIR_P2P_TUNEL.get(), PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY, Component.translatable("appliedpneumatics.pneumatic"));
        P2PTunnelAttunement.registerAttunementTag(APItems.HEAT_P2P_TUNEL.get()); // 用P2PTunnelAttunement.getAttunementTag(APItems.HEAT_P2P_TUNEL.get());获取此标签来标记物品

        // 升级卡支持
        // ME气压接口
        Upgrades.add(APItems.VOLUME_CARD.get(), APBlocks.ME_PRESSURE_INTERFACE_BLOCK.get(), 4);
        Upgrades.add(APItems.VACUUM_CARD.get(), APBlocks.ME_PRESSURE_INTERFACE_BLOCK.get(), 1);
        Upgrades.add(APItems.SECURITY_CARD.get(), APBlocks.ME_PRESSURE_INTERFACE_BLOCK.get(), 1);

        // ME温控接口
        Upgrades.add(APItems.VOLUME_CARD.get(), APBlocks.ME_TEMPERATURE_INTERFACE.get(), 4);
        Upgrades.add(AEItems.SPEED_CARD, APBlocks.ME_TEMPERATURE_INTERFACE.get(), 4);

        // 存储元件支持的升级卡（安全卡、真空卡） 其中真空卡为气体版溢出销毁卡
        for (RegistryObject<AirStorageCell> cell : APItems.getCELLS())
        {
            Upgrades.add(APItems.SECURITY_CARD.get(), cell.get(), 1, CELL_UPGRADE_GROUP);
            Upgrades.add(APItems.VACUUM_CARD.get(), cell.get(), 1, CELL_UPGRADE_GROUP);
        }
        // 便携气体元件支持的升级卡（安全卡、真空卡、能量卡、充气卡）
        for (RegistryObject<PortableAirStorageCell> portableCell : APItems.getPortableCells())
        {
            Upgrades.add(APItems.SECURITY_CARD.get(), portableCell.get(), 1, PORTABLE_CELL_UPGRADE_GROUP);
            Upgrades.add(APItems.VACUUM_CARD.get(), portableCell.get(), 1, PORTABLE_CELL_UPGRADE_GROUP);
            Upgrades.add(AEItems.ENERGY_CARD, portableCell.get(), 2, PORTABLE_CELL_UPGRADE_GROUP);
            Upgrades.add(APItems.CHARGING_CARD.get(), portableCell.get(), 1, PORTABLE_CELL_UPGRADE_GROUP);
        }

        // 亚马龙终端
        Upgrades.add(AEItems.ENERGY_CARD, APItems.AMADRON_WIRELESS_TERMINAL.get(), 2);

        // 亚马龙处理站
        Upgrades.add(AEItems.SPEED_CARD, APBlocks.ME_AMADRON_PROCESS_STATION.get(), 4);
        Upgrades.add(AEItems.SPEED_CARD, APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION.get(), 4);

        // 添加无线终端绑定支持
        GridLinkables.register(APItems.AMADRON_WIRELESS_TERMINAL.get(), AmadronWirelessTerminalItem.LINKABLE_HANDLER);

    }

    public static StackImportStrategy createAirImport(ServerLevel level, BlockPos fromPos, Direction fromSide)
    {
        return new StorageImportStrategy<>(
                PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY,
                AirHandlerStrategy.INSTANCE,
                level,
                fromPos,
                fromSide
        );
    }

    public static StackExportStrategy createAirExport(ServerLevel level, BlockPos fromPos, Direction fromSide)
    {
        // 不知道为什么1.20.1的StorageExportStrategy的可见性被设为protected
        // 但是对比了其与1.21.1版本的实现，继续用使用应该没有问题，这里使用一个最小继承来绕过
        return new StorageExportStrategy<>(
                PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY,
                AirHandlerStrategy.INSTANCE,
                level,
                fromPos,
                fromSide
        )
        {
        };
    }
}
