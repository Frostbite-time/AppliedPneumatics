package com.wintercogs.appliedpneumatics.common.init;

import appeng.api.config.Actionable;
import appeng.api.upgrades.IUpgradeableItem;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.Config;
import com.wintercogs.appliedpneumatics.common.items.IAirStorageCell;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.function.Supplier;

public class APCreativeModeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AppliedPneumatics.MODID);

    public static final Supplier<CreativeModeTab> AP_CREATIVE_MODE_TAB = CREATIVE_MODE_TAB.register(
            "ap_creative_mode_tab",
            ()->CreativeModeTab.builder()
                    .icon(()->new ItemStack(APBlocks.ME_AMADRON_PROCESS_STATION.get()))
                    .title(Component.translatable("creativetab.appliedpneumatics.items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        // 存储元件
                        output.accept(APItems.AIR_CELL_SHELL.get());
                        if(AppliedPneumatics.MEGA_CELL_LOADED || Config.alwaysShowExtendedContent)
                        {
                            output.accept(APItems.MEGA_AIR_CELL_SHELL.get());
                        }
                        ArrayList<ItemStack> cells = new ArrayList<>();
                        cells.add(new ItemStack(APItems.AIR_CELL_1K.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_4K.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_16K.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_64K.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_256K.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_1M.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_4M.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_16M.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_64M.get()));
                        cells.add(new ItemStack(APItems.AIR_CELL_256M.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_1K.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_4K.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_16K.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_64K.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_256K.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_1M.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_4M.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_16M.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_64M.get()));
                        cells.add(new ItemStack(APItems.PORTABLE_AIR_CELL_256M.get()));
                        for(ItemStack cellStack : cells)
                        {
                            // 空的
                            if(cellStack.getItem() instanceof IAirStorageCell cell)
                            {
                                if(cell.getTotalBytes() <= 270000)
                                {
                                    output.accept(cellStack.copy());
                                }
                                else if(AppliedPneumatics.MEGA_CELL_LOADED || Config.alwaysShowExtendedContent) // 超过256k需要安装mega元件，这里给一些冗余
                                    output.accept(cellStack.copy());
                            }
                            // 满电的
                            if(cellStack.getItem() instanceof PoweredContainerItem poweredContainerItem)
                            {
                                ItemStack copy = cellStack.copy();
                                poweredContainerItem.injectAEPower(copy, Double.MAX_VALUE, Actionable.MODULATE);
                                if(copy.getItem() instanceof IUpgradeableItem upgradeableItem)
                                    upgradeableItem.getUpgrades(copy).addItems(APItems.SECURITY_CARD.toStack());

                                if(copy.getItem() instanceof IAirStorageCell cell)
                                {
                                    if(cell.getTotalBytes() <= 270000)
                                        output.accept(copy);
                                    else if(AppliedPneumatics.MEGA_CELL_LOADED || Config.alwaysShowExtendedContent) // 超过256k需要安装mega元件，这里给一些冗余
                                        output.accept(copy);
                                }
                            }
                            // 满电满空气状态
                            if(cellStack.getItem() instanceof IAirStorageCell cell)
                            {
                                ItemStack copy = cellStack.copy();
                                copy.set(APDataComponents.AIR_STORED, (long) cell.getTotalBytes() * AirKeyType.INSTANCE.getAmountPerByte());
                                if(copy.getItem() instanceof PoweredContainerItem poweredContainerItem)
                                    poweredContainerItem.injectAEPower(copy, Double.MAX_VALUE, Actionable.MODULATE);
                                if(copy.getItem() instanceof IUpgradeableItem upgradeableItem)
                                    upgradeableItem.getUpgrades(copy).addItems(APItems.SECURITY_CARD.toStack());

                                if(cell.getTotalBytes() <= 270000)
                                    output.accept(copy);
                                else if(AppliedPneumatics.MEGA_CELL_LOADED || Config.alwaysShowExtendedContent) // 超过256k需要安装mega元件，这里给一些冗余
                                    output.accept(copy);
                            }
                        }
                        output.accept(APBlocks.ME_PRESSURE_INTERFACE_BLOCK.get());
                        output.accept(APBlocks.ME_TEMPERATURE_INTERFACE.get());
                        output.accept(APItems.AIR_P2P_TUNEL.get());
                        output.accept(APItems.HEAT_P2P_TUNEL.get());
                        output.accept(APItems.VOLUME_CARD.get());
                        output.accept(APItems.SECURITY_CARD.get());
                        output.accept(APItems.VACUUM_CARD.get());
                        output.accept(APItems.CHARGING_CARD.get());

                        // 亚马龙终端以及其满电状态
                        output.accept(APItems.AMADRON_WIRELESS_TERMINAL.get());
                        ItemStack amadronWirelessTerminalFull = new ItemStack(APItems.AMADRON_WIRELESS_TERMINAL.get());
                        ((PoweredContainerItem)amadronWirelessTerminalFull.getItem()).injectAEPower(amadronWirelessTerminalFull, Double.MAX_VALUE, Actionable.MODULATE);
                        output.accept(amadronWirelessTerminalFull);

                        output.accept(APBlocks.ME_AMADRON_PROCESS_STATION.get());
                        if(AppliedPneumatics.EAE_LOADED || Config.alwaysShowExtendedContent)
                        {
                            output.accept(APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION.get());
                            output.accept(APItems.AMADRON_PROCESS_UPGRADE.get());
                        }
                        output.accept(APItems.AMADRON_PATTERN.get());
                    })
                    .build());


    public static void register(IEventBus eventBus)
    {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
