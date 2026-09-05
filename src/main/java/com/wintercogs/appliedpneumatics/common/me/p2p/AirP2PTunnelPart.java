package com.wintercogs.appliedpneumatics.common.me.p2p;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import com.wintercogs.appliedpneumatics.common.me.keys.types.AirKeyType;
import it.unimi.dsi.fastutil.floats.FloatPredicate;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * 空气 P2P：
 * - 外界在输入端向 addAir(amount) 时，输入端把 amount 按“压强差”权重分发到每个输出端的相邻接口；
 * - 输入端对外暴露的气体、压强、容量为“输出端邻居的聚合视图”（压强取均值，气体量和容量取总和）；
 * - 输出端仅作为“可连接点”，返回极高压强，防止外界主动对内输入。
 */
// 不仅实现了IAirHandlerMachine，还实现了两个，XD
public class AirP2PTunnelPart extends P2PTunnelPart<AirP2PTunnelPart>
{
    private static final P2PModels MODELS =
            new P2PModels(ResourceLocation.tryBuild(AppliedPneumatics.MODID, "part/p2p/p2p_tunnel_air"));


    @PartModels
    public static List<IPartModel> getModels()
    {
        return MODELS.getModels();
    }

    @Override
    public IPartModel getStaticModels()
    {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private static final Capability<IAirHandlerMachine> AIR_CAP =
            PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY;

    /**
     * 端口自己的能力
     */
    LazyOptional<IAirHandlerMachine> opt = LazyOptional.empty();

    /**
     * 端口所对的方块的能力缓存
     */
    private @NotNull LazyOptional<IAirHandlerMachine> inputAdjacentCache = LazyOptional.empty();

    // —— 递归保护，避免极端情况下的重入查询 ——
    private int reentryDepth = 0;

    // —— 对外暴露的两个 Handler（稳定对象；输入端/输出端各一个） ——
    private final IAirHandlerMachine inputHandler = new InputHandler();
    private final IAirHandlerMachine outputHandler = new OutputHandler();

    public AirP2PTunnelPart(IPartItem<?> partItem)
    {
        super(partItem);
    }

    // 提供Part能力注册
    public IAirHandlerMachine getExposedApi()
    {
        return isOutput() ? outputHandler : inputHandler;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capabilityClass)
    {
        if (capabilityClass == AIR_CAP)
        {
            if (!opt.isPresent())
                opt = LazyOptional.of(this::getExposedApi);
            return opt.cast();
        }
        return super.getCapability(capabilityClass);
    }

    /**
     * 仅在“输入端实例”上调用：解析输入端所面对邻格的真实 IAirHandlerMachine，可能为 null。
     */
    private @Nullable IAirHandlerMachine resolveInputAdjacentOrNull()
    {
        if (reentryDepth++ > 0)
        {
            reentryDepth--;
            return null;
        }
        try
        {
            BlockEntity be = getBlockEntity();
            Direction side = getSide();
            if (be == null || side == null) return null;
            Level level = be.getLevel();
            if (level == null) return null;

            if (level instanceof ServerLevel sl)
            {
                if (!inputAdjacentCache.isPresent())
                {
                    BlockPos relativedPos = be.getBlockPos().relative(side);
                    Direction face = side.getOpposite();
                    BlockEntity blockEntity = level.getBlockEntity(relativedPos);
                    if (blockEntity == null) return null;

                    inputAdjacentCache = blockEntity.getCapability(AIR_CAP, face);
                }
                return inputAdjacentCache.resolve().orElse(null);
            }
            else
            {
                BlockEntity nbe = level.getBlockEntity(be.getBlockPos().relative(side));
                if (nbe == null) return null;
                return nbe.getCapability(AIR_CAP, side.getOpposite()).resolve().orElse(null);
            }
        }
        finally
        {
            reentryDepth--;
        }
    }

    /**
     * 解析“某个输出端”的相邻真实 IAirHandlerMachine，可能为 null。
     */
    private static @Nullable IAirHandlerMachine resolveOutputAdjacentOrNull(AirP2PTunnelPart out)
    {
        var be = out.getBlockEntity();
        var face = out.getSide();
        if (be == null || face == null) return null;
        var level = be.getLevel();
        if (level == null) return null;

        BlockPos relativedPos = be.getBlockPos().relative(face);
        Direction capSide = face.getOpposite();
        BlockEntity blockEntity = level.getBlockEntity(relativedPos);
        if (blockEntity == null) return null;
        return blockEntity.getCapability(AIR_CAP, capSide).resolve().orElse(null);
    }


    /**
     * 输入端相邻能力失效 / 网络或邻居变化 → 失效并清缓存，让外界重新拿实例
     */
    private void resetInputAdjacentCacheAndInvalidate()
    {
        if (opt.isPresent()) opt.invalidate();
        opt = LazyOptional.empty();
        inputAdjacentCache = LazyOptional.empty(); // 虽然我感觉可能不需要清理缓存，但是我打算在此保留与1.21.1最大相似度
        // 自己
        getBlockEntity().invalidateCaps();
        // 输入端 → 让所有输出端一起失效；输出端 → 让输入端失效
        if (!isOutput())
        {
            for (var out : getOutputs()) out.getBlockEntity().invalidateCaps();
        }
        else
        {
            var in = getInput();
            if (in != null) in.getBlockEntity().invalidateCaps();
        }
    }

    @Override
    public void onTunnelNetworkChange()
    {
        resetInputAdjacentCacheAndInvalidate();
    }

    @Override
    public void onTunnelConfigChange()
    {
        resetInputAdjacentCacheAndInvalidate();
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor)
    {
        resetInputAdjacentCacheAndInvalidate();
    }

    @Override
    public void onUpdateShape(Direction side)
    {
        if (side == getSide()) resetInputAdjacentCacheAndInvalidate();
    }

    // 输入端在接收到气体时将其按权重推送到每个输出端
    // 权重即为（连接到输入端的气压 - 连接到对应输出端的气压）
    // 输入端本体对外暴露气压为输出端的均值
    // 输入端本体对外暴露的容量和当前气体均为输入端总和
    private class InputHandler implements IAirHandlerMachine
    {
        /**
         * 将 ml 空气按“(我方压-对方压)”的正差作为权重分配到各输出端相邻 handler
         */
        @Override
        public void addAir(int inputAir)
        {
            // 不允许反向抽取空气
            if (inputAir <= 0) return;

            // 我方压力：输入端相邻真实 handler 的压力（用于权重）
            IAirHandlerMachine src = resolveInputAdjacentOrNull();
            if (src == null) return;
            float myP = src.getPressure();

            List<AirP2PTunnelPart> outs = getOutputs();
            if (outs.isEmpty()) return;

            // 收集目标与权重
            record Target(IAirHandlerMachine handler, float weight)
            {
            }
            List<Target> targets = new ArrayList<>(outs.size());
            float sumW = 0f;

            for (AirP2PTunnelPart out : outs)
            {
                IAirHandlerMachine outHandler = resolveOutputAdjacentOrNull(out);
                if (outHandler == null) continue;
                float outWeight = Math.max(0f, myP - outHandler.getPressure()); // 我方越高、对方越低 → 权重越大
                if (outWeight > 0f)
                { // 我方压力必须大于对方才被允许输出
                    targets.add(new Target(outHandler, outWeight));
                    sumW += outWeight;
                }
            }

            if (targets.isEmpty() || sumW <= 0f) return;

            // 按权重分配；注意保留余数，最后一轮吃掉
            int delivered = 0;
            int remain = inputAir;
            for (int i = 0; i < targets.size(); i++)
            {
                Target target = targets.get(i);
                int toSend = (i == targets.size() - 1) ? remain
                        : Math.max(0, Math.round(inputAir * (target.weight / sumW)));
                if (toSend == 0) continue;

                target.handler.addAir(toSend);
                delivered += toSend;
                remain -= toSend;
                if (remain <= 0) break;
            }

            if (delivered > 0)
            {
                // 计算耗能
                deductTransportCost(delivered, AirKeyType.INSTANCE);
            }
        }

        @Override
        public int getBaseVolume()
        {
            return 0;
        }

        @Override
        public void setBaseVolume(int i)
        {
        }

        // 没有任何邻居时返回50气压，防止外界对其输入
        @Override
        public float getPressure()
        {
            float sum = 0f;
            int n = 0;

            for (var out : getOutputs())
            {
                var h = resolveOutputAdjacentOrNull(out);
                if (h != null)
                {
                    sum += h.getPressure();
                    n++;
                }
            }

            return n == 0 ? 50f : (sum / n);
        }

        @Override
        public int getAir()
        {
            int total = 0;
            for (var out : getOutputs())
            {
                var h = resolveOutputAdjacentOrNull(out);
                if (h != null) total += h.getAir();
            }
            return total;
        }

        @Override
        public int getVolume()
        {
            int total = 0;
            for (var out : getOutputs())
            {
                var h = resolveOutputAdjacentOrNull(out);
                if (h != null) total += h.getVolume();
            }
            return total;
        }

        @Override
        public float maxPressure()
        {
            return 0;
        }

        @Override
        public float getDangerPressure()
        {
            return 0f;
        }

        @Override
        public float getCriticalPressure()
        {
            return 0f;
        }

        @Override
        public void setPressure(float p)
        { /* no-op */ }

        @Override
        public void setVolumeUpgrades(int v)
        { /* no-op */ }

        @Override
        public void enableSafetyVenting(FloatPredicate c, Direction d)
        { /* no-op */ }

        @Override
        public void disableSafetyVenting()
        { /* no-op */ }

        @Override
        public void tick(BlockEntity ownerTE)
        { /* no-op */ }

        @Override
        public void setSideLeaking(@Nullable Direction dir)
        { /* no-op */ }

        @Override
        public @Nullable Direction getSideLeaking()
        {
            return null;
        }

        @Override
        public List<IAirHandlerMachine.Connection> getConnectedAirHandlers(BlockEntity ownerTE)
        {
            return List.of();
        }

        @Override
        public void setConnectedFaces(List<Direction> list)
        {
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
        }

        @Override
        public void printManometerMessage(Player p, List<Component> curInfo)
        {
            curInfo.add(Component.translatable("appliedpneumatics.cur.tooltip.p2p_input", String.format(Locale.ROOT, "%.2f", getPressure())));
        }
    }

    // 输出端handler仅让管道或者其他有连接态的系统可以获取到一个能力，并允许对方进行连接
    // 自身不处理任何操作、返回极大气压确保外界不会对此处推送气体
    private static class OutputHandler implements IAirHandlerMachine
    {
        // 输出端仅展示能力用于连接，返回50气压，防止邻居推气体
        @Override
        public float getPressure()
        {
            return 50f; // 绝对够了，创造压缩机都只能25压强
        }

        // 实际气体注入已经在输入端解决
        @Override
        public void addAir(int ml)
        {
        }

        @Override
        public int getBaseVolume()
        {
            return 1;
        }

        @Override
        public void setBaseVolume(int i)
        {
        }

        @Override
        public int getAir()
        {
            return 1;
        }

        @Override
        public int getVolume()
        {
            return 1;
        }

        @Override
        public float maxPressure()
        {
            return 0;
        }

        @Override
        public float getDangerPressure()
        {
            return Float.MAX_VALUE;
        }

        @Override
        public float getCriticalPressure()
        {
            return Float.MAX_VALUE;
        }

        @Override
        public void setPressure(float p)
        { /* no-op */ }

        @Override
        public void setVolumeUpgrades(int v)
        { /* no-op */ }

        @Override
        public void enableSafetyVenting(FloatPredicate c, Direction d)
        { /* no-op */ }

        @Override
        public void disableSafetyVenting()
        { /* no-op */ }

        @Override
        public void tick(BlockEntity ownerTE)
        { /* no-op */ }

        @Override
        public void setSideLeaking(@Nullable Direction dir)
        { /* no-op */ }

        @Override
        public @Nullable Direction getSideLeaking()
        {
            return null;
        }

        @Override
        public List<IAirHandlerMachine.Connection> getConnectedAirHandlers(BlockEntity ownerTE)
        {
            return List.of();
        }

        @Override
        public void setConnectedFaces(List<Direction> list)
        {
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
        }

        @Override
        public void printManometerMessage(Player p, List<Component> curInfo)
        {
            curInfo.add(Component.translatable("appliedpneumatics.cur.tooltip.p2p_output", String.format(Locale.ROOT, "%.2f", getPressure())));
        }
    }
}
