package com.wintercogs.appliedpneumatics.datagen;

import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.google.common.collect.ImmutableList;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.init.APBlocks;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import gripe._90.megacells.definition.MEGAItems;
import me.desht.pneumaticcraft.api.crafting.AmadronTradeResource;
import me.desht.pneumaticcraft.api.crafting.recipe.AssemblyRecipe;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.upgrades.ModUpgrades;
import me.desht.pneumaticcraft.datagen.recipe.AmadronRecipeBuilder;
import me.desht.pneumaticcraft.datagen.recipe.AssemblyRecipeBuilder;
import me.desht.pneumaticcraft.datagen.recipe.PressureChamberRecipeBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder
{

    public ModRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> recipeOutput)
    {
        // 外壳
        pressureChamber(ImmutableList.of(Ingredient.of(new ItemStack(ModBlocks.PRESSURE_TUBE.get(), 2)),
                        Ingredient.of(new ItemStack(ModBlocks.PRESSURE_CHAMBER_GLASS.get(), 3)),
                        Ingredient.of(new ItemStack(ModItems.COMPRESSED_IRON_INGOT.get(), 2)),
                        Ingredient.of(new ItemStack(ModItems.PRESSURE_GAUGE.get(), 1))),
                4f,
                new ItemStack(APItems.AIR_CELL_SHELL.get()))
                .build(recipeOutput, housingShapedId(APItems.AIR_CELL_SHELL.get()));

        pressureChamber(ImmutableList.of(Ingredient.of(new ItemStack(ModBlocks.REINFORCED_PRESSURE_TUBE.get(), 2)),
                        Ingredient.of(new ItemStack(MEGAItems.SKY_STEEL_INGOT, 2)),
                        Ingredient.of(new ItemStack(ModItems.COMPRESSED_IRON_GEAR.get(), 2)),
                        Ingredient.of(new ItemStack(ModItems.PRINTED_CIRCUIT_BOARD.get(), 1)),
                        Ingredient.of(new ItemStack(ModItems.NETWORK_DATA_STORAGE.get(), 1)),
                        Ingredient.of(new ItemStack(APItems.AIR_CELL_SHELL.get(), 1))),
                5f,
                new ItemStack(APItems.MEGA_AIR_CELL_SHELL.get()))
                .addCondition(new ModLoadedCondition(AppliedPneumatics.MEGA_CELL_MODID))
                .build(recipeOutput, housingShapedId(APItems.MEGA_AIR_CELL_SHELL.get()));

        // 所有元件
        buildAllCellRecipe(recipeOutput);

        // ME气压接口
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APBlocks.ME_PRESSURE_INTERFACE_BLOCK.get())
                .pattern("ABA")
                .pattern("CED")
                .pattern("ABA")
                .define('A', ModItems.COMPRESSED_IRON_INGOT.get())
                .define('B', Tags.Items.GLASS)
                .define('C', AEItems.ANNIHILATION_CORE)
                .define('D', AEItems.FORMATION_CORE)
                .define('E', ModBlocks.AIR_COMPRESSOR.get())
                .unlockedBy("unlock_me_pressure_interface_block", has(AEItems.CELL_COMPONENT_256K))
                .save(recipeOutput);

        // ME温控接口
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APBlocks.ME_TEMPERATURE_INTERFACE.get())
                .pattern("ABA")
                .pattern("DCE")
                .pattern("ABA")
                .define('A', ModBlocks.COMPRESSED_IRON_BLOCK.get())
                .define('B', ModBlocks.VORTEX_TUBE.get())
                .define('C', ModBlocks.ADVANCED_AIR_COMPRESSOR.get())
                .define('D', AEItems.ANNIHILATION_CORE)
                .define('E', AEItems.FORMATION_CORE)
                .unlockedBy("unlock_me_temperature_interface_block", has(ModBlocks.ADVANCED_AIR_COMPRESSOR.get()))
                .save(recipeOutput);

        // 亚马龙无线终端
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APItems.AMADRON_WIRELESS_TERMINAL.get())
                .pattern(" A ")
                .pattern(" B ")
                .pattern(" C ")
                .define('A', AEItems.WIRELESS_RECEIVER)
                .define('B', ModItems.AMADRON_TABLET.get())
                .define('C', AEBlocks.DENSE_ENERGY_CELL)
                .unlockedBy("unlock_amadron_wireless_terminal", has(ModItems.AMADRON_TABLET.get()))
                .save(recipeOutput);

        // 亚马龙处理站
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APBlocks.ME_AMADRON_PROCESS_STATION.get())
                .pattern("EAE")
                .pattern("CBC")
                .pattern("EDE")
                .define('A', ModBlocks.CHARGING_STATION.get())
                .define('B', AEBlocks.PATTERN_PROVIDER)
                .define('C', AEBlocks.INTERFACE)
                .define('D', ModItems.PRINTED_CIRCUIT_BOARD.get())
                .define('E', Tags.Items.GLASS)
                .unlockedBy("unlock_me_amadron_process_station", has(APItems.AMADRON_WIRELESS_TERMINAL.get()))
                .save(recipeOutput);

        // 扩展亚马龙处理站
        ShapelessRecipeBuilder amadronExtendedProcessBuilder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION.get())
                .requires(APBlocks.ME_AMADRON_PROCESS_STATION.get(), 2)
                .requires(AEItems.CAPACITY_CARD, 2)
                .unlockedBy("unlock_me_amadron_process_station", has(APItems.AMADRON_WIRELESS_TERMINAL.get()));
        ConditionalRecipe.builder()
                .addCondition(new ModLoadedCondition(AppliedPneumatics.EAE_MODID))
                .addRecipe(amadronExtendedProcessBuilder::save)
                .build(recipeOutput, APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION.getId());

        // 亚马龙处理站升级
        ShapelessRecipeBuilder amadronProcessUpgradeBuilder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APItems.AMADRON_PROCESS_UPGRADE.get())
                .requires(APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION.get())
                .requires(Tags.Items.INGOTS)
                .unlockedBy("unlock_amadron_process_upgrade", has(APBlocks.ME_AMADRON_EXTENDED_PROCESS_STATION.get()));
        ConditionalRecipe.builder()
                .addCondition(new ModLoadedCondition(AppliedPneumatics.EAE_MODID))
                .addRecipe(amadronProcessUpgradeBuilder::save)
                .build(recipeOutput, APItems.AMADRON_PROCESS_UPGRADE.getId());

        // 安全卡
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APItems.SECURITY_CARD.get())
                .requires(AEItems.ADVANCED_CARD)
                .requires(ModUpgrades.SECURITY.get().getItem())
                .unlockedBy("unlock_security_card", has(AEItems.ADVANCED_CARD))
                .save(recipeOutput);

        // 容积卡
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APItems.VOLUME_CARD.get())
                .requires(AEItems.ADVANCED_CARD)
                .requires(ModUpgrades.VOLUME.get().getItem())
                .unlockedBy("unlock_volume_card", has(AEItems.ADVANCED_CARD))
                .save(recipeOutput);

        // 充气卡
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, APItems.CHARGING_CARD.get())
                .requires(AEItems.ADVANCED_CARD)
                .requires(ModUpgrades.CHARGING.get().getItem())
                .unlockedBy("unlock_charge_card", has(AEItems.ADVANCED_CARD))
                .save(recipeOutput);

        // 真空卡
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, APItems.VACUUM_CARD.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', ModItems.PRINTED_CIRCUIT_BOARD.get())
                .define('B', ModBlocks.VACUUM_PUMP.get())
                .define('C', AEItems.ADVANCED_CARD)
                .unlockedBy("unlock_vacuum_card", has(AEItems.ADVANCED_CARD))
                .save(recipeOutput);

        // 联动配方------------------------------------------------------------------------------------------------------

        // 绿宝石到物品
        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 8)),
                AmadronTradeResource.of(new ItemStack(AEItems.CERTUS_QUARTZ_CRYSTAL, 8)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_certus_quartz_crystal"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 8)),
                AmadronTradeResource.of(new ItemStack(AEItems.FLUIX_CRYSTAL, 4)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_fluix_crystal"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 10)),
                AmadronTradeResource.of(new ItemStack(AEBlocks.SKY_STONE_BLOCK, 16)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_sky_stone_block"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 32)),
                AmadronTradeResource.of(new ItemStack(AEItems.ITEM_CELL_HOUSING)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_item_cell_housing"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 32)),
                AmadronTradeResource.of(new ItemStack(AEItems.FLUID_CELL_HOUSING)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_fluid_cell_housing"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 32)),
                AmadronTradeResource.of(new ItemStack(APItems.AIR_CELL_SHELL.get())))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_air_cell_shell"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(Items.EMERALD, 24)),
                AmadronTradeResource.of(new ItemStack(AEItems.BLANK_PATTERN)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/emerald_to_blank_pattern"));

        // 物品到绿宝石
        amadronStatic(AmadronTradeResource.of(new ItemStack(AEItems.CERTUS_QUARTZ_CRYSTAL, 16)),
                AmadronTradeResource.of(new ItemStack(Items.EMERALD, 8)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/certus_quartz_crystal_to_emerald"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(AEItems.FLUIX_CRYSTAL, 16)),
                AmadronTradeResource.of(new ItemStack(Items.EMERALD, 4)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/fluix_crystal_to_emerald"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(AEBlocks.SKY_STONE_BLOCK, 32)),
                AmadronTradeResource.of(new ItemStack(Items.EMERALD, 10)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/sky_stone_block_to_emerald"));

        amadronStatic(AmadronTradeResource.of(new ItemStack(AEItems.SINGULARITY, 1)),
                AmadronTradeResource.of(new ItemStack(Items.EMERALD, 99)))
                .build(recipeOutput, AppliedPneumatics.makeId("amadron/singularity_to_emerald"));

        // 装配室联动配方
        assembly(Ingredient.of(new ItemStack(AEParts.SMART_CABLE.item(AEColor.TRANSPARENT), 16)),
                new ItemStack(AEParts.SMART_DENSE_CABLE.item(AEColor.TRANSPARENT), 8),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/smart_cable_to_smart_dense_cable"));

        assembly(Ingredient.of(new ItemStack(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), 16)),
                new ItemStack(AEParts.SMART_CABLE.item(AEColor.TRANSPARENT), 16),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/glass_cable_to_smart_cable"));

        assembly(Ingredient.of(new ItemStack(AEBlocks.DAMAGED_BUDDING_QUARTZ, 1)),
                new ItemStack(AEBlocks.CHIPPED_BUDDING_QUARTZ, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/damage_budding_quartz_to_chipped_budding_quartz"));

        assembly(Ingredient.of(new ItemStack(AEBlocks.CHIPPED_BUDDING_QUARTZ, 1)),
                new ItemStack(AEBlocks.FLAWED_BUDDING_QUARTZ, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/chipped_budding_quartz_to_flaw_budding_quartz"));

        assembly(Ingredient.of(new ItemStack(AEBlocks.FLAWED_BUDDING_QUARTZ, 64)),
                new ItemStack(AEBlocks.FLAWLESS_BUDDING_QUARTZ, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/flaw_budding_quartz_to_flawless_budding_quartz"));

        assembly(Ingredient.of(new ItemStack(AEBlocks.ENERGY_CELL, 8)),
                new ItemStack(AEBlocks.DENSE_ENERGY_CELL, 1),
                AssemblyRecipe.AssemblyProgramType.DRILL)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/energy_cell_to_dense_energy_cell"));

        assembly(Ingredient.of(new ItemStack(AEBlocks.NOT_SO_MYSTERIOUS_CUBE, 1)),
                new ItemStack(AEBlocks.MYSTERIOUS_CUBE, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/not_so_mysterious_cube_to_mysterious_cube"));

        assembly(Ingredient.of(new ItemStack(Blocks.TNT, 1)),
                new ItemStack(AEBlocks.TINY_TNT, 8),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/tnt_to_tiny_tnt"));

        assembly(Ingredient.of(new ItemStack(AEItems.MATTER_BALL, 64)),
                new ItemStack(AEItems.SINGULARITY, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/matter_ball_to_singularity"));

        assembly(Ingredient.of(new ItemStack(AEItems.CERTUS_QUARTZ_CRYSTAL, 1)),
                new ItemStack(AEItems.CALCULATION_PROCESSOR_PRINT, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/certus_quartz_crystal_to_calculation_processor_print"));

        assembly(Ingredient.of(new ItemStack(Items.DIAMOND, 1)),
                new ItemStack(AEItems.ENGINEERING_PROCESSOR_PRINT, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/diamond_to_engineering_processor_print"));

        assembly(Ingredient.of(new ItemStack(Items.GOLD_INGOT, 1)),
                new ItemStack(AEItems.LOGIC_PROCESSOR_PRINT, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/gold_ingot_to_logic_processor_print"));

        assembly(Ingredient.of(new ItemStack(AEItems.SILICON, 1)),
                new ItemStack(AEItems.SILICON_PRINT, 1),
                AssemblyRecipe.AssemblyProgramType.LASER)
                .build(recipeOutput, AppliedPneumatics.makeId("assembly/silicon_to_silicon_print"));
    }

    // 配方路径统一
    private static ResourceLocation housingShapedId(ItemLike housing)
    {
        return AppliedPneumatics.makeId("cells/housing/" + BuiltInRegistries.ITEM.getKey(housing.asItem()).getPath());
    }
    private static ResourceLocation cellShapedId(ItemLike cell)
    {
        return AppliedPneumatics.makeId("cells/shaped/" + BuiltInRegistries.ITEM.getKey(cell.asItem()).getPath());
    }
    private static ResourceLocation cellShapelessId(ItemLike cell)
    {
        return AppliedPneumatics.makeId("cells/shapeless/" + BuiltInRegistries.ITEM.getKey(cell.asItem()).getPath());
    }

    // 添加所有元件
    private static void buildAllCellRecipe(Consumer<FinishedRecipe> recipeOutput)
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
        for(TierRow tierRow : commonTierRows)
        {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tierRow.cell)
                    .requires(APItems.AIR_CELL_SHELL.get())
                    .requires(tierRow.component)
                    .unlockedBy("has_correct_component", has(tierRow.component))
                    .save(recipeOutput, cellShapelessId(tierRow.cell));

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tierRow.portableCell)
                    .requires(tierRow.component)
                    .requires(AEBlocks.CHEST)
                    .requires(AEBlocks.ENERGY_CELL)
                    .requires(APItems.AIR_CELL_SHELL.get())
                    .unlockedBy("has_correct_component", has(tierRow.component))
                    .save(recipeOutput, cellShapelessId(tierRow.portableCell));
        }
        // m系列
        for(TierRow tierRow : megaTierRows)
        {
            ShapelessRecipeBuilder cellBuilder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tierRow.cell)
                    .requires(APItems.MEGA_AIR_CELL_SHELL.get())
                    .requires(tierRow.component)
                    .unlockedBy("has_correct_component", has(tierRow.component));
            ConditionalRecipe.builder()
                    .addCondition(new ModLoadedCondition(AppliedPneumatics.MEGA_CELL_MODID))
                    .addRecipe(cellBuilder::save)
                    .build(recipeOutput, cellShapelessId(tierRow.cell));

            ShapelessRecipeBuilder portableCellBuilder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tierRow.portableCell)
                    .requires(tierRow.component)
                    .requires(AEBlocks.CHEST)
                    .requires(AEBlocks.ENERGY_CELL)
                    .requires(APItems.MEGA_AIR_CELL_SHELL.get())
                    .unlockedBy("has_correct_component", has(tierRow.component));
            ConditionalRecipe.builder()
                    .addCondition(new ModLoadedCondition(AppliedPneumatics.MEGA_CELL_MODID))
                    .addRecipe(portableCellBuilder::save)
                    .build(recipeOutput, cellShapelessId(tierRow.portableCell));
        }
    }

    // 用于快速添加亚马龙交易
    private AmadronRecipeBuilder amadronStatic(AmadronTradeResource in, AmadronTradeResource out) {
        return new AmadronRecipeBuilder(in, out, true, 0)
                .addCriterion(getHasName(ModItems.AMADRON_TABLET.get()), has(ModItems.AMADRON_TABLET.get()));
    }

    // 快速添加压力室配方
    private PressureChamberRecipeBuilder pressureChamber(List<Ingredient> in, float pressure, ItemStack... out) {
        return new PressureChamberRecipeBuilder(in, pressure, out)
                .addCriterion(getHasName(ModBlocks.PRESSURE_CHAMBER_VALVE.get()), has(ModBlocks.PRESSURE_CHAMBER_VALVE.get()));
    }

    // 快速添加装配室配方
    private AssemblyRecipeBuilder assembly(Ingredient input, ItemStack output, AssemblyRecipe.AssemblyProgramType programType) {
        return new AssemblyRecipeBuilder(input, output, programType)
                .addCriterion(getHasName(ModBlocks.ASSEMBLY_CONTROLLER.get()), has(ModBlocks.ASSEMBLY_CONTROLLER.get()));
    }

    /** 用于描述元件与组件之间的对应关系 */
    private record TierRow(
            ItemLike component,
            ItemLike cell,
            ItemLike portableCell
    ) {}

}
