package com.wintercogs.appliedpneumatics.datagen;

import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.extendedae.recipe.CrystalAssemblerRecipeBuilder;
import com.google.common.collect.ImmutableList;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.datagen.builder.CellDisassemblyRecipeBuilder;
import gripe._90.megacells.definition.MEGAItems;
import me.desht.pneumaticcraft.api.crafting.AmadronTradeResource;
import me.desht.pneumaticcraft.api.crafting.recipe.AssemblyRecipe;
import me.desht.pneumaticcraft.common.registry.ModBlocks;
import me.desht.pneumaticcraft.common.registry.ModItems;
import me.desht.pneumaticcraft.common.upgrades.ModUpgrades;
import me.desht.pneumaticcraft.datagen.recipe.AmadronRecipeBuilder;
import me.desht.pneumaticcraft.datagen.recipe.AssemblyRecipeBuilder;
import me.desht.pneumaticcraft.datagen.recipe.PressureChamberRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder
{

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput)
    {
        // 外壳
        pressureChamber(ImmutableList.of(SizedIngredient.of(ModBlocks.PRESSURE_TUBE, 2),
                        SizedIngredient.of(ModBlocks.PRESSURE_CHAMBER_GLASS, 3),
                        SizedIngredient.of(ModItems.COMPRESSED_IRON_INGOT, 2),
                        SizedIngredient.of(ModItems.PRESSURE_GAUGE, 1)),
                4f,
                new ItemStack(APItems.AIR_CELL_SHELL.get()))
                .save(recipeOutput, housingShapedId(APItems.AIR_CELL_SHELL.get()));

        pressureChamber(ImmutableList.of(SizedIngredient.of(ModBlocks.REINFORCED_PRESSURE_TUBE, 2),
                        SizedIngredient.of(MEGAItems.SKY_STEEL_INGOT, 2),
                        SizedIngredient.of(ModItems.COMPRESSED_IRON_GEAR, 2),
                        SizedIngredient.of(ModItems.PRINTED_CIRCUIT_BOARD, 1),
                        SizedIngredient.of(ModItems.NETWORK_DATA_STORAGE, 1),
                        SizedIngredient.of(APItems.AIR_CELL_SHELL, 1)),
                4.95f,
                new ItemStack(APItems.MEGA_AIR_CELL_SHELL.get()))
                .save(recipeOutput.withConditions(new ModLoadedCondition(AppliedPneumatics.MEGA_CELL_MODID)),
                        housingShapedId(APItems.MEGA_AIR_CELL_SHELL.get()));

        // 所有元件
        buildAllCellRecipe(recipeOutput);

        // ME气压接口
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APBlocks.ME_PRESSURE_INTERFACE_BLOCK)
                .pattern("ABA")
                .pattern("CED")
                .pattern("ABA")
                .define('A', ModItems.COMPRESSED_IRON_INGOT)
                .define('B', Tags.Items.GLASS_BLOCKS)
                .define('C', AEItems.ANNIHILATION_CORE)
                .define('D', AEItems.FORMATION_CORE)
                .define('E', ModBlocks.AIR_COMPRESSOR)
                .unlockedBy("unlock_me_pressure_interface_block", has(AEItems.CELL_COMPONENT_256K))
                .save(recipeOutput);

        // ME温控接口
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APBlocks.ME_TEMPERATURE_INTERFACE)
                .pattern("ABA")
                .pattern("DCE")
                .pattern("ABA")
                .define('A', ModBlocks.COMPRESSED_IRON_BLOCK)
                .define('B', ModBlocks.VORTEX_TUBE)
                .define('C', ModBlocks.ADVANCED_AIR_COMPRESSOR)
                .define('D', AEItems.ANNIHILATION_CORE)
                .define('E', AEItems.FORMATION_CORE)
                .unlockedBy("unlock_me_temperature_interface_block", has(ModBlocks.ADVANCED_AIR_COMPRESSOR))
                .save(recipeOutput);

        // 亚马龙无线终端
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APItems.AMADRON_WIRELESS_TERMINAL)
                .pattern(" A ")
                .pattern(" B ")
                .pattern(" C ")
                .define('A', AEItems.WIRELESS_RECEIVER)
                .define('B', ModItems.AMADRON_TABLET)
                .define('C', AEBlocks.DENSE_ENERGY_CELL)
                .unlockedBy("unlock_amadron_wireless_terminal", has(ModItems.AMADRON_TABLET))
                .save(recipeOutput);

        // 亚马龙处理站
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APBlocks.ME_AMADRON_PROCESS_STATION)
                .pattern("EAE")
                .pattern("CBC")
                .pattern("EDE")
                .define('A', ModBlocks.CHARGING_STATION)
                .define('B', AEBlocks.PATTERN_PROVIDER)
                .define('C', AEBlocks.INTERFACE)
                .define('D', ModItems.PRINTED_CIRCUIT_BOARD)
                .define('E', Tags.Items.GLASS_BLOCKS)
                .unlockedBy("unlock_me_amadron_process_station", has(APItems.AMADRON_WIRELESS_TERMINAL))
                .save(recipeOutput);

        // 扩展亚马龙处理站
        CrystalAssemblerRecipeBuilder.assemble(APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION)
                .input(APBlocks.ME_AMADRON_PROCESS_STATION)
                .input(AEItems.CAPACITY_CARD, 3)
                .input(Items.CRAFTING_TABLE, 3)
                .input(EAESingletons.CONCURRENT_PROCESSOR)
                .input(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), 6)
                .save(recipeOutput.withConditions(new ModLoadedCondition(AppliedPneumatics.EAE_MODID)),
                        AppliedPneumatics.makeId("assembler/me_amadron_extened_process_station"));

        // 亚马龙处理站升级
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APItems.AMADRON_PROCESS_UPGRADE)
                .requires(APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION)
                .requires(Tags.Items.INGOTS)
                .unlockedBy("unlock_amadron_process_upgrade", has(APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION))
                .save(recipeOutput.withConditions(modLoaded(AppliedPneumatics.EAE_MODID)));

        // 安全卡
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APItems.SECURITY_CARD)
                .requires(AEItems.ADVANCED_CARD)
                .requires(ModUpgrades.SECURITY.get().getItem())
                .unlockedBy("unlock_security_card", has(AEItems.ADVANCED_CARD))
                .save(recipeOutput);

        // 容积卡
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APItems.VOLUME_CARD)
                .requires(AEItems.ADVANCED_CARD)
                .requires(ModUpgrades.VOLUME.get().getItem())
                .unlockedBy("unlock_volume_card", has(AEItems.ADVANCED_CARD))
                .save(recipeOutput);

        // 充气卡
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APItems.CHARGING_CARD)
                .requires(AEItems.ADVANCED_CARD)
                .requires(ModUpgrades.CHARGING.get().getItem())
                .unlockedBy("unlock_charge_card", has(AEItems.ADVANCED_CARD))
                .save(recipeOutput);

        // 真空卡
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APItems.VACUUM_CARD)
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', ModItems.PRINTED_CIRCUIT_BOARD)
                .define('B', ModBlocks.VACUUM_PUMP)
                .define('C', AEItems.ADVANCED_CARD)
                .unlockedBy("unlock_vacuum_card", has(AEItems.ADVANCED_CARD))
                .save(recipeOutput);

        // 联动配方------------------------------------------------------------------------------------------------------

        // 绿宝石到物品
        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 8)),
                AmadronTradeResource.of(new ItemStack(AEItems.CERTUS_QUARTZ_CRYSTAL, 8)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_certus_quartz_crystal"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 8)),
                AmadronTradeResource.of(new ItemStack(AEItems.FLUIX_CRYSTAL, 4)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_fluix_crystal"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 10)),
                AmadronTradeResource.of(new ItemStack(AEBlocks.SKY_STONE_BLOCK, 16)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_sky_stone_block"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 32)),
                AmadronTradeResource.of(new ItemStack(AEItems.ITEM_CELL_HOUSING)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_item_cell_housing"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 32)),
                AmadronTradeResource.of(new ItemStack(AEItems.FLUID_CELL_HOUSING)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_fluid_cell_housing"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 32)),
                AmadronTradeResource.of(new ItemStack(APItems.AIR_CELL_SHELL.get())))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_air_cell_shell"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 24)),
                AmadronTradeResource.of(new ItemStack(AEItems.BLANK_PATTERN)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_blank_pattern"));

        // 物品到绿宝石
        amadronStatic(AmadronTradeResource.of(new ItemStack(AEItems.CERTUS_QUARTZ_CRYSTAL, 16)),
                AmadronTradeResource.of(new ItemStack(Items.EMERALD, 8)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/certus_quartz_crystal_to_emerald"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(AEItems.FLUIX_CRYSTAL, 16)),
                AmadronTradeResource.of(new ItemStack(Items.EMERALD, 4)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/fluix_crystal_to_emerald"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(AEBlocks.SKY_STONE_BLOCK, 32)),
                AmadronTradeResource.of(new ItemStack(Items.EMERALD, 10)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/sky_stone_block_to_emerald"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(AEItems.SINGULARITY, 1)),
                AmadronTradeResource.of(new ItemStack(Items.EMERALD, 99)))
                .save(recipeOutput, AppliedPneumatics.makeId("amadron/singularity_to_emerald"));

        // 装配室联动配方
        assembly(SizedIngredient.of(AEParts.SMART_CABLE.item(AEColor.TRANSPARENT), 16),
                new ItemStack(AEParts.SMART_DENSE_CABLE.item(AEColor.TRANSPARENT), 8),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/smart_cable_to_smart_dense_cable"));

        assembly(SizedIngredient.of(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), 16),
                new ItemStack(AEParts.SMART_CABLE.item(AEColor.TRANSPARENT), 16),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/glass_cable_to_smart_cable"));

        assembly(SizedIngredient.of(AEBlocks.DAMAGED_BUDDING_QUARTZ, 1),
                new ItemStack(AEBlocks.CHIPPED_BUDDING_QUARTZ, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/damage_budding_quartz_to_chipped_budding_quartz"));

        assembly(SizedIngredient.of(AEBlocks.CHIPPED_BUDDING_QUARTZ, 1),
                new ItemStack(AEBlocks.FLAWED_BUDDING_QUARTZ, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/chipped_budding_quartz_to_flaw_budding_quartz"));

        assembly(SizedIngredient.of(AEBlocks.FLAWED_BUDDING_QUARTZ, 64),
                new ItemStack(AEBlocks.FLAWLESS_BUDDING_QUARTZ, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/flaw_budding_quartz_to_flawless_budding_quartz"));

        assembly(SizedIngredient.of(AEBlocks.ENERGY_CELL, 8),
                new ItemStack(AEBlocks.DENSE_ENERGY_CELL, 1),
                AssemblyRecipe.AssemblyProgramType.DRILL)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/energy_cell_to_dense_energy_cell"));

        assembly(SizedIngredient.of(AEBlocks.NOT_SO_MYSTERIOUS_CUBE, 1),
                new ItemStack(AEBlocks.MYSTERIOUS_CUBE, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/not_so_mysterious_cube_to_mysterious_cube"));

        assembly(SizedIngredient.of(Blocks.TNT, 1),
                new ItemStack(AEBlocks.TINY_TNT, 8),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/tnt_to_tiny_tnt"));

        assembly(SizedIngredient.of(AEItems.MATTER_BALL, 64),
                new ItemStack(AEItems.SINGULARITY, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/matter_ball_to_singularity"));

        assembly(SizedIngredient.of(AEItems.CERTUS_QUARTZ_CRYSTAL, 1),
                new ItemStack(AEItems.CALCULATION_PROCESSOR_PRINT, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/certus_quartz_crystal_to_calculation_processor_print"));

        assembly(SizedIngredient.of(Items.DIAMOND, 1),
                new ItemStack(AEItems.ENGINEERING_PROCESSOR_PRINT, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/diamond_to_engineering_processor_print"));

        assembly(SizedIngredient.of(Items.GOLD_INGOT, 1),
                new ItemStack(AEItems.LOGIC_PROCESSOR_PRINT, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/gold_ingot_to_logic_processor_print"));

        assembly(SizedIngredient.of(AEItems.SILICON, 1),
                new ItemStack(AEItems.SILICON_PRINT, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput, AppliedPneumatics.makeId("assembly/silicon_to_silicon_print"));

        // eae联动-恩特罗装配线
        assembly(SizedIngredient.of(EAESingletons.HARDLY_ENTROIZED_FLUIX_BUDDING, 1),
                new ItemStack(EAESingletons.HALF_ENTROIZED_FLUIX_BUDDING, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput.withConditions(new ModLoadedCondition(AppliedPneumatics.EAE_MODID)),
                        AppliedPneumatics.makeId("assembly/hardly_entropized_fluix_budding_to_half_entropized_fluix_budding"));

        assembly(SizedIngredient.of(EAESingletons.HALF_ENTROIZED_FLUIX_BUDDING, 1),
                new ItemStack(EAESingletons.MOSTLY_ENTROIZED_FLUIX_BUDDING, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput.withConditions(new ModLoadedCondition(AppliedPneumatics.EAE_MODID)),
                        AppliedPneumatics.makeId("assembly/half_entropized_fluix_budding_to_mostly_entropized_fluix_budding"));

        assembly(SizedIngredient.of(EAESingletons.MOSTLY_ENTROIZED_FLUIX_BUDDING, 1),
                new ItemStack(EAESingletons.FULLY_ENTROIZED_FLUIX_BUDDING, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .save(recipeOutput.withConditions(new ModLoadedCondition(AppliedPneumatics.EAE_MODID)),
                        AppliedPneumatics.makeId("assembly/mostly_entropized_fluix_budding_to_fully_entropized_fluix_budding"));

    }

    // 添加所有元件
    private static void buildAllCellRecipe(RecipeOutput recipeOutput)
    {
        List<TierRow> commonTierRows = new ArrayList<>(5);
        List<TierRow> megaTierRows = new ArrayList<>(5);
        commonTierRows.add(new TierRow(AEItems.CELL_COMPONENT_1K, APItems.AIR_CELL_1K.get(), APItems.PORTABLE_AIR_CELL_1K.get()));
        commonTierRows.add(new TierRow(AEItems.CELL_COMPONENT_4K, APItems.AIR_CELL_4K.get(), APItems.PORTABLE_AIR_CELL_4K.get()));
        commonTierRows.add(new TierRow(AEItems.CELL_COMPONENT_16K, APItems.AIR_CELL_16K.get(), APItems.PORTABLE_AIR_CELL_16K.get()));
        commonTierRows.add(new TierRow(AEItems.CELL_COMPONENT_64K, APItems.AIR_CELL_64K.get(), APItems.PORTABLE_AIR_CELL_64K.get()));
        commonTierRows.add(new TierRow(AEItems.CELL_COMPONENT_256K, APItems.AIR_CELL_256K.get(), APItems.PORTABLE_AIR_CELL_256K.get()));
        megaTierRows.add(new TierRow(MEGAItems.CELL_COMPONENT_1M, APItems.AIR_CELL_1M.get(), APItems.PORTABLE_AIR_CELL_1M.get()));
        megaTierRows.add(new TierRow(MEGAItems.CELL_COMPONENT_4M, APItems.AIR_CELL_4M.get(), APItems.PORTABLE_AIR_CELL_4M.get()));
        megaTierRows.add(new TierRow(MEGAItems.CELL_COMPONENT_16M, APItems.AIR_CELL_16M.get(), APItems.PORTABLE_AIR_CELL_16M.get()));
        megaTierRows.add(new TierRow(MEGAItems.CELL_COMPONENT_64M, APItems.AIR_CELL_64M.get(), APItems.PORTABLE_AIR_CELL_64M.get()));
        megaTierRows.add(new TierRow(MEGAItems.CELL_COMPONENT_256M, APItems.AIR_CELL_256M.get(), APItems.PORTABLE_AIR_CELL_256M.get()));

        // k系列
        for (TierRow tierRow : commonTierRows)
        {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tierRow.cell)
                    .requires(APItems.AIR_CELL_SHELL.get())
                    .requires(tierRow.component)
                    .unlockedBy("has_correct_component", has(tierRow.component))
                    .save(recipeOutput, cellShapelessId(tierRow.cell));
            CellDisassemblyRecipeBuilder.cell(tierRow.cell)
                    .add(APItems.AIR_CELL_SHELL)
                    .add(tierRow.component)
                    .save(recipeOutput, disassemblyId("air_cell", tierRow.cell, false));

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tierRow.portableCell)
                    .requires(tierRow.component)
                    .requires(AEBlocks.ME_CHEST)
                    .requires(AEBlocks.ENERGY_CELL)
                    .requires(APItems.AIR_CELL_SHELL.get())
                    .unlockedBy("has_correct_component", has(tierRow.component))
                    .save(recipeOutput, cellShapelessId(tierRow.portableCell));
            CellDisassemblyRecipeBuilder.cell(tierRow.portableCell)
                    .add(tierRow.component)
                    .add(AEBlocks.ME_CHEST)
                    .add(AEBlocks.ENERGY_CELL)
                    .add(APItems.AIR_CELL_SHELL)
                    .save(recipeOutput, disassemblyId("air_cell", tierRow.portableCell, true));
        }
        // m系列
        for (TierRow tierRow : megaTierRows)
        {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tierRow.cell)
                    .requires(APItems.MEGA_AIR_CELL_SHELL.get())
                    .requires(tierRow.component)
                    .unlockedBy("has_correct_component", has(tierRow.component))
                    .save(recipeOutput.withConditions(new ModLoadedCondition(AppliedPneumatics.MEGA_CELL_MODID)),
                            cellShapelessId(tierRow.cell));
            CellDisassemblyRecipeBuilder.cell(tierRow.cell)
                    .add(APItems.MEGA_AIR_CELL_SHELL)
                    .add(tierRow.component)
                    .whenModLoaded(AppliedPneumatics.MEGA_CELL_MODID)
                    .save(recipeOutput, disassemblyId("air_cell", tierRow.cell, false));

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tierRow.portableCell)
                    .requires(tierRow.component)
                    .requires(AEBlocks.ME_CHEST)
                    .requires(AEBlocks.ENERGY_CELL)
                    .requires(APItems.MEGA_AIR_CELL_SHELL.get())
                    .unlockedBy("has_correct_component", has(tierRow.component))
                    .save(recipeOutput.withConditions(new ModLoadedCondition(AppliedPneumatics.MEGA_CELL_MODID)),
                            cellShapelessId(tierRow.portableCell));
            CellDisassemblyRecipeBuilder.cell(tierRow.portableCell)
                    .add(tierRow.component)
                    .add(AEBlocks.ME_CHEST)
                    .add(AEBlocks.ENERGY_CELL)
                    .add(APItems.MEGA_AIR_CELL_SHELL)
                    .whenModLoaded(AppliedPneumatics.MEGA_CELL_MODID)
                    .save(recipeOutput, disassemblyId("air_cell", tierRow.portableCell, true));
        }
    }

    // 统一配方id
    private static ResourceLocation housingShapedId(ItemLike housing)
    {
        return AppliedPneumatics.makeId("cells/housing/" + BuiltInRegistries.ITEM.getKey(housing.asItem()).getPath());
    }

    private static ResourceLocation cellShapelessId(ItemLike cell)
    {
        return AppliedPneumatics.makeId("cells/shapeless/" + BuiltInRegistries.ITEM.getKey(cell.asItem()).getPath());
    }

    private static ResourceLocation disassemblyId(String series, ItemLike idLike, boolean portable)
    {
        String base = (portable ? "disassembly/portable/" : "disassembly/") + series + "/" + BuiltInRegistries.ITEM.getKey(idLike.asItem()).getPath();
        return AppliedPneumatics.makeId(base);
    }

    // 用于快速添加亚马龙交易
    private RecipeBuilder amadronStatic(AmadronTradeResource in, AmadronTradeResource out)
    {
        return (new AmadronRecipeBuilder(in, out, true, 0)).unlockedBy(getHasName((ItemLike) ModItems.AMADRON_TABLET.get()), has((ItemLike) ModItems.AMADRON_TABLET.get()));
    }

    // 快速添加压力室配方
    private RecipeBuilder pressureChamber(List<SizedIngredient> in, float pressure, ItemStack... out)
    {
        return (new PressureChamberRecipeBuilder(in, pressure, out)).unlockedBy(getHasName((ItemLike) ModBlocks.PRESSURE_CHAMBER_VALVE.get()), has((ItemLike) ModBlocks.PRESSURE_CHAMBER_VALVE.get()));
    }

    // 快速添加装配室配方
    private RecipeBuilder assembly(SizedIngredient input, ItemStack output, AssemblyRecipe.AssemblyProgramType programType)
    {
        return (new AssemblyRecipeBuilder(input, output, programType)).unlockedBy(getHasName((ItemLike) ModBlocks.ASSEMBLY_CONTROLLER.get()), has((ItemLike) ModBlocks.ASSEMBLY_CONTROLLER.get()));
    }

    /**
     * 用于描述元件与组件之间的对应关系
     */
    private record TierRow(
            ItemLike component,
            ItemLike cell,
            ItemLike portableCell
    )
    {
    }

}
