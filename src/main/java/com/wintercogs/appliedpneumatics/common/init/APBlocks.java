package com.wintercogs.appliedpneumatics.common.init;

import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.blocks.MEAmadronProcessStation;
import com.wintercogs.appliedpneumatics.common.blocks.MEPressureInterfaceBlock;
import com.wintercogs.appliedpneumatics.common.blocks.METemperatureInterface;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class APBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AppliedPneumatics.MODID);

    // ME气压接口
    public static final DeferredBlock<MEPressureInterfaceBlock> ME_PRESSURE_INTERFACE_BLOCK = registerBlock("me_pressure_interface_block",
            () -> new MEPressureInterfaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    // ME亚马龙处理站
    public static final DeferredBlock<MEAmadronProcessStation> ME_AMADRON_PROCESS_STATION = registerBlock("me_amadron_process_station",
            () -> new MEAmadronProcessStation(BlockBehaviour.Properties.of().strength(2f)));

    // 扩展亚马龙处理站
    public static final DeferredBlock<MEAmadronProcessStation> ME_AMADRON_EXTENDED_PROCESS_STATION = registerBlock("me_amadron_extended_process_station",
            () -> new MEAmadronProcessStation(BlockBehaviour.Properties.of().strength(2f)));

    // ME温控接口
    public static final DeferredBlock<METemperatureInterface> ME_TEMPERATURE_INTERFACE = registerBlock("me_temperature_interface",
            () -> new METemperatureInterface(BlockBehaviour.Properties.of().strength(2f)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block)
    {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block)
    {
        APItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
