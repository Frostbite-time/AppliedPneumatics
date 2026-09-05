package com.wintercogs.appliedpneumatics.common.menu;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;
import com.wintercogs.appliedpneumatics.client.gui.MEAmadronProcessStationGUI;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEAmadronProcessStationBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MEAmadronProcessStationMenu extends UpgradeableMenu<MEAmadronProcessStationBlockEntity>
{
    private static String cancelAllJobsAction = "cancel_all_jobs";

    @GuiSync(10)
    public int latestJobs = 0;

    // 双端构造，AE自动传递host
    public MEAmadronProcessStationMenu(int id, Inventory playerInv, @NotNull MEAmadronProcessStationBlockEntity host)
    {
        super(APMenus.ME_AMADRON_PROCESS_STATION_MENU.get(), id, playerInv, host);
        registerClientAction(cancelAllJobsAction, this::onJobCancel);
    }

    private void onJobCancel()
    {
        MEAmadronProcessStationBlockEntity be = getBlockEntity();
        if (be != null)
            be.cancelAllJobs(Component.translatable("amadron.appliedpneumatics.process_fail.order_cancel", be.getBlockPos().toShortString()));
    }

    public void senCancelJobAction()
    {
        sendClientAction(cancelAllJobsAction);
    }

    public String getScreenStyle()
    {
        if (getBlockEntity() != null)
        {
            if (getBlockEntity().getTerminalPatternInventory().size() > 9)
                return MEAmadronProcessStationGUI.EXTENDED;
            else
                return MEAmadronProcessStationGUI.COMMON;
        }
        return MEAmadronProcessStationGUI.COMMON;
    }

    // 放除了升级槽之外的其他真实库存
    // 注：玩家槽位已经由UpgradeableMenu处理，不必再写
    @Override
    protected void setupInventorySlots()
    {
        if (getBlockEntity() == null) return;

        for (int i = 0; i < getBlockEntity().getTerminalPatternInventory().size(); i++)
        {
            AppEngSlot slot = new AppEngSlot(getHost().getTerminalPatternInventory(), i);
            this.addSlot(slot, SlotSemantics.ENCODED_PATTERN);
        }
        ConfigMenuInventory inputWrapper = getBlockEntity().getInputInv().createMenuWrapper();
        for (int i = 0; i < inputWrapper.size(); i++)
        {
            AppEngSlot slot = new AppEngSlot(inputWrapper, i);
            this.addSlot(slot, SlotSemantics.MACHINE_INPUT);
        }
        ConfigMenuInventory outputWrapper = getBlockEntity().getOutputInv().createMenuWrapper();
        for (int i = 0; i < outputWrapper.size(); i++)
        {
            AppEngSlot slot = new AppEngSlot(outputWrapper, i)
            {
                @Override
                public boolean mayPlace(ItemStack stack)
                {
                    return false;
                }
            };
            this.addSlot(slot, SlotSemantics.MACHINE_OUTPUT);
        }
    }

    @Override
    public void broadcastChanges()
    {
        latestJobs = getBlockEntity() == null ? 0 : getBlockEntity().getJobAmount();
        super.broadcastChanges();
    }

    public @Nullable MEAmadronProcessStationBlockEntity getBlockEntity()
    {
        return getHost();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return !getHost().isRemoved();
    }
}
