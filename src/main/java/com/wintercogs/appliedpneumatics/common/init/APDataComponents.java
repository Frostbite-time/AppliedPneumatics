package com.wintercogs.appliedpneumatics.common.init;

import com.mojang.serialization.Codec;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.me.crafting.EncodedAmadronPattern;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class APDataComponents
{
    public static DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AppliedPneumatics.MODID);


    // 存储元件中空气数量
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> AIR_STORED = register(
            "air_stored", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
    );

    // 存储一个亚马龙处理站的位置
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> AMADRON_PROCESS_POS = register(
            "amadron_process_pos", builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC)
    );

    // 存储一个物品列表
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> COMMON_ITEM_CONTENT = register(
            "common_item_content", builder -> builder.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC)
    );

    // 存储样板编码的配方id
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EncodedAmadronPattern>> AMADRON_PATTERN = register(
            "amadron_pattern", builder -> builder.persistent(EncodedAmadronPattern.CODEC).networkSynchronized(EncodedAmadronPattern.STREAM_CODEC)
    );

    // 内存卡保存的接口目标值
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> EXPECTED_TEMPERATURE = register(
            "expected_temperature", builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> EXPECTED_PRESSURE = register(
            "expected_pressure", builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT)
    );


    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder)
    {
        return DATA_COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus)
    {
        DATA_COMPONENTS.register(eventBus);
    }
}
