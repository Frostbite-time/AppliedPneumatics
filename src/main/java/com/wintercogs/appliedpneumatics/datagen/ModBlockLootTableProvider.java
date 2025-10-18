package com.wintercogs.appliedpneumatics.datagen;

import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider
{
    protected ModBlockLootTableProvider()
    {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate()
    {
        dropSelf(APBlocks.ME_PRESSURE_INTERFACE_BLOCK.get());
        dropSelf(APBlocks.ME_AMADRON_PROCESS_STATION.get());
        dropSelf(APBlocks.ME_TEMPERATURE_INTERFACE.get());
        dropSelf(APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION.get());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks()
    {
        return APBlocks.BLOCKS.getEntries().stream().flatMap(RegistryObject::stream)::iterator;
    }
}