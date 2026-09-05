package com.wintercogs.appliedpneumatics.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;


/**
 * 在UI中绘制水面纹理的辅助类
 */
public class IngredientRenderer
{
    private static final int TEXTURE_SIZE = 16;
    private static final int MIN_FLUID_HEIGHT = 1;

    /**
     * 为传入的液体画一个标准的16x16的贴图
     */
    public static void darwFluidAs16WHTiledSprite(@NotNull GuiGraphics guiGraphics, @NotNull Fluid fluid, int posX, int posY)
    {
        IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation still = props.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);

        if (sprite != null && sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
        {
            int tint = IClientFluidTypeExtensions.of(fluid).getTintColor();
            // 复用项目现有的绘制工具
            drawTiledSprite(guiGraphics, 16, 16, tint, 16, sprite, posX, posY);
        }
    }

    /**
     * 为传入的液体画一个标准的16x16的贴图
     */
    public static void darwFluidAs16WHTiledSprite(@NotNull GuiGraphics guiGraphics, @NotNull FluidStack fluidStack, int posX, int posY)
    {
        if (!fluidStack.isEmpty())
        {
            Fluid fluid = fluidStack.getFluid();
            IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(fluid);
            ResourceLocation still = props.getStillTexture(fluidStack);
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);

            if (sprite != null && sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
            {
                int tint = IClientFluidTypeExtensions.of(fluid).getTintColor();
                // 复用项目现有的绘制工具
                drawTiledSprite(guiGraphics, 16, 16, tint, 16, sprite, posX, posY);
            }
        }
    }

    public static void drawTiledSprite(GuiGraphics guiGraphics, final int tiledWidth, final int tiledHeight, int color, long scaledAmount, TextureAtlasSprite sprite, int posX, int posY)
    {

        RenderSystem.enableBlend();


        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        Matrix4f matrix = guiGraphics.pose().last().pose();
        setGLColorFromInt(color);

        final int xTileCount = tiledWidth / TEXTURE_SIZE;
        final int xRemainder = tiledWidth - (xTileCount * TEXTURE_SIZE);
        final long yTileCount = scaledAmount / TEXTURE_SIZE;
        final long yRemainder = scaledAmount - (yTileCount * TEXTURE_SIZE);

        final int yStart = tiledHeight + posY;

        for (int xTile = 0; xTile <= xTileCount; xTile++)
        {
            for (int yTile = 0; yTile <= yTileCount; yTile++)
            {
                int width = (xTile == xTileCount) ? xRemainder : TEXTURE_SIZE;
                long height = (yTile == yTileCount) ? yRemainder : TEXTURE_SIZE;
                int x = posX + (xTile * TEXTURE_SIZE);
                int y = yStart - ((yTile + 1) * TEXTURE_SIZE);
                if (width > 0 && height > 0)
                {
                    long maskTop = TEXTURE_SIZE - height;
                    int maskRight = TEXTURE_SIZE - width;

                    drawTextureWithMasking(matrix, x, y, sprite, maskTop, maskRight, 100);
                }
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

    }

    private static void setGLColorFromInt(int color)
    {
        float red = ((color >> 16) & 255) / 256f;
        float green = ((color >> 8) & 255) / 256f;
        float blue = (color & 255) / 256f;
        //float alpha = ((color >> 24) & 0xFF) / 255F;
        float alpha = 1;

        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    private static void drawTextureWithMasking(Matrix4f matrix, float xCoord, float yCoord, TextureAtlasSprite textureSprite, long maskTop, long maskRight, float zLevel)
    {
        float uMin = textureSprite.getU0();
        float uMax = textureSprite.getU1();
        float vMin = textureSprite.getV0();
        float vMax = textureSprite.getV1();
        uMax = uMax - (maskRight / 16F * (uMax - uMin));
        vMax = vMax - (maskTop / 16F * (vMax - vMin));

        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, xCoord, yCoord + 16, zLevel).uv(uMin, vMax).endVertex();
        bufferBuilder.vertex(matrix, xCoord + 16 - maskRight, yCoord + 16, zLevel).uv(uMax, vMax).endVertex();
        bufferBuilder.vertex(matrix, xCoord + 16 - maskRight, yCoord + maskTop, zLevel).uv(uMax, vMin).endVertex();
        bufferBuilder.vertex(matrix, xCoord, yCoord + maskTop, zLevel).uv(uMin, vMin).endVertex();
        tessellator.end();
    }
}
