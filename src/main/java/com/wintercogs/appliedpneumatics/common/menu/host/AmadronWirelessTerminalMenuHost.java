package com.wintercogs.appliedpneumatics.common.menu.host;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.wintercogs.appliedpneumatics.common.items.AmadronWirelessTerminalItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class AmadronWirelessTerminalMenuHost extends WirelessTerminalMenuHost implements InternalInventoryHost
{
    // me网络存储、无线接入点连接状态均由父类处理

    /**
     * 样板槽序列化名
     */
    private static final String PATTERN_INV_NAME = "amadron_pattern_inv";

    /**
     * 物品类记录
     */
    private final AmadronWirelessTerminalItem terminalItem;

    /**
     * 样板槽位
     */
    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 2);

    public AmadronWirelessTerminalMenuHost(Player player, @Nullable Integer slot, ItemStack itemStack, BiConsumer<Player, ISubMenu> returnToMainMenu)
    {
        super(player, slot, itemStack, returnToMainMenu);
        Item item = itemStack.getItem();
        if (item instanceof AmadronWirelessTerminalItem ti)
        {
            terminalItem = ti;
            this.inventory.readFromNBT(this.getItemStack().getOrCreateTag(), PATTERN_INV_NAME);
        }
        else
        {
            throw new IllegalArgumentException("Can't build AmadronWirelessTerminalMenuHost with invalid item");
        }
    }

    @Override
    public void saveChanges()
    {
        this.inventory.writeToNBT(this.getItemStack().getOrCreateTag(), PATTERN_INV_NAME);
    }

    @Override
    public void onChangeInventory(InternalInventory internalInventory, int i)
    {

    }

    public InternalInventory getPatternInv()
    {
        return inventory;
    }

    public AmadronWirelessTerminalItem getTerminalItem()
    {
        return terminalItem;
    }

//    @Override
//    public boolean isValid()
//    {
//        return
//    }
}
