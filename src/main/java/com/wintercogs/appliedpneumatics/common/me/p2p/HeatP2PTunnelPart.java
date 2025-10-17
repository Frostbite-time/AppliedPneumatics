package com.wintercogs.appliedpneumatics.common.me.p2p;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AEConfig;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import com.wintercogs.appliedpneumatics.AppliedPneumatics;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.heat.HeatBehaviour;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.api.heat.TemperatureListener;
import me.desht.pneumaticcraft.common.config.ConfigHelper;
import me.desht.pneumaticcraft.common.heat.HeatExchangerLogicAmbient;
import me.desht.pneumaticcraft.common.heat.HeatExchangerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * 热量 P2P（按“空气 P2P”的思路）：
 * - 外界在输入端向 addHeat(q) 时，输入端把 q 按“温差x热容”权重分发到每个输出端的相邻热接口；
 * - 输入端对外暴露的温度/容量为“输出端邻居的聚合视图”（温度取热容加权均值，容量取总和）；
 * - 输出端仅作为“可连接点”，不接受被动换热（容量返回极小值以阻断 HeatExchangerLogicTicking 的 exchange）。
 */
// XD，又实现了2个在注释里面被标记不要实现的接口
public class HeatP2PTunnelPart extends P2PTunnelPart<HeatP2PTunnelPart>
{

    private static final P2PModels MODELS =
            new P2PModels(ResourceLocation.tryBuild(AppliedPneumatics.MODID, "part/p2p/p2p_tunnel_heat"));

    @PartModels
    public static List<IPartModel> getModels() { return MODELS.getModels(); }

    @Override
    public IPartModel getStaticModels() { return MODELS.getModel(this.isPowered(), this.isActive()); }

    private static final Capability<IHeatExchangerLogic> HEAT_CAP = PNCCapabilities.HEAT_EXCHANGER_CAPABILITY;

    /** 端口自己的能力 */
    LazyOptional<IHeatExchangerLogic> opt = LazyOptional.empty();

    /** 输入端面对的邻居的能力缓存 */
    private @NotNull LazyOptional<IHeatExchangerLogic> inputAdjacentCache = LazyOptional.empty();

    // 递归保护
    private int reentryDepth = 0;

    // 暴露给外界的 handler（输入 / 输出 各一份稳定对象）
    private final IHeatExchangerLogic inputHandler  = new InputHeatAdapter();
    private final IHeatExchangerLogic outputHandler = new OutputHeatAdapter();

    public HeatP2PTunnelPart(IPartItem<?> partItem)
    {
        super(partItem);
    }

    /** P2P 对外提供的能力实例 */
    public IHeatExchangerLogic getExposedApi()
    {
        return isOutput() ? outputHandler : inputHandler;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capabilityClass)
    {
        if(capabilityClass == HEAT_CAP)
        {
            if(!opt.isPresent())
                opt = LazyOptional.of(this::getExposedApi);
            return opt.cast();
        }
        return super.getCapability(capabilityClass);
    }

    /** 仅“输入端实例”使用：解析输入端所面对邻格的真实热量逻辑，可能为 null。 */
    private @Nullable IHeatExchangerLogic resolveInputAdjacentOrNull()
    {
        if (reentryDepth++ > 0) { reentryDepth--; return null; }
        try {
            BlockEntity be = getBlockEntity();
            Direction face = getSide();
            if (be == null || face == null) return null;
            Level level = be.getLevel();
            if (level == null) return null;

            if (level instanceof ServerLevel sl) {
                if(!inputAdjacentCache.isPresent())
                {
                    BlockPos relativedPos = be.getBlockPos().relative(face);
                    Direction capSide = face.getOpposite();
                    inputAdjacentCache = HeatExchangerManager.getInstance().getLogic(sl, relativedPos, capSide);
                }
                return inputAdjacentCache.resolve().orElse(null);
            } else {
                BlockPos relativedPos = be.getBlockPos().relative(face);
                Direction capSide = face.getOpposite();
                return HeatExchangerManager.getInstance().getLogic(level, relativedPos, capSide).resolve().orElse(null);
            }
        } finally {
            reentryDepth--;
        }
    }

