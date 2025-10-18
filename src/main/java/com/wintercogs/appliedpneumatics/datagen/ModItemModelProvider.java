package com.wintercogs.appliedpneumatics.datagen;

import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider
{

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, AppliedPneumatics.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels()
    {
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_1K.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_4K.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_16K.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_64K.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_256K.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_1M.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_4M.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_16M.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_64M.get());
        airCellWithOwnBaseAndAeLed(APItems.AIR_CELL_256M.get());
        portableAirCell(APItems.PORTABLE_AIR_CELL_1K.get(), "appliedpneumatics:item/portable_cell_air_housing", "ae2:item/portable_cell_side_1k");
        portableAirCell(APItems.PORTABLE_AIR_CELL_4K.get(), "appliedpneumatics:item/portable_cell_air_housing", "ae2:item/portable_cell_side_4k");
        portableAirCell(APItems.PORTABLE_AIR_CELL_16K.get(), "appliedpneumatics:item/portable_cell_air_housing", "ae2:item/portable_cell_side_16k");
        portableAirCell(APItems.PORTABLE_AIR_CELL_64K.get(), "appliedpneumatics:item/portable_cell_air_housing", "ae2:item/portable_cell_side_64k");
        portableAirCell(APItems.PORTABLE_AIR_CELL_256K.get(), "appliedpneumatics:item/portable_cell_air_housing", "ae2:item/portable_cell_side_256k");
        portableAirCell(APItems.PORTABLE_AIR_CELL_1M.get(), "appliedpneumatics:item/portable_mega_cell_air_housing", "appliedpneumatics:item/portable_cell_side_1m");
        portableAirCell(APItems.PORTABLE_AIR_CELL_4M.get(), "appliedpneumatics:item/portable_mega_cell_air_housing", "appliedpneumatics:item/portable_cell_side_4m");
        portableAirCell(APItems.PORTABLE_AIR_CELL_16M.get(), "appliedpneumatics:item/portable_mega_cell_air_housing", "appliedpneumatics:item/portable_cell_side_16m");
        portableAirCell(APItems.PORTABLE_AIR_CELL_64M.get(), "appliedpneumatics:item/portable_mega_cell_air_housing", "appliedpneumatics:item/portable_cell_side_64m");
        portableAirCell(APItems.PORTABLE_AIR_CELL_256M.get(), "appliedpneumatics:item/portable_mega_cell_air_housing", "appliedpneumatics:item/portable_cell_side_256m");
        basicItem(APItems.VOLUME_CARD.get());
        basicItem(APItems.SECURITY_CARD.get());
        basicItem(APItems.VACUUM_CARD.get());
        basicItem(APItems.CHARGING_CARD.get());
        basicItem(APItems.AMADRON_WIRELESS_TERMINAL.get());
        basicItem(APItems.AMADRON_PATTERN.get());
        basicItem(APItems.AIR_CELL_SHELL.get());
        basicItem(APItems.MEGA_AIR_CELL_SHELL.get());
        basicItem(APItems.AMADRON_PROCESS_UPGRADE.get());
    }

    /**
     * 生成便携气体单元的模型
     * @param item    物品
     * @param housing layer0 纹理（通常用你自己的）
     * @param side    layer3 纹理（可能来自 ae2/megacells）
     */
    protected void portableAirCell(ItemLike item, String housing, String side) {
        // 让 EFH 放行校验
        allowExternalTexture(housing);
        allowExternalTexture("ae2:item/portable_cell_led");
        allowExternalTexture("ae2:item/portable_cell_screen");
        allowExternalTexture(side);

        withExistingParent(getItemName(item), mcLoc("item/generated"))
                .texture("layer0", housing)
                .texture("layer1", "ae2:item/portable_cell_led")
                .texture("layer2", "ae2:item/portable_cell_screen")
                .texture("layer3", side);
    }

    private String getItemName(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    /**
     * 把非本模组命名空间的纹理标记为“已生成”，从而绕过存在性校验
     */
    private void allowExternalTexture(String path)
    {
        ResourceLocation rl = new ResourceLocation(path);
        if (!rl.getNamespace().equals(AppliedPneumatics.MODID))
        {
            this.existingFileHelper.trackGenerated(rl, ModelProvider.TEXTURE);
        }
    }


    // 快速注册带led灯状态的存储元件模型
    public ItemModelBuilder airCellWithOwnBaseAndAeLed(Item item)
    {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        var base = modLoc("item/" + id.getPath());

        allowExternalTexture("ae2:item/portable_cell_led");
        return withExistingParent(id.getPath(), mcLoc("item/generated"))
                .texture("layer0", base)
                .texture("layer1", "ae2:item/portable_cell_led");
    }

}
