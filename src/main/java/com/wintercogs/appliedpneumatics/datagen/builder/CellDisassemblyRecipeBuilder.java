package com.wintercogs.appliedpneumatics.datagen.builder;


import appeng.recipes.game.StorageCellDisassemblyRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AE2 “存储盘拆解”配方 Builder（精简版）。
 * 仅使用：
 * type: "ae2:storage_cell_disassembly"
 * keys: "cell", "cell_disassembly_items"
 */
public class CellDisassemblyRecipeBuilder
{
    private final ItemLike cell;                 // 要被拆解的存储盘
    private final List<ItemStack> outputs = new ArrayList<>();
    private final List<ICondition> conditions = new ArrayList<>();

    // 可选命名控制
    private String overrideNamespace = null;     // 默认用 cell 的 namespace
    private String folder = "";                  // 可选子目录（如 "storage"）

    private CellDisassemblyRecipeBuilder(ItemLike cell)
    {
        this.cell = Objects.requireNonNull(cell, "cell");
    }

    /**
     * 入口：以要拆解的存储盘（ItemLike）开建
     */
    public static CellDisassemblyRecipeBuilder cell(ItemLike cell)
    {
        return new CellDisassemblyRecipeBuilder(cell);
    }

    /**
     * 添加一个输出（默认 1 个）
     */
    public CellDisassemblyRecipeBuilder add(ItemLike item)
    {
        return add(item, 1);
    }

    /**
     * 添加一个输出（指定数量）
     */
    public CellDisassemblyRecipeBuilder add(ItemLike item, int count)
    {
        outputs.add(new ItemStack(item.asItem(), count));
        return this;
    }

    /**
     * 直接添加 ItemStack（可带 Data Components/NBT）
     */
    public CellDisassemblyRecipeBuilder add(ItemStack stack)
    {
        outputs.add(stack.copy());
        return this;
    }

    /**
     * 条件：任意 ICondition
     */
    public CellDisassemblyRecipeBuilder when(ICondition condition)
    {
        conditions.add(Objects.requireNonNull(condition));
        return this;
    }

    /**
     * 条件：仅在指定 mod 存在时生效
     */
    public CellDisassemblyRecipeBuilder whenModLoaded(String modid)
    {
        return when(new ModLoadedCondition(modid));
    }

    /**
     * 可选：强制命名空间
     */
    public CellDisassemblyRecipeBuilder namespace(String namespace)
    {
        this.overrideNamespace = namespace;
        return this;
    }

    /**
     * 可选：子目录（例："storage" -> storage/<cell>_disassembly）
     */
    public CellDisassemblyRecipeBuilder folder(String folder)
    {
        this.folder = folder == null ? "" : folder;
        return this;
    }

    /**
     * 用默认 id 保存：<ns>:<folder?/><cell_path>_disassembly
     */
    public void save(RecipeOutput out)
    {
        save(out, defaultId());
    }

    /**
     * 指定 path（仍用默认/覆盖的 namespace）
     */
    public void save(RecipeOutput out, String path)
    {
        var base = BuiltInRegistries.ITEM.getKey(cell.asItem());
        String ns = overrideNamespace != null ? overrideNamespace : base.getNamespace();
        String fullPath = (folder == null || folder.isEmpty()) ? path : (folder + "/" + path);
        save(out, ResourceLocation.fromNamespaceAndPath(ns, fullPath));
    }

    /**
     * 完全自定义 id 保存
     */
    public void save(RecipeOutput out, ResourceLocation id)
    {
        if (outputs.isEmpty())
        {
            throw new IllegalStateException("cell_disassembly recipe must have at least one output");
        }

        // 直接 new 出 AE2 的配方实例（无需 registries）
        var recipe = new StorageCellDisassemblyRecipe(
                cell.asItem(),
                List.copyOf(outputs) // 保护性复制
        );

        if (conditions.isEmpty())
        {
            out.accept(id, recipe, null); // 无条件：标准提交
        }
        else
        {
            out.withConditions(conditions.toArray(ICondition[]::new))
                    .accept(id, recipe, null); // 有条件：写入 neoforge:conditions
        }
    }

    private ResourceLocation defaultId()
    {
        var itemId = BuiltInRegistries.ITEM.getKey(cell.asItem());
        String ns = (overrideNamespace != null) ? overrideNamespace : itemId.getNamespace();
        String path = itemId.getPath() + "_disassembly";
        if (folder != null && !folder.isEmpty()) path = folder + "/" + path;
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }
}
