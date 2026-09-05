package com.wintercogs.appliedpneumatics.common.eventlistner;

import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.init.APItems;
import com.wintercogs.appliedpneumatics.common.items.IAirStorageCell;
import me.desht.pneumaticcraft.common.particle.AirParticleData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当我们需要模拟驱动器的气压爆炸时，通过此类来延迟一个tick爆炸
 */
@Mod.EventBusSubscriber(modid = AppliedPneumatics.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class APDelayedBreaker
{
    private static final Set<BlockKey> QUEUE = ConcurrentHashMap.newKeySet();

    /**
     * 去重入队：同一 tick 内同一方块只炸一次
     */
    public static void breakNextTick(ResourceKey<Level> dim, BlockPos pos)
    {
        QUEUE.add(new BlockKey(dim, pos.immutable()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e)
    {
        if (QUEUE.isEmpty()) return;

        MinecraftServer server = e.getServer();

        var todo = new ArrayList<>(QUEUE);
        QUEUE.clear();

        for (var key : todo)
        {
            var level = server.getLevel(key.dim());
            if (level == null || !level.hasChunkAt(key.pos())) continue;
            // 破坏方块
            ejectOrBreak(level, key.pos);
        }
    }

    /**
     * 对指定位置执行“弹出或破坏”
     * 如果能取出任一一个不合要求的元件就弹出
     * 否则就破坏方块
     */
    public static void ejectOrBreak(Level level, BlockPos pos)
    {
        if (!(level instanceof ServerLevel server))
        {
            return; // 只在服务端跑
        }
        boolean didAnything = false;
        // 走能力系统尝试找元件
        BlockEntity blockEntity = server.getBlockEntity(pos);
        IItemHandler itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        if (itemHandler != null)
        {
            for (int i = 0; i < itemHandler.getSlots(); i++)
            {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() instanceof IAirStorageCell cell)
                {
                    // 剩余容量小于等于0，且没有安装安全卡或者真空卡
                    CompoundTag tag = stack.getOrCreateTag(); // 已经插入的必然有tag，所以我们不做判断，直接getOrCreateTag没有问题
                    long airStored = tag.getLong(IAirStorageCell.AIR_STORED_TAG);
                    if (IAirStorageCell.remainingAmount(cell.getTotalBytes(), airStored) <= 0
                            && !cell.getUpgrades(stack).isInstalled(APItems.SECURITY_CARD.get()) && !cell.getUpgrades(stack).isInstalled(APItems.VACUUM_CARD.get()))
                    {
                        ItemStack extracted = itemHandler.extractItem(i, 1, false);
                        if (!extracted.isEmpty())
                        {
                            // 掉落物生成
                            ItemEntity drop = new ItemEntity(level,
                                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                                    extracted.copy());
                            drop.setDefaultPickUpDelay();
                            level.addFreshEntity(drop);

                            spawnLeakBurst(server, pos);

                            // 打上标记
                            didAnything = true;
                        }
                    }
                }
            }
        }
        // 如果没有成功取出任何元件，则破坏当前方块
        if (!didAnything)
        {
            level.destroyBlock(pos, true);
            spawnLeakBurst(server, pos);
        }
    }

    /**
     * 统一的“漏气”效果：服务端广播到附近玩家
     */
    private static void spawnLeakBurst(ServerLevel server, BlockPos pos)
    {
        // 粒子中心与参数
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.1; // 稍微偏上，避免被遮挡
        double cz = pos.getZ() + 0.5;

        // count/offset/speed 调大一点，保证能看见
        int count = 16; // 粒子数量
        double spread = 0.2; // XYZ 方向散射
        double speed = 0.25; // 粒子初速度标量

        server.sendParticles(AirParticleData.DENSE, cx, cy, cz, count, spread, spread, spread, speed);

        // 给一个火被浇灭的音效，我感觉这个比气动的泄漏音效更适配
        server.playSound(
                null, pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                1.0f, // 音量
                1.0f // 音高
        );

    }

    public record BlockKey(ResourceKey<Level> dim, BlockPos pos)
    {
    }
}
