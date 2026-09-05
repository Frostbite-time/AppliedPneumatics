package com.wintercogs.appliedpneumatics.datagen;

import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.init.APDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModDataComponentTypeTagProvider extends TagsProvider<DataComponentType<?>>
{
    private static final TagKey<DataComponentType<?>> AE_EXPORTED_SETTINGS = TagKey.create(
            Registries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath("ae2", "exported_settings"));

    public ModDataComponentTypeTagProvider(PackOutput output,
                                           CompletableFuture<HolderLookup.Provider> provider,
                                           @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, Registries.DATA_COMPONENT_TYPE, provider, AppliedPneumatics.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        tag(AE_EXPORTED_SETTINGS)
                .add(APDataComponents.EXPECTED_TEMPERATURE.getKey())
                .add(APDataComponents.EXPECTED_PRESSURE.getKey());
    }
}
