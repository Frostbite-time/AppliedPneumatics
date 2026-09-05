package com.wintercogs.appliedpneumatics.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GuiRenderHelper
{

    /**
     * 绘制具有边框的九宫格纹理，并自动处理拉伸（统一使用 9 参 blit）
     *
     * @param guiGraphics  GUI 渲染上下文
     * @param texture      纹理资源位置
     * @param x            目标位置 X
     * @param y            目标位置 Y
     * @param width        目标总宽度
     * @param height       目标总高度
     * @param borderTop    上边框大小 (像素)
     * @param borderBottom 下边框大小 (像素)
     * @param borderLeft   左边框大小 (像素)
     * @param borderRight  右边框大小 (像素)
     * @param origWidth    原始纹理宽度
     * @param origHeight   原始纹理高度
     */
    public static void renderBorderedPanel(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x, int y,
            int width, int height,
            int borderTop, int borderBottom,
            int borderLeft, int borderRight,
            int origWidth, int origHeight)
    {

        // === 1. 四个角（不拉伸） ===
        // 左上
        guiGraphics.blit(texture,
                x, y,
                borderLeft, borderTop,
                0, 0,
                borderLeft, borderTop,
                origWidth, origHeight);

        // 右上
        guiGraphics.blit(texture,
                x + width - borderRight, y,
                borderRight, borderTop,
                origWidth - borderRight, 0,
                borderRight, borderTop,
                origWidth, origHeight);

        // 左下
        guiGraphics.blit(texture,
                x, y + height - borderBottom,
                borderLeft, borderBottom,
                0, origHeight - borderBottom,
                borderLeft, borderBottom,
                origWidth, origHeight);

        // 右下
        guiGraphics.blit(texture,
                x + width - borderRight, y + height - borderBottom,
                borderRight, borderBottom,
                origWidth - borderRight, origHeight - borderBottom,
                borderRight, borderBottom,
                origWidth, origHeight);

        // === 2. 四条边（单向拉伸） ===
        int dstEdgeW = width - borderLeft - borderRight;
        int dstEdgeH = height - borderTop - borderBottom;
        int srcEdgeW = origWidth - borderLeft - borderRight;
        int srcEdgeH = origHeight - borderTop - borderBottom;

        // 上边
        if (borderTop > 0)
        {
            guiGraphics.blit(texture,
                    x + borderLeft, y,
                    dstEdgeW, borderTop,
                    borderLeft, 0,
                    srcEdgeW, borderTop,
                    origWidth, origHeight);
        }

        // 下边
        if (borderBottom > 0)
        {
            guiGraphics.blit(texture,
                    x + borderLeft, y + height - borderBottom,
                    dstEdgeW, borderBottom,
                    borderLeft, origHeight - borderBottom,
                    srcEdgeW, borderBottom,
                    origWidth, origHeight);
        }

        // 左边
        if (borderLeft > 0)
        {
            guiGraphics.blit(texture,
                    x, y + borderTop,
                    borderLeft, dstEdgeH,
                    0, borderTop,
                    borderLeft, srcEdgeH,
                    origWidth, origHeight);
        }

        // 右边
        if (borderRight > 0)
        {
            guiGraphics.blit(texture,
                    x + width - borderRight, y + borderTop,
                    borderRight, dstEdgeH,
                    origWidth - borderRight, borderTop,
                    borderRight, srcEdgeH,
                    origWidth, origHeight);
        }

        // === 3. 中心（双向拉伸） ===
        guiGraphics.blit(texture,
                x + borderLeft, y + borderTop,
                dstEdgeW, dstEdgeH,
                borderLeft, borderTop,
                srcEdgeW, srcEdgeH,
                origWidth, origHeight);
    }

    /**
     * 绘制整张纹理并缩放到指定宽高
     * <p>已帮你绑定 shader & 纹理；调用方不用再 setShaderTexture。</p>
     *
     * @param guiGraphics 渲染上下文
     * @param texture     纹理资源路径（不需要是在图集里的）
     * @param x           目标左上角 X
     * @param y           目标左上角 Y
     * @param width       希望绘制出的宽度
     * @param height      希望绘制出的高度
     */
    public static void renderFullTexture(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x, int y,
            int width, int height,
            int originalWidth, int originalHeight)
    {

        // 1. 绑定默认 PositionTex shader（同 blitSprite 内部做的事）
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        // 3. 把整张图 (0,0 → texW,texH) 按目标宽高拉伸
        guiGraphics.blit(texture,
                x, y,                     // 目标起点
                width, height,            // 目标尺寸
                0, 0,                     // 纹理起点 (u,v)
                originalWidth, originalHeight,               // 采样整张纹理
                originalWidth, originalHeight);              // 纹理原尺寸（用于 UV 归一化）
    }

    public static void drawRightAnchoredText(GuiGraphics guiGraphics,
                                             Font font,
                                             Component text,      // 要绘的文字
                                             int xRight,          // 想让文字右边对齐到的 x 坐标
                                             int y,               // y 坐标
                                             int color,
                                             boolean dropShadow)
    {         // 颜色 0xAARRGGBB
        // 1. 计算文字宽度
        int width = font.width(text);

        // 2. 计算左上角起点
        int xStart = xRight - width;

        // 3. 绘制
        guiGraphics.drawString(font, text, xStart, y, color, dropShadow);
    }


    /**
     * 在水平区间 [xA, xB] 的中央绘制一段文本。
     *
     * @param guiGraphics 渲染上下文
     * @param font        字体
     * @param text        要绘制的文字
     * @param xA          区域端点 A（左/右都可，内部会自动取 min/max）
     * @param xB          区域端点 B（左/右都可）
     * @param y           文本左上角的 y 坐标
     * @param color       颜色（0xAARRGGBB）
     * @param dropShadow  是否绘制阴影
     */
    public static void drawCenteredInRegion(GuiGraphics guiGraphics,
                                            Font font,
                                            Component text,
                                            int xA, int xB,
                                            int y,
                                            int color,
                                            boolean dropShadow)
    {
        // 1) 规范化左右边界
        int left = Math.min(xA, xB);
        int right = Math.max(xA, xB);

        // 2) 计算文本宽与区域宽
        int textWidth = font.width(text);
        int regionWidth = right - left;

        // 3) 居中起点
        int xStart = left + (regionWidth - textWidth) / 2;

        // 4) 绘制（注意：如果文本比区域还宽，会自然溢出；需要裁剪/省略号的话另行处理）
        guiGraphics.drawString(font, text, xStart, y, color, dropShadow);
    }
}
