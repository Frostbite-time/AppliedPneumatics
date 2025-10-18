//package com.wintercogs.appliedpneumatics.client.gui.widgets;
//
//import appeng.core.AppEng;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.wintercogs.appliedpneumatics.AppliedPneumatics;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.util.Mth;
//
///** 完成美工大人的任务罢了 */
//public class AE2TinyButton extends AE2Button
//{
//    protected static final WidgetSprites SPRITES = new WidgetSprites(
//            ResourceLocation.fromNamespaceAndPath(AppliedPneumatics.MODID, "ae_tiny_button"),
//            AppEng.makeId("button_disabled"),
//            ResourceLocation.fromNamespaceAndPath(AppliedPneumatics.MODID, "ae_tiny_button_highlighted"));
//
//    public AE2TinyButton(int pX, int pY, int pWidth, int pHeight, Component component, OnPress onPress)
//    {
//        super(pX, pY, pWidth, pHeight, component, onPress);
//    }
//
//    public AE2TinyButton(Component component, OnPress onPress)
//    {
//        super(component, onPress);
//    }
//
//
//    @Override
//    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
//        Minecraft minecraft = Minecraft.getInstance();
//        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
//        RenderSystem.enableBlend();
//        RenderSystem.enableDepthTest();
//        pGuiGraphics.blitSprite(SPRITES.get(this.active, this.isHovered()), this.getX(), this.getY(), this.getWidth(),
//                this.getHeight());
//        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
//        if (!this.active) {
//            this.renderButtonText(pGuiGraphics, minecraft.font, 2, 0x413f54 | Mth.ceil(this.alpha * 255.0F) << 24, -1);
//        } else if (this.isHovered()) {
//            this.renderButtonText(pGuiGraphics, minecraft.font, 2, 0x517497 | Mth.ceil(this.alpha * 255.0F) << 24, 0);
//        } else {
//            this.renderButtonText(pGuiGraphics, minecraft.font, 2, 0xf2f2f2 | Mth.ceil(this.alpha * 255.0F) << 24, 1);
//        }
//    }
//}