    /** 解析“某个输出端”的相邻真实热量逻辑，可能为 null（输出端不缓存，按需取）。 */
    private static @Nullable IHeatExchangerLogic resolveOutputAdjacentOrNull(HeatP2PTunnelPart out)
    {
        BlockEntity be = out.getBlockEntity();
        Direction face = out.getSide();
        if (be == null || face == null) return null;
        Level level = be.getLevel();
        if (level == null) return null;

        BlockPos relativedPos = be.getBlockPos().relative(face);
        Direction capSide = face.getOpposite();
        return HeatExchangerManager.getInstance().getLogic(level, relativedPos, capSide).resolve().orElse(null);
    }

    // —— 网络/邻居变化：统一失效缓存并让两侧重新拿能力 —— //
    private void resetInputAdjacentCacheAndInvalidate()
    {
        if(opt.isPresent()) opt.invalidate();
        opt = LazyOptional.empty();
        inputAdjacentCache = LazyOptional.empty();
        var be = getBlockEntity();
        if (be != null) be.invalidateCaps();

        if (!isOutput()) {
            for (var out : getOutputs()) {
                var obe = out.getBlockEntity();
                if (obe != null) obe.invalidateCaps();
            }
        } else {
            var in = getInput();
            if (in != null) {
                var ibe = in.getBlockEntity();
                if (ibe != null) ibe.invalidateCaps();
            }
        }
    }

