package com.wintercogs.appliedpneumatics.common.items;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.AEConfig;
import appeng.core.localization.PlayerMessages;
import appeng.core.localization.Tooltips;
import appeng.items.storage.StorageCellTooltipComponent;
import appeng.recipes.game.StorageCellDisassemblyRecipe;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AirStorageCell extends Item implements ICellWorkbenchItem, IAirStorageCell
{
    private final double idleDrain;
    private final int totalBytes;

    public AirStorageCell(Properties properties, double idleDrain, int kilobytes)
    {
        super(properties);
        this.idleDrain = idleDrain;
        this.totalBytes = kilobytes * 1024;
    }

    public static int getColor(ItemStack stack, int tintIndex)
    {
        if (tintIndex != 1) return 0xFFFFFF; // 白

        long stored = IAirStorageCell.getStoredAir(stack);
        CellState state = IAirStorageCell.calcState(((IAirStorageCell) stack.getItem()).getTotalBytes(), stored);
        return state.getStateColor();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull TooltipContext context,
                                @NotNull List<Component> lines,
                                @NotNull TooltipFlag advancedTooltips)
    {
        if (Platform.isClient())
        {
            // 基础容量/使用
            long stored = IAirStorageCell.getStoredAir(stack);
            long used = IAirStorageCell.usedBytes(stored);
            lines.add(Tooltips.bytesUsed(used, getTotalBytes()));
            // 单类型：0 或 1
            int typesUsed = stored > 0 ? 1 : 0;
            lines.add(Tooltips.typesUsed(typesUsed, 1));
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack)
    {
        var showUpg = AEConfig.instance().isTooltipShowCellUpgrades();
        var showCnt = AEConfig.instance().isTooltipShowCellContent();

        // 升级图标
        var upgrades = new ArrayList<ItemStack>();
        if (showUpg) getUpgrades(stack).forEach(upgrades::add);

        // 内容预览（只有 Air 一种）
        List<GenericStack> content = Collections.emptyList();
        boolean hasMore = false;
        if (showCnt)
        {
            long stored = IAirStorageCell.getStoredAir(stack);
            if (stored > 0)
            {
                content = List.of(new GenericStack(AirKey.INSTANCE, stored));
            }
        }

        return Optional.of(new StorageCellTooltipComponent(
                upgrades, content, hasMore, true  // 显示进度条
        ));
    }

    @Override
    public int getTotalBytes()
    {
        return this.totalBytes;
    }

    @Override
    public double getIdleDrain()
    {
        return idleDrain;
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is)
    {
        return UpgradeInventories.forItem(is, 2);
    }

    // 仅实现接口，此元件模糊匹配无意义
    @Override
    public FuzzyMode getFuzzyMode(ItemStack is)
    {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode)
    {
    }


    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand)
    {
        this.disassembleDrive(player.getItemInHand(hand), level, player);
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()),
                player.getItemInHand(hand));
    }

    private boolean disassembleDrive(ItemStack stack, Level level, Player player)
    {
        if (!InteractionUtil.isInAlternateUseMode(player))
        {
            return false;
        }

        var disassembledStacks = StorageCellDisassemblyRecipe.getDisassemblyResult(level, stack.getItem());
        if (disassembledStacks.isEmpty())
        {
            return false;
        }

        var playerInventory = player.getInventory();
        if (playerInventory.getSelected() != stack)
        {
            return false;
        }

        var inv = StorageCells.getCellInventory(stack, null);
        if (inv != null && !inv.getAvailableStacks().isEmpty())
        {
            player.displayClientMessage(PlayerMessages.OnlyEmptyCellsCanBeDisassembled.text(), true);
            return false;
        }

        playerInventory.setItem(playerInventory.selected, ItemStack.EMPTY);

        for (var disassembledStack : disassembledStacks)
        {
            playerInventory.placeItemBackInInventory(disassembledStack.copy());
        }

        getUpgrades(stack).forEach(playerInventory::placeItemBackInInventory);

        return true;
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, UseOnContext context)
    {
        return this.disassembleDrive(stack, context.getLevel(), context.getPlayer())
                ? InteractionResult.sidedSuccess(context.getLevel().isClientSide())
                : InteractionResult.PASS;
    }
}
