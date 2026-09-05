package com.wintercogs.appliedpneumatics.common.init;

import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.blocks.MEAmadronProcessStation;
import com.wintercogs.appliedpneumatics.common.blocks.MEPressureInterfaceBlock;
import com.wintercogs.appliedpneumatics.common.blocks.METemperatureInterface;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class APBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AppliedPneumatics.MODID);

    // ME气压接口
    public static final RegistryObject<MEPressureInterfaceBlock> ME_PRESSURE_INTERFACE_BLOCK = registerBlock("me_pressure_interface_block",
            () -> new MEPressureInterfaceBlock(BlockBehaviour.Properties.of().strength(2f)));

    // ME亚马龙处理站
    public static final RegistryObject<MEAmadronProcessStation> ME_AMADRON_PROCESS_STATION = registerBlock("me_amadron_process_station",
            () -> new MEAmadronProcessStation(BlockBehaviour.Properties.of().strength(2f)));

    // 扩展亚马龙处理站
    public static final RegistryObject<MEAmadronProcessStation> ME_AMADRON_EXTENDED_PROCESS_STATION = registerBlock("me_amadron_extended_process_station",
            () -> new MEAmadronProcessStation(BlockBehaviour.Properties.of().strength(2f)));

    // ME温控接口
    public static final RegistryObject<METemperatureInterface> ME_TEMPERATURE_INTERFACE = registerBlock("me_temperature_interface",
            () -> new METemperatureInterface(BlockBehaviour.Properties.of().strength(2f)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block)
    {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block)
    {
        APItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
