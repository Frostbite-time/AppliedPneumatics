package com.wintercogs.appliedpneumatics.client.me.render;

import appeng.api.client.AEKeyRenderHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AirKeyRenderHandler implements AEKeyRenderHandler<AirKey>
{
    /**
     * PNC 空气粒子整图 32×32
     */
    private static final ResourceLocation AIR_PARTICLE_TEX =
            new ResourceLocation("pneumaticcraft", "textures/particle/air_particle.png");

    /**
     * 槽位尺寸
     */
    private static final int SLOT_SIZE = 16;

    /**
     * 发射密度（每 tick 粒子数）
     */
    private static final int EMIT_MIN = 3;
    private static final int EMIT_MAX = 10;

    /**
     * 运动与显示参数
     */
    private static final float SPEED_PX_PER_TICK = 0.3f; // 速率
    private static final int BASE_SCREEN_SIZE = 3; // 粒子在屏幕上的基准缩放尺寸（像素）
    private static final int SIZE_JITTER_PX = 1; // 尺寸微抖动（±1）
    private static final int MAX_TICKS_LIFE = 50; // 兜底最大寿命（tick）
    private static final float START_ALPHA = 0.9f; // 最高透明度
    private static final float Z_LAYER = 0.01f; // 用 pose.translate 控制深度

    /**
     * 全局共享的发射器（在(0,0)-(16,16)坐标系内运动）
     */
    private final Emitter sharedEmitter = new Emitter();

    @Override
    public void drawInGui(Minecraft mc, GuiGraphics gg, int x, int y, AirKey key)
    {
        Level level = mc.level;
        if (level == null) return;

        // 时间统一为 tick：gameTime(tick) + partial(tick)
        final long gameTime = level.getGameTime();
        final float partial = (mc.getFrameTimeNs() / 1_000_000_000f) * 20f;
        final float nowT = gameTime + partial;

        // 渲染状态
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, AIR_PARTICLE_TEX);

        // 全局仅按 tick 发射一次
        sharedEmitter.tickAndSpawn(level, gameTime);

        gg.pose().pushPose();
        gg.pose().translate(0, 0, Z_LAYER);
        // 用同一套粒子，在本槽位位置进行偏移渲染
        sharedEmitter.drawAt(gg, nowT, x, y);
        gg.pose().popPose();

        gg.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    @Override
    public void drawOnBlockFace(PoseStack poseStack, MultiBufferSource buffers, AirKey what, float scale, int combinedLight, Level level)
    {

    }

    @Override
    public Component getDisplayName(AirKey stack)
    {
        return AirKeyType.NAME;
    }

    // 内部工具-------------------------------------------------------------------------------------------------

    /**
     * 共享发射器：
     * 在规范槽位（左上角 0,0；可运动区域 [0, 16 - size]）中发射与推进粒子。
     * 各槽位绘制时仅做平移偏移（offsetX, offsetY）。
     */
    private static final class Emitter
    {
        private final List<P> particles = new ArrayList<>();
        private long lastEmitTick = Long.MIN_VALUE;

        Emitter()
        {
        }

        /**
         * 每 tick 生成若干粒子；寿命由命中边界的最小正时间决定
         */
        void tickAndSpawn(Level level, long gameTime)
        {
            // 清理超寿命
            particles.removeIf(p -> ((float) gameTime - p.birthTick) > MAX_TICKS_LIFE);

            // 每 tick 只发一次
            if (lastEmitTick == gameTime) return;
            lastEmitTick = gameTime;

            final int count = Mth.nextInt(level.random, EMIT_MIN, EMIT_MAX);

            // 规范槽位中心（不含偏移）
            final float centerX = SLOT_SIZE / 2f;
            final float centerY = SLOT_SIZE / 2f;

            for (int i = 0; i < count; i++)
            {
                // 随机方向单位向量（均匀 0..2π）
                final float angle = level.random.nextFloat() * (float) (Math.PI * 2.0);
                float dirX = Mth.cos(angle);
                float dirY = Mth.sin(angle);
                final float len = Mth.sqrt(dirX * dirX + dirY * dirY);
                if (len != 0)
                {
                    dirX /= len;
                    dirY /= len;
                }

                // 粒子屏幕尺寸（整图缩放）
                final int screenSize = Mth.clamp(
                        BASE_SCREEN_SIZE + Mth.nextInt(level.random, -SIZE_JITTER_PX, SIZE_JITTER_PX),
                        1, SLOT_SIZE
                );

                // 出生位置（以左上角定位：中心减半尺寸）
                final float xStart = centerX - screenSize / 2f;
                final float yStart = centerY - screenSize / 2f;

                // 速度（px/tick）
                final float speed = SPEED_PX_PER_TICK * (0.9f + 0.2f * level.random.nextFloat()); // ±10%
                final float vx = dirX * speed;
                final float vy = dirY * speed;

                // 允许运动区域（规范槽位）
                final float left = 0f;
                final float top = 0f;
                final float right = SLOT_SIZE - screenSize;
                final float bottom = SLOT_SIZE - screenSize;

                // 命中边的时间 t（沿轨迹的真实时间）
                float timeX = Float.POSITIVE_INFINITY;
                float timeY = Float.POSITIVE_INFINITY;
                if (vx > 0f) timeX = (right - xStart) / vx;
                else if (vx < 0f) timeX = (left - xStart) / vx;
                if (vy > 0f) timeY = (bottom - yStart) / vy;
                else if (vy < 0f) timeY = (top - yStart) / vy;

                float timeToEdge = Float.POSITIVE_INFINITY;
                if (timeX > 0f && Float.isFinite(timeX)) timeToEdge = Math.min(timeToEdge, timeX);
                if (timeY > 0f && Float.isFinite(timeY)) timeToEdge = Math.min(timeToEdge, timeY);
                if (!Float.isFinite(timeToEdge)) timeToEdge = 1.0f; // 守备

                final float life = Math.min(timeToEdge, MAX_TICKS_LIFE);

                // 轻微抖动
                final float spawnPhase = level.random.nextFloat() * 0.1f;

                particles.add(new P(xStart, yStart, vx, vy, gameTime - spawnPhase, life, screenSize));
            }
        }

        /**
         * 绘制到指定槽位：把规范槽位坐标整体加上 (offsetX, offsetY) 偏移
         */
        void drawAt(GuiGraphics gg, float nowT, int offsetX, int offsetY)
        {
            final int texW = 32, texH = 32; // 源纹理大小（像素）

            for (int i = particles.size() - 1; i >= 0; --i)
            {
                final P p = particles.get(i);

                float age = nowT - p.birthTick; // tick
                if (age < 0) age = 0;
                if (age > p.life)
                {
                    particles.remove(i);
                    continue;
                }

                float posX = p.x0 + p.vx * age;
                float posY = p.y0 + p.vy * age;

                // 保险夹取（规范槽位范围）
                final float left = 0f;
                final float top = 0f;
                final float right = SLOT_SIZE - p.screenSize;
                final float bottom = SLOT_SIZE - p.screenSize;
                posX = Mth.clamp(posX, left, right);
                posY = Mth.clamp(posY, top, bottom);

                // 透明度：快速淡入 + 线性淡出
                float t = age / p.life; // 0..1
                float alpha = (t < 0.15f)
                        ? START_ALPHA * (t / 0.15f)
                        : START_ALPHA * (1.0f - (t - 0.15f) / 0.85f);
                alpha = Mth.clamp(alpha, 0f, 1f);

                RenderSystem.setShaderTexture(0, AIR_PARTICLE_TEX);
                RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

                // 这里把规范槽位坐标平移到 (offsetX, offsetY)
                gg.blit(
                        AIR_PARTICLE_TEX,
                        (int) (offsetX + posX), (int) (offsetY + posY),
                        p.screenSize, p.screenSize,
                        0f, 0f,
                        texW, texH,
                        texW, texH
                );
            }

            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    /**
     * 单个粒子（左上角定位；整图缩放；坐标在规范槽位内）
     *
     * @param y0         出生位置（左上角）
     * @param vy         速度（px/tick）
     * @param birthTick  出生时间（tick）
     * @param life       寿命（tick）
     * @param screenSize 屏幕绘制尺寸（把 32×32 等比缩放到此像素）
     */
    private record P(float x0, float y0, float vx, float vy, float birthTick, float life, int screenSize)
    {
    }
}