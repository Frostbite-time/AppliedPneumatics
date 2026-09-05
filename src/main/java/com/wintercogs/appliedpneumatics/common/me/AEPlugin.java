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
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
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
        P2PTunnelAttunement.registerAttunementApi(APItems.AIR_P2P_TUNEL, PNCCapabilities.AIR_HANDLER_ITEM, Component.translatable("appliedpneumatics.pneumatic"));
        P2PTunnelAttunement.registerAttunementTag(APItems.HEAT_P2P_TUNEL); // 用P2PTunnelAttunement.getAttunementTag(APItems.HEAT_P2P_TUNEL.get());获取此标签来标记物品

        // 升级卡支持
        // ME气压接口
        Upgrades.add(APItems.VOLUME_CARD, APBlocks.ME_PRESSURE_INTERFACE_BLOCK, 4);
        Upgrades.add(APItems.VACUUM_CARD, APBlocks.ME_PRESSURE_INTERFACE_BLOCK, 1);
        Upgrades.add(APItems.SECURITY_CARD, APBlocks.ME_PRESSURE_INTERFACE_BLOCK, 1);

        // ME温控接口
        Upgrades.add(APItems.VOLUME_CARD, APBlocks.ME_TEMPERATURE_INTERFACE, 4);
        Upgrades.add(AEItems.SPEED_CARD, APBlocks.ME_TEMPERATURE_INTERFACE, 4);

        // 存储元件支持的升级卡（安全卡、真空卡） 其中真空卡为气体版溢出销毁卡
        // 1k ~ 256M
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_1K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_1K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_4K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_4K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_16K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_16K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_64K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_64K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_256K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_256K, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_1M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_1M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_4M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_4M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_16M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_16M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_64M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_64M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.AIR_CELL_256M, 1, CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.AIR_CELL_256M, 1, CELL_UPGRADE_GROUP);
        // 便携气体元件支持的升级卡（安全卡、真空卡、能量卡、充气卡）
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_1K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_1K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_1K, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_1K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_4K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_4K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_4K, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_4K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_16K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_16K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_16K, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_16K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_64K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_64K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_64K, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_64K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_256K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_256K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_256K, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_256K, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_1M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_1M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_1M, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_1M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_4M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_4M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_4M, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_4M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_16M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_16M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_16M, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_16M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_64M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_64M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_64M, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_64M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.SECURITY_CARD, APItems.PORTABLE_AIR_CELL_256M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.VACUUM_CARD, APItems.PORTABLE_AIR_CELL_256M, 1, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(AEItems.ENERGY_CARD, APItems.PORTABLE_AIR_CELL_256M, 2, PORTABLE_CELL_UPGRADE_GROUP);
        Upgrades.add(APItems.CHARGING_CARD, APItems.PORTABLE_AIR_CELL_256M, 1, PORTABLE_CELL_UPGRADE_GROUP);

        // 亚马龙终端
        Upgrades.add(AEItems.ENERGY_CARD, APItems.AMADRON_WIRELESS_TERMINAL, 2);

        // 亚马龙处理站
        Upgrades.add(AEItems.SPEED_CARD, APBlocks.ME_AMADRON_PROCESS_STATION, 4);
        Upgrades.add(AEItems.SPEED_CARD, APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION, 4);

        // 添加无线终端绑定支持
        GridLinkables.register(APItems.AMADRON_WIRELESS_TERMINAL, AmadronWirelessTerminalItem.LINKABLE_HANDLER);

    }

    public static StackImportStrategy createAirImport(ServerLevel level, BlockPos fromPos, Direction fromSide)
    {
        return new StorageImportStrategy<>(
                PNCCapabilities.AIR_HANDLER_MACHINE,
                AirHandlerStrategy.INSTANCE,
                level,
                fromPos,
                fromSide
        );
    }

    public static StackExportStrategy createAirExport(ServerLevel level, BlockPos fromPos, Direction fromSide)
    {
        return new StorageExportStrategy<>(
                PNCCapabilities.AIR_HANDLER_MACHINE,
                AirHandlerStrategy.INSTANCE,
                level,
                fromPos,
                fromSide
        );
    }
}