    @Override public void onTunnelNetworkChange() { resetInputAdjacentCacheAndInvalidate(); }
    @Override public void onTunnelConfigChange()  { resetInputAdjacentCacheAndInvalidate(); }
    @Override public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) { resetInputAdjacentCacheAndInvalidate(); }
    @Override public void onUpdateShape(Direction side) { if (side == getSide()) resetInputAdjacentCacheAndInvalidate(); }

    /* 输入适配 - addHeat将温度按权重分配到输出端 */
    /* 对外显示的信息为输入端信息的总和，如果没有输入端，则按空气的热力信息计算 */
    private class InputHeatAdapter implements IHeatExchangerLogic
    {
        private static final double ABSOLUTE_ZERO_K   = 0.0;
        private static final double MAX_BLOCK_TEMP_K  = 2273.0;
        private double ambientTemperature = 300; // 默认环境温度
        private boolean ambientInited = false;

        private double resolveAmbientTemperature()
        {
            if(!ambientInited)
            {
                BlockEntity be = getBlockEntity();
                if(be != null && be.getLevel() != null)
                {
                    ambientTemperature = HeatExchangerLogicAmbient.getAmbientTemperature(be.getLevel(), be.getBlockPos());
                    ambientInited = true;
                }
            }
            return ambientTemperature;
        }



        @Override
        public void addHeat(double heatJ)
        {
            if (heatJ == 0.0) return;

            BlockEntity selfBe = getBlockEntity();
            if (selfBe == null) return;
            Level level = selfBe.getLevel();
            if (level == null || level.isClientSide) return;

            IHeatExchangerLogic source = resolveInputAdjacentOrNull();
            if (source == null) return;

            // 源温度，用于计算温差
            double sourceTempK = source.getTemperature();
            List<HeatP2PTunnelPart> outputs = List.copyOf(getOutputs());
            if (outputs.isEmpty()) return;

            final double flowSign = Math.signum(heatJ); // +1 放热到下游；-1 从下游回收热

            // 可分配目标（sinks）：allocatableEnergyJ = min(需求能量, 物理限制)
            record Sink(IHeatExchangerLogic exchanger, double allocatableEnergyJ) {}
            List<Sink> sinks = new ArrayList<>(outputs.size());
            double totalAllocatableJ = 0.0;

            for (HeatP2PTunnelPart out : outputs) {
                IHeatExchangerLogic neighbor = resolveOutputAdjacentOrNull(out);
                if (neighbor == null) continue;

                // 获取目标热容
                double capacity = Math.max(0.0, neighbor.getThermalCapacity());
                if (capacity <= 0.0) continue;

                double targetTempK = neighbor.getTemperature();

                // 计算需求能量（温差*热容）
                double demandToSourceJ = (flowSign > 0)
                        ? Math.max(0.0, sourceTempK - targetTempK) * capacity
                        : Math.max(0.0, targetTempK - sourceTempK) * capacity;
                if (demandToSourceJ <= 0.0) continue;

                // 不越过温度极限（0..2273K）
                double headroomJ = (flowSign > 0)
                        ? Math.max(0.0, (MAX_BLOCK_TEMP_K - targetTempK) * capacity)
                        : Math.max(0.0, (targetTempK - ABSOLUTE_ZERO_K) * capacity);

                double allocatableJ = Math.min(demandToSourceJ, headroomJ);
                if (allocatableJ <= 0.0) continue;

                sinks.add(new Sink(neighbor, allocatableJ));
                totalAllocatableJ += allocatableJ;
            }

            if (totalAllocatableJ <= 0.0) return;

            // 计算分配系数，不分配超出需求的热量 fraction (0,1]
            double fraction = Math.min(1.0, Math.abs(heatJ) / totalAllocatableJ);

            for (Sink s : sinks) {
                double assignedJ = flowSign * fraction * s.allocatableEnergyJ();
                if (assignedJ != 0.0) s.exchanger().addHeat(assignedJ);
            }

            // 为AE进行能量计费
            double deliveredAbsJ = fraction * totalAllocatableJ;
            double costPerOp = AEConfig.instance().getP2PTunnelTransportTax();
            if (costPerOp > 0.0 && deliveredAbsJ > 0.0) {
                double operations = deliveredAbsJ / 1000; // 每 1000 单位算 1 次操作
                double tax = operations * costPerOp;
                if (tax > 0.0) {
                    HeatP2PTunnelPart.this.getMainNode().ifPresent(grid ->
                            grid.getEnergyService().extractAEPower(tax, Actionable.MODULATE, PowerMultiplier.CONFIG)
                    );
                }
            }
        }

        // 对外视图聚合输出端，当没有输出端时返回环境温度和阻值，模拟逸散
        // 另外，返回的温度为热容加权平均数
        @Override
        public double getTemperature()
        {
            final var outs = List.copyOf(getOutputs());
            double sumW = 0.0;
            double sumTW = 0.0;

            for (HeatP2PTunnelPart out : outs) {
                IHeatExchangerLogic h = resolveOutputAdjacentOrNull(out);
                if (h == null) continue;
                double c = Math.max(0.0, h.getThermalCapacity());
                if (c <= 0.0) continue; // 0 容量不参与加权
                sumW  += c;
                sumTW += h.getTemperature() * c;
            }

            if (sumW > 0.0) return sumTW / sumW;

            // 若全为 0 容量或拿不到容量
            double sumT = 0.0; int n = 0;
            for (HeatP2PTunnelPart out : outs) {
                IHeatExchangerLogic h = resolveOutputAdjacentOrNull(out);
                if (h != null) { sumT += h.getTemperature(); n++; }
            }
            return n > 0 ? (sumT / n) : getAmbientTemperature();
        }

        @Override public int getTemperatureAsInt() { return (int) Math.round(getTemperature()); }

        // 取输出端邻居环境温度的均值；没有就用默认环境温度
        @Override
        public double getAmbientTemperature()
        {
            List<HeatP2PTunnelPart> outs = List.copyOf(getOutputs());
            double sum = 0.0; int n = 0;
            for (HeatP2PTunnelPart out : outs) {
                IHeatExchangerLogic h = resolveOutputAdjacentOrNull(out);
                if (h != null) { sum += h.getAmbientTemperature(); n++; }
            }
            return n == 0 ? resolveAmbientTemperature() : (sum / n);
        }

        /* 阻值应当为输出端平均值，没有输出端则取环境阻值 */
        @Override
        public double getThermalResistance()
        {
            List<HeatP2PTunnelPart> outs = List.copyOf(getOutputs());
            double sum = 0.0; int n = 0;
            for (HeatP2PTunnelPart out : outs) {
                IHeatExchangerLogic h = resolveOutputAdjacentOrNull(out);
                if (h != null) { sum += h.getThermalResistance(); n++; }
            }
            return n == 0 ? ConfigHelper.common().heat.airThermalResistance.get() : (sum / n);
        }

        /* 有输出端时返回所有输出端邻居热容总和 */
        @Override
        public double getThermalCapacity()
        {
            List<HeatP2PTunnelPart> outs = List.copyOf(getOutputs());
            double total = 0.0;
            for (HeatP2PTunnelPart out : outs) {
                IHeatExchangerLogic h = resolveOutputAdjacentOrNull(out);
                if (h != null) total += Math.max(0.0, h.getThermalCapacity());
            }
            return Math.max(0, total); // 无输出时返回0，让上游（气动的默认实现）exchange函数早退，避免额外性能消耗
        }

        @Override public void setTemperature(double temperature) {}
        @Override public void setThermalResistance(double r) {}
        @Override public void setThermalCapacity(double c) { /* no-op */ }
        // 其余接口：输入端不作为 hull，不主动连接扫描；只在朝向这一侧连通
        @Override public void tick() { }
        @Override public void initializeAsHull(Level level, BlockPos pos, BiPredicate<LevelAccessor, BlockPos> f, Direction... sides) { }
        @Override public void initializeAmbientTemperature(Level level, BlockPos pos) { }
        @Override public boolean isSideConnected(Direction side) { return side == getSide(); }

        // 温度监听/序列化保持空实现
        @Override public void addTemperatureListener(TemperatureListener l) { }
        @Override public void removeTemperatureListener(TemperatureListener l) { }
        @Override public CompoundTag serializeNBT() { return new CompoundTag(); }
        @Override public void deserializeNBT(CompoundTag nbt) { }

        @Override
        public <T extends HeatBehaviour> Optional<T> getHeatBehaviour(BlockPos pos, Class<T> cls) {
            return Optional.empty();
        }
    }

    // 作为一个空容器，仅用于为需要显示连接状态的情况提供一个可获取的能力
    private class OutputHeatAdapter implements IHeatExchangerLogic
    {

        private double ambientTemperature = 300; // 默认环境温度
        private boolean ambientInited = false;

        private double resolveAmbientTemperature()
        {
            if(!ambientInited)
            {
                BlockEntity be = getBlockEntity();
                if(be != null && be.getLevel() != null)
                {
                    ambientTemperature = HeatExchangerLogicAmbient.getAmbientTemperature(be.getLevel(), be.getBlockPos());
                    ambientInited = true;
                }
            }
            return ambientTemperature;
        }


        // 输出端不接受被动换热；容量返回极小值使得对方 exchange() 早退
        @Override public double getThermalCapacity() { return 0.0; } // < 0.1 即可触发对方早退

        // 实际推送已经由输入端统一处理
        @Override public void addHeat(double amount) {}
        // 返回环境温度
        @Override public double getTemperature() { return getAmbientTemperature(); }
        @Override public int getTemperatureAsInt() { return (int) Math.round(getTemperature()); }
        @Override public double getAmbientTemperature() { return resolveAmbientTemperature(); }
        @Override public void setTemperature(double temperature) {}
        @Override public void setThermalResistance(double r) {}
        // 返回环境阻值
        @Override public double getThermalResistance() { return ConfigHelper.common().heat.airThermalResistance.get(); }
        @Override public void setThermalCapacity(double c) {}
        @Override public void tick() {}
        @Override public void initializeAsHull(Level level, BlockPos pos, BiPredicate<LevelAccessor, BlockPos> f, Direction... sides) {}
        @Override public void initializeAmbientTemperature(Level level, BlockPos pos) {}
        @Override public boolean isSideConnected(Direction side) { return side == getSide(); }
        @Override public void addTemperatureListener(TemperatureListener l) {}
        @Override public void removeTemperatureListener(TemperatureListener l) {}
        @Override public CompoundTag serializeNBT() { return new CompoundTag(); }
        @Override public void deserializeNBT(CompoundTag nbt) {}

        @Override
        public <T extends HeatBehaviour> Optional<T> getHeatBehaviour(BlockPos pos, Class<T> cls) {
            return Optional.empty();
        }
    }
}