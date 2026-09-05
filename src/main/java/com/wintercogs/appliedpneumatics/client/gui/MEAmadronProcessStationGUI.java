package com.wintercogs.appliedpneumatics.client.gui;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.StyleManager;
import com.wintercogs.appliedpneumatics.common.menu.MEAmadronProcessStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MEAmadronProcessStationGUI extends UpgradeableScreen<MEAmadronProcessStationMenu>
{
    public static final String COMMON = "/screens/me_amadron_process_station.json";
    public static final String EXTENDED = "/screens/me_amadron_extended_process_station.json";

    private final String usingStyle;
    private final Button cancelButton;

    public MEAmadronProcessStationGUI(MEAmadronProcessStationMenu menu, Inventory inv, Component title, String stylePath)
    {
        super(menu, inv, title, StyleManager.loadStyleDoc(stylePath));
        cancelButton = Button.builder(Component.translatable("menu.appliedpneumatics.button.cancel_jobs"), button -> menu.senCancelJobAction()).build();
        cancelButton.setTooltip(Tooltip.create(Component.translatable("menu.appliedpneumatics.button.cancel_jobs.tooltip")));
        widgets.add("cancel_jobs_button", this.cancelButton);
        widgets.addOpenPriorityButton();
        usingStyle = stylePath;
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY)
    {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        if (this.usingStyle.equals(EXTENDED))
            guiGraphics.drawString(font, Component.translatable("menu.label.appliedpneumatics.me_amadron_process_station.job_amount", menu.latestJobs), 8, 75, 4210752, false);
        else
            guiGraphics.drawString(font, Component.translatable("menu.label.appliedpneumatics.me_amadron_process_station.job_amount", menu.latestJobs), 8, 39, 4210752, false);
    }
}
