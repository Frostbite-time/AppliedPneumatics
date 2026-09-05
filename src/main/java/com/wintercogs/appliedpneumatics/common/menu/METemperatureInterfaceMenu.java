package com.wintercogs.appliedpneumatics.common.menu;

import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import com.wintercogs.appliedpneumatics.common.blocks.entitis.METemperatureInterfaceBlockEntity;
import com.wintercogs.appliedpneumatics.common.init.APMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class METemperatureInterfaceMenu extends UpgradeableMenu<METemperatureInterfaceBlockEntity>
{
    // 动作常量 - 改变期望气压值
    public static String changeExpectedTemperatureAction = "change_expected_temperature";

    // @GuiSync 同步字段（客户端可见最新值，走AE的DataSynchronization同步方案）
    // id从10开始，避免和父类冲突
    @GuiSync(10)
    public double latestTemperature = 0;
    @GuiSync(11)
    public double latestHeatCap = 0;
    @GuiSync(12)
    public double latestExpectedTemperature = 0f;

    public METemperatureInterfaceMenu(int id, Inventory playerInv, @NotNull METemperatureInterfaceBlockEntity host)
    {
        super(APMenus.ME_TEMPERATURE_INTERFACE_MENU.get(), id, playerInv, host);

        registerClientAction(changeExpectedTemperatureAction, Double.class, this::onClientChangeExpectedTemperature);
    }

    // 同步：把最新值写入 @GuiSync 字段，再交给基类广播
    @Override
    public void broadcastChanges()
    {
        var be = getHost();
        this.latestTemperature = be.getHeatHandler().getTemperature();
        this.latestHeatCap = be.getHeatHandler().getThermalCapacity();
        this.latestExpectedTemperature = be.getExpectedTemperature();

        // 最后触发基类调用，进行广播
        super.broadcastChanges();
    }

    // 客户端动作回调
    public void sendExpectedTemperatureActionToServer(double expectedTemperatureDelta)
    {
        sendClientAction(changeExpectedTemperatureAction, expectedTemperatureDelta);
    }

    private void onClientChangeExpectedTemperature(double delta)
    {
        var be = getHost();
        if (be.isRemoved()) return;
        double now = be.getExpectedTemperature();
        be.setExpectedTemperature(now + delta); // 内部自限幅
    }

    public @Nullable METemperatureInterfaceBlockEntity getBlockEntity()
    {
        return getHost();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return !getHost().isRemoved();
    }
}
