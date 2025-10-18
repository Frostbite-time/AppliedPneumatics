package com.wintercogs.appliedpneumatics.datagen;

import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.init.APBlockStates;
import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;


public class ModBlockStateProvider extends BlockStateProvider
{

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, AppliedPneumatics.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels()
    {
        blockWithItem(APBlocks.ME_PRESSURE_INTERFACE_BLOCK);
        cubeAllPerState(APBlocks.ME_TEMPERATURE_INTERFACE, APBlockStates.TEMP_STATE);
        blockWithItem(APBlocks.ME_AMADRON_PROCESS_STATION);
        blockWithItem(APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION);
    }

    private void blockWithItem(RegistryObject<? extends Block> deferredBlock)
    {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    /**
     * 为某个方块的枚举属性生成：
     * - blockstates/<id>.json（每个取值一个 variant）
     * - models/block/<id>/<state>.json（cube_all）
     * - models/item/<id>.json（指向默认状态对应的 block 模型）
     *
     * 贴图路径：textures/block/<id>/<state>.png
     */
    public <E extends Enum<E> & StringRepresentable>
    void cubeAllPerState(RegistryObject<? extends Block> defBlock, EnumProperty<E> prop)
    {
        Block block = defBlock.get();

        var def = block.getStateDefinition();
        if (!def.getProperties().contains(prop)) {
            throw new IllegalArgumentException("Block " + block + " does not contain property " + prop.getName());
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        VariantBlockStateBuilder builder = getVariantBuilder(block);

        E defaultVal = block.defaultBlockState().getValue(prop);
        ModelFile defaultModel = null;

        for (E value : prop.getPossibleValues()) {
            String state = value.getSerializedName();
            String name  = id.getPath() + "/" + state; // 模型 ID（注意：不带 "block/" 前缀）
            // 方块模型：models/block/<name>.json，纹理：textures/block/<name>.png
            ModelFile model = models().cubeAll(name, modLoc("block/" + name));

            builder.partialState().with(prop, value).addModels(new ConfiguredModel(model));

            if (value == defaultVal) defaultModel = model;         // 记住默认状态的模型
        }

        // 物品模型指向默认方块模型
        simpleBlockItem(block, defaultModel);
    }

}