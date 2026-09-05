package com.wintercogs.appliedpneumatics.client.gui;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.StyleManager;
import com.wintercogs.appliedpneumatics.common.menu.METemperatureInterfaceMenu;
import com.wintercogs.appliedpneumatics.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public class METemperatureInterfaceGUI extends UpgradeableScreen<METemperatureInterfaceMenu>
{
    public METemperatureInterfaceGUI(METemperatureInterfaceMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title, StyleManager.loadStyleDoc("/screens/me_temperature_interface.json"));

        widgets.addButton("add_button_10", Component.literal("+10"), () -> {
            double mult = hasShiftDown() ? 10 : 1;
            menu.sendExpectedTemperatureActionToServer(10 * mult);
        });
        widgets.addButton("add_button_1", Component.literal("+1"), () -> {
            double mult = hasShiftDown() ? 10 : 1;
            menu.sendExpectedTemperatureActionToServer(1 * mult);
        });
        widgets.addButton("reduce_button_1", Component.literal("-1"), () -> {
            double mult = hasShiftDown() ? 10 : 1;
            menu.sendExpectedTemperatureActionToServer(-1 * mult);
        });
        widgets.addButton("reduce_button_10", Component.literal("-10"), () -> {
            double mult = hasShiftDown() ? 10 : 1;
            menu.sendExpectedTemperatureActionToServer(-10 * mult);
        });
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY)
    {
        // 有关温度的部分，减去273，转为摄氏度
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        String temperatureStr = String.format(Locale.ROOT, "%.1f", menu.latestTemperature - 273) + "℃";
        String heatCapStr = String.format(Locale.ROOT, "%.0f", menu.latestHeatCap);
        String expectedTemperatureStr = String.format(Locale.ROOT, "%.1f", menu.latestExpectedTemperature - 273);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.translatable("menu.label.appliedpneumatics.me_temperature_interface.temperature", temperatureStr), 13, 168, 22, 4210752, false);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.translatable("menu.label.appliedpneumatics.me_temperature_interface.heat_cap", heatCapStr), 13, 168, 38, 4210752, false);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.literal(expectedTemperatureStr), 13, 168, 82, 4210752, false);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.translatable("menu.label.appliedpneumatics.me_temperature_interface.expected_temperature_text"), 13, 168, 60, 4210752, false);
        GuiRenderHelper.drawCenteredInRegion(guiGraphics, this.font, Component.translatable("menu.label.appliedpneumatics.me_temperature_interface.expected_temperature_change_mult_text"), 13, 168, 100, 4210752, false);
    }
}
