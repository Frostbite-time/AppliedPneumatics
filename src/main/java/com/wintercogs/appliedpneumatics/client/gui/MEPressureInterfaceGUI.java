package com.wintercogs.appliedpneumatics.client.gui;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.StyleManager;
import com.wintercogs.appliedpneumatics.common.menu.MEPressureInterfaceMenu;
import com.wintercogs.appliedpneumatics.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;


public class MEPressureInterfaceGUI extends UpgradeableScreen<MEPressureInterfaceMenu>
{
    // 将使用样式 JSON，背景由样式管理
    public MEPressureInterfaceGUI(MEPressureInterfaceMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title, StyleManager.loadStyleDoc("/screens/me_pressure_interface.json"));

        widgets.addButton("add_button_1", Component.literal("+1"), () -> {
            float mult = hasShiftDown() ? 5 : 1;
            menu.sendExpectedPressureActionToServer(1 * mult);
        });
        widgets.addButton("add_button_01", Component.literal("+0.1"), () -> {
            float mult = hasShiftDown() ? 5 : 1;
            menu.sendExpectedPressureActionToServer(0.1f * mult);
        });
        widgets.addButton("reduce_button_01", Component.literal("-0.1"), () -> {
            float mult = hasShiftDown() ? 5 : 1;
            menu.sendExpectedPressureActionToServer(-0.1f * mult);
        });
        widgets.addButton("reduce_button_1", Component.literal("-1"), () -> {
            float mult = hasShiftDown() ? 5 : 1;
            menu.sendExpectedPressureActionToServer(-1 * mult);
        });
    }

    // 代码中写一些文本绘制
    // AE的json中似乎无法传入动态数据
    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY)
    {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        String airStr = String.format(Locale.ROOT, "%.1f", (float) menu.latestAir / 1000f);
        String maxStr = String.format(Locale.ROOT, "%.0f", (float) menu.latestVolume / 1000f);
        String currentPressureStr = String.format(Locale.ROOT, "%.0f", (float) menu.latestAir / (float) menu.latestVolume);
        String pressureStr = String.format(Locale.ROOT, "%.1f", menu.latestExpectedPressure);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.translatable("menu.label.appliedpneumatics.me_pressure_interface.air_amount", airStr, maxStr, currentPressureStr), 13, 148, 22, 4210752, false);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.translatable("menu.label.appliedpneumatics.me_pressure_interface.max_pressure", menu.latestDangerPressure), 13, 148, 38, 4210752, false);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.literal(pressureStr), 13, 168, 82, 4210752, false);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.translatable("menu.label.appliedpneumatics.me_pressure_interface.expected_pressure_text"), 13, 168, 60, 4210752, false);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.translatable("menu.label.appliedpneumatics.me_pressure_interface.expected_pressure_change_mult_text"), 13, 168, 100, 4210752, false);
    }
}
