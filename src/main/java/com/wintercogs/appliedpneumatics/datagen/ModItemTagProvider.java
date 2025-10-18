package com.wintercogs.appliedpneumatics.datagen;

import appeng.api.features.P2PTunnelAttunement;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider
{
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, blockTags, AppliedPneumatics.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider)
    {
        // 能用来调频p2p到温度通道的物品 仅在datagen时尝试获取，防止提前触发
        TagKey<Item> HEAT_ATTUNE = P2PTunnelAttunement.getAttunementTag(APItems.HEAT_P2P_TUNEL.get());

        tag(HEAT_ATTUNE)
                .add(ModBlocks.COMPRESSED_IRON_BLOCK.get().asItem())
                .add(ModBlocks.HEAT_PIPE.get().asItem())
                .add(ModBlocks.HEAT_SINK.get().asItem());
    }
}
