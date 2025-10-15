package com.wintercogs.appliedpneumatics.common.menu.host;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.contents.StackDependentSupplier;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.SupplierInternalInventory;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.function.BiConsumer;

public class AmadronWirelessTerminalMenuHost extends WirelessTerminalMenuHost<AmadronWirelessTerminalItem>
{
    // me网络存储、无线接入点连接状态均由父类处理
    // 样板槽位
    private final SupplierInternalInventory<InternalInventory> inventory;

    public AmadronWirelessTerminalMenuHost(AmadronWirelessTerminalItem item, Player player, ItemMenuHostLocator locator, BiConsumer<Player, ISubMenu> returnToMainMenu)
    {
        super(item, player, locator, returnToMainMenu);

        this.inventory = new SupplierInternalInventory<>(
                new StackDependentSupplier<>(
                        this::getItemStack,
                        stack -> createPatternInv(player, stack)));
    }

    // 两个槽位，槽位0放空白样板，槽位1出当前样板
    private static InternalInventory createPatternInv(Player player, ItemStack stack)
    {
        AppEngInternalInventory patternGrid = new AppEngInternalInventory(new InternalInventoryHost()
        {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv)
            {
                stack.set(APDataComponents.COMMON_ITEM_CONTENT, inv.toItemContainerContents());
            }

            @Override
            public boolean isClientSide()
            {
                return player.level().isClientSide();
            }
        }, 2);
        patternGrid.fromItemContainerContents(stack.getOrDefault(APDataComponents.COMMON_ITEM_CONTENT, ItemContainerContents.EMPTY));
        return patternGrid;
    }

    public InternalInventory getPatternInv()
    {
        return inventory;
    }

    @Override
    public boolean isValid()
    {
        return super.isValid() && getLinkStatus().connected() && getItem().getAECurrentPower(getItemStack()) > 0;
    }
}
