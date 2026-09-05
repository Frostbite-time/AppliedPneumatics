package com.wintercogs.appliedpneumatics.common.menu;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.MEPressureInterfaceBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MEPressureInterfaceMenu extends UpgradeableMenu<MEPressureInterfaceBlockEntity>
{
    // 动作常量 - 改变期望气压值
    public static String changeExpectedPressureAction = "change_expected_pressure";

    // @GuiSync 同步字段（客户端可见最新值，走AE的DataSynchronization同步方案）
    // id从10开始，避免和父类冲突
    @GuiSync(10)
    public int latestAir = 0;
    @GuiSync(11)
    public int latestVolume = 0;
    @GuiSync(12)
    public double latestExpectedPressure = 0f;
    @GuiSync(13)
    public double latestDangerPressure = 0f;

    public MEPressureInterfaceMenu(int id, Inventory playerInv, @NotNull MEPressureInterfaceBlockEntity host)
    {
        super(APMenus.ME_PRESSURE_INTERFACE_MENU.get(), id, playerInv, host);

        // 客户端->服务端（用 AE 的动作机制处理按钮/步进）
        registerClientAction(changeExpectedPressureAction, Float.class, this::onClientChangeExpectedPressure);
    }

    // 放除了升级槽之外的其他真实库存
    // 注：玩家槽位已经由UpgradeableMenu处理，不必再写
    @Override
    protected void setupInventorySlots()
    {
        // 你的机器输入槽（示例：索引0）
        AppEngSlot slot = new AppEngSlot(getHost().getInventory(), 0);
        // 用 AE 的语义化添加
        this.addSlot(slot, SlotSemantics.MACHINE_INPUT);
    }

    // 同步：把最新值写入 @GuiSync 字段，再交给基类广播
    @Override
    public void broadcastChanges()
    {
        var be = getHost();
        this.latestAir = be.getAirHandler().getAir();
        this.latestVolume = be.getVolume();
        this.latestExpectedPressure = be.getExpectedPressure();
        this.latestDangerPressure = be.getAirHandler().getDangerPressure();

        // 最后触发基类调用，进行广播
        super.broadcastChanges();
    }

    // 客户端动作回调
    public void sendExpectedPressureActionToServer(float expectedPressureDelta)
    {
        sendClientAction(changeExpectedPressureAction, expectedPressureDelta);
    }

    private void onClientChangeExpectedPressure(float delta)
    {
        var be = getHost();
        if (be.isRemoved()) return;
        float now = be.getExpectedPressure();
        be.setExpectedPressure(now + delta); // 内部自限幅
    }

    public @Nullable MEPressureInterfaceBlockEntity getBlockEntity()
    {
        return getHost();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return !getHost().isRemoved();
    }
}
